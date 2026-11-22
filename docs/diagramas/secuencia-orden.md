# Secuencia de una orden de punta a punta

Camino feliz de una orden válida. Los tres workers trabajan en paralelo (el bloque `par`); el orden en que llegan sus
resultados al dashboard puede variar.

```mermaid
sequenceDiagram
    autonumber
    actor C as Navegador
    participant API as order-api<br/>(OrdenController,<br/>RegistrarOrdenService)
    participant DB as PostgreSQL
    participant OUT as order-api<br/>(PublicadorBandejaSalida)
    participant K as Kafka
    participant P as order-processor
    participant R as RabbitMQ<br/>(fanout order_tasks)
    participant W as billing / inventory /<br/>notification-worker
    participant A as analytics-service
    participant D as dashboard-aggregator

    C->>API: POST /order {cliente, items}
    API->>API: OrdenBuilder.construir()<br/>orden.calcularTotal(descuento, IVA)
    API->>DB: INSERT ordenes, items_orden, bandeja_salida<br/>(una sola transacción)
    API-->>C: 202 Accepted {orderId, estado: CREADA, total}

    loop cada 1 s
        OUT->>DB: lee bandeja_salida
        OUT->>K: new_orders (clave = orderId)
        K-->>OUT: confirmación
        OUT->>DB: DELETE de la fila publicada
    end

    par processor-group
        K->>P: OrdenCreada
        P->>P: TraductorEventoAComando
        P->>R: ProcesarOrden
    and analytics-group
        K->>A: OrdenCreada
        A->>A: AgregadorVentas.registrar()<br/>Bitacora.escribir()
    end

    R->>W: una copia a billing_queue, inventory_queue y notification_queue
    par cada worker (prefetch 1, 2 hilos)
        W->>W: EjecutorTarea: pausa + ReglaTarea.evaluar()
        W->>K: ResultadoTarea en billing_events / inventory_events / notification_events
        K-->>W: confirmación
        W-->>R: ack (solo después de Kafka)
    end

    par dashboard-group
        K->>D: ResultadoTarea
        D->>D: TableroOrdenes.aplicar()
        D-->>C: WebSocket /ws/ordenes {payment, stock, email}
    and order-api-group
        K->>API: ResultadoTarea
        API->>DB: INSERT auditoria_ordenes (ON CONFLICT DO NOTHING)
        opt resultado de billing_events
            API->>DB: orden.confirmarPago() → PAGADA<br/>u orden.cancelar() → CANCELADA
        end
    end
```

Casos alternativos:

| Caso | Qué pasa |
|---|---|
| Datos inválidos (cantidad 0, precio 0, sin ítems) | `ManejadorErrores` responde **400** con el mensaje en español; no se guarda nada |
| Total > 1.000.000 (RN-11) | `ReglaPago` publica `PAYMENT_FAILED`; `order-api` cancela la orden |
| Un ítem con más de 10 unidades (RN-12) | `ReglaInventario` publica `STOCK_OUT_OF_STOCK`; queda en la auditoría y en el dashboard |
| Kafka caído al recibir la orden | La orden se acepta (202); el evento espera en `bandeja_salida` hasta que Kafka vuelve |
| Un worker falla 3 veces | El comando pasa a `order_tasks_dlx` → `failed_tasks` |
| Un resultado llega repetido | La auditoría lo ignora (`UNIQUE`) y la orden no cambia (solo se modifica si sigue `CREADA`) |
