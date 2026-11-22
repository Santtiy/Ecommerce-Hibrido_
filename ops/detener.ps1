# Detiene SOLO los servicios lanzados por ops\arrancar.

param(
    [switch]$ConInfra
)

$raiz        = Split-Path -Parent $PSScriptRoot
$archivoPids = Join-Path $PSScriptRoot 'pids.txt'
$compose     = Join-Path $raiz 'docker-compose.yml'
$composeDev  = Join-Path $raiz 'docker-compose.dev.yml'

function Stop-PidSeguro([string]$pidTexto, [string]$nombreEsperado) {
    if (-not $pidTexto) { return 'sin PID registrado' }

    $proceso = Get-Process -Id ([int]$pidTexto) -ErrorAction SilentlyContinue
    if (-not $proceso) {
        return "PID $pidTexto ya no existe"
    }
    if ($proceso.ProcessName -ne $nombreEsperado) {
        return "PID $pidTexto ahora es '$($proceso.ProcessName)', no '$nombreEsperado': NO se toca"
    }
    Stop-Process -Id $proceso.Id -Force
    return "PID $pidTexto detenido"
}

if (-not (Test-Path $archivoPids)) {
    Write-Host "No existe $archivoPids : no hay servicios registrados que detener." -ForegroundColor Yellow
} else {
    Write-Host "== Deteniendo servicios registrados en pids.txt" -ForegroundColor Cyan

    foreach ($linea in Get-Content $archivoPids) {
        if (-not $linea.Trim()) { continue }

        $partes  = $linea.Split(';')
        $nombre  = $partes[0]
        $pidJava = if ($partes.Count -gt 1) { $partes[1] } else { '' }

        $resultado = Stop-PidSeguro $pidJava 'java'
        $color = if ($resultado -like '*detenido') { 'Green' } else { 'Yellow' }
        Write-Host ("  {0,-22} {1}" -f $nombre, $resultado) -ForegroundColor $color
    }

    Remove-Item $archivoPids
}

if ($ConInfra) {
    Write-Host "== docker compose stop postgres-db kafka rabbitmq" -ForegroundColor Cyan
    if (-not (Test-Path (Join-Path $raiz '.env'))) {
        foreach ($v in @('POSTGRES_PASSWORD', 'RABBITMQ_PASSWORD')) {
            if (-not [Environment]::GetEnvironmentVariable($v, 'Process')) {
                [Environment]::SetEnvironmentVariable($v, 'no-usada-al-detener', 'Process')
            }
        }
    }
    docker compose -f $compose -f $composeDev stop postgres-db kafka rabbitmq
    if ($LASTEXITCODE -ne 0) {
        Write-Host "docker compose stop devolvio un error (codigo $LASTEXITCODE)." -ForegroundColor Red
        exit 1
    }
}

Write-Host "Listo." -ForegroundColor Green
