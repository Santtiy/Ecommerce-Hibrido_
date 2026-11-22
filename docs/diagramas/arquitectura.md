# Arquitectura

Rectángulos: microservicios. Cilindros: almacenamiento. Las flechas indican el sentido de los datos; la etiqueta
es el topic, la cola o el grupo de consumidores. Los nombres salen de `backend/common/.../nombres/`.

```mermaid
flowchart LR
    cliente([Navegador / cliente HTTP])

    subgraph PG[PostgreSQL]
        db[(ordenes · items_orden<br/>bandeja_salida · auditoria_ordenes)]
    end

    subgraph K[Kafka - log de eventos]
        new_orders[[new_orders<br/>3 particiones]]
        billing_events[[billing_events]]
        inventory_events[[inventory_events]]
        notification_events[[notification_events]]
    end

    subgraph R[RabbitMQ - broker de tareas]
        order_tasks{{order_tasks<br/>fanout}}
        billing_queue[/billing_queue/]
        inventory_queue[/inventory_queue/]
        notification_queue[/notification_queue/]
        dlx{{order_tasks_dlx<br/>fanout}}
        failed[/failed_tasks/]
    end

    api[order-api]
    proc[order-processor]
    bill[billing-worker]
    inv[inventory-worker]
    notif[notification-worker]
    ana[analytics-service]
    dash[dashboard-aggregator]

    cliente -- "POST /order :8080" --> api
    api -- "orden + evento<br/>(una transacción)" --> db
    api -- "PublicadorBandejaSalida" --> new_orders

    new_orders -- processor-group --> proc
    new_orders -- analytics-group --> ana

    proc -- ProcesarOrden --> order_tasks
    order_tasks --> billing_queue --> bill
    order_tasks --> inventory_queue --> inv
    order_tasks --> notification_queue --> notif
    billing_queue -. "3 intentos fallidos" .-> dlx
    inventory_queue -.-> dlx
    notification_queue -.-> dlx
    dlx --> failed

    bill -- "PAYMENT_SUCCESS / PAYMENT_FAILED" --> billing_events
    inv -- "STOCK_RESERVED / STOCK_OUT_OF_STOCK" --> inventory_events
    notif -- EMAIL_SENT --> notification_events

    billing_events & inventory_events & notification_events -- dashboard-group --> dash
    billing_events & inventory_events & notification_events -- order-api-group --> api
    api -- "auditoría + estado" --> db

    dash -- "WebSocket /ws/ordenes :8081<br/>{payment, stock, email}" --> cliente
    ana -- "bitácora (SO)" --> bitacora[(ordenes.log)]
```

Puntos clave:

- `order-api` nunca publica directamente durante la petición: guarda el evento en `bandeja_salida` y
  `PublicadorBandejaSalida` lo envía cada segundo (patrón outbox).
- `analytics-service` y `dashboard-aggregator` releen sus topics desde el inicio al arrancar (`ConsumidorQueReleeTodo`).
- Cada worker solo conoce su cola y su topic (`TipoTarea`); el procesamiento común está en `EjecutorTarea`.
- Por qué Kafka para los eventos y RabbitMQ para los comandos: [kafka-vs-rabbitmq.md](../kafka-vs-rabbitmq.md).
