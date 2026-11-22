# Modelo entidad-relación

Tablas de [`schema.sql`](../../backend/order-api/src/main/resources/schema.sql). Solo `order-api` usa PostgreSQL.

```mermaid
erDiagram
    ordenes ||--|{ items_orden : "tiene (ON DELETE CASCADE)"
    ordenes ||--o{ auditoria_ordenes : "orden_id (sin FK)"

    ordenes {
        uuid id PK
        varchar(120) cliente
        varchar(20) estado "CHECK IN (CREADA, PAGADA, CANCELADA)"
        numeric total_monto "CHECK >= 0"
        varchar(3) total_moneda
        timestamptz creada_en
    }
    items_orden {
        bigint id PK "IDENTITY"
        uuid orden_id FK
        varchar(60) sku
        varchar(120) nombre
        integer cantidad "CHECK > 0"
        numeric precio_monto "CHECK > 0"
        varchar(3) precio_moneda
    }
    auditoria_ordenes {
        bigint id PK "IDENTITY"
        uuid orden_id "UNIQUE (orden_id, origen, status)"
        varchar(30) origen "PAGO, INVENTARIO, NOTIFICACION"
        varchar(30) status "PAYMENT_SUCCESS, ..."
        varchar(255) detalle
        timestamptz ocurrido_en
        timestamptz registrado_en
    }
    bandeja_salida {
        bigint id PK "IDENTITY"
        varchar(60) topico
        varchar(60) clave
        text contenido "OrdenCreada en JSON"
        timestamptz creado_en
    }
```

| Tabla | Clase JPA | Notas |
|---|---|---|
| `ordenes` | `OrdenEntity` (`persistencia/orden/`) | `OrdenMapper` la traduce a `Orden` con `Orden.reconstituir(...)` |
| `items_orden` | `ItemOrdenEntity` | Se borran con su orden |
| `auditoria_ordenes` | `AuditoriaEntity` (`persistencia/auditoria/`) | Sin clave foránea: la relación con `ordenes` es lógica, por `orden_id`. `UNIQUE` + `ON CONFLICT DO NOTHING` la hace idempotente |
| `bandeja_salida` | `MensajePendienteEntity` (`bandejasalida/`) | Se escribe en la misma transacción que la orden y se borra cuando Kafka confirma |
