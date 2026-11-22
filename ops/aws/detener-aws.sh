#!/usr/bin/env bash
# detener-aws.sh - Detiene los diez contenedores de docker-compose.yml SIN borrar nada.

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ARCHIVO_COMPOSE="$RAIZ/docker-compose.yml"
COMPOSE=(docker compose -f "$ARCHIVO_COMPOSE")

MODO="stop"
case "${1:-}" in
    "")      ;;
    --down)  MODO="down" ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    *)       echo "Opcion desconocida: $1 (usa --down o nada)" >&2; exit 2 ;;
esac

[ -f "$ARCHIVO_COMPOSE" ] || { echo "No existe $ARCHIVO_COMPOSE" >&2; exit 1; }
docker info >/dev/null 2>&1 || { echo "El demonio de Docker no responde." >&2; exit 1; }

if [ ! -f "$RAIZ/.env" ]; then
    export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-no-usada-al-detener}"
    export RABBITMQ_PASSWORD="${RABBITMQ_PASSWORD:-no-usada-al-detener}"
    echo "(sin .env: se usan valores de relleno solo para poder detener)"
fi

paso() { printf '\n== %s\n' "$*"; }

paso "Deteniendo order-processor"
"${COMPOSE[@]}" stop order-processor

paso "Deteniendo workers, analytics-service y dashboard-aggregator"
"${COMPOSE[@]}" stop billing-worker inventory-worker notification-worker analytics-service dashboard-aggregator

paso "Deteniendo order-api"
"${COMPOSE[@]}" stop order-api

paso "Deteniendo infraestructura"
"${COMPOSE[@]}" stop rabbitmq kafka postgres-db

if [ "$MODO" = "down" ]; then
    paso "docker compose down (SIN -v: los volumenes se conservan)"
    "${COMPOSE[@]}" down
fi

paso "Estado final"
"${COMPOSE[@]}" ps -a --format 'table {{.Name}}\t{{.Status}}' || true
echo "--- Volumenes conservados"
docker volume ls --format '{{.Name}}' | grep -E '^(ecommerce-hibrido_postgres-data|analytics-datos)$' || echo "(ninguno encontrado)"
echo "--- free -h"
free -h
echo "Listo."
