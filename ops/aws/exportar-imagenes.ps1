# Exporta las siete imagenes de los microservicios a UN archivo.

param(
    [string]$Destino   = (Join-Path $env:USERPROFILE 'ecommerce-aws-export'),
    [string]$IpServidor = '<IP_PUBLICA_EC2>',
    [string]$RutaClave  = '<RUTA_A_TU_CLAVE.pem>',
    [string]$Usuario    = 'ubuntu',
    [string]$RutaRemota = '/home/ubuntu',
    [switch]$ConservarTar
)

$raiz    = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$compose = Join-Path $raiz 'docker-compose.yml'

$modulos = @('order-api', 'order-processor', 'billing-worker', 'inventory-worker',
             'notification-worker', 'analytics-service', 'dashboard-aggregator')
$imagenes = $modulos | ForEach-Object { "ecommerce-hibrido-${_}:latest" }

$nombreBase = 'ecommerce-imagenes'
$rutaTar    = Join-Path $Destino "$nombreBase.tar"
$rutaGz     = Join-Path $Destino "$nombreBase.tar.gz"
$rutaSha    = Join-Path $Destino "$nombreBase.tar.gz.sha256"

function Stop-ConError([string]$mensaje) {
    Write-Host ""
    Write-Host "ERROR: $mensaje" -ForegroundColor Red
    exit 1
}
function Format-Tamano([long]$bytes) { '{0:N1} MB' -f ($bytes / 1MB) }

Write-Host "== 1/5 Docker e imagenes" -ForegroundColor Cyan
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) { Stop-ConError "docker no esta en el PATH." }
& docker info *> $null
if ($LASTEXITCODE -ne 0) { Stop-ConError "Docker no responde. Abre Docker Desktop y vuelve a intentarlo." }

$faltan = @()
foreach ($img in $imagenes) {
    $info = & docker image inspect --format '{{.Id}}|{{.Architecture}}|{{.Os}}|{{.Created}}|{{.Size}}' $img 2>$null
    if ($LASTEXITCODE -ne 0 -or -not $info) { $faltan += $img; continue }
    $p = $info.Split('|')
    $id = $p[0] -replace '^sha256:', ''
    Write-Host ("  {0,-48} id={1} {2}/{3} creada={4} tamano={5}" -f $img, $id.Substring(0, 12), $p[2], $p[1], $p[3].Substring(0, 19), (Format-Tamano ([long]$p[4])))
    if ($p[1] -ne 'amd64') { Stop-ConError "$img es $($p[1]); EC2 t3 necesita amd64." }
}
if ($faltan.Count -gt 0) {
    Write-Host "  Faltan: $($faltan -join ', ')" -ForegroundColor Yellow
    Stop-ConError ("Construyelas primero:  docker compose -f `"$compose`" build " + (($faltan | ForEach-Object { ($_ -replace '^ecommerce-hibrido-', '') -replace ':latest$', '' }) -join ' '))
}

Write-Host "  Etiquetas existentes de ecommerce-hibrido-*:"
& docker images --format '    {{.Repository}}:{{.Tag}}  {{.ID}}' | Where-Object { $_ -match 'ecommerce-hibrido-' }

Write-Host "== 2/5 Nombres frente a docker-compose.yml" -ForegroundColor Cyan
if (-not (Test-Path $compose)) { Stop-ConError "No existe $compose" }
$variablesCompose = @('POSTGRES_PASSWORD', 'RABBITMQ_PASSWORD')
$previas = @{}
foreach ($v in $variablesCompose) {
    $previas[$v] = [Environment]::GetEnvironmentVariable($v, 'Process')
    [Environment]::SetEnvironmentVariable($v, 'solo-para-listar-imagenes', 'Process')
}
try {
    $enCompose = & docker compose -f $compose config --images 2>$null
    $codigoConfig = $LASTEXITCODE
} finally {
    foreach ($v in $variablesCompose) { [Environment]::SetEnvironmentVariable($v, $previas[$v], 'Process') }
}
if ($codigoConfig -ne 0) { Stop-ConError "docker compose config fallo sobre $compose" }
$propiasCompose = @($enCompose | Where-Object { $_ -like 'ecommerce-hibrido-*' } | ForEach-Object {
    if ($_ -match ':') { $_ } else { "${_}:latest" } })
$noCoinciden = @(Compare-Object ($imagenes | Sort-Object) ($propiasCompose | Sort-Object))
if ($noCoinciden.Count -gt 0) {
    $noCoinciden | ForEach-Object { Write-Host "  $($_.InputObject) ($($_.SideIndicator))" -ForegroundColor Yellow }
    Stop-ConError "Los nombres de imagen del script y de docker-compose.yml no coinciden."
}
Write-Host "  Los siete nombres coinciden." -ForegroundColor Green

Write-Host "== 3/5 docker save" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $Destino | Out-Null
foreach ($f in @($rutaTar, $rutaGz, $rutaSha)) {
    if (Test-Path $f) { Stop-ConError "Ya existe $f. Muevelo o usa otro -Destino (el script no sobrescribe)." }
}
$unidad = (Get-Item $Destino).PSDrive
$necesario = 0L
foreach ($img in $imagenes) { $necesario += [long](& docker image inspect --format '{{.Size}}' $img) }
if ($unidad.Free -lt [long]($necesario * 1.5)) {
    Stop-ConError ("Poco espacio libre en {0}: {1} libres, se recomiendan {2}." -f $unidad.Name, (Format-Tamano $unidad.Free), (Format-Tamano ([long]($necesario * 1.5))))
}

$cron = [System.Diagnostics.Stopwatch]::StartNew()
& docker save -o $rutaTar @imagenes
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $rutaTar)) { Stop-ConError "docker save fallo." }
$tamTar = (Get-Item $rutaTar).Length
Write-Host ("  {0}  {1}  ({2:N0} s)" -f $rutaTar, (Format-Tamano $tamTar), $cron.Elapsed.TotalSeconds)
Write-Host ("  Suma de las siete imagenes por separado: {0} (lo compartido cuenta siete veces)" -f (Format-Tamano $necesario))

Write-Host "== 4/5 Compresion gzip (.NET GZipStream)" -ForegroundColor Cyan
$cron.Restart()
$entrada = $null; $salida = $null; $gzip = $null
try {
    $entrada = [System.IO.File]::OpenRead($rutaTar)
    $salida  = [System.IO.File]::Create($rutaGz)
    $gzip    = New-Object System.IO.Compression.GZipStream($salida, [System.IO.Compression.CompressionLevel]::Optimal)
    $entrada.CopyTo($gzip, 1MB)
} finally {
    if ($gzip)    { $gzip.Dispose() }
    if ($salida)  { $salida.Dispose() }
    if ($entrada) { $entrada.Dispose() }
}
$tamGz = (Get-Item $rutaGz).Length
Write-Host ("  {0}  {1}  ({2:P0} del tar, {3:N0} s)" -f $rutaGz, (Format-Tamano $tamGz), ($tamGz / $tamTar), $cron.Elapsed.TotalSeconds)

$hash = (Get-FileHash -Algorithm SHA256 -Path $rutaGz).Hash.ToLower()
[System.IO.File]::WriteAllText($rutaSha, "$hash  $nombreBase.tar.gz`n", (New-Object System.Text.UTF8Encoding($false)))
Write-Host "  SHA-256: $hash"

if (-not $ConservarTar) {
    Remove-Item $rutaTar
    Write-Host "  Tar intermedio eliminado (usa -ConservarTar para guardarlo)."
}

Write-Host "== 5/5 Siguientes pasos" -ForegroundColor Cyan
$destinoRemoto = "${Usuario}@${IpServidor}:${RutaRemota}/"
Write-Host ""
Write-Host "  [Windows PowerShell] transferir imagenes y su suma SHA-256:" -ForegroundColor Yellow
Write-Host "    scp -i `"$RutaClave`" `"$rutaGz`" `"$rutaSha`" $destinoRemoto"
Write-Host ""
Write-Host "  [Ubuntu por SSH] verificar e importar:" -ForegroundColor Yellow
Write-Host "    cd $RutaRemota && sha256sum -c $nombreBase.tar.gz.sha256"
Write-Host "    docker load -i $RutaRemota/$nombreBase.tar.gz"
Write-Host "    docker images --format '{{.Repository}}:{{.Tag}} {{.Size}}' | grep ecommerce-hibrido-"
Write-Host ""
Write-Host "  [Ubuntu por SSH] imagenes de infraestructura (se descargan en EC2, no se transfieren):" -ForegroundColor Yellow
Write-Host "    cd ~/ecommerce-hibrido && docker compose pull postgres-db kafka rabbitmq"
Write-Host ""
Write-Host ("Resumen: {0} imagenes, tar {1}, tar.gz {2}." -f $imagenes.Count, (Format-Tamano $tamTar), (Format-Tamano $tamGz)) -ForegroundColor Green
