# Decisiones técnicas

Registro de las decisiones tomadas al reconstruir el proyecto, sobre todo donde el código se aparta del plan.

| # | Decisión | Por qué |
|---|---|---|
| D-01 | **Bandeja de salida (outbox)** en lugar de publicar en Kafka dentro de la petición | Elimina la escritura dual: la orden y su evento se guardan en la misma transacción. Entrega "al menos una vez"; por eso todos los consumidores son idempotentes |
| D-02 | El esquema se crea con **`schema.sql`** (Spring Boot) y no con Flyway | Flyway no estaba disponible sin conexión al reconstruir. `schema.sql` usa `CREATE TABLE IF NOT EXISTS` y conserva todas las restricciones `CHECK` y `UNIQUE`. Pasar a Flyway es mover el archivo a `db/migration/V1__esquema.sql` y añadir la dependencia |
| D-03 | `spring.jpa.hibernate.ddl-auto: none` | La base de datos la define `schema.sql`; Hibernate no la modifica |
| D-04 | La pureza del dominio se comprueba con **`DominioPuroTest`** (JUnit) en vez de ArchUnit | ArchUnit no estaba disponible sin conexión. La prueba recorre `dominio/` y falla si algún archivo importa Spring, Jakarta o Kafka |
| D-05 | Los DTO de entrada (`CrearOrdenRequest`, `ItemRequest`) están en `infraestructura/rest` | Llevan anotaciones de Jakarta Validation; así `aplicacion/` no depende de Jakarta. El caso de uso recibe objetos de dominio (`ItemOrden`) |
| D-06 | Los productores de Kafka envían el JSON **como texto** (`StringSerializer` por defecto) | No hace falta ningún bean de serialización y las fechas salen en ISO-8601 con el `ObjectMapper` de Spring. Los consumidores usan `JsonDeserializer` configurado en `application.yml` |
| D-07 | Dashboard y analytics **releen sus topics desde el inicio** al arrancar (`AbstractConsumerSeekAware`) | Su estado vive en memoria; releer lo reconstruye. Es seguro porque los dos son idempotentes. La bitácora de analytics se vacía al arrancar para no duplicar líneas |
| D-08 | Solo el resultado del **pago** cambia el estado de la orden (RN-16) | Si un pago aprobado convive con "sin stock", la orden queda `PAGADA` y la falta de stock queda en la auditoría. Modelar la compensación (reembolso) está fuera del alcance |
| D-09 | El dashboard guarda como máximo **1.000 órdenes** | Evita que la memoria crezca sin límite; las más antiguas salen primero |
| D-10 | Los valores por defecto de `application.yml` apuntan al **modo desarrollo** (Kafka `localhost:29092`, PostgreSQL `localhost:5433`) | Coinciden con `docker-compose.dev.yml` y `ops/arrancar.ps1`. En Docker, `docker-compose.yml` los sobrescribe con variables de entorno |
| D-11 | La configuración común de Kafka y RabbitMQ vive en el módulo `common-mensajeria` y cada servicio la importa con `@Import`, sin escaneo ni autoconfiguración | Antes el `DefaultErrorHandler`, el conversor JSON y la topología de los workers estaban copiados en cada servicio. Kafka y RabbitMQ son dependencias `<optional>`: analytics, el dashboard y order-api no reciben RabbitMQ y la memoria no cambia |
| D-12 | El campo JSON se llama `orderId` (camelCase) y no `order_id` | Es el nombre del componente del record; el ejemplo del enunciado es ilustrativo |
