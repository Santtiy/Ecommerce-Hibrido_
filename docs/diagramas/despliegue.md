# Despliegue en AWS EC2 t3.small

Una sola instancia **t3.small** (2 vCPU, 2 GiB de RAM + 2 GiB de swap) con Ubuntu Server 24.04 y Docker Engine con
Compose v2. Las imágenes `ecommerce-hibrido-*:latest` se construyen en Windows y se copian con
[`exportar-imagenes.ps1`](../../ops/aws/exportar-imagenes.ps1); en la EC2 se arranca por tandas con
[`arrancar-aws.sh`](../../ops/aws/arrancar-aws.sh). Detalle completo: [despliegue-aws.md](../despliegue-aws.md).

```mermaid
flowchart TB
    nav([Navegador<br/>de quien evalúa])
    pc([PC de desarrollo<br/>Windows + Docker Desktop])

    subgraph AWS[AWS · Security Group: 22 y 8080/8081 solo para IPs permitidas]
        subgraph EC2["EC2 t3.small · Ubuntu 24.04 · Docker Engine"]
            subgraph NET[red ecommerce-net]
                api["order-api<br/>256M / 0.5 CPU"]
                dash["dashboard-aggregator<br/>192M / 0.4 CPU"]
                proc["order-processor<br/>160M / 0.25"]
                bill["billing-worker<br/>160M / 0.25"]
                inv["inventory-worker<br/>160M / 0.25"]
                notif["notification-worker<br/>160M / 0.25"]
                ana["analytics-service<br/>160M / 0.25"]
                kafka[("kafka (KRaft)<br/>448M / 1.0")]
                rabbit[("rabbitmq<br/>224M / 0.4")]
                pg[("postgres-db<br/>128M / 0.3")]
            end
            vpg[(volumen<br/>postgres-data)]
            vana[(volumen<br/>analytics-datos)]
        end
    end

    nav -- ":8080 POST /order" --> api
    nav -- ":8081 página + WebSocket" --> dash
    pc -- "ssh :22 · túnel a 127.0.0.1:15672<br/>(consola de RabbitMQ)" --> rabbit
    pc -. "scp de las imágenes<br/>(docker save + gzip)" .-> EC2

    pg --- vpg
    ana --- vana
```

| Puerto | Publicado en | Quién lo usa |
|---|---|---|
| 8080 | todas las interfaces (lo filtra el Security Group) | `POST /order` |
| 8081 | todas las interfaces (lo filtra el Security Group) | Tienda (`/`), panel de administración (`/admin.html`) y `/ws/ordenes` |
| 15672 | solo `127.0.0.1` de la EC2 | Consola de RabbitMQ, por túnel SSH |
| 5432, 9092, 5672 | no publicados | Solo dentro de `ecommerce-net` |

Suma de límites: 2048 MiB (techos individuales, no reserva). Consumo medido en local con los mismos límites:
ver el registro de [cumplimiento-enunciado.md](../cumplimiento-enunciado.md).
