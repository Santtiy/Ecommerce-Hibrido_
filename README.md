# Ecommerce-Hibrido

Plataforma de órdenes de comercio electrónico con **arquitectura de microservicios orientada a eventos e híbrida**:

- **Kafka** guarda los eventos (lo que ya ocurrió) y es el bus central.
- **RabbitMQ** reparte los comandos de trabajo (lo que hay que hacer) entre los workers con un exchange **fanout**.
- **PostgreSQL** guarda las órdenes, la bandeja de salida y la auditoría.
- Un **dashboard** web muestra en tiempo real el estado de cada orden.

Por qué Kafka para los eventos y RabbitMQ para los comandos: [`docs/kafka-vs-rabbitmq.md`](docs/kafka-vs-rabbitmq.md).

Proyecto integrador (POO, Sistemas Distribuidos y Sistemas Operativos). Cada requisito del enunciado, con el archivo
donde se cumple y cómo comprobarlo, está en [`docs/cumplimiento-enunciado.md`](docs/cumplimiento-enunciado.md);
las correcciones respecto a la versión anterior, en [`docs/correcciones.md`](docs/correcciones.md).

## Arquitectura

```
 Cliente ─POST /order─► order-api ──(una transacción)──► PostgreSQL: ordenes + items_orden + bandeja_salida
                            │
                            └─ PublicadorBandejaSalida (cada 1 s) ──► Kafka: new_orders (3 particiones)
                                                                          │
                       ┌──────────────────────────────────────────────────┤
                       ▼ analytics-group                                  ▼ processor-group
                analytics-service                                  order-processor ── evento → comando
             (ventas + bitácora SO)                                        │
                                                       RabbitMQ: fanout order_tasks
                                              ┌───────────────┼────────────────┐
                                        billing_queue   inventory_queue   notification_queue
                                              ▼               ▼                ▼
                                       billing-worker  inventory-worker  notification-worker
                                              ▼               ▼                ▼
                                       billing_events  inventory_events  notification_events   (Kafka)
                                              └───────┬───────┴────────┬───────┘
                                                      ▼ dashboard-group ▼ order-api-group
                                           dashboard-aggregator      order-api
                                           (estado + WebSocket)      (auditoría + estado de la orden)

 Fallos en RabbitMQ: 3 intentos → order_tasks_dlx → failed_tasks
```

1. `POST /order` valida, calcula descuento e IVA y guarda **en una sola transacción** la orden y su evento
   en la bandeja de salida. Responde **202 Accepted**.
2. `PublicadorBandejaSalida` envía los eventos pendientes a `new_orders` y solo los borra cuando Kafka confirma.
   Si Kafka está caído, la orden se acepta igual y el evento sale cuando Kafka vuelve.
3. `order-processor` traduce cada evento en un comando `ProcesarOrden` y lo publica en el fanout `order_tasks`.
4. Cada worker aplica su regla (pago, stock de **todos** los ítems, correo) y publica su resultado en Kafka.
   El ack a RabbitMQ va después de la confirmación de Kafka.
5. `dashboard-aggregator` combina los tres resultados (`{payment, stock, email}`) y los envía por WebSocket.
6. `order-api` audita cada resultado y aplica el del pago: `PAYMENT_SUCCESS` → `PAGADA`, `PAYMENT_FAILED` → `CANCELADA`.
7. `analytics-service` acumula las ventas y registra cada orden en un archivo (módulo de Sistemas Operativos).

## Estructura del repositorio

| Carpeta | Qué contiene |
|---|---|
| `backend/` | Proyecto Maven (`pom.xml` padre), los 9 módulos Java (`common`, `common-mensajeria` y los 7 servicios) y el `Dockerfile` |
| `frontend/` | Dos páginas web sin build step, que solo usan `POST /order` y `/ws/ordenes`. **Tienda** (`index.html`, `css/tienda.css`, `js/tienda/`): la app con catálogo (búsqueda, categorías, orden, favoritos y página de cada producto en `#producto/SKU`), carrito, confirmación y seguimiento del pedido en vivo, en pasos, más los escenarios de la exposición. Las fotos de producto (`img/productos/`) son reales y con licencia libre de Wikimedia Commons; autor y licencia de cada una en `img/productos/CREDITOS.md` y al pie de la tienda. **Panel de administración** (`admin.html`, `css/admin.css`, `js/admin/`): órdenes en vivo con filtros, recorrido de una orden por la arquitectura y última petición a order-api. Comparten `css/base.css` y los módulos de `js/` (`config`, `api`, `canal`, `calculo`, `estados`, `formato`, `iconos`, `registro` y `peticiones`); `registro` y `peticiones` pasan las órdenes y la última petición de la tienda al panel por `localStorage`. Lo sirve `dashboard-aggregator` en `http://localhost:8081`: Maven lo empaqueta como `classpath:/static` |
| `ops/` | Scripts de arranque/parada (local y AWS) |
| `docs/` | Documentación |

`docker-compose.yml` y `.env` siguen en la raíz; el contexto de build de Docker es la raíz porque necesita `backend/` y `frontend/`.
Tras editar el front, vuelve a empaquetar `dashboard-aggregator` (o reinicia el servicio desde IntelliJ) para ver el cambio.
La organización de los módulos, los límites de tamaño y las pruebas del frontend están en [`docs/frontend.md`](docs/frontend.md).

## Estructura de cada servicio

Cada servicio tiene **solo las capas que necesita**. La regla para encontrar cualquier cosa: lo de negocio en
`dominio/`, los casos de uso en `aplicacion/`, cada tecnología en su subpaquete de `infraestructura/` y la
configuración en `configuracion/`.

| Paquete | Qué contiene |
|---|---|
| `dominio/` | Reglas de negocio en Java puro, sin Spring, Jakarta, JPA ni Kafka. Lo comprueba el `DominioPuroTest` de cada servicio que lo tiene |
| `aplicacion/` | Casos de uso que coordinan puertos, y las interfaces de esos puertos (`puertos/`). Única anotación de Spring permitida: `@Transactional` |
| `infraestructura/` | Adaptadores, cada uno en el subpaquete de su tecnología: `rest/`, `persistencia/`, `kafka/`, `rabbitmq/`, `websocket/`. En order-api, `bandejasalida/` (outbox: JPA + Kafka) va junto |
| `configuracion/` | Solo clases `@Configuration`: `ConfiguracionDominio` (beans de negocio), `ConfiguracionMensajeria` (Kafka/RabbitMQ) y, si hay web, `ConfiguracionWeb` o `ConfiguracionWebSocket` |

El criterio de cada capa está en el `package-info.java` de su paquete. Rutas relativas a
`backend/<módulo>/src/main/java/co/edu/proyecto/`:

**`common/`** (Java puro, sin Spring: lo que viaja por los brokers)

| Paquete | Clases |
|---|---|
| `common/contratos/` | Records `OrdenCreada`, `ProcesarOrden`, `ResultadoTarea`, `ItemEvento`; enum `EstadoTarea` (`PAYMENT_SUCCESS`…); interfaz `ReglaTarea`; `Validaciones` |
| `common/nombres/` | `Topicos`, `Colas`, `Exchanges`, `GruposConsumidores` y el enum `TipoTarea` (cola y topic de cada worker) |

**`common-mensajeria/`** (Spring; Kafka y RabbitMQ son dependencias `<optional>`)

| Paquete | Clases |
|---|---|
| `mensajeria/kafka/` | `ConfiguracionKafkaBase` (3 intentos), `ConsumidorQueReleeTodo` (relee el topic al arrancar) |
| `mensajeria/rabbitmq/` | `ConfiguracionRabbitBase` (JSON + fanout `order_tasks`), `TopologiaWorker` (cola con DLX + `failed_tasks`) |
| `mensajeria/tareas/` | `EjecutorTarea` (pausa, regla, envío a Kafka y ack después) |
| `resources/worker-comun.yml` | `prefetch: 1`, `concurrency: 2`, 3 reintentos y productor Kafka de los tres workers |

**`order-api/`** → `orden/`

| Paquete | Clases |
|---|---|
| `dominio/` | `Orden`, `EstadoOrden`, `Dinero`, `OrdenId`, `ItemOrden`, `OrdenBuilder`; `descuentos/`, `impuestos/` (Strategy), `excepciones/`, `puertos/OrdenRepository` |
| `aplicacion/` | `RegistrarOrdenService`, `RegistrarResultadoService`; `puertos/` (`BandejaSalida`, `AuditoriaRepository`, `RegistroAuditoria`) |
| `infraestructura/rest/` | `OrdenController`, `CrearOrdenRequest`, `ItemRequest`, `RespuestaOrden`, `RespuestaError`, `ManejadorErrores` |
| `infraestructura/persistencia/orden/` | `OrdenEntity`, `ItemOrdenEntity`, `OrdenMapper`, `OrdenRepositoryJpa`, `SpringDataOrdenes` |
| `infraestructura/persistencia/auditoria/` | `AuditoriaEntity`, `AuditoriaRepositoryJpa`, `SpringDataAuditoria` |
| `infraestructura/bandejasalida/` | `BandejaSalidaJpa`, `MensajePendienteEntity`, `SpringDataBandeja`, `PublicadorBandejaSalida` |
| `infraestructura/kafka/` | `ConsumidorResultados` (los tres `*_events`, grupo `order-api-group`) |
| `configuracion/` | `ConfiguracionDominio`, `ConfiguracionMensajeria`, `ConfiguracionWeb` (CORS) |

**`order-processor/`** → `processor/`

| Paquete | Clases |
|---|---|
| `aplicacion/` | `TraductorEventoAComando` (evento `OrdenCreada` → comando `ProcesarOrden`) |
| `infraestructura/kafka/` | `ConsumidorNuevasOrdenes` (`new_orders`, grupo `processor-group`) |
| `infraestructura/rabbitmq/` | `PublicadorTareas` (fanout `order_tasks`) |
| `configuracion/` | `ConfiguracionDominio`, `ConfiguracionMensajeria` |

**`billing-worker/`** → `billing/`, **`inventory-worker/`** → `inventory/`, **`notification-worker/`** → `notification/`

| Paquete | billing | inventory | notification |
|---|---|---|---|
| `dominio/` | `ReglaPago` (RN-11) | `ReglaInventario` (RN-12) | `ReglaNotificacion` |
| `infraestructura/rabbitmq/` | `ConsumidorPagos` | `ConsumidorInventario` | `ConsumidorNotificaciones` |
| `configuracion/` | `ConfiguracionDominio`, `ConfiguracionMensajeria` | igual | igual |

Las tres reglas implementan `ReglaTarea`; cada consumidor solo escucha su cola y delega en `EjecutorTarea`.

**`analytics-service/`** → `analytics/`

| Paquete | Clases |
|---|---|
| `dominio/` | `AgregadorVentas` |
| `sistemasoperativos/` | `Bitacora`, `MonitorMemoria`, `DemostracionMemoria`, `OperacionES` (módulo de Sistemas Operativos, al primer nivel para encontrarlo sin buscar) |
| `infraestructura/kafka/` | `ConsumidorNuevasOrdenes` (`new_orders`, grupo `analytics-group`; extiende `ConsumidorQueReleeTodo`) |
| `configuracion/` | `ConfiguracionDominio`, `ConfiguracionMensajeria` |

**`dashboard-aggregator/`** → `dashboard/`

| Paquete | Clases |
|---|---|
| `dominio/` | `TableroOrdenes` (máx. 1.000 órdenes), `ProgresoOrden` (`{payment, stock, email}`; `"pending"` vive aquí) |
| `infraestructura/kafka/` | `ConsumidorResultados` (los tres `*_events`, grupo `dashboard-group`; extiende `ConsumidorQueReleeTodo`) |
| `infraestructura/websocket/` | `CanalWebSocket` (`/ws/ordenes`) |
| `configuracion/` | `ConfiguracionDominio`, `ConfiguracionMensajeria`, `ConfiguracionWebSocket` |

Diagramas de clases, secuencia, estados, entidad-relación y despliegue: [`docs/diagramas/`](docs/diagramas/README.md).

## Idioma

- **En inglés**, porque los dicta el enunciado: los módulos (`order-api`, `billing-worker`…), los topics
  (`new_orders`, `billing_events`…), el exchange `order_tasks`, las colas (`billing_queue`…), los grupos
  (`processor-group`…), los estados (`PAYMENT_SUCCESS`…, `"pending"`), los campos `{payment, stock, email}` y
  `orderId`/`status` de los mensajes.
- **En español**: el código (clases, métodos, variables, paquetes), los comentarios, los mensajes de error, los
  scripts y la documentación.

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 21 |
| Maven | 3.9.x |
| Docker | Docker Desktop o Docker Engine con Compose v2 |

## Credenciales (`.env`)

```powershell
Copy-Item .env.example .env      # Windows   (Ubuntu: cp .env.example .env && chmod 600 .env)
```

Rellena `POSTGRES_PASSWORD` y `RABBITMQ_PASSWORD` (solo letras, números, `-` y `_`). `APP_ORIGENES` define qué
páginas pueden llamar a la API y abrir el WebSocket (por defecto `http://localhost:8081`).

## Compilar y probar

Desde la raíz del repositorio (el `pom.xml` de la raíz solo agrega `backend/`, donde está el `pom.xml` padre):

```powershell
mvn verify    # compila los 9 módulos y ejecuta las 90 pruebas unitarias
```

Las pruebas no necesitan Docker ni Spring y todas rotulan sus partes con `// Arrange`, `// Act` y `// Assert`:
dominio, servicios y consumidores con Mockito (`@Mock`, `@InjectMocks`, `verify`), el WebSocket con sesiones simuladas
y, en cada servicio con `dominio/`, un `DominioPuroTest` que verifica que no importa ningún framework.
Los datos de prueba (`DatosDePrueba`) y el `VerificadorDominioPuro` están una sola vez en el test-jar de `common`.

## Ejecutar

**Todo en Docker:**
```powershell
docker compose build
docker compose up -d --wait
```

**Desarrollo local en Windows** (infraestructura en Docker, servicios como jar):
```powershell
powershell -ExecutionPolicy Bypass -File ops\arrancar.ps1
powershell -ExecutionPolicy Bypass -File ops\enviar-ordenes.ps1
powershell -ExecutionPolicy Bypass -File ops\detener.ps1 -ConInfra
```

`arrancar.ps1` deja todo lo que genera en `.run/` (ignorada por Git): `pids.txt`, `logs/` y la bitácora de
`analytics-service` en `datos/ordenes.log`.

**AWS EC2:** ver [`docs/despliegue-aws.md`](docs/despliegue-aws.md).

| Qué | URL |
|---|---|
| API | `POST http://localhost:8080/order` |
| Tienda (la app: catálogo, carrito, compra y seguimiento) | `http://localhost:8081` |
| Panel de administración (órdenes en vivo, recorrido y última petición) | `http://localhost:8081/admin.html` |
| Consola de RabbitMQ | `http://localhost:15672` (usuario `ecommerce`) |

Ejemplo:
```powershell
$orden = @{ cliente = "Ana Perez"; items = @(@{ sku = "LIB-001"; nombre = "Libro"; cantidad = 1; precioUnitario = 150000 }) } | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri http://localhost:8080/order -ContentType "application/json" -Body $orden
```

## Prueba de punta a punta

Con el sistema levantado (en Docker o con `ops\arrancar.ps1`):

```powershell
powershell -ExecutionPolicy Bypass -File ops\verificar-flujo.ps1 -Memoria
```

Envía 3 órdenes válidas (todo correcto, pago rechazado por RN-11 y sin stock por RN-12) y 2 inválidas, y comprueba
14 puntos: la respuesta HTTP (202/400 y el total), el estado y la auditoría en PostgreSQL, el registro de
`analytics-service` y el `{payment, stock, email}` que entrega el WebSocket. `-Memoria` suma al final el consumo de
cada contenedor. Contra otra máquina: `-Api http://<IP>:8080/order -WebSocket ws://<IP>:8081/ws/ordenes -Origen http://<IP>:8081`.

## Agregar un microservicio

Un servicio nuevo (por ejemplo `shipping-worker`) se integra en estos sitios; si falta uno, el servicio no se compila,
no tiene imagen o no se despliega:

1. **`backend/pom.xml`:** añadir `<module>shipping-worker</module>`. El `pom.xml` del módulo hereda del padre y declara
   `common` y, si usa Kafka o RabbitMQ, `common-mensajeria` más `spring-kafka` / `spring-boot-starter-amqp` (en
   `common-mensajeria` son opcionales: cada servicio declara solo lo que usa).
2. **Código:** paquete `co.edu.proyecto.<nombre>` con `dominio/`, `infraestructura/<tecnología>/` y `configuracion/`
   (`ConfiguracionDominio` y `ConfiguracionMensajeria` con `@Import` de lo que necesite de `common-mensajeria`), y un
   `DominioPuroTest`. Los nombres nuevos de topics o colas van en `common/nombres/`. Si es un worker, basta con su
   `ReglaTarea`, un valor nuevo en `TipoTarea` y su consumidor de ~10 líneas.
3. **`backend/Dockerfile`:** añadir `COPY backend/shipping-worker/pom.xml backend/shipping-worker/` junto a los demás.
4. **`docker-compose.yml`:** un servicio con `<<: *microservicio`, `image: ecommerce-hibrido-shipping-worker:latest`,
   `build` con `MODULO: shipping-worker`, sus variables (`*kafka-env`, `*rabbit-env`), `depends_on` y
   `deploy.resources.limits` (o los anchors `x-java-opts-pequeno` y `x-limites-pequeno`). Revisar antes el presupuesto de
   memoria de la t3.small en [`docs/despliegue-aws.md`](docs/despliegue-aws.md).
5. **`ops\arrancar.ps1`:** añadirlo a `$contenedoresApp` y a la tanda que le corresponda (`ops\detener.ps1` no
   cambia: detiene lo que `arrancar.ps1` anotó en `.run\pids.txt`).
6. **`ops/aws/exportar-imagenes.ps1`:** añadirlo a la lista de imágenes, y en `ops/aws/arrancar-aws.sh` a
   `MICROSERVICIOS` y a su tanda (en `ops/aws/detener-aws.sh`, a la parada).
7. **Documentación:** [`docs/servicios.md`](docs/servicios.md), la tabla de estructura de este README y los diagramas.

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/cumplimiento-enunciado.md`](docs/cumplimiento-enunciado.md) | Cada requisito del enunciado, dónde se cumple y cómo comprobarlo |
| [`docs/kafka-vs-rabbitmq.md`](docs/kafka-vs-rabbitmq.md) | Cuándo un log de eventos y cuándo un broker de tareas, con ejemplos de este sistema |
| [`docs/diagramas/`](docs/diagramas/README.md) | Arquitectura, clases del dominio, secuencia, estados, entidad-relación y despliegue (Mermaid) |
| [`docs/servicios.md`](docs/servicios.md) | Servicios, topics, grupos, colas y tablas |
| [`docs/revision-solid.md`](docs/revision-solid.md) | Dónde se ve cada principio SOLID y cada patrón |
| [`docs/decisiones.md`](docs/decisiones.md) | Decisiones técnicas y por qué |
| [`docs/correcciones.md`](docs/correcciones.md) | Cada corrección respecto a la versión anterior y dónde está en el código |
| [`docs/despliegue-aws.md`](docs/despliegue-aws.md) | Despliegue en EC2 t3.small |
