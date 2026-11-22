# Envia cinco ordenes de prueba a order-api y compara lo esperado con lo obtenido.

$url = 'http://localhost:8080/order'
$segundosEntrePeticiones = 2

$casos = @(
    @{
        Nombre      = '1. Libro x1 a 150.000'
        Json        = '{"cliente":"Cliente Demo","items":[{"sku":"LIB-001","nombre":"Libro","cantidad":1,"precioUnitario":150000}]}'
        Codigo      = 202
        Total       = 160650
        Asincrono   = 'PAYMENT_SUCCESS, STOCK_RESERVED, EMAIL_SENT'
    },
    @{
        Nombre      = '2. Televisor x1 a 1.500.000'
        Json        = '{"cliente":"Cliente Demo","items":[{"sku":"TV-001","nombre":"Televisor","cantidad":1,"precioUnitario":1500000}]}'
        Codigo      = 202
        Total       = 1606500
        Asincrono   = 'PAYMENT_FAILED (supera 1.000.000, RN-11), STOCK_RESERVED, EMAIL_SENT'
    },
    @{
        Nombre      = '3. Memoria USB x15 a 20.000'
        Json        = '{"cliente":"Cliente Demo","items":[{"sku":"USB-001","nombre":"Memoria USB","cantidad":15,"precioUnitario":20000}]}'
        Codigo      = 202
        Total       = 321300
        Asincrono   = 'PAYMENT_SUCCESS, STOCK_OUT_OF_STOCK (mas de 10 unidades, RN-12), EMAIL_SENT'
    },
    @{
        Nombre      = '4. Lista de items vacia'
        Json        = '{"cliente":"Cliente Demo","items":[]}'
        Codigo      = 400
        Total       = $null
        Asincrono   = $null
    },
    @{
        Nombre      = '5. Cantidad 0'
        Json        = '{"cliente":"Cliente Demo","items":[{"sku":"LIB-001","nombre":"Libro","cantidad":0,"precioUnitario":150000}]}'
        Codigo      = 400
        Total       = $null
        Asincrono   = $null
    }
)

function Send-Orden([string]$json) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    try {
        $respuesta = Invoke-WebRequest -Uri $url -Method Post -Body $bytes `
                                       -ContentType 'application/json; charset=utf-8' `
                                       -UseBasicParsing -ErrorAction Stop
        return @{ Codigo = [int]$respuesta.StatusCode; Cuerpo = $respuesta.Content }
    } catch {
        $http = $_.Exception.Response
        if (-not $http) {
            return @{ Codigo = 0; Cuerpo = "Sin respuesta del servidor: $($_.Exception.Message)" }
        }
        $cuerpo = $_.ErrorDetails.Message
        if (-not $cuerpo) {
            $lector = New-Object System.IO.StreamReader($http.GetResponseStream())
            $cuerpo = $lector.ReadToEnd()
            $lector.Close()
        }
        return @{ Codigo = [int]$http.StatusCode; Cuerpo = $cuerpo }
    }
}

$fallos = 0

for ($i = 0; $i -lt $casos.Count; $i++) {
    $caso = $casos[$i]
    Write-Host ""
    Write-Host "== $($caso.Nombre)" -ForegroundColor Cyan

    $r = Send-Orden $caso.Json

    $okCodigo = ($r.Codigo -eq $caso.Codigo)
    Write-Host ("  HTTP   esperado {0} | obtenido {1}" -f $caso.Codigo, $r.Codigo) `
               -ForegroundColor $(if ($okCodigo) { 'Green' } else { 'Red' })
    if (-not $okCodigo) { $fallos++ }

    if ($caso.Codigo -eq 202 -and $okCodigo) {
        $orden = $r.Cuerpo | ConvertFrom-Json
        $okTotal = ([decimal]$orden.total -eq [decimal]$caso.Total)
        Write-Host ("  Total  esperado {0} | obtenido {1} {2}" -f $caso.Total, $orden.total, $orden.moneda) `
                   -ForegroundColor $(if ($okTotal) { 'Green' } else { 'Red' })
        if (-not $okTotal) { $fallos++ }
        Write-Host "  orderId: $($orden.orderId)   estado: $($orden.estado)"
        Write-Host "  Asincrono esperado (verlo en el dashboard): $($caso.Asincrono)" -ForegroundColor DarkGray
    } else {
        Write-Host "  Cuerpo: $($r.Cuerpo)"
    }

    if ($i -lt $casos.Count - 1) { Start-Sleep -Seconds $segundosEntrePeticiones }
}

Write-Host ""
if ($fallos -eq 0) {
    Write-Host "Todo lo comprobable desde HTTP coincide. Revisa ahora el panel de administracion: http://localhost:8081/admin.html" -ForegroundColor Green
    exit 0
} else {
    Write-Host "$fallos comprobacion(es) no coincidieron. Revisa las ventanas de los servicios." -ForegroundColor Red
    exit 1
}
