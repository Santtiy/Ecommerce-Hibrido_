# Correcciones de la auditoría y dónde están

| Id | Problema en la versión anterior | Solución | Dónde verlo |
|---|---|---|---|
| C-01 | El evento y el comando llevaban solo el primer ítem; RN-12 se podía saltar | `OrdenCreada` y `ProcesarOrden` llevan `List<ItemEvento>`; la regla revisa todos | `common/.../contratos/`, `ReglaInventario`, `ReglaInventarioTest.unItemExcedidoAunqueNoSeaElPrimeroDejaSinStock` |
| C-02 | El 202 no esperaba a Kafka: si fallaba, el evento se perdía (escritura dual) | Bandeja de salida en la misma transacción + publicador que solo borra tras la confirmación | `RegistrarOrdenService`, `BandejaSalidaJpa`, `PublicadorBandejaSalida` y su prueba |
| C-03 | Nadie llamaba a `confirmarPago()` ni a `cancelar()` | `order-api` consume `billing_events` y aplica el resultado | `ConsumidorResultados`, `RegistrarResultadoService` |
| C-04 | No había auditoría en PostgreSQL | Tabla `auditoria_ordenes` con una fila por resultado | `schema.sql`, `AuditoriaRepositoryJpa` |
| C-05 | `ItemOrden` aceptaba precio 0 | Validado en el constructor compacto + `CHECK` en BD | `ItemOrden`, `ItemOrdenTest.rechazaPrecioCero` |
| C-06 | `calcularTotal()` funcionaba en cualquier estado | Solo en `CREADA` | `Orden`, `OrdenTest.noSePuedeCalcularElTotalDeUnaOrdenCancelada` |
| C-07 | La fecha del evento era la de publicación | Usa `creadaEn` de la orden | `RegistrarOrdenService.aEvento`, `RegistrarOrdenServiceTest` |
| C-08 | Estado del dashboard y analytics en memoria, sin límite y perdido al reiniciar | Relectura de los topics al arrancar; máximo 1.000 órdenes en el tablero | `ConsumidorResultados` (dashboard), `ConsumidorNuevasOrdenes` (analytics), `TableroOrdenes` |
| C-09 | Los consumidores de Kafka no tenían reintentos propios | `DefaultErrorHandler` (3 intentos, 1 s) en cada servicio | `ConfiguracionKafka` de cada servicio |
| C-10 | Resultados repetidos podían duplicar efectos | Auditoría con `UNIQUE` + `ON CONFLICT DO NOTHING`; el estado solo cambia si sigue `CREADA`; tablero y agregador idempotentes | `SpringDataAuditoria`, `RegistrarResultadoServiceTest.unPagoRepetidoNoVuelveAGuardarLaOrden`, `AgregadorVentasTest` |
| C-11 | Releer `new_orders` duplicaría la bitácora | La bitácora se vacía al arrancar | `Bitacora`, `BitacoraTest.alCrearseVaciaUnArchivoExistente` |
| C-12 | CORS y WebSocket abiertos a `*` | `APP_ORIGENES` (por defecto `http://localhost:8081`) | `ConfiguracionCors`, `ConfiguracionWebSocket` |
| C-13 | Esquema generado por Hibernate, sin `CHECK` | `schema.sql` con `CHECK` y `UNIQUE`; `ddl-auto: none` | `backend/order-api/src/main/resources/schema.sql` |
| C-14 | `rabbitListenerContainerFactory` repetido sin aportar | Eliminado: basta el `Jackson2JsonMessageConverter` | `ConfiguracionRabbit` de los workers |
| C-15 | Fábricas de productor y consumidor de Kafka escritas a mano en 6 servicios | Todo en `application.yml`; productores envían JSON como texto | `application.yml` de cada servicio |
| C-16 | Nombres de topics y colas escritos como texto en varios sitios | Clases de constantes | `common/.../nombres/` |
| C-17 | `PublicadorEventosLog` y perfil `sin-kafka` sobrantes | No existen | — |
| C-18 | Métodos sin uso (`OrdenId.de`, `leerTodo`) | No existen | — |
| C-19 | `reservarBloque()` sin uso | Se usa al arrancar analytics | `DemostracionMemoria` |
| C-20 | La E/S se medía en milisegundos (casi todo "0 ms") | Microsegundos | `Bitacora.escribir`, `OperacionES` |
| C-21 | `SinDescuento`/`SinImpuesto` sin pruebas | Probadas | `CalculoTotalTest.sinDescuentoNiImpuestoElTotalEsElSubtotal` |
| C-22 | La pureza del dominio se verificaba "por inspección" | Prueba automática | `DominioPuroTest` |
