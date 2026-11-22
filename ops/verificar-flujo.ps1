# Prueba de punta a punta: envia tres ordenes validas y dos invalidas, y sigue cada orden valida por todo el sistema.
#   1. order-api responde 202 (o 400 en las invalidas) con el total esperado.
#   2. PostgreSQL: la orden queda PAGADA o CANCELADA y auditoria_ordenes tiene el resultado de los tres workers.
#   3. analytics-service registro la orden (log).
#   4. dashboard-aggregator: el WebSocket devuelve { payment, stock, email } con los estados esperados.
# Con -Memoria, al final muestra el consumo de cada contenedor y la suma (para comparar con la t3.small).
#
# Uso (con los contenedores levantados):  powershell -ExecutionPolicy Bypass -File ops\verificar-flujo.ps1 -Memoria

param(
    [string]$Api = 'http://localhost:8080/order',
    [string]$WebSocket = 'ws://localhost:8081/ws/ordenes',
    # El panel solo acepta los origenes de APP_ORIGENES: el script se presenta como la pagina del dashboard.
    [string]$Origen = 'http://localhost:8081',
    [int]$SegundosMaximo = 60,
    [switch]$Memoria
)

$casos = @(
    @{ Nombre = 'Libro x1 (todo correcto)'; Total = 160650; Estado = 'PAGADA'
       Pago = 'PAYMENT_SUCCESS'; Stock = 'STOCK_RESERVED'; Correo = 'EMAIL_SENT'
       Json = '{"cliente":"Verificacion","items":[{"sku":"LIB-001","nombre":"Libro","cantidad":1,"precioUnitario":150000}]}' },
    @{ Nombre = 'Televisor x1 (pago rechazado, RN-11)'; Total = 1606500; Estado = 'CANCELADA'
       Pago = 'PAYMENT_FAILED'; Stock = 'STOCK_RESERVED'; Correo = 'EMAIL_SENT'
       Json = '{"cliente":"Verificacion","items":[{"sku":"TV-001","nombre":"Televisor","cantidad":1,"precioUnitario":1500000}]}' },
    @{ Nombre = 'Memoria USB x15 (sin stock, RN-12)'; Total = 321300; Estado = 'PAGADA'
       Pago = 'PAYMENT_SUCCESS'; Stock = 'STOCK_OUT_OF_STOCK'; Correo = 'EMAIL_SENT'
       Json = '{"cliente":"Verificacion","items":[{"sku":"USB-001","nombre":"Memoria USB","cantidad":15,"precioUnitario":20000}]}' }
)
$invalidas = @(
    @{ Nombre = 'Lista de items vacia'; Json = '{"cliente":"Verificacion","items":[]}' },
    @{ Nombre = 'Cantidad 0'; Json = '{"cliente":"Verificacion","items":[{"sku":"LIB-001","nombre":"Libro","cantidad":0,"precioUnitario":150000}]}' }
)

$fallos = 0
function Write-Resultado([bool]$ok, [string]$texto) {
    if ($ok) { Write-Host "  OK    $texto" -ForegroundColor Green }
    else     { Write-Host "  FALLA $texto" -ForegroundColor Red; $script:fallos++ }
}

function Send-Orden([string]$json) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    try {
        $r = Invoke-WebRequest -Uri $Api -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8' -UseBasicParsing -ErrorAction Stop
        return @{ Codigo = [int]$r.StatusCode; Cuerpo = $r.Content }
    } catch {
        if ($_.Exception.Response) { return @{ Codigo = [int]$_.Exception.Response.StatusCode; Cuerpo = '' } }
        return @{ Codigo = 0; Cuerpo = $_.Exception.Message }
    }
}

function Get-EstadoEnBaseDeDatos([string]$id) {
    $sql = "SELECT o.estado || '|' || COALESCE((SELECT string_agg(a.origen || '=' || a.status, ',' ORDER BY a.origen) " +
           "FROM auditoria_ordenes a WHERE a.orden_id = o.id), '') FROM ordenes o WHERE o.id = '$id'"
    return (& docker exec postgres-db psql -U ecommerce -d ecommerce -At -c $sql 2>$null) -join ''
}

# El cliente WebSocket de .NET Framework (PowerShell 5.1) rechaza en el primer intento de cada proceso la
# cabecera "Connection: upgrade, keep-alive" que envia Tomcat; el segundo intento conecta. Por eso se reintenta.
function Connect-WebSocket {
    for ($intento = 1; $intento -le 3; $intento++) {
        $cliente = New-Object System.Net.WebSockets.ClientWebSocket
        $cliente.Options.SetRequestHeader('Origin', $Origen)
        try {
            $cliente.ConnectAsync([Uri]$WebSocket, [System.Threading.CancellationToken]::None).Wait()
            return $cliente
        } catch {
            $cliente.Dispose()
            if ($intento -eq 3) { throw }
        }
    }
}

function Get-TableroDelDashboard {
    $cliente = Connect-WebSocket
    $cancelar = New-Object System.Threading.CancellationTokenSource 10000
    $buffer = New-Object byte[] 65536
    $segmento = New-Object System.ArraySegment[byte] -ArgumentList (,$buffer)
    $contenido = New-Object System.IO.MemoryStream
    do {
        $recibido = $cliente.ReceiveAsync($segmento, $cancelar.Token).Result
        $contenido.Write($buffer, 0, $recibido.Count)
    } while (-not $recibido.EndOfMessage)
    $cliente.Dispose()
    return [System.Text.Encoding]::UTF8.GetString($contenido.ToArray()) | ConvertFrom-Json
}

# --- 1. order-api
Write-Host "== 1. order-api (HTTP)" -ForegroundColor Cyan
foreach ($caso in $casos) {
    $r = Send-Orden $caso.Json
    if ($r.Codigo -ne 202) { Write-Resultado $false "$($caso.Nombre): HTTP $($r.Codigo), se esperaba 202"; continue }
    $orden = $r.Cuerpo | ConvertFrom-Json
    $caso.Id = $orden.orderId
    Write-Resultado ([decimal]$orden.total -eq [decimal]$caso.Total) "$($caso.Nombre): 202, total $($orden.total) $($orden.moneda) (esperado $($caso.Total))"
}
foreach ($caso in $invalidas) {
    $r = Send-Orden $caso.Json
    Write-Resultado ($r.Codigo -eq 400) "$($caso.Nombre): HTTP $($r.Codigo) (esperado 400)"
}

$aceptadas = @($casos | Where-Object { $_.Id })
if ($aceptadas.Count -eq 0) { Write-Host "Ninguna orden fue aceptada: se detiene la prueba." -ForegroundColor Red; exit 1 }

# --- 2. PostgreSQL (espera a que lleguen los tres resultados de cada orden)
Write-Host "== 2. PostgreSQL: estado de la orden y auditoria" -ForegroundColor Cyan
foreach ($caso in $aceptadas) {
    $caso.Esperado = "$($caso.Estado)|INVENTARIO=$($caso.Stock),NOTIFICACION=$($caso.Correo),PAGO=$($caso.Pago)"
}
$limite = (Get-Date).AddSeconds($SegundosMaximo)
do {
    foreach ($caso in $aceptadas) { $caso.Obtenido = Get-EstadoEnBaseDeDatos $caso.Id }
    $pendientes = @($aceptadas | Where-Object { $_.Obtenido -ne $_.Esperado })
    if ($pendientes.Count -gt 0) { Start-Sleep -Seconds 2 }
} while ($pendientes.Count -gt 0 -and (Get-Date) -lt $limite)
foreach ($caso in $aceptadas) {
    Write-Resultado ($caso.Obtenido -eq $caso.Esperado) "$($caso.Nombre): $($caso.Obtenido)"
}

# --- 3. analytics-service
Write-Host "== 3. analytics-service" -ForegroundColor Cyan
$logAnalytics = (& docker logs analytics-service 2>&1) -join "`n"
foreach ($caso in $aceptadas) {
    Write-Resultado ($logAnalytics -match [regex]::Escape($caso.Id)) "$($caso.Nombre): orden registrada en el log"
}

# --- 4. dashboard-aggregator
Write-Host "== 4. dashboard-aggregator (WebSocket)" -ForegroundColor Cyan
try {
    $tablero = Get-TableroDelDashboard
    foreach ($caso in $aceptadas) {
        $p = $tablero.($caso.Id)
        $ok = $p -and $p.payment -eq $caso.Pago -and $p.stock -eq $caso.Stock -and $p.email -eq $caso.Correo
        Write-Resultado ([bool]$ok) "$($caso.Nombre): payment=$($p.payment) stock=$($p.stock) email=$($p.email)"
    }
} catch {
    Write-Resultado $false "No se pudo leer el WebSocket ${WebSocket}: $($_.Exception.Message)"
}

# --- Memoria (opcional)
if ($Memoria) {
    Write-Host "== Memoria en uso por contenedor" -ForegroundColor Cyan
    $totalMiB = 0
    foreach ($linea in (& docker stats --no-stream --format '{{.Name}};{{.MemUsage}}')) {
        $nombre, $uso = $linea -split ';'
        $usado = ($uso -split '/')[0].Trim()
        $valor = [double]($usado -replace '[^0-9.]', '')
        if ($usado -match 'GiB') { $valor *= 1024 } elseif ($usado -match 'KiB') { $valor /= 1024 }
        $totalMiB += $valor
        Write-Host ("  {0,-22} {1}" -f $nombre, $uso)
    }
    Write-Host ("  TOTAL                  {0:F0} MiB  (en la t3.small hay ~1600 MiB libres para contenedores)" -f $totalMiB) -ForegroundColor Yellow
}

Write-Host ""
if ($fallos -eq 0) { Write-Host "Flujo completo verificado." -ForegroundColor Green; exit 0 }
Write-Host "$fallos comprobacion(es) fallaron." -ForegroundColor Red
exit 1
