# Cumplimiento del enunciado

Cada requisito del enunciado "Arquitectura Híbrida de E-commerce" con el archivo donde se cumple y cómo
comprobarlo. **Se revisa entero al terminar cada fase de la reorganización:** si una fila pasa de ✅ a otro
estado, la fase no está terminada. Si un archivo cambia de sitio, se actualiza su enlace en la misma fase.

Estado: ✅ cumplido · ⚠️ cumplido a medias (con la fase que lo completa).

Rutas abreviadas: `orden/` = `backend/order-api/src/main/java/co/edu/proyecto/orden/`.

## A. Arquitectura y mensajería

| # | Requisito | Estado | Dónde se cumple | Cómo comprobarlo |
|---|---|---|---|---|
| 1 | Kafka es el log de eventos inmutable y el bus central | ✅ | Topics en [Topicos.java](../backend/common/src/main/java/co/edu/proyecto/common/nombres/Topicos.java); `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` en [docker-compose.yml](../docker-compose.yml): solo existen los topics declarados | `kafka-topics.sh --list` muestra `new_orders`, `billing_events`, `inventory_events`, `notification_events` |
| 2 | RabbitMQ es el agente de tareas que enruta comandos | ✅ | [PublicadorTareas.java](../backend/order-processor/src/main/java/co/edu/proyecto/processor/infraestructura/rabbitmq/PublicadorTareas.java) publica el comando `ProcesarOrden` | Consola de RabbitMQ (`:15672`): mensajes en las colas |
| 3 | PostgreSQL para la persistencia transaccional | ✅ | [schema.sql](../backend/order-api/src/main/resources/schema.sql); [RegistrarOrdenService.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/aplicacion/RegistrarOrdenService.java) guarda orden y evento en una sola transacción | `SELECT * FROM ordenes;` tras un `POST /order` |
| 4 | Registro de auditoría de órdenes | ✅ | Tabla `auditoria_ordenes` en [schema.sql](../backend/order-api/src/main/resources/schema.sql); [RegistrarResultadoService.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/aplicacion/RegistrarResultadoService.java) | `SELECT * FROM auditoria_ordenes;`: 3 filas por orden |
| 5 | Justificar cuándo usar un log de eventos (Kafka) y cuándo un broker (RabbitMQ) | ⚠️ fase 8 | Solo una línea en el [README.md](../README.md) (y el informe) | Falta `docs/kafka-vs-rabbitmq.md` |
| 6 | Varios consumidores independientes de Kafka (procesamiento de flujos) | ✅ | [GruposConsumidores.java](../backend/common/src/main/java/co/edu/proyecto/common/nombres/GruposConsumidores.java): `analytics-group` y `processor-group` sobre `new_orders`; `dashboard-group` y `order-api-group` sobre los resultados | `kafka-consumer-groups.sh --list` |
| 7 | Exchange fanout `order_tasks` vinculado a las 3 colas | ✅ | [Exchanges.java](../backend/common/src/main/java/co/edu/proyecto/common/nombres/Exchanges.java), [Colas.java](../backend/common/src/main/java/co/edu/proyecto/common/nombres/Colas.java); el fanout lo declara [ConfiguracionRabbitBase.java](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria/rabbitmq/ConfiguracionRabbitBase.java) y cada worker enlaza su cola con [TopologiaWorker.java](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria/rabbitmq/TopologiaWorker.java) desde su `ConfiguracionMensajeria` ([billing](../backend/billing-worker/src/main/java/co/edu/proyecto/billing/configuracion/ConfiguracionMensajeria.java)) | Consola de RabbitMQ → Exchanges → `order_tasks` (tipo fanout, 3 bindings) |
| 8 | Patrón work-queue | ✅ | `prefetch: 1` y `concurrency: 2` en [worker-comun.yml](../backend/common-mensajeria/src/main/resources/worker-comun.yml), que cada worker carga con `spring.config.import` ([billing](../backend/billing-worker/src/main/resources/application.yml)); los tres siguen siendo módulos y contenedores separados; cola durable con dead-letter a `failed_tasks` en [TopologiaWorker.java](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria/rabbitmq/TopologiaWorker.java) | Consola de RabbitMQ o `rabbitmqctl list_consumers`: 2 consumidores por cola, prefetch 1 |

## B. POO y principios SOLID

| # | Requisito | Estado | Dónde se cumple | Cómo comprobarlo |
|---|---|---|---|---|
| 9 | Entidad rica con métodos de negocio semánticos (`confirmarPago()`, `cancelar()`) | ✅ | [Orden.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/dominio/Orden.java) | [OrdenTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/dominio/OrdenTest.java) |
| 10 | Sin getters/setters indiscriminados; persistir sin romper el encapsulamiento | ✅ | `Orden` no tiene setters; JPA usa [OrdenEntity.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/infraestructura/persistencia/orden/OrdenEntity.java) y [OrdenMapper.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/infraestructura/persistencia/orden/OrdenMapper.java) | [OrdenMapperTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/infraestructura/persistencia/orden/OrdenMapperTest.java) |
| 11 | `record` para valores monetarios (`Dinero`) con Fail-Fast en el constructor compacto | ✅ | [Dinero.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/dominio/Dinero.java) | [DineroTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/dominio/DineroTest.java) |
| 12 | `record` para identificadores y detalles de ítems, con Fail-Fast | ✅ | [OrdenId.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/dominio/OrdenId.java), [ItemOrden.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/dominio/ItemOrden.java); contratos en [common/contratos/](../backend/common/src/main/java/co/edu/proyecto/common/contratos) | [ItemOrdenTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/dominio/ItemOrdenTest.java) |
| 13 | Patrón Strategy para descuentos e impuestos | ✅ | `orden/dominio/descuentos/` ([EstrategiaDescuento.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/dominio/descuentos/EstrategiaDescuento.java)) y `orden/dominio/impuestos/` ([EstrategiaImpuesto.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/dominio/impuestos/EstrategiaImpuesto.java)) | [CalculoTotalTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/dominio/CalculoTotalTest.java) |
| 14 | Patrón Builder para construir órdenes de forma segura | ✅ | [OrdenBuilder.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/dominio/OrdenBuilder.java) (el constructor de `Orden` no es público) | [OrdenBuilderTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/dominio/OrdenBuilderTest.java) |
| 15 | Patrón Repository (`OrdenRepository`) | ✅ | Puerto [OrdenRepository.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/dominio/puertos/OrdenRepository.java), adaptador [OrdenRepositoryJpa.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/infraestructura/persistencia/orden/OrdenRepositoryJpa.java). Los puertos que solo necesitan los casos de uso (`BandejaSalida`, `AuditoriaRepository`) están en [aplicacion/puertos/](../backend/order-api/src/main/java/co/edu/proyecto/orden/aplicacion/puertos) | El dominio no importa JPA: [DominioPuroTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/arquitectura/DominioPuroTest.java) |
| 16 | DIP: inyección estricta por constructor de interfaces y servicios | ✅ | [ConfiguracionDominio.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/configuracion/ConfiguracionDominio.java) (cada servicio tiene la suya; la mensajería entra por `ConfiguracionMensajeria` con `@Import` de [common-mensajeria](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria)); todas las clases reciben sus dependencias por constructor | `grep -rn "@Autowired" backend/*/src/main` → 0 resultados |

## C. Pruebas unitarias

| # | Requisito | Estado | Dónde se cumple | Cómo comprobarlo |
|---|---|---|---|---|
| 17 | Pruebas en memoria del dominio y los Value Objects, sin Spring Boot ni contenedores | ✅ | `backend/*/src/test/`: 70 pruebas, ninguna con `@SpringBootTest` | `mvn verify` en la raíz, sin Docker |
| 18 | Patrón AAA (Arrange, Act, Assert) | ⚠️ fase 6 | Las pruebas separan las tres partes con líneas en blanco, pero sin rótulos | Faltan los comentarios `// Arrange`, `// Act`, `// Assert` |
| 19 | Dobles de prueba con Mockito (`@Mock`, `@InjectMocks`, `verify`) para los servicios | ✅ | [RegistrarResultadoServiceTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/aplicacion/RegistrarResultadoServiceTest.java), [RegistrarOrdenServiceTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/aplicacion/RegistrarOrdenServiceTest.java), [PublicadorBandejaSalidaTest.java](../backend/order-api/src/test/java/co/edu/proyecto/orden/infraestructura/bandejasalida/PublicadorBandejaSalidaTest.java), [ConsumidorNuevasOrdenesTest.java](../backend/order-processor/src/test/java/co/edu/proyecto/processor/infraestructura/kafka/ConsumidorNuevasOrdenesTest.java), [EjecutorTareaTest.java](../backend/common-mensajeria/src/test/java/co/edu/proyecto/mensajeria/tareas/EjecutorTareaTest.java) | `mvn verify` |

## D. Docker y control de recursos

| # | Requisito | Estado | Dónde se cumple | Cómo comprobarlo |
|---|---|---|---|---|
| 20 | Compose con Kafka (coordinador del clúster), RabbitMQ (con interfaz de gestión), PostgreSQL y los microservicios | ✅ | [docker-compose.yml](../docker-compose.yml): Kafka en modo KRaft (`broker,controller`), `rabbitmq:3.13-management-alpine`, `postgres:16-alpine` | `docker compose ps`: 10 servicios `healthy`/`running` |
| 21 | Dockerfile multietapa: Maven + OpenJDK para compilar, JRE Alpine y usuario no root para ejecutar | ✅ | [backend/Dockerfile](../backend/Dockerfile): `maven:3.9-eclipse-temurin-21` → `eclipse-temurin:21-jre-alpine`, `USER ecommerce` | `docker compose exec order-api whoami` → `ecommerce` |
| 22 | ~10 servicios con límites de CPU y RAM por servicio | ✅ | `deploy.resources.limits` en los 10 servicios de [docker-compose.yml](../docker-compose.yml); presupuesto para la t3.small en [despliegue-aws.md](despliegue-aws.md) | `docker stats --no-stream`: columna `LIMIT` |

## E. Componentes y flujo de datos

| # | Requisito | Estado | Dónde se cumple | Cómo comprobarlo |
|---|---|---|---|---|
| 23 | `order_api`: `POST /order`, DTOs `record`, entidad rica `Orden`, persiste con `OrdenRepository` y publica en `new_orders` | ✅ | [OrdenController.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/infraestructura/rest/OrdenController.java), [CrearOrdenRequest.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/infraestructura/rest/CrearOrdenRequest.java), [PublicadorBandejaSalida.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/infraestructura/bandejasalida/PublicadorBandejaSalida.java) (patrón outbox, junto con [BandejaSalidaJpa.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/infraestructura/bandejasalida/BandejaSalidaJpa.java) en `infraestructura/bandejasalida/`) | `ops\enviar-ordenes.ps1` → `202 Accepted` |
| 24 | `order_processor`: consume `new_orders`, traduce evento → comando y publica en el fanout `order_tasks` | ✅ | [TraductorEventoAComando.java](../backend/order-processor/src/main/java/co/edu/proyecto/processor/aplicacion/TraductorEventoAComando.java), [ConsumidorNuevasOrdenes.java](../backend/order-processor/src/main/java/co/edu/proyecto/processor/infraestructura/kafka/ConsumidorNuevasOrdenes.java) | Log: `Orden … enviada a RabbitMQ como comando` |
| 25 | Workers de pago, inventario y notificación: consumen su cola, aplican reglas de dominio y publican `PAYMENT_SUCCESS`/`PAYMENT_FAILED`, `STOCK_RESERVED`/`STOCK_OUT_OF_STOCK` y `EMAIL_SENT` en su topic | ✅ | [ReglaPago.java](../backend/billing-worker/src/main/java/co/edu/proyecto/billing/dominio/ReglaPago.java), [ReglaInventario.java](../backend/inventory-worker/src/main/java/co/edu/proyecto/inventory/dominio/ReglaInventario.java), [ReglaNotificacion.java](../backend/notification-worker/src/main/java/co/edu/proyecto/notification/dominio/ReglaNotificacion.java); las tres implementan [ReglaTarea.java](../backend/common/src/main/java/co/edu/proyecto/common/contratos/ReglaTarea.java) y las aplica [EjecutorTarea.java](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria/tareas/EjecutorTarea.java) (pausa, regla, envío a Kafka y ack después); cada worker solo escucha su cola ([ConsumidorPagos.java](../backend/billing-worker/src/main/java/co/edu/proyecto/billing/infraestructura/rabbitmq/ConsumidorPagos.java)); estados en el enum [EstadoTarea.java](../backend/common/src/main/java/co/edu/proyecto/common/contratos/EstadoTarea.java) (su nombre es el texto del JSON; sabe su tipo y si es éxito) y cola/topic de cada worker en [TipoTarea.java](../backend/common/src/main/java/co/edu/proyecto/common/nombres/TipoTarea.java); un estado que llega por el topic de otra tarea se rechaza en [RegistrarResultadoService.java](../backend/order-api/src/main/java/co/edu/proyecto/orden/aplicacion/RegistrarResultadoService.java) (ya no cancela la orden) | Botones de ejemplo del dashboard (pago rechazado, sin stock) |
| 26 | `analytics_service`: consume `new_orders` directamente y acumula el total de ventas en tiempo real | ✅ | [AgregadorVentas.java](../backend/analytics-service/src/main/java/co/edu/proyecto/analytics/dominio/AgregadorVentas.java), [ConsumidorNuevasOrdenes.java](../backend/analytics-service/src/main/java/co/edu/proyecto/analytics/infraestructura/kafka/ConsumidorNuevasOrdenes.java) (relee desde el inicio con [ConsumidorQueReleeTodo.java](../backend/common-mensajeria/src/main/java/co/edu/proyecto/mensajeria/kafka/ConsumidorQueReleeTodo.java)) | `docker compose logs analytics-service`: `total acumulado=…` |
| 27 | `dashboard_aggregator`: consume los 3 topics, guarda en memoria `order_id → { payment, stock, email }` con `"pending"` y lo emite de forma incremental por WebSocket | ✅ | [TableroOrdenes.java](../backend/dashboard-aggregator/src/main/java/co/edu/proyecto/dashboard/dominio/TableroOrdenes.java), [ProgresoOrden.java](../backend/dashboard-aggregator/src/main/java/co/edu/proyecto/dashboard/dominio/ProgresoOrden.java) (`"pending"` es una constante privada suya; emite `{payment, stock, email}` como texto), [CanalWebSocket.java](../backend/dashboard-aggregator/src/main/java/co/edu/proyecto/dashboard/infraestructura/websocket/CanalWebSocket.java) | `http://localhost:8081`: cada orden pasa de `pending` a su estado final |
| 28 | Formato JSON de los mensajes tal como lo muestra el enunciado (`order_id`, `"order_123"`) | ⚠️ decisión | Se decidió conservar `orderId` (camelCase, la convención de Java y de Jackson por defecto) en todos los contratos de [common/contratos/](../backend/common/src/main/java/co/edu/proyecto/common/contratos) y en la respuesta HTTP; el id es un UUID que genera order-api, no `"order_123"`. Cambiarlo obligaría a leer los dos nombres (`@JsonAlias`), porque Kafka conserva los mensajes viejos y analytics y el dashboard los releen al arrancar. Lo que el enunciado fija sí coincide exactamente: los estados `PAYMENT_SUCCESS`… (enum [EstadoTarea.java](../backend/common/src/main/java/co/edu/proyecto/common/contratos/EstadoTarea.java)), `"pending"` y `{payment, stock, email}` | `kafka-console-consumer.sh --topic billing_events --from-beginning --max-messages 1` → `{"orderId":"…","status":"PAYMENT_SUCCESS","detalle":"Pago aprobado","ocurridoEn":"…"}`; se justifica en el informe |

## Registro por fase

Cada fase se cierra con:

```powershell
mvn verify
docker compose build
docker compose up -d --wait
powershell -ExecutionPolicy Bypass -File ops\verificar-flujo.ps1 -Memoria
```

[`ops/verificar-flujo.ps1`](../ops/verificar-flujo.ps1) envía 3 órdenes válidas y 2 inválidas y comprueba la respuesta HTTP,
el estado y la auditoría en PostgreSQL, el log de analytics y el estado `{payment, stock, email}` que entrega el WebSocket.
La memoria se mide en local (Docker Desktop, mismos límites que en AWS) con el sistema recién arrancado y 5 órdenes procesadas.

| Fase | Fecha | `mvn verify` | Punta a punta | Memoria total | Filas ✅ |
|---|---|---|---|---|---|
| 0 · Línea base | 2026-11-21 | 56/56 ✅ | 14/14 ✅ | 1417 MiB (order-api 210/256, kafka 372/448) | 25/28 |
| 1 · Configuraciones | 2026-11-21 | 56/56 ✅ | 14/14 ✅ | 1403 MiB (order-api 209/256, kafka 357/448) | 25/28 |
| 2 · Workers | 2026-11-22 | 60/60 ✅ | 14/14 ✅ | 1403 MiB (order-api 212/256, kafka 346/448) | 25/28 |
| 3 · Tipos | 2026-11-22 | 69/69 ✅ | 14/14 ✅ | 1319 MiB (order-api 207/256, kafka 301/448) | 25/28 (28 queda en ⚠️ por decisión: se conserva `orderId`) |
| 4 · Paquetes | 2026-11-22 | 70/70 ✅ | 14/14 ✅ | 1385 MiB (order-api 208/256, kafka 359/448) | 25/28 (28 queda en ⚠️ por decisión: se conserva `orderId`) |
