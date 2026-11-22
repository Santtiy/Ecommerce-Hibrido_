# Arranca la infraestructura (si hace falta) y los siete microservicios en segundo plano, esperando de verdad a que cada uno este listo antes de seguir.

param(
    [switch]$Reiniciar
)

# Todo lo que generan arrancar.ps1 y detener.ps1 queda en .run\ (ignorada por Git): pids.txt, logs\ y datos\.
$raiz        = Split-Path -Parent $PSScriptRoot
$carpetaRun  = Join-Path $raiz '.run'
$archivoPids = Join-Path $carpetaRun 'pids.txt'
$compose     = Join-Path $raiz 'docker-compose.yml'
$composeDev  = Join-Path $raiz 'docker-compose.dev.yml'
$archivoEnv  = Join-Path $raiz '.env'
$carpetaLogs = Join-Path $carpetaRun 'logs'

$contenedores   = @('postgres-db', 'kafka', 'rabbitmq')
$puertosInfra   = @{ 'postgres-db' = 5433; 'kafka' = 29092; 'rabbitmq' = 5672 }
$contenedoresApp = @('order-api', 'order-processor', 'billing-worker', 'inventory-worker',
                     'notification-worker', 'dashboard-aggregator', 'analytics-service')
$segundosMaximo = 90

$puertos = @{
    'order-api'            = 8080
    'dashboard-aggregator' = 8081
}

$tandas = @(
    @('billing-worker', 'inventory-worker', 'notification-worker', 'dashboard-aggregator'),
    @('order-api'),
    @('analytics-service', 'order-processor')
)

function Get-RutaJar([string]$modulo) { Join-Path $raiz "backend\$modulo\target\$modulo-1.0.0.jar" }
function Get-RutaLog([string]$modulo) { Join-Path $carpetaLogs "$modulo.log" }

function Stop-ConError([string]$mensaje) {
    Write-Host ""
    Write-Host "ERROR: $mensaje" -ForegroundColor Red
    exit 1
}

function Show-FinalLog([string]$modulo, [int]$lineas = 30) {
    $log = Get-RutaLog $modulo
    Write-Host ""
    Write-Host "----- Ultimas $lineas lineas de $log -----" -ForegroundColor Yellow
    if (Test-Path $log) { Get-Content $log -Tail $lineas -Encoding UTF8 } else { Write-Host "(el log no existe)" }
    $err = $log -replace '\.log$', '.err.log'
    if ((Test-Path $err) -and (Get-Item $err).Length -gt 0) {
        Write-Host "----- $err -----" -ForegroundColor Yellow
        Get-Content $err -Tail $lineas -Encoding UTF8
    }
    Write-Host "------------------------------------------" -ForegroundColor Yellow
}

foreach ($herramienta in @('docker', 'java', 'mvn')) {
    if (-not (Get-Command $herramienta -ErrorAction SilentlyContinue)) {
        Stop-ConError "No se encontro '$herramienta' en el PATH."
    }
}
$java = (Get-Command java).Source

if ($Reiniciar) {
    Write-Host "== Reiniciando: ejecutando detener.ps1" -ForegroundColor Cyan
    & (Join-Path $PSScriptRoot 'detener.ps1')
}

if (Test-Path $archivoPids) {
    $vivos = @()
    foreach ($linea in Get-Content $archivoPids) {
        $partes = $linea.Split(';')
        if ($partes.Count -lt 2 -or -not $partes[1]) { continue }
        $proceso = Get-Process -Id ([int]$partes[1]) -ErrorAction SilentlyContinue
        if ($proceso -and $proceso.ProcessName -eq 'java') { $vivos += $partes[0] }
    }
    if ($vivos.Count -gt 0) {
        Stop-ConError ("Ya hay servicios en marcha: " + ($vivos -join ', ') +
                       ". Usa -Reiniciar o ejecuta antes ops\detener.ps1.")
    }
    Write-Host "pids.txt anterior sin procesos vivos: se sobrescribe."
}

if (-not (Test-Path $archivoEnv)) {
    Stop-ConError "No existe .env. Crealo con: Copy-Item .env.example .env  (y rellena POSTGRES_PASSWORD y RABBITMQ_PASSWORD)."
}
$valoresEnv = @{}
foreach ($linea in Get-Content $archivoEnv) {
    if ($linea -match '^\s*(POSTGRES_PASSWORD|RABBITMQ_PASSWORD)=(.*)$') {
        $valoresEnv[$Matches[1]] = $Matches[2].TrimEnd("`r").Trim()
    }
}
foreach ($v in @('POSTGRES_PASSWORD', 'RABBITMQ_PASSWORD')) {
    if (-not $valoresEnv[$v]) { Stop-ConError "$v no tiene valor en .env (plantilla: .env.example)." }
}
$env:DB_PASSWORD       = $valoresEnv['POSTGRES_PASSWORD']
$env:RABBITMQ_PASSWORD = $valoresEnv['RABBITMQ_PASSWORD']
$env:ARCHIVO_BITACORA  = Join-Path $carpetaRun 'datos\ordenes.log'
Write-Host "  .env leido (POSTGRES_PASSWORD y RABBITMQ_PASSWORD con valor; no se muestran)."

$appEnMarcha = @($contenedoresApp | Where-Object { (docker inspect --format '{{.State.Running}}' $_ 2>$null) -eq 'true' })
if ($appEnMarcha.Count -gt 0) {
    Stop-ConError ("Hay contenedores de microservicios en marcha (modo AWS): " + ($appEnMarcha -join ', ') +
                   ". Detenlos (no borra datos) con: docker compose stop " + ($appEnMarcha -join ' '))
}

foreach ($puerto in $puertos.Values) {
    if (Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue) {
        Stop-ConError "El puerto $puerto ya esta en uso. Cierra lo que lo ocupa (netstat -ano | findstr :$puerto)."
    }
}

function Test-InfraSana {
    foreach ($c in $contenedores) {
        $estado = docker inspect --format '{{.State.Health.Status}}' $c 2>$null
        if ($LASTEXITCODE -ne 0 -or $estado -ne 'healthy') {
            Write-Host "  $c -> $(if ($estado) { $estado } else { 'no existe o no esta corriendo' })"
            return $false
        }
        $puerto = $puertosInfra[$c]
        $cliente = New-Object System.Net.Sockets.TcpClient
        try   { $abierto = $cliente.ConnectAsync('127.0.0.1', $puerto).Wait(2000) -and $cliente.Connected }
        catch { $abierto = $false }
        finally { $cliente.Dispose() }
        if (-not $abierto) {
            Write-Host "  $c -> healthy, pero 127.0.0.1:$puerto no responde (arrancado sin docker-compose.dev.yml)"
            return $false
        }
        Write-Host "  $c -> healthy (127.0.0.1:$puerto)"
    }
    return $true
}

Write-Host "== 1/4 Infraestructura" -ForegroundColor Cyan
docker info *> $null
if ($LASTEXITCODE -ne 0) { Stop-ConError "Docker no responde. Abre Docker Desktop y vuelve a intentarlo." }

if (-not (Test-InfraSana)) {
    Write-Host "Levantando la infraestructura (modo desarrollo) con docker compose (puede tardar un par de minutos)..."
    Write-Host "  Si estaba arrancada en modo AWS, compose recrea esos tres contenedores para publicar los puertos:"
    Write-Host "  PostgreSQL conserva sus datos (volumen); topics y colas los recrean los servicios al arrancar."
    docker compose -f $compose -f $composeDev up -d --wait @contenedores
    if ($LASTEXITCODE -ne 0) { Stop-ConError "docker compose up fallo. Revisa: docker compose -f `"$compose`" -f `"$composeDev`" logs" }
    if (-not (Test-InfraSana)) { Stop-ConError "La infraestructura sigue sin estar sana." }
}

Write-Host "== 2/4 Jar" -ForegroundColor Cyan
$todos  = $tandas | ForEach-Object { $_ }
$faltan = @($todos | Where-Object { -not (Test-Path (Get-RutaJar $_)) })
if ($faltan.Count -gt 0) {
    Write-Host "Faltan jar de: $($faltan -join ', '). Ejecutando mvn clean package -DskipTests..."
    mvn -f (Join-Path $raiz 'backend\pom.xml') clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { Stop-ConError "Maven fallo al empaquetar." }
} else {
    Write-Host "  Los siete jar existen."
}

function Start-Servicio([string]$modulo) {
    $jar = Get-RutaJar $modulo
    $log = Get-RutaLog $modulo
    $err = $log -replace '\.log$', '.err.log'

    $proceso = Start-Process -FilePath $java `
                             -ArgumentList "-Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar `"$jar`"" `
                             -WorkingDirectory $raiz `
                             -RedirectStandardOutput $log `
                             -RedirectStandardError $err `
                             -WindowStyle Hidden `
                             -PassThru
    $pidJava = $proceso.Id

    Add-Content -Path $archivoPids -Value "$modulo;$pidJava" -Encoding ASCII
    Write-Host ("  lanzado {0,-22} java PID {1,-7} log {2}" -f $modulo, $pidJava, $log)
    return $pidJava
}

function Esperar-Puerto([string]$modulo, [int]$puerto, [int]$pidJava) {
    $limite = (Get-Date).AddSeconds($segundosMaximo)
    while ((Get-Date) -lt $limite) {
        if (-not (Get-Process -Id $pidJava -ErrorAction SilentlyContinue)) {
            Show-FinalLog $modulo
            Stop-ConError "$modulo termino antes de abrir el puerto $puerto."
        }
        if (Test-NetConnection -ComputerName localhost -Port $puerto -InformationLevel Quiet `
                               -WarningAction SilentlyContinue) {
            return
        }
        Start-Sleep -Seconds 1
    }
    Show-FinalLog $modulo
    Stop-ConError "$modulo no abrio el puerto $puerto en $segundosMaximo segundos."
}

function Esperar-Log([string]$modulo, [int]$pidJava) {
    $log    = Get-RutaLog $modulo
    $limite = (Get-Date).AddSeconds($segundosMaximo)
    while ((Get-Date) -lt $limite) {
        if ((Test-Path $log) -and (Select-String -Path $log -Pattern 'Started \S+ in [\d.,]+ seconds' -Quiet)) {
            return
        }
        if (-not (Get-Process -Id $pidJava -ErrorAction SilentlyContinue)) {
            Show-FinalLog $modulo
            Stop-ConError "$modulo termino antes de completar el arranque."
        }
        Start-Sleep -Seconds 1
    }
    Show-FinalLog $modulo
    Stop-ConError "$modulo no escribio 'Started ...' en $segundosMaximo segundos."
}

Write-Host "== 3/4 Servicios" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $carpetaLogs | Out-Null
[System.IO.File]::WriteAllText($archivoPids, '')

for ($t = 0; $t -lt $tandas.Count; $t++) {
    Write-Host "Tanda $($t + 1): $($tandas[$t] -join ', ')"

    $lanzados = @{}
    foreach ($modulo in $tandas[$t]) { $lanzados[$modulo] = Start-Servicio $modulo }

    foreach ($modulo in $tandas[$t]) {
        $cronometro = [System.Diagnostics.Stopwatch]::StartNew()
        Esperar-Log $modulo $lanzados[$modulo]
        if ($puertos.ContainsKey($modulo)) { Esperar-Puerto $modulo $puertos[$modulo] $lanzados[$modulo] }
        Write-Host ("  listo    {0,-22} ({1:N0} s)" -f $modulo, $cronometro.Elapsed.TotalSeconds) -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "== 4/4 Los siete servicios estan listos" -ForegroundColor Green
Write-Host "PIDs en $archivoPids  |  logs en $carpetaLogs"
Write-Host ""
Write-Host "  Dashboard ............ http://localhost:8081"
Write-Host "  API de ordenes ....... http://localhost:8080/order   (POST; prueba con ops\enviar-ordenes.ps1)"
Write-Host "  RabbitMQ (consola) ... http://localhost:15672        usuario ecommerce / clave: RABBITMQ_PASSWORD de .env"
Write-Host ""
Write-Host "Para detener: ops\detener.ps1   (con -ConInfra detiene tambien postgres-db, kafka y rabbitmq)"
