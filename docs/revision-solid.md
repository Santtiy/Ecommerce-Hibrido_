# Revisión SOLID y patrones

Rutas relativas a `backend/order-api/src/main/java/co/edu/proyecto/orden/` salvo que se indique otra cosa.

## Principios SOLID

| Principio | Dónde se ve | Qué problema evita |
|---|---|---|
| **S** Responsabilidad única | `Orden` protege sus reglas; `Dinero` solo hace aritmética; `RegistrarOrdenService` solo orquesta; `OrdenMapper` solo traduce a JPA | Que un cambio en la base de datos o en Kafka obligue a tocar las reglas de negocio |
| **O** Abierto/cerrado | `dominio/descuentos/` y `dominio/impuestos/`: para un descuento nuevo se crea una clase y se cambia un `@Bean` en `ConfiguracionDominio` | Editar `Orden` cada vez que cambia una regla de precios. No hay ningún `if`/`switch` por tipo de estrategia |
| **L** Sustitución de Liskov | Las 4 estrategias cumplen el contrato documentado en sus interfaces (no `null`, no negativos, misma moneda). `CalculoTotalTest` las intercambia | Un total corrupto por una implementación que rompa el contrato |
| **I** Segregación de interfaces | Tres puertos pequeños: `OrdenRepository`, `BandejaSalida`, `AuditoriaRepository` | Que un adaptador o un mock tenga que implementar métodos que no usa |
| **D** Inversión de dependencias | Los casos de uso dependen de los puertos de `dominio/puertos/`; los adaptadores de `infraestructura/` los implementan. `DominioPuroTest` verifica que `dominio/` no importa Spring, Jakarta ni Kafka | Que el dominio dependa de la tecnología y no se pueda probar sin base de datos ni broker |

## Patrones del enunciado

| Patrón | Clases | Prueba que lo demuestra |
|---|---|---|
| Strategy | `EstrategiaDescuento` (`DescuentoPorMonto`, `SinDescuento`), `EstrategiaImpuesto` (`IvaColombia`, `SinImpuesto`) | `CalculoTotalTest` |
| Builder | `OrdenBuilder` (el constructor de `Orden` es de paquete) | `OrdenBuilderTest` |
| Repository | Puerto `OrdenRepository`, adaptador `OrdenRepositoryJpa` | `RegistrarOrdenServiceTest` (con mock) |

## Otros patrones usados

| Patrón | Dónde | Para qué |
|---|---|---|
| Outbox (bandeja de salida) | `BandejaSalida`, `PublicadorBandejaSalida` | Que guardar la orden y publicar su evento no pueda quedar a medias |
| Consumidor idempotente | `RegistrarResultadoService`, `TableroOrdenes`, `AgregadorVentas`, auditoría con `UNIQUE` | Que un mensaje repetido no cambie el resultado |
| Puertos y adaptadores | `dominio/puertos/` ↔ `infraestructura/` | Probar el dominio y los casos de uso sin infraestructura |
