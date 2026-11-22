# Diagramas

Diagramas Mermaid del código actual. GitHub los dibuja directamente al abrir cada archivo; en IntelliJ o VS Code
hace falta el complemento de Mermaid.

| Diagrama | Qué muestra |
|---|---|
| [arquitectura.md](arquitectura.md) | Los 10 contenedores, los topics de Kafka, el fanout de RabbitMQ y quién lee qué |
| [clases-dominio.md](clases-dominio.md) | Clases del dominio de `order-api`: `Orden`, value objects, Builder, Strategy y Repository |
| [secuencia-orden.md](secuencia-orden.md) | Una orden de punta a punta, desde `POST /order` hasta el dashboard |
| [estados-orden.md](estados-orden.md) | Estados de `Orden` (`CREADA`, `PAGADA`, `CANCELADA`) y de cada tarea en el dashboard |
| [entidad-relacion.md](entidad-relacion.md) | Tablas de PostgreSQL (`schema.sql`) |
| [despliegue.md](despliegue.md) | Despliegue en la EC2 t3.small: contenedores, límites de memoria, puertos y volúmenes |
