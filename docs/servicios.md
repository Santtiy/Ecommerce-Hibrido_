# Servicios, mensajería y datos

Diagrama de conjunto: [arquitectura](diagramas/arquitectura.md). Paquetes de cada servicio: [README](../README.md#estructura-de-cada-servicio).

## Servicios

| Servicio | Responsabilidad | Consume | Produce | Puerto host |
|---|---|---|---|---|
| `postgres-db` | Persistencia | — | — | — |
| `kafka` | Bitácora de eventos (KRaft) | — | — | — |
| `rabbitmq` | Reparto de comandos | — | — | `127.0.0.1:15672` |
| `order-api` | Registrar órdenes; auditar y aplicar resultados | `billing_events`, `inventory_events`, `notification_events` | `new_orders` (vía bandeja de salida) | `8080` |
| `order-processor` | Evento → comando | `new_orders` | `order_tasks` | — |
| `billing-worker` | Pago (RN-11) | `billing_queue` | `billing_events` | — |
| `inventory-worker` | Stock de todos los ítems (RN-12) | `inventory_queue` | `inventory_events` | — |
| `notification-worker` | Correo | `notification_queue` | `notification_events` | — |
| `analytics-service` | Ventas acumuladas + Sistemas Operativos | `new_orders` | — | — |
| `dashboard-aggregator` | `{payment, stock, email}` por WebSocket | los 3 `*_events` | `/ws/ordenes` | `8081` |

## Kafka

| Topic | Particiones | Clave | Grupos que lo leen |
|---|---|---|---|
| `new_orders` | 3 | `orderId` | `analytics-group`, `processor-group` |
| `billing_events` | 1 | `orderId` | `dashboard-group`, `order-api-group` |
| `inventory_events` | 1 | `orderId` | `dashboard-group`, `order-api-group` |
| `notification_events` | 1 | `orderId` | `dashboard-group`, `order-api-group` |

Reintentos de los consumidores: `DefaultErrorHandler` con 3 intentos y 1 s entre ellos (`ConfiguracionKafkaBase`, en
`common-mensajeria`). `analytics-group` y `dashboard-group` releen sus topics desde el inicio al arrancar
(`ConsumidorQueReleeTodo`) para reconstruir su estado en memoria. Los nombres están en `common/nombres/`
(`Topicos`, `GruposConsumidores`); la cola y el topic de cada worker, en el enum `TipoTarea`.
`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`: cada topic lo crea su productor con el número de particiones correcto.

## RabbitMQ

| Elemento | Tipo | Notas |
|---|---|---|
| `order_tasks` | fanout | Copia cada comando a las 3 colas |
| `billing_queue`, `inventory_queue`, `notification_queue` | colas durables | `x-dead-letter-exchange = order_tasks_dlx` |
| `order_tasks_dlx` | fanout | Recibe lo que falló 3 veces |
| `failed_tasks` | cola durable | Mensajes fallidos para revisar |

Workers: `prefetch: 1`, `concurrency: 2`, 3 intentos con 1 s de espera, `default-requeue-rejected: false`
(en `common-mensajeria/src/main/resources/worker-comun.yml`, compartido por los tres). Cada worker solo aporta su
regla (`ReglaTarea`), su cola y su topic; el procesamiento común lo hace `EjecutorTarea`.

## PostgreSQL (`backend/order-api/src/main/resources/schema.sql`)

Diagrama: [entidad-relación](diagramas/entidad-relacion.md).

| Tabla | Para qué |
|---|---|
| `ordenes` | La orden: estado (`CREADA`, `PAGADA`, `CANCELADA`), total, fecha de creación |
| `items_orden` | Sus ítems (`CHECK cantidad > 0`, `CHECK precio_monto > 0`) |
| `bandeja_salida` | Eventos guardados con la orden y aún no publicados |
| `auditoria_ordenes` | Un resultado por orden, worker y estado (`UNIQUE`, idempotente); `origen` es el nombre del `TipoTarea` (`PAGO`, `INVENTARIO`, `NOTIFICACION`) |

## Contratos (`common/.../contratos`)

| Record | Dónde viaja | Campos |
|---|---|---|
| `OrdenCreada` | Kafka `new_orders` | `orderId`, `cliente`, `items[]`, `total`, `moneda`, `creadaEn` |
| `ProcesarOrden` | RabbitMQ `order_tasks` | `orderId`, `items[]`, `total`, `solicitadoEn` |
| `ResultadoTarea` | Kafka `*_events` | `orderId`, `status`, `detalle`, `ocurridoEn` |
| `ItemEvento` | dentro de los dos primeros | `sku`, `nombre`, `cantidad`, `precioUnitario` |

`status` es el enum `EstadoTarea`: en el JSON viaja su nombre (`PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `STOCK_RESERVED`,
`STOCK_OUT_OF_STOCK`, `EMAIL_SENT`), y cada valor sabe a qué `TipoTarea` pertenece y si es éxito.
Los workers implementan `ReglaTarea.evaluar(ProcesarOrden, Instant) → ResultadoTarea`.

Por qué los eventos van por Kafka y los comandos por RabbitMQ: [kafka-vs-rabbitmq.md](kafka-vs-rabbitmq.md).
