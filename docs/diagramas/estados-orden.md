# Estados de la orden

## `Orden` en order-api (`EstadoOrden`)

Solo el resultado del **pago** cambia el estado (decisión D-08). Las transiciones las protege la propia entidad:
cualquier otra lanza `EstadoInvalidoException`.

```mermaid
stateDiagram-v2
    [*] --> CREADA : OrdenBuilder.construir()<br/>POST /order → 202
    CREADA --> CREADA : calcularTotal()
    CREADA --> PAGADA : confirmarPago()<br/>billing_events = PAYMENT_SUCCESS
    CREADA --> CANCELADA : cancelar()<br/>billing_events = PAYMENT_FAILED
    PAGADA --> [*]
    CANCELADA --> [*]

    note right of PAGADA
        cancelar() en una orden PAGADA
        lanza EstadoInvalidoException
    end note
```

Un resultado de pago repetido no hace nada: `RegistrarResultadoService` solo aplica el pago si la orden sigue `CREADA`.

## Cada tarea en el dashboard (`ProgresoOrden`)

El dashboard guarda `order_id → {payment, stock, email}`. Cada campo empieza en `"pending"` y cambia una sola vez,
cuando llega el resultado de su topic.

```mermaid
stateDiagram-v2
    state payment {
        [*] --> pending_p
        pending_p --> PAYMENT_SUCCESS
        pending_p --> PAYMENT_FAILED
    }
    state stock {
        [*] --> pending_s
        pending_s --> STOCK_RESERVED
        pending_s --> STOCK_OUT_OF_STOCK
    }
    state email {
        [*] --> pending_e
        pending_e --> EMAIL_SENT
    }
    pending_p : pending
    pending_s : pending
    pending_e : pending
```

Los estados son el enum `EstadoTarea` (en `common`): cada uno sabe a qué `TipoTarea` pertenece y si es éxito;
un estado que llega por el topic de otra tarea se rechaza.
