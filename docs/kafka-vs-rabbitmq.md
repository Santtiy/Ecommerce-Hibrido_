# Kafka frente a RabbitMQ: cuándo un log de eventos y cuándo un broker de tareas

El sistema usa los dos a propósito, cada uno para lo que hace mejor. La regla corta:

- **Kafka guarda hechos** ("la orden se creó", "el pago se aprobó"). Un hecho no se consume ni se borra al leerlo:
  queda en el log y cada interesado lo lee a su ritmo, tantas veces como quiera.
- **RabbitMQ reparte trabajo** ("cobra esta orden", "reserva este stock"). Un comando lo hace **un** trabajador,
  se confirma con un ack y desaparece de la cola.

| Pregunta | Log de eventos (Kafka) | Broker de tareas (RabbitMQ) |
|---|---|---|
| ¿Qué viaja? | Eventos: hechos en pasado, inmutables | Comandos: trabajo pendiente |
| ¿Qué pasa al leer un mensaje? | Nada: el consumidor avanza su *offset* y el mensaje sigue en el log | El consumidor hace ack y el broker lo **borra** |
| ¿Quién lo recibe? | Cada **grupo de consumidores** recibe todos los mensajes; dentro del grupo, las particiones se reparten | Cada **cola** enlazada recibe una copia (fanout); dentro de la cola, cada mensaje va a **un** consumidor (work-queue) |
| ¿Se puede releer? | Sí, mientras dure la retención: se mueve el *offset* (`seekToBeginning`) | No: lo confirmado ya no existe |
| ¿Qué pasa si falla? | El consumidor reintenta y el log no cambia | Reintentos y, al agotarlos, la cola de fallidos (dead-letter) |
| ¿Qué controla el ritmo? | El consumidor: lee cuando puede | El broker: `prefetch` limita cuántos mensajes tiene cada trabajador a la vez |
| ¿Orden garantizado? | Dentro de cada partición (la clave es el `orderId`) | Por cola, aunque con 2 hilos se procesan en paralelo |

## Dónde se ve cada uno en este sistema

### Kafka: `new_orders` y los tres `*_events`

**1. Retención y relectura.** `analytics-service` (total de ventas) y `dashboard-aggregator`
(estado `{payment, stock, email}` de cada orden) guardan su estado **en memoria**. Si se reinician, lo pierden; pero
como Kafka conserva los eventos (retención por defecto: 7 días), al arrancar vuelven al principio de sus topics y
reconstruyen el estado: `analytics-service` relee `new_orders` y `dashboard-aggregator` relee `billing_events`,
`inventory_events` y `notification_events`. Lo hace [`ConsumidorQueReleeTodo`](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria/kafka/ConsumidorQueReleeTodo.java)
con `seekToBeginning` cada vez que reciben sus particiones. Es seguro porque los dos son idempotentes
(`AgregadorVentas` y `TableroOrdenes` ignoran una orden o un resultado repetido). Con RabbitMQ esto sería imposible:
los mensajes ya confirmados no existen.

> **Advertencia:** en `docker-compose.yml` Kafka **no tiene volumen**: el log vive dentro de su contenedor. La
> relectura funciona cuando se reinicia un servicio (o el propio Kafka con `stop`/`start`), pero si se **recrea**
> el contenedor de Kafka (`docker compose down`, `up --force-recreate` o un cambio en su configuración), los topics
> y los offsets se pierden y no queda historial que releer. Ver [despliegue-aws.md §5](despliegue-aws.md#5-kafka-rabbitmq-y-postgresql).

**2. Varios grupos de consumidores sobre el mismo topic.** Un mismo evento lo leen consumidores independientes, sin
que el productor sepa que existen ([`GruposConsumidores.java`](../backend/common/src/main/java/co/edu/proyecto/common/nombres/GruposConsumidores.java)):

| Topic | Grupos | Qué hace cada uno |
|---|---|---|
| `new_orders` | `processor-group` | Traduce el evento a un comando para RabbitMQ (`order-processor`) |
| | `analytics-group` | Suma el total de ventas y escribe la bitácora (`analytics-service`) |
| `billing_events`, `inventory_events`, `notification_events` | `dashboard-group` | Actualiza el tablero y lo envía por WebSocket |
| | `order-api-group` | Registra la auditoría y aplica el pago a la orden (`PAGADA` o `CANCELADA`) |

Agregar un consumidor nuevo (por ejemplo, un servicio de fraude que lea `new_orders`) es crear un grupo más; ni
`order-api` ni los demás grupos cambian. Cada grupo lleva su propio *offset*: si `analytics-service` se cae, los demás
siguen y él retoma donde iba.

**3. Particiones y orden.** `new_orders` tiene 3 particiones (lo crea `order-api`) y la clave de cada mensaje es el
`orderId`: los eventos de una orden van siempre a la misma partición y se leen en orden. `order-processor` usa
`concurrency: 3`, un hilo por partición.

**4. Un log que no se borra al publicar desde la bandeja de salida.** `order-api` guarda la orden y su evento en la misma
transacción de PostgreSQL y [`PublicadorBandejaSalida`](../backend/order-api/src/main/java/co/edu/proyecto/orden/infraestructura/bandejasalida/PublicadorBandejaSalida.java)
lo envía a `new_orders`; solo borra la fila cuando Kafka confirma. Una vez en el log, el hecho queda disponible para
todos los grupos, presentes y futuros.

### RabbitMQ: el fanout `order_tasks` y las colas de trabajo

**1. Fanout.** `order-processor` publica cada comando `ProcesarOrden` **una vez** en el exchange `order_tasks`
([`PublicadorTareas`](../backend/order-processor/src/main/java/co/edu/proyecto/processor/infraestructura/rabbitmq/PublicadorTareas.java)),
y RabbitMQ entrega una copia a cada cola enlazada: `billing_queue`, `inventory_queue` y `notification_queue`. Cada
worker declara y enlaza su propia cola ([`TopologiaWorker`](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria/rabbitmq/TopologiaWorker.java)):
un worker nuevo se suscribe sin cambiar a quien publica.

**2. Work-queue.** Dentro de cada cola, cada comando lo procesa **un solo** hilo: no se cobra dos veces la misma orden.
[`worker-comun.yml`](../backend/common-mensajeria/src/main/resources/worker-comun.yml) fija `prefetch: 1` (un mensaje
a la vez por hilo: el broker no le adelanta trabajo a un hilo ocupado) y `concurrency: 2` (dos hilos por cola).
Si hubiera más carga se agregan réplicas del worker y RabbitMQ reparte entre todas.

**3. Ack después de Kafka.** [`EjecutorTarea`](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria/tareas/EjecutorTarea.java)
aplica la regla, envía el resultado a Kafka y **espera la confirmación** (`.get(10, SECONDS)`). Solo si eso termina sin
excepción el listener vuelve y Spring hace el ack. Si Kafka no confirma, el comando no se pierde: se reintenta. El
precio es que un resultado puede publicarse dos veces (al menos una vez), y por eso la auditoría tiene
`UNIQUE (orden_id, origen, status)` y el tablero es idempotente.

**4. Cola de fallidos.** Cada cola de trabajo es durable y declara `x-dead-letter-exchange = order_tasks_dlx`. Tras
3 intentos (1 s entre ellos) con `default-requeue-rejected: false`, el comando pasa al fanout `order_tasks_dlx` y de
ahí a la cola `failed_tasks`, donde queda para revisarlo sin bloquear a los demás.

## Por qué no usar solo uno

- **Solo Kafka:** se podría hacer que los workers leyeran `new_orders`, pero el reparto de trabajo quedaría atado al
  número de particiones, no habría `prefetch` ni cola de fallidos sin programarlos, y un comando mezclado con los
  hechos del log se "reproduciría" cada vez que alguien relee el topic.
- **Solo RabbitMQ:** analytics y el dashboard no podrían reconstruir su estado al reiniciar, y cada consumidor nuevo de
  "orden creada" necesitaría que alguien le enviara una copia de todo el historial.

Por eso el sistema traduce en un solo punto (`order-processor`, [`TraductorEventoAComando`](../backend/order-processor/src/main/java/co/edu/proyecto/processor/aplicacion/TraductorEventoAComando.java))
el **evento** `OrdenCreada` en el **comando** `ProcesarOrden`, y los resultados vuelven a Kafka como eventos
(`ResultadoTarea`) para que los lea quien los necesite.

Ver también: [secuencia de una orden](diagramas/secuencia-orden.md) y [arquitectura](diagramas/arquitectura.md).
