#!/usr/bin/env bash
# arrancar-aws.sh - Arranque POR TANDAS de los diez contenedores en AWS EC2 (Ubuntu 24.04).

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARCHIVO_COMPOSE="$RAIZ/docker-compose.yml"
COMPOSE=(docker compose -f "$ARCHIVO_COMPOSE")

TIMEOUT_INFRA="${TIMEOUT_INFRA:-180}"
TIMEOUT_APP="${TIMEOUT_APP:-240}"
INTERVALO=5

INFRA=(postgres-db kafka rabbitmq)
MICROSERVICIOS=(order-api dashboard-aggregator analytics-service billing-worker inventory-worker notification-worker order-processor)
IMAGENES_INFRA=(postgres:16-alpine apache/kafka:3.7.0 rabbitmq:3.13-management-alpine)

PATRON_STARTED='Started [^ ]+ in [0-9.,]+ seconds'

if [ -t 1 ]; then C_AZUL=$'\e[36m'; C_VERDE=$'\e[32m'; C_ROJO=$'\e[31m'; C_AMAR=$'\e[33m'; C_FIN=$'\e[0m'
else C_AZUL=''; C_VERDE=''; C_ROJO=''; C_AMAR=''; C_FIN=''; fi

titulo() { printf '\n%s== %s%s\n' "$C_AZUL" "$*" "$C_FIN"; }
ok()     { printf '%s  OK  %s%s\n' "$C_VERDE" "$*" "$C_FIN"; }
aviso()  { printf '%s  !!  %s%s\n' "$C_AMAR" "$*" "$C_FIN"; }
ahora()  { date +%s; }

fallar() {
    printf '\n%sERROR: %s%s\n' "$C_ROJO" "$*" "$C_FIN" >&2
    printf '%sLa secuencia se detuvo. No se borro nada. Para parar lo arrancado: bash ops/aws/detener-aws.sh%s\n' "$C_ROJO" "$C_FIN" >&2
    exit 1
}

mostrar_recursos() {
    echo "--- free -h"
    free -h
    echo "--- docker stats (instantanea)"
    docker stats --no-stream --format 'table {{.Name}}\t{{.MemUsage}}\t{{.MemPerc}}\t{{.CPUPerc}}\t{{.PIDs}}' || true
    echo "--- docker compose ps"
    "${COMPOSE[@]}" ps --format 'table {{.Name}}\t{{.Status}}\t{{.Ports}}' || true
}

diagnosticar() {
    local c="$1"
    printf '\n%s----- Diagnostico de %s -----%s\n' "$C_AMAR" "$c" "$C_FIN"
    docker inspect -f 'Estado={{.State.Status}} ExitCode={{.State.ExitCode}} OOMKilled={{.State.OOMKilled}} Reinicios={{.RestartCount}} Error="{{.State.Error}}"' "$c" 2>/dev/null || echo "(el contenedor $c no existe)"
    docker inspect -f '{{if .State.Health}}{{range .State.Health.Log}}healthcheck exit={{.ExitCode}} {{.Output}}{{end}}{{end}}' "$c" 2>/dev/null | tail -n 5 || true
    printf '%s----- Ultimas 80 lineas de log de %s -----%s\n' "$C_AMAR" "$c" "$C_FIN"
    docker logs --tail 80 "$c" 2>&1 || true
    echo "------------------------------------------"
    mostrar_recursos
}

estado()     { docker inspect -f '{{.State.Status}}' "$1" 2>/dev/null || echo "inexistente"; }
salud()      { docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' "$1" 2>/dev/null || echo "inexistente"; }
reinicios()  { docker inspect -f '{{.RestartCount}}' "$1" 2>/dev/null || echo 0; }
inicio_iso() { docker inspect -f '{{.State.StartedAt}}' "$1" 2>/dev/null || echo ""; }

started_en_log() {
    local c="$1" desde
    desde="$(inicio_iso "$c")"
    [ -n "$desde" ] || { echo 0; return; }
    docker logs --since "$desde" "$c" 2>&1 | grep -cE "$PATRON_STARTED" || true
}

esperar_listos() {
    local timeout="$1"; shift
    local servicios=("$@")
    local limite=$(( $(ahora) + timeout ))
    declare -A reinicios_ini=()
    declare -A listo=()
    local s

    for s in "${servicios[@]}"; do reinicios_ini[$s]="$(reinicios "$s")"; done

    while :; do
        local pendientes=0
        for s in "${servicios[@]}"; do
            [ "${listo[$s]:-0}" = 1 ] && continue
            local st sa r
            st="$(estado "$s")"; sa="$(salud "$s")"; r="$(reinicios "$s")"

            case "$st" in
                exited|dead|inexistente) diagnosticar "$s"; fallar "$s termino o no existe (estado: $st)." ;;
            esac
            if [ "$r" != "${reinicios_ini[$s]}" ]; then
                diagnosticar "$s"; fallar "$s se reinicio durante el arranque (reinicios: ${reinicios_ini[$s]} -> $r). Posible OOM o error de arranque."
            fi
            if [ "$sa" = "unhealthy" ]; then
                diagnosticar "$s"; fallar "$s esta unhealthy."
            fi

            if [ "$sa" = "healthy" ]; then
                listo[$s]=1; ok "$s healthy"
            elif [ "$sa" = "sin-healthcheck" ] && [ "$st" = "running" ] && [ "$(started_en_log "$s")" -gt 0 ]; then
                listo[$s]=1; ok "$s arrancado (log 'Started ...')"
            else
                pendientes=$(( pendientes + 1 ))
            fi
        done

        [ "$pendientes" -eq 0 ] && return 0

        if [ "$(ahora)" -ge "$limite" ]; then
            for s in "${servicios[@]}"; do
                [ "${listo[$s]:-0}" = 1 ] || diagnosticar "$s"
            done
            fallar "Tiempo agotado (${timeout}s) esperando: $(for s in "${servicios[@]}"; do [ "${listo[$s]:-0}" = 1 ] || printf '%s ' "$s"; done)"
        fi
        sleep "$INTERVALO"
    done
}

tanda() {
    local numero="$1" timeout="$2"; shift 2
    local servicios=("$@")
    titulo "Tanda $numero: ${servicios[*]}"
    local t0; t0="$(ahora)"
    "${COMPOSE[@]}" up -d --no-build "${servicios[@]}" \
        || { for s in "${servicios[@]}"; do diagnosticar "$s"; done; fallar "docker compose up fallo en la tanda $numero."; }
    esperar_listos "$timeout" "${servicios[@]}"
    ok "Tanda $numero lista en $(( $(ahora) - t0 )) s"
    mostrar_recursos
}

titulo "Comprobaciones previas"

command -v docker >/dev/null 2>&1 || fallar "docker no esta instalado."
docker info >/dev/null 2>&1      || fallar "El demonio de Docker no responde (sudo systemctl status docker) o el usuario no esta en el grupo docker."
docker compose version >/dev/null 2>&1 || fallar "Docker Compose V2 no esta disponible."
[ -f "$ARCHIVO_COMPOSE" ]        || fallar "No existe $ARCHIVO_COMPOSE"
[ -f "$RAIZ/ops/aws/rabbitmq/20-aws.conf" ] || fallar "Falta ops/aws/rabbitmq/20-aws.conf (lo monta rabbitmq)."

tiene_crlf() { [ "$(tr -cd '\r' < "$1" | wc -c)" -gt 0 ]; }

if tiene_crlf "$RAIZ/ops/aws/rabbitmq/20-aws.conf"; then
    fallar "ops/aws/rabbitmq/20-aws.conf tiene finales de linea CRLF. Corrige con: sed -i 's/\r\$//' ops/aws/rabbitmq/20-aws.conf ops/aws/*.sh"
fi

ARCHIVO_ENV="$RAIZ/.env"
if [ -f "$ARCHIVO_ENV" ]; then
    if tiene_crlf "$ARCHIVO_ENV"; then
        fallar ".env tiene finales de linea CRLF. Corrige con: sed -i 's/\r\$//' .env"
    fi
    for var in POSTGRES_PASSWORD RABBITMQ_PASSWORD; do
        grep -Eq "^${var}=.+" "$ARCHIVO_ENV" || [ -n "${!var:-}" ] \
            || fallar "$var no tiene valor en .env (plantilla: .env.example)."
    done
    permisos="$(stat -c '%a' "$ARCHIVO_ENV")"
    case "$permisos" in
        600|400) ok ".env presente (permisos $permisos)" ;;
        *)       aviso ".env tiene permisos $permisos; recomendado: chmod 600 .env" ;;
    esac
elif [ -n "${POSTGRES_PASSWORD:-}" ] && [ -n "${RABBITMQ_PASSWORD:-}" ]; then
    ok "Credenciales tomadas de variables de entorno (sin .env)"
else
    fallar "No existe .env. Crealo con: cp .env.example .env && chmod 600 .env (y rellena los valores)."
fi

"${COMPOSE[@]}" config --quiet || fallar "docker-compose.yml no es valido (o faltan variables de .env)."
ok "docker-compose.yml valido"

faltan=()
while read -r img; do
    [ -n "$img" ] || continue
    docker image inspect "$img" >/dev/null 2>&1 || faltan+=("$img")
done < <("${COMPOSE[@]}" config --images)
if [ "${#faltan[@]}" -gt 0 ]; then
    printf '  falta: %s\n' "${faltan[@]}"
    fallar "Faltan imagenes. Propias: docker load -i ecommerce-imagenes.tar.gz | Infra: docker compose pull postgres-db kafka rabbitmq"
fi
ok "Las $( "${COMPOSE[@]}" config --images | wc -l ) imagenes existen localmente"

mem_disp_mb=$(( $(awk '/MemAvailable/ {print $2}' /proc/meminfo) / 1024 ))
swap_total_mb=$(( $(awk '/SwapTotal/ {print $2}' /proc/meminfo) / 1024 ))
echo "  MemAvailable=${mem_disp_mb} MiB  SwapTotal=${swap_total_mb} MiB  CPUs=$(nproc)"
[ "$mem_disp_mb" -lt 1300 ] && aviso "Menos de 1300 MiB disponibles: el sistema completo (~1.2-1.35 GiB estimados) puede usar swap."
[ "$swap_total_mb" -eq 0 ]  && aviso "No hay swap: un pico de memoria terminaria en OOM kill."

mostrar_recursos

INICIO_TOTAL="$(ahora)"

tanda 1 "$TIMEOUT_INFRA" "${INFRA[@]}"

titulo "Comprobando que 15672 solo escucha en 127.0.0.1"
PUBLICACIONES_15672="$(docker port rabbitmq 15672/tcp 2>/dev/null || true)"
echo "  docker port rabbitmq 15672 -> ${PUBLICACIONES_15672:-(sin publicar)}"
if [ -z "$PUBLICACIONES_15672" ] || printf '%s\n' "$PUBLICACIONES_15672" | grep -qv '^127\.0\.0\.1:15672$'; then
    fallar "15672 no esta publicado exclusivamente en 127.0.0.1:15672. Revisa ports de rabbitmq en docker-compose.yml."
fi
ok "Consola de RabbitMQ solo en 127.0.0.1:15672 (acceso por tunel SSH)"

tanda 2 "$TIMEOUT_APP"   order-api
tanda 3 "$TIMEOUT_APP"   dashboard-aggregator analytics-service
tanda 4 "$TIMEOUT_APP"   billing-worker inventory-worker notification-worker
tanda 5 "$TIMEOUT_APP"   order-processor

titulo "Los diez contenedores estan listos ($(( $(ahora) - INICIO_TOTAL )) s)"

mem_disp_fin=$(( $(awk '/MemAvailable/ {print $2}' /proc/meminfo) / 1024 ))
swap_usado_fin=$(( ( $(awk '/SwapTotal/ {print $2}' /proc/meminfo) - $(awk '/SwapFree/ {print $2}' /proc/meminfo) ) / 1024 ))
echo "  RAM disponible: ${mem_disp_fin} MiB   swap en uso: ${swap_usado_fin} MiB"
if [ "$mem_disp_fin" -lt 250 ] || [ "$swap_usado_fin" -gt 150 ]; then
    aviso "RAM justa en reposo (disponible < 250 MiB o swap > 150 MiB). El swap no sustituye a la RAM:"
    aviso "mide con 'vmstat 5 5' (si/so sostenidos > 0 = falta RAM) y valora t3.medium o subir limites solo donde haga falta."
else
    ok "Margen de RAM suficiente en reposo"
fi
TOKEN_IMDS="$(curl -s -m 2 -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60' 2>/dev/null || true)"
IP_PUBLICA=""
[ -n "$TOKEN_IMDS" ] && IP_PUBLICA="$(curl -s -m 2 -H "X-aws-ec2-metadata-token: $TOKEN_IMDS" http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || true)"
IP_PUBLICA="${IP_PUBLICA:-<IP_PUBLICA_EC2>}"
cat <<EOF

  Dashboard ............ http://${IP_PUBLICA}:8081
  API de ordenes ....... http://${IP_PUBLICA}:8080/order   (POST)
  RabbitMQ (consola) ... solo por tunel SSH. En Windows PowerShell:
                           ssh -i <RUTA_CLAVE.pem> -N -L 15672:127.0.0.1:15672 ubuntu@${IP_PUBLICA}
                         y despues abrir http://localhost:15672 (usuario ecommerce)

  Memoria por contenedor:  docker stats --no-stream
  Detener (conserva datos): bash ops/aws/detener-aws.sh
EOF
