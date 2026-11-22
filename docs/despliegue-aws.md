# Despliegue en AWS EC2 (t3.small, Ubuntu Server 24.04)

El proyecto tiene **un único archivo Compose, `docker-compose.yml`**, con la configuración
optimizada para AWS EC2 t3.small. Este documento la describe junto con los scripts de `ops/aws/`.
La configuración anterior de la **etapa 7** (VM de 8 GB / 4 vCPU, límites de 4368M y puertos de
infraestructura publicados) ya no está en el árbol de trabajo; se conserva en el historial de Git
(commit `a0b8dbb`) y es la referencia de las comparaciones de este documento.

| Archivo | Uso |
|---|---|
| `docker-compose.yml` | Archivo Compose principal y único (configuración AWS): `docker compose ...` |
| `.env.example` | Plantilla de credenciales sin valores; se copia a `.env` (ignorado por Git). Ver §7.1. |
| `ops/aws/rabbitmq/20-aws.conf` | Umbral de memoria absoluto de RabbitMQ (montado en solo lectura). |
| `ops/aws/arrancar-aws.sh` | Arranque por tandas en Ubuntu (`--no-build`). |
| `ops/aws/detener-aws.sh` | Parada sin borrar volúmenes ni datos. |
| `ops/aws/exportar-imagenes.ps1` | Exportación de las 7 imágenes en Windows (`docker save` + gzip). |
| `ops/aws/.gitattributes` | Fuerza LF en `.sh`/`.conf` aunque Git use `core.autocrlf=true`. |

> **Estado:** configuración desplegada y verificada en EC2: los diez contenedores arrancaron, el
> dashboard respondió y una orden de prueba se procesó de punta a punta. Los consumos de memoria
> de §12 siguen siendo estimaciones hasta registrar una medición con `docker stats` en la instancia.

---

## 1. Arquitectura

Diez contenedores en la red `ecommerce-net`:

```
                    HTTP :8080                       HTTP/WS :8081
 navegador ──► order-api ──(new_orders, 3 part.)──► Kafka ◄── dashboard-aggregator ◄── navegador
                  │                                  │  ▲        (billing/inventory/notification_events)
              PostgreSQL                             │  │
                                   ┌─────────────────┤  └──── billing / inventory / notification-worker
                                   ▼                 ▼                     ▲  (cada uno crea su topic)
                          analytics-service    order-processor             │
                          (analytics-group)    (processor-group)           │
                                                     │                     │
                                                     ▼                     │
                                      RabbitMQ: exchange fanout order_tasks ──► billing_queue
                                                                            ├─► inventory_queue
                                                                            └─► notification_queue
                                                     (DLX order_tasks_dlx ──► failed_tasks)
```

| Tipo | Servicios |
|---|---|
| Infraestructura | `postgres-db` (PostgreSQL 16 Alpine), `kafka` (Apache Kafka 3.7.0, KRaft), `rabbitmq` (3.13 management Alpine) |
| Microservicios (Java 21, Spring Boot 3.3.5) | `order-api`, `order-processor`, `billing-worker`, `inventory-worker`, `notification-worker`, `analytics-service`, `dashboard-aggregator` |

Contratos que **no cambian** en la variante AWS: topics `new_orders` (3 particiones, lo crea
`order-api`), `billing_events`, `inventory_events`, `notification_events` (1 partición, los crea cada
worker); grupos `processor-group`, `analytics-group`, `dashboard-group`; exchanges `order_tasks`
(fanout) y `order_tasks_dlx`; colas `*_queue` y `failed_tasks`; concurrencia 2 y prefetch 1 en los
workers; `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`. Todo eso está en el código Java, en los
`application.yml` y en `worker-comun.yml` (común a los workers), que no cambiaron para AWS (después solo se les retiró la contraseña por
defecto; ver §7.1).

---

## 2. Requisitos de AWS

| | Mínimo | Recomendado |
|---|---|---|
| Instancia | **t3.small** (2 GiB, 2 vCPU) | t3.medium (4 GiB) si las mediciones superan ~1,5 GiB |
| t3.micro (1 GiB) | **No viable**: solo los contenedores necesitan ~1,2-1,35 GiB | — |
| Swap | 2 GiB (ya configurado) | 2 GiB, `vm.swappiness` bajo (10-20) |
| Disco EBS | 16 GiB gp3 | 20 GiB gp3 (imágenes ~1,5 GiB descomprimidas + logs + datos) |
| SO / Docker | Ubuntu 24.04, Docker Engine ≥ 25 (`start_interval`), Compose V2 ≥ 2.20 | Docker 29.1.3 / Compose 2.40.3 (los instalados) |
| Arquitectura | x86_64 (las imágenes se construyen `amd64` en Windows) | — |

Memoria disponible real en t3.small: `MemTotal` ≈ 1,9 GiB; Ubuntu + `dockerd` + `containerd` +
`sshd` consumen ~200-300 MiB. Quedan **~1,6 GiB para contenedores** más el swap como colchón.

---

## 3. Límites de la etapa 7 frente a los actuales (AWS)

| Servicio | Etapa 7 RAM / CPU | Propuesta inicial | **AWS final** | Heap (etapa 7 → AWS) | Medido en reposo (etapa 7) |
|---|---:|---:|---:|---|---:|
| kafka | 1200M / 1.0 | 420M / 1.0 | **448M / 1.0** | 768m/Xms512m → **256m/Xms128m** | 302,5 MiB |
| order-api | 384M / 0.5 | 230M / 0.5 | **256M / 0.5** | 224m → **128m** (metaspace 128m) | 214,7 MiB |
| rabbitmq | 512M / 0.5 | 170M / 0.4 | **224M / 0.4** | — (watermark absoluto 168MiB) | 130,4 MiB |
| dashboard-aggregator | 384M / 0.5 | 170M / 0.4 | **192M / 0.4** | 224m → **96m** | 143,0 MiB |
| order-processor | 320M / 0.25 | 140M / 0.25 | **160M / 0.25** | 192m → **80m** | 128,1 MiB |
| billing-worker | 288M / 0.25 | 140M / 0.25 | **160M / 0.25** | 160m → **80m** | 126,6 MiB |
| inventory-worker | 288M / 0.25 | 140M / 0.25 | **160M / 0.25** | 160m → **80m** | 125,4 MiB |
| notification-worker | 288M / 0.25 | 140M / 0.25 | **160M / 0.25** | 160m → **80m** | 130,5 MiB |
| analytics-service | 320M / 0.5 | 140M / 0.25 | **160M / 0.25** | 192m → **80m** | 114,4 MiB |
| postgres-db | 384M / 0.5 | 110M / 0.3 | **128M / 0.3** | shared_buffers 128MB → **32MB** | 38,0 MiB |
| **Total** | **4368M / 4.5** | **~1800M / 3.85** | **2048M / 3.85** | | **1453,6 MiB** (`evidencias/e7/d-stats-limites.txt`) |

### Por qué se ajustaron algunos valores de la propuesta inicial

Una JVM ocupa `heap + metaspace + code cache + pilas de hilos + estructuras del GC + buffers
nativos`. Con Spring Boot la parte fuera del heap es de **~65-80 MB** en los workers y de
**~100-120 MB** en `order-api` (Hibernate). Regla aplicada: **límite ≥ -Xmx + fuera de heap**.

* **Workers con -Xmx96m y 140M**: 96 + ~70 = ~166 MB > 140M. Si el heap crece bajo carga, el
  contenedor llega al límite y usa swap o lo mata el OOM killer (exit 137). Se eligió **-Xmx80m
  con 160M** (80 + ~70 = ~150 MB < 160M). En reposo un worker usa ~30-40 MB de heap vivo: 80m sobra.
* **order-api con 230M y metaspace 96m**: Hibernate/JPA suele superar los 96 MB de metaspace
  (→ `OutOfMemoryError: Metaspace`). Se usa **metaspace 128m y límite 256M**; heap 128m.
* **dashboard-aggregator con -Xmx128m y 170M**: 128 + ~75 > 170. Se bajó a **-Xmx96m / 192M**.
  Su estado en memoria (órdenes del dashboard) es pequeño.
* **Kafka con -Xmx320m y 420M**: 320 + ~130 (G1, ~110 hilos, metaspace) ≈ 450 > 420, y además el
  healthcheck lanza **otra JVM dentro del mismo contenedor**. Se usa **-Xmx256m -Xms128m / 448M**
  y un heap propio de 48m para la JVM del healthcheck.
* **RabbitMQ con 170M**: en reposo ya usaba ~130 MiB, y su CLI de healthcheck (otra VM Erlang)
  corre dentro del contenedor. Se usa **224M** con un umbral de memoria absoluto (ver §5).
* **PostgreSQL 110M → 128M**: margen para autovacuum (`maintenance_work_mem`) y checkpoints.
  El uso real esperado es ~40 MiB.

### Presupuesto de RAM: el swap no sustituye a la RAM

| Concepto | MiB |
|---|---:|
| RAM de la t3.small (`MemTotal` real) | ~1900 |
| Ubuntu + `dockerd` + `containerd` + `sshd` + caché mínima | ~250-300 |
| **Disponible para contenedores** | **~1600-1650** |
| Consumo esperado de los 10 contenedores en reposo (§12, estimado) | ~1150-1350 |
| **Margen en RAM** | **~250-500** |
| Suma de límites (techos por contenedor) | 2048 |

* Lo que tiene que caber en la RAM es el **consumo real**, no la suma de límites. Cada límite es
  un techo individual: protege a los demás si un servicio se desboca, pero no reserva memoria.
* La suma (2048M) supera en ~400 MiB la RAM disponible. Esa diferencia **no** está pensada para
  vivir en el swap: solo se alcanzaría si varios servicios llegaran a su techo a la vez, algo que
  no ocurre con la carga del proyecto. No se bajó porque, por debajo de esos valores, cada
  límite dejaría de cubrir `heap + memoria fuera del heap` de su JVM (OOM kill garantizado bajo carga).
* El swap de 2 GiB es un **colchón para picos transitorios** (arranque de las JVM, un pico
  puntual). Docker permite a cada contenedor usar tanto swap como su límite (`memswap_limit` no
  definido), pero una JVM con el heap en swap tiene pausas de GC de segundos: si se usa de forma
  sostenida, el sistema "funciona" pero degradado.
* **Criterio de aceptación en la t3.small** (medir a los ~10 min y tras ~1 h, con órdenes enviadas):
  - total de `docker stats` ≤ ~1450 MiB;
  - `MemAvailable` ≥ ~250 MiB;
  - `vmstat 5 5` con `si`/`so` ≈ 0 (sin intercambio sostenido) y swap en uso estable < ~150 MiB.

  Si no se cumple, la instancia está justa: subir solo los límites que lo necesiten **no
  arregla la falta de RAM**; la solución es t3.medium (4 GiB) con los mismos archivos.
  `arrancar-aws.sh` hace esta comprobación al final y avisa si la RAM está justa.

---

## 4. Parámetros JVM (Java 21, Temurin JRE Alpine)

`JAVA_OPTS` de los siete microservicios (el `Dockerfile` los aplica con `exec java $JAVA_OPTS -jar app.jar`):

```
-Xmx<heap> -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -XX:TieredStopAtLevel=1 -Xss512k
-XX:MaxDirectMemorySize=32m -XX:+ExitOnOutOfMemoryError -Dspring.jmx.enabled=false
```

`order-api` usa `-XX:MaxMetaspaceSize=128m` y conserva `-Duser.language=es -Duser.country=CO`.

| Opción | Efecto | Riesgo / análisis |
|---|---|---|
| `-XX:+UseSerialGC` | GC de un hilo, mínima memoria auxiliar. | Con <2 CPU o <1792 MB la JVM **ya elegía SerialGC** (los contenedores tienen 0.25-0.5 CPU), así que no cambia el comportamiento medido; se fija para no depender de la detección. Pausas de pocos ms con heaps de 80-128 MB. |
| `-XX:TieredStopAtLevel=1` | Solo compilador C1. | Menos code cache (~8-15 MB menos) y menos CPU de JIT (ahorra créditos T3). El código caliente es ~20-40 % más lento que con C2: irrelevante para unas pocas órdenes por segundo. El arranque es igual o más rápido. |
| `-XX:MaxMetaspaceSize=96m` / `128m` | Techo de metaspace. | Spring Boot + Kafka + AMQP carga ~55-75 MB de clases; 96m deja margen. Hibernate (order-api) puede pasar de 96m: por eso 128m. Si se supera → `OutOfMemoryError: Metaspace` → la JVM sale (ver `ExitOnOutOfMemoryError`) y el fallo es visible. |
| `-XX:MaxDirectMemorySize=32m` | Techo de buffers directos NIO. | Tomcat usa buffers de heap por defecto; el cliente AMQP y el driver JDBC usan sockets bloqueantes; el cliente Kafka usa buffers de heap (la JDK solo cachea pequeños buffers directos temporales). Con mensajes de pocos KB, 32m sobra. Si se agotara → `OutOfMemoryError: Direct buffer memory`, visible en el log. |
| `-Xss512k` | Pila de 512 KB por hilo (defecto 1 MB). | Ahorro pequeño (la pila se reserva, no se ocupa entera). No hay recursión profunda en el código (validaciones, Jackson con DTO planos). Riesgo bajo de `StackOverflowError`. |
| `-XX:+ExitOnOutOfMemoryError` | **Añadida.** Ante cualquier OOM la JVM termina. | Con `restart: unless-stopped` el contenedor reinicia y `RestartCount` sube: el fallo **no se oculta**. Sin ella, un listener podría quedar muerto con el proceso vivo. |
| `-Dspring.jmx.enabled=false` | JMX de Spring apagado. | Ya es el valor por defecto en Spring Boot 3: inocuo, se deja explícito. |

Kafka: `KAFKA_HEAP_OPTS="-Xmx256m -Xms128m"`. Se mantiene el G1 que configuran los scripts de
Kafka (`KAFKA_JVM_PERFORMANCE_OPTS` sin tocar). Un único broker con 4 topics de usuario y
`__consumer_offsets` tiene un conjunto vivo de pocas decenas de MB. `-Xms128m` (en lugar de
`Xms=Xmx`) deja que G1 se quede cerca de 128m en reposo y crezca si hace falta.

---

## 5. Kafka, RabbitMQ y PostgreSQL

### Kafka
* Listeners **sin cambios** (`INTERNAL://kafka:9092`, `EXTERNAL://localhost:29092`, `CONTROLLER :9093`).
  EXTERNAL sigue definido pero sin publicar: inalcanzable desde fuera de Docker. No se eliminó
  para no tocar la configuración de mensajería; quitarlo más adelante sería opcional.
* Healthcheck: **la misma comprobación** que en la etapa 7 (`kafka-topics.sh --list`). Problema
  detectado: esa herramienta es una JVM que corre dentro del cgroup de `kafka` y heredaba
  `KAFKA_HEAP_OPTS` del broker. En AWS se ejecuta con `-Xmx48m`, SerialGC y C1, y cada 30 s en
  lugar de cada 10 s (cada 10 s durante el arranque, con `start_interval`).
* Sin volumen (igual que en la etapa 7): topics y offsets viven en el contenedor. Sobreviven a
  `stop`/`start` y a reinicios de EC2, pero **no a `docker compose down`**.

### RabbitMQ
* `RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS="+S 1:1 +sbwt none +sbwtdcpu none +sbwtdio none"`:
  variable soportada por los scripts de RabbitMQ 3.13 (no es de las variables obsoletas de la
  imagen Docker). `+S 1:1` = 1 planificador; `+sbwt* none` desactiva la espera activa, que en
  reposo consume CPU (créditos T3). **No afecta** a la concurrencia de los listeners de Spring
  (concurrency 2 y prefetch 1 siguen igual): esos son hilos Java en los workers, no planificadores
  Erlang.
* **Umbral de memoria absoluto** (`ops/aws/rabbitmq/20-aws.conf`, montado en
  `/etc/rabbitmq/conf.d/`): `vm_memory_high_watermark.absolute = 168MiB`. Con el umbral relativo
  por defecto (0,4) RabbitMQ lo calcularía sobre la RAM del host (~780 MB, nunca se dispararía
  antes del límite de 224M) o sobre el límite del contenedor (~90 MB, **por debajo de su consumo
  normal**: alarma permanente y `order-processor` bloqueado al publicar). El valor absoluto evita
  ambos casos. Si el montaje falta, compose falla (`create_host_path: false`) en vez de arrancar mal.
* Comprobar en EC2: `docker exec rabbitmq rabbitmq-diagnostics -q alarms` (debe estar vacío) y
  `docker exec rabbitmq rabbitmq-diagnostics -q memory_breakdown --unit mb`.

### PostgreSQL 16 Alpine
`command: postgres -c shared_buffers=32MB -c max_connections=20 -c work_mem=2MB -c maintenance_work_mem=16MB`

* Son parámetros de **ejecución**: no modifican ni reinicializan los datos del volumen
  `ecommerce-hibrido_postgres-data`; quitarlos devuelve los valores por defecto.
* Único cliente: Hikari de `order-api` con un máximo de 3 conexiones, más `pg_isready` y alguna
  sesión `psql` manual. 20 conexiones sobran.
* Tablas pequeñas (`ordenes`, `items_orden`): 32 MB de `shared_buffers` y 2 MB de `work_mem` bastan.

### Spring Boot (por variables de entorno, sin tocar `application.yml`)
| Variable | Servicio | Valor | Análisis |
|---|---|---|---|
| `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | order-api | 3 | Cada `POST /order` es una transacción corta. Con más peticiones simultáneas que conexiones, esperan (timeout 30 s), no fallan. |
| `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE` | order-api | 1 | Una conexión en reposo. |
| `SERVER_TOMCAT_THREADS_MAX` | order-api, dashboard | 15 | `ops/enviar-ordenes.ps1` envía en secuencia. Los WebSocket del dashboard no ocupan un hilo mientras están abiertos (NIO). |
| `SERVER_TOMCAT_THREADS_MIN_SPARE` | order-api, dashboard | 2 | Menos hilos en reposo (defecto 10). |

---

## 6. Puertos

| Puerto | Servicio | Etapa 7 | Actual (AWS) |
|---|---|---|---|
| 8080 | order-api (`POST /order`) | publicado | **publicado** |
| 8081 | dashboard-aggregator (página + `/ws/ordenes`) | publicado | **publicado** |
| 15672 | consola RabbitMQ | publicado | **solo `127.0.0.1` de EC2** (acceso por túnel SSH) |
| 5433→5432 | PostgreSQL | publicado | **interno** (`postgres-db:5432`) |
| 9092 / 29092 | Kafka | publicados | **internos** |
| 5672 | RabbitMQ AMQP | publicado | **interno** |

El dashboard calcula las URL a partir de `window.location.hostname`
(`http://<host>:8080/order`, `ws://<host>:8081/ws/ordenes`), así que funciona con la IP pública
de EC2 sin recompilar. Los orígenes permitidos (CORS de la API y WebSocket del panel) se definen con `APP_ORIGENES` en `.env`;
en EC2 debe ser `APP_ORIGENES=http://<IP_PUBLICA_EC2>:8081`.

### RabbitMQ Management por túnel SSH
La consola da acceso de administrador con el usuario `ecommerce`, así que **no se publica en ninguna interfaz
externa**: `docker-compose.yml` la publica como `127.0.0.1:15672:15672`, es decir, solo en el
loopback de la instancia. Aunque el Security Group tuviera 15672 abierto, no sería alcanzable
desde Internet. `arrancar-aws.sh` lo comprueba tras la tanda 1 (`docker port rabbitmq 15672`) y
se detiene si aparece publicado en otra dirección.

Acceso desde tu computadora:

```powershell
# [Windows PowerShell] abre el túnel; la ventana queda "colgada" mientras el túnel está activo
ssh -i <RUTA_CLAVE.pem> -N -L 15672:127.0.0.1:15672 ubuntu@<IP_PUBLICA_EC2>
```
Después, en el navegador del PC: `http://localhost:15672` (usuario `ecommerce`). El túnel viaja
cifrado por el puerto 22, que ya está permitido para tu IP. Se cierra con `Ctrl+C`.

* Si en el PC el 15672 local está ocupado (p. ej., el proyecto levantado también
  en Docker Desktop, que publica 127.0.0.1:15672 en Windows), usa otro puerto local: `-L 25672:127.0.0.1:15672` y abre
  `http://localhost:25672`.
* En el Security Group, **elimina la regla de 15672** si existe: ya no hace falta.
* Comprobación en Ubuntu: `docker port rabbitmq` debe mostrar solo `15672/tcp -> 127.0.0.1:15672`,
  y `curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:15672/` debe devolver `200`.

---

## 7. Seguridad: Docker, nftables y Security Group

* **Los puertos publicados por Docker NO pasan por la cadena `input` de nftables.** Docker
  redirige el tráfico con DNAT en `prerouting` y lo filtra en `forward` (cadenas `DOCKER`,
  `DOCKER-USER`). Una regla nftables en `input` **no bloquea** un puerto publicado. Opciones
  efectivas: el **Security Group** (primera barrera), publicar en `127.0.0.1` (lo que se hace
  con 15672), o reglas en `DOCKER-USER`. 8080 y 8081 dependen del Security Group.
* Docker 29 usa por defecto el backend iptables (en Ubuntu 24.04, `iptables-nft`, que escribe en
  tablas nftables). **No uses `flush ruleset`** en `/etc/nftables.conf`: si `nftables.service` se
  recarga después de Docker, borra sus reglas y los contenedores pierden red y puertos. Usa tablas
  propias (`table inet filtro { ... }`) sin vaciar las de Docker. Tras recargar nftables, verificar
  con `sudo nft list ruleset | grep -c DOCKER` y, si hace falta, `sudo systemctl restart docker`.
* Security Group recomendado: 22 (tu IP), 8080 y 8081 (tu IP o el rango de quien evalúe).
  **15672 cerrado** (se usa el túnel). Nunca 5432/5433, 9092, 29092, 5672.
* Las contraseñas de PostgreSQL y RabbitMQ **no están en el repositorio**: se leen de `.env`
  (ver §7.1). La contraseña usada en versiones anteriores sí está en el historial público de Git,
  por eso debe cambiarse en los servicios (no basta con quitarla de los archivos).

### 7.1 Credenciales (`.env`)

| Variable | Servidor que la define | Clientes que la usan |
|---|---|---|
| `POSTGRES_PASSWORD` | `postgres-db` (`POSTGRES_PASSWORD`) | `order-api` (`DB_PASSWORD`) |
| `RABBITMQ_PASSWORD` | `rabbitmq` (`RABBITMQ_DEFAULT_PASS`; consola 15672) | `order-processor`, `billing-worker`, `inventory-worker`, `notification-worker` (`SPRING_RABBITMQ_PASSWORD`) |

* Una sola variable por contraseña: servidor y clientes no pueden quedar con valores distintos.
* Son **obligatorias** (`${VAR:?...}`): sin ellas `docker compose` se niega a arrancar.
* `.env` va junto a `docker-compose.yml`, está ignorado por Git (`.gitignore`) y por Docker
  (`.dockerignore`). Plantilla versionada sin valores: `.env.example`.
* En EC2: `cp .env.example .env && chmod 600 .env` y editarlo con `nano` (no con `echo` ni
  `export`, que dejan el valor en el historial de la shell). Valores con letras, números, `-` y `_`;
  sin `$`, `#`, comillas ni espacios; finales de línea **LF**. `arrancar-aws.sh` comprueba que
  `.env` existe, tiene LF, ambas variables tienen valor y avisa si los permisos no son 600/400,
  sin mostrar ningún valor.
* **Cambiar un valor en `.env` no cambia la contraseña de un servicio que ya tiene datos**:
  `POSTGRES_PASSWORD` solo se aplica al inicializar un volumen vacío y `RABBITMQ_DEFAULT_PASS`
  solo si el nodo no tiene base de datos. La rotación se hace primero dentro del servicio
  (`\password ecommerce` en `psql`, `rabbitmqctl change_password`) y en orden; se documentará
  en la fase de migración.
* No ejecutar `docker compose config` sin `--quiet` (imprime los valores resueltos) ni compartir
  la salida de `docker inspect` de los contenedores (incluye las variables de entorno).
* `ops/aws/detener-aws.sh` funciona aunque falte `.env` (usa valores de relleno que nunca llegan
  a ningún contenedor, porque `stop`/`down` no crean ni recrean nada).
  `ops/aws/exportar-imagenes.ps1` tampoco necesita `.env` en Windows.
* Los `application.yml` y `worker-comun.yml` ya no tienen contraseña por defecto (`${DB_PASSWORD:}`,
  `${RABBITMQ_PASSWORD:}`). En Docker no cambia nada: la variable del compose tiene prioridad.
  Las imágenes ya cargadas en EC2 conservan el valor antiguo dentro del jar hasta que se
  reconstruyan, pero no lo usan. La contraseña antigua sigue en el historial de Git: **no debe
  usarse en el `.env` de EC2**.
* Los microservicios corren con usuario sin privilegios (`ecommerce`), como en la etapa 7.

---

## 8. Construir y exportar las imágenes en Windows

Las imágenes se construyen en Windows (no se compila Maven en EC2). Los nombres reales,
comprobados en `evidencias/e7/c-imagenes.txt` y con `docker compose config --images`, son
`ecommerce-hibrido-<modulo>:latest`, que es exactamente lo que declara `docker-compose.yml`.

```powershell
# [Windows PowerShell] desde C:\Projects\Ecommerce-Hibrido (Docker Desktop abierto)
docker compose build
powershell -ExecutionPolicy Bypass -File ops\aws\exportar-imagenes.ps1 -IpServidor <IP> -RutaClave <RUTA.pem>
```

El script verifica las 7 imágenes (existencia, etiqueta, `amd64`), que coinciden con el compose,
hace **un único `docker save` de las 7** (las capas de `eclipse-temurin:21-jre-alpine` se guardan
una sola vez), comprime con `GZipStream` de .NET (no necesita gzip ni 7-Zip), genera el SHA-256 y
muestra los tamaños **medidos** y los comandos siguientes. Salida por defecto:
`%USERPROFILE%\ecommerce-aws-export\` (fuera del repo, para no versionar cientos de MB).

Tamaño: no se ha medido todavía. Referencia: las 7 imágenes suman ~2,56 GB de "disk usage" y
~0,79 GB de "content size", con la capa base compartida; el `.tar.gz` debería quedar bastante por
debajo de esa suma, pero la cifra real la da el script.

## 9. Transferencia por SCP e importación en Ubuntu

```powershell
# [Windows PowerShell]
scp -i <RUTA_CLAVE.pem> "$env:USERPROFILE\ecommerce-aws-export\ecommerce-imagenes.tar.gz" "$env:USERPROFILE\ecommerce-aws-export\ecommerce-imagenes.tar.gz.sha256" ubuntu@<IP_PUBLICA_EC2>:/home/ubuntu/
```
```bash
# [Ubuntu por SSH]
cd /home/ubuntu && sha256sum -c ecommerce-imagenes.tar.gz.sha256
docker load -i /home/ubuntu/ecommerce-imagenes.tar.gz           # acepta gzip directamente
docker images --format '{{.Repository}}:{{.Tag}} {{.Size}}' | grep ecommerce-hibrido-
cd ~/ecommerce-hibrido && docker compose pull postgres-db kafka rabbitmq
```

### Transferir los archivos de configuración
Opción A (recomendada si `/home/ubuntu/ecommerce-hibrido` es un clon Git): hacer commit y push en
Windows y `git pull` en Ubuntu. El `.gitattributes` de `ops/aws/` garantiza LF.

Opción B (SCP de los archivos de configuración):
```powershell
# [Windows PowerShell] desde C:\Projects\Ecommerce-Hibrido
scp -i <RUTA_CLAVE.pem> docker-compose.yml .env.example ubuntu@<IP>:/home/ubuntu/ecommerce-hibrido/
scp -i <RUTA_CLAVE.pem> -r ops/aws ubuntu@<IP>:/home/ubuntu/ecommerce-hibrido/ops/
scp -i <RUTA_CLAVE.pem> docs/despliegue-aws.md ubuntu@<IP>:/home/ubuntu/ecommerce-hibrido/docs/
```
```bash
# [Ubuntu] comprobar finales de línea y validar
cd ~/ecommerce-hibrido && file ops/aws/*.sh ops/aws/rabbitmq/20-aws.conf docker-compose.yml
# si aparece "CRLF" en .sh o .conf: sed -i 's/\r$//' ops/aws/*.sh ops/aws/rabbitmq/20-aws.conf
# (docker-compose.yml admite CRLF sin problema)
# .env: se crea en EC2 desde .env.example (nunca se copia por SCP ni se versiona). Ver §7.1.
docker compose config --quiet && echo OK     # --quiet: no imprime las contraseñas
```

---

## 10. Arranque por etapas

```bash
# [Ubuntu]
cd ~/ecommerce-hibrido && bash ops/aws/arrancar-aws.sh      # requiere .env (§7.1)
```

| Tanda | Servicios | Condición para continuar | Motivo |
|---|---|---|---|
| 1 | postgres-db, kafka, rabbitmq | los 3 `healthy` (máx. 180 s) | base de todo |
| 2 | order-api | `healthy` | crea `new_orders` con **3 particiones** |
| 3 | dashboard-aggregator, analytics-service | dashboard `healthy`; analytics con log `Started` | consumen `new_orders` (ya existe) y topics de resultados (el dashboard los detecta a los 10 s con `metadata.max.age.ms`) |
| 4 | billing/inventory/notification-worker | log `Started` | **declaran sus colas** y enlaces a `order_tasks`; crean sus topics |
| 5 | order-processor | log `Started` | **siempre al final**: `order_tasks` es fanout y **descarta** lo publicado si aún no hay colas enlazadas (`order-processor` solo declara el exchange) |

Verificado contra el código y los `depends_on` (idénticos a la etapa 7). El script:
comprueba Docker, compose válido, CRLF, la conf de RabbitMQ y que **las 10 imágenes existen**
(falla con instrucciones si no); lanza cada tanda con `up -d --no-build`; espera por estado real
(healthcheck o la línea `Started ... in ... seconds` **desde el último arranque**), no por esperas
fijas; aborta si un contenedor sale, lo mata el OOM killer, reinicia o queda `unhealthy`, y
entonces muestra estado, `OOMKilled`, salida del healthcheck y las últimas 80 líneas de log;
muestra `free -h`, `docker stats` y `docker compose ps` entre tandas; tras la tanda 1 verifica
que 15672 está publicado **solo** en `127.0.0.1`; al final evalúa la RAM disponible y el swap en
uso y avisa si la instancia está justa (§3). Nunca borra nada.
Tiempos ajustables: `TIMEOUT_INFRA=240 TIMEOUT_APP=300 bash ops/aws/arrancar-aws.sh`.

### Healthchecks (etapa 7 → AWS)
| Servicio | Etapa 7 | AWS | Detección de un fallo real tras el arranque |
|---|---|---|---|
| postgres-db | 5s, 10 reintentos | 5s, 20 reintentos, start_period 30s | ~100 s |
| kafka | 10s, 12 reint., sp 15s | 30s (10s al arrancar), 5 reint., sp 90s | ~150 s |
| rabbitmq | 10s, 10 reint., sp 20s | 30s (10s al arrancar), 5 reint., sp 90s | ~150 s |
| order-api | 5s, 30 reint., sp 20s | 10s (5s al arrancar), 6 reint., sp 120s | ~60 s |
| dashboard-aggregator | 5s, 30 reint., sp 20s | 10s (5s al arrancar), 6 reint., sp 120s | ~60 s |

`start_period` amplio no retrasa el estado `healthy` (el primer chequeo correcto lo marca sano);
solo tolera fallos mientras la CPU T3 arranca las JVM. Después, los reintentos son pocos para no
ocultar fallos reales. Se descartó la propuesta "retries 60" (5 minutos sin detectar una caída).

---

## 11. Verificación funcional

```bash
# [Ubuntu] API: una orden válida -> 202 con estado CREADA
curl -s -i -X POST http://localhost:8080/order -H 'Content-Type: application/json' \
  -d '{"cliente":"Cliente Demo","items":[{"sku":"LIB-001","nombre":"Libro","cantidad":1,"precioUnitario":150000}]}'
# validación -> 400 con mensaje en español
curl -s -X POST http://localhost:8080/order -H 'Content-Type: application/json' \
  -d '{"cliente":"Cliente Demo","items":[{"sku":"LIB-001","nombre":"Libro","cantidad":0,"precioUnitario":150000}]}'
```

Dashboard: `http://<IP_PUBLICA_EC2>:8081` en el navegador; la orden debe pasar a
`PAYMENT_*`, `STOCK_*`, `EMAIL_*`. Desde Windows también sirve `ops\enviar-ordenes.ps1` cambiando
la URL base a la IP pública.

### Flujo de eventos
```bash
docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic new_orders   # 3 particiones
docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups # LAG 0
docker exec rabbitmq rabbitmqctl list_queues name messages consumers   # 3 colas con 2 consumidores cada una
docker exec rabbitmq rabbitmqctl list_exchanges name type | grep order_tasks
docker exec postgres-db psql -U ecommerce -d ecommerce -c 'select estado, count(*) from ordenes group by estado;'
docker exec analytics-service tail -n 3 /app/datos/ordenes.log
docker logs --tail 20 order-processor
```

---

## 12. Monitoreo de memoria, CPU y swap

```bash
free -h
docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.CPUPerc}}'
docker stats --no-stream --format '{{.MemUsage}}' | awk '{v=$1; if (v ~ /GiB/) {sub(/GiB/,"",v); v*=1024} else sub(/MiB/,"",v); t+=v} END {printf "Total contenedores: %.0f MiB\n", t}'
vmstat 5 5                                   # si/so > 0 de forma sostenida = se está usando swap
docker inspect -f '{{.Name}} OOM={{.State.OOMKilled}} reinicios={{.RestartCount}}' $(docker ps -aq)
sudo dmesg -T | grep -i -E 'oom|killed process' | tail
docker logs order-api 2>&1 | grep -i -E 'OutOfMemoryError|Metaspace|Direct buffer'
```

### Estimación de consumo en reposo (no medida)
| Servicio | Medido etapa 7 | Estimado AWS |
|---|---:|---:|
| 5 × JVM sin web (workers, processor, analytics) | 625 MiB | 480-550 MiB |
| order-api | 215 MiB | 165-195 MiB |
| dashboard-aggregator | 143 MiB | 105-125 MiB |
| kafka | 303 MiB | 250-330 MiB |
| rabbitmq | 130 MiB | 100-125 MiB |
| postgres-db | 38 MiB | 35-45 MiB |
| **Total** | **1454 MiB** | **~1150-1350 MiB** (centro ~1250) |

El objetivo de ~1210 MiB es alcanzable pero **no está garantizado**. Kafka (G1 crece con el
tiempo) y RabbitMQ (consola de gestión) son los valores más inciertos. Se debe medir a los
~10 minutos de arrancar, tras enviar algunas órdenes, y otra vez pasada una hora.

---

## 13. Créditos de CPU T3

* t3.small: línea base del **20 % por vCPU** (0,4 vCPU sostenidos en total). Por encima se
  gastan créditos; acumula 24 créditos/hora.
* Las T3 se lanzan por defecto en modo **unlimited**: al agotar créditos no se ralentiza, **se
  cobra** el exceso. En modo **standard** se limita al 20 % y todo (arranques, healthchecks)
  se vuelve mucho más lento. Revisar en la consola: *EC2 → instancia → Acciones → Configuración
  de instancias → Cambiar la especificación de crédito*. Vigilar `CPUCreditBalance` y
  `CPUSurplusCreditBalance` en CloudWatch.
* El arranque de 7 JVM es el mayor consumo de créditos: por eso las tandas, C1 en lugar de C2 y
  healthchecks JVM/Erlang menos frecuentes. En reposo, Kafka es el mayor consumidor de CPU
  (en la medición de la etapa 7 llegó a 41 % en una instantánea, probablemente durante su healthcheck).
* **Reinicio de EC2:** con `restart: unless-stopped`, Docker relanza a la vez todos los
  contenedores que estaban en marcha (sin tandas ni `depends_on`). Topics y colas ya existen,
  así que no hay pérdida de eventos por orden, pero sí un pico de CPU de varios minutos. Para un
  reinicio controlado: `bash ops/aws/detener-aws.sh`, reiniciar, y `bash ops/aws/arrancar-aws.sh`.

---

## 14. Detener y reiniciar

```bash
bash ops/aws/detener-aws.sh          # stop en orden inverso; conserva contenedores y volúmenes
bash ops/aws/arrancar-aws.sh         # vuelve a arrancar por tandas (reutiliza los contenedores)
bash ops/aws/detener-aws.sh --down   # elimina contenedores y red, NUNCA volúmenes (sin -v)
docker compose restart order-api     # reiniciar un servicio concreto
```

`stop` conserva también los topics de Kafka y las colas de RabbitMQ (viven en el contenedor);
`--down` los pierde y se recrean al arrancar. **Nunca** usar `down -v` (borraría
`ecommerce-hibrido_postgres-data` y `analytics-datos`).

---

## 15. Reversión si la optimización falla

La optimización solo afecta a `docker-compose.yml`, `ops/aws/` y esta guía; el código Java, los
`application.yml`, `pom.xml` y el `Dockerfile` no se tocaron. Punto de restauración de la
configuración de la etapa 7: commit `a0b8dbb`.

| Síntoma | Ajuste (solo en `docker-compose.yml`) |
|---|---|
| `OOMKilled=true` / exit 137 en un servicio | subir su `memory` 32-64M |
| `OutOfMemoryError: Metaspace` | subir `MaxMetaspaceSize` a 128m (o 160m en order-api) |
| `OutOfMemoryError: Java heap space` | subir `-Xmx` 16-32m **y** el límite en la misma cantidad |
| Alarma de memoria en RabbitMQ | subir `vm_memory_high_watermark.absolute` y el límite |
| Arranque lento / timeouts del script | `TIMEOUT_APP=420`, o subir `cpus` temporalmente |
| No cabe en 2 GiB | t3.medium con los mismos archivos, o los límites de la etapa 7 |

Recuperar la configuración de la etapa 7 (solo en una máquina con más RAM: sus límites suman
4368M; mismos volúmenes y mismo nombre de proyecto):
```bash
git show a0b8dbb:docker-compose.yml > /tmp/docker-compose.etapa7.yml   # sin tocar el archivo actual
bash ops/aws/detener-aws.sh
docker compose -p ecommerce-hibrido -f /tmp/docker-compose.etapa7.yml up -d --no-build --wait
```
(La etapa 7 no declara `image:`, pero compose usa por defecto el mismo nombre
`ecommerce-hibrido-<servicio>`, así que reutiliza las imágenes cargadas. `-p` fija el nombre de
proyecto, porque el archivo fuera de la carpeta del proyecto tomaría el nombre `tmp`. Publica de
nuevo 5433, 9092, 29092 y 5672.)

Revertir los archivos en Windows: `git revert <commit>` del cambio correspondiente, o
`git checkout a0b8dbb -- docker-compose.yml` para recuperar solo el Compose de la etapa 7.

---

## Anexo: diferencias con la configuración de la etapa 7

Comparación entre el `docker-compose.yml` actual y el de la etapa 7 (`git show a0b8dbb:docker-compose.yml`),
sobre el modelo resuelto por `docker compose config` (`--format json`):

* **Iguales:** los 10 servicios y sus `container_name`, `build` (contexto, Dockerfile, `MODULO`),
  `depends_on`, redes (`ecommerce-net`), volúmenes (`postgres-data`, `analytics-datos`),
  `restart: unless-stopped`, toda la configuración de Kafka excepto el heap, usuarios,
  `KAFKA_BOOTSTRAP`, `SPRING_RABBITMQ_*`, `DB_*`, `CORS_ORIGINS`, `ARCHIVO_ORDENES`.
* **Nuevo nivel superior:** `name: ecommerce-hibrido` (fija el nombre de proyecto: volúmenes e
  imágenes no dependen del nombre de la carpeta) y `x-logging` (json-file 10m × 3 en los 10 servicios).
* **Microservicios:** `image: ecommerce-hibrido-<modulo>:latest`, `pull_policy: never`,
  `JAVA_OPTS` (§4), límites (§3), Hikari/Tomcat en order-api y Tomcat en dashboard (§5),
  healthchecks de order-api y dashboard (§10). analytics-service baja de 0.5 a 0.25 CPU.
* **Kafka:** sin `ports`; `KAFKA_HEAP_OPTS`; healthcheck con heap propio y 30 s; límite 448M.
* **RabbitMQ:** solo 15672 publicado, y únicamente en `127.0.0.1` (túnel SSH); `RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS`;
  montaje de `20-aws.conf`; healthcheck 30 s; límite 224M / 0.4 CPU.
* **PostgreSQL:** sin `ports`; `command` con 4 parámetros; healthcheck 20 reintentos; 128M / 0.3 CPU.
* **Credenciales:** en la etapa 7 estaban escritas en el archivo; ahora `POSTGRES_PASSWORD` y
  `RABBITMQ_PASSWORD` llegan de `.env` y son obligatorias (§7.1).
