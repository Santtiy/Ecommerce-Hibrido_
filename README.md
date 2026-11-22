# Ecommerce-Hibrido (v2)

Plataforma de órdenes de comercio electrónico con **arquitectura de microservicios orientada a eventos e híbrida**:

- **Kafka** guarda los eventos (lo que ya ocurrió) y es el bus central.
- **RabbitMQ** reparte los comandos de trabajo (lo que hay que hacer) entre los workers con un exchange **fanout**.
- **PostgreSQL** guarda las órdenes, la bandeja de salida y la auditoría.
- Un **dashboard** web muestra en tiempo real el estado de cada orden.

Proyecto integrador (POO, Sistemas Distribuidos y Sistemas Operativos). Esta versión es una reconstrucción
completa con las correcciones de la auditoría: ver [`docs/correcciones.md`](docs/correcciones.md).

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
| `frontend/` | Dashboard web (`index.html`). Lo sirve `dashboard-aggregator` en `http://localhost:8081`: Maven lo empaqueta como `classpath:/static` |
| `ops/` | Scripts de arranque/parada (local y AWS) |
| `docs/` | Documentación |

`docker-compose.yml` y `.env` siguen en la raíz; el contexto de build de Docker es la raíz porque necesita `backend/` y `frontend/`.
Tras editar el front, vuelve a empaquetar `dashboard-aggregator` (o reinicia el servicio desde IntelliJ) para ver el cambio.

## Estructura de cada servicio

Todos los servicios tienen los mismos paquetes:

| Paquete | Qué contiene |
|---|---|
| `dominio/` | Reglas de negocio en Java puro (sin Spring). Lo comprueba `DominioPuroTest` |
| `aplicacion/` | Casos de uso (única anotación de Spring permitida: `@Transactional`) |
| `infraestructura/` | Adaptadores: REST, JPA, Kafka, RabbitMQ, WebSocket |
| `configuracion/` | Clases `@Configuration` |

Los nombres de topics, colas, exchanges, estados y grupos están en un solo sitio:
`backend/common/src/main/java/co/edu/proyecto/common/nombres/`.

La configuración de Kafka y RabbitMQ que comparten los servicios (reintentos, JSON, fanout `order_tasks`, cola de
fallidos) está en `backend/common-mensajeria/`; cada servicio la importa con `@Import` desde su `ConfiguracionMensajeria`.

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

```powershell
mvn -B -f backend/pom.xml verify    # compila los 9 módulos y ejecuta las 56 pruebas unitarias
```

Las pruebas no necesitan Docker ni Spring: dominio (AAA), servicios con Mockito (`@Mock`, `@InjectMocks`,
`verify`) y una prueba que verifica que el dominio no importa ningún framework.

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

**AWS EC2:** ver [`docs/despliegue-aws.md`](docs/despliegue-aws.md).

| Qué | URL |
|---|---|
| API | `POST http://localhost:8080/order` |
| Dashboard | `http://localhost:8081` |
| Consola de RabbitMQ | `http://localhost:15672` (usuario `ecommerce`) |

Ejemplo:
```powershell
$orden = @{ cliente = "Ana Perez"; items = @(@{ sku = "LIB-001"; nombre = "Libro"; cantidad = 1; precioUnitario = 150000 }) } | ConvertTo-Json -Depth 5
Invoke-RestMethod -Method Post -Uri http://localhost:8080/order -ContentType "application/json" -Body $orden
```

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/servicios.md`](docs/servicios.md) | Servicios, topics, grupos, colas y tablas |
| [`docs/correcciones.md`](docs/correcciones.md) | Cada corrección de la auditoría y dónde está en el código |
| [`docs/decisiones.md`](docs/decisiones.md) | Decisiones técnicas tomadas durante la reconstrucción |
| [`docs/revision-solid.md`](docs/revision-solid.md) | Dónde se ve cada principio SOLID y cada patrón |
| [`docs/despliegue-aws.md`](docs/despliegue-aws.md) | Despliegue en EC2 t3.small |
