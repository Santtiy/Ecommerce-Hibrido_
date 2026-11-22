# Revisión SOLID y patrones

Rutas abreviadas: `orden/` = `backend/order-api/src/main/java/co/edu/proyecto/orden/`; `common/` y `mensajeria/` son
los módulos `backend/common` y `backend/common-mensajeria`.

## Principios SOLID

| Principio | Dónde se ve | Qué problema evita |
|---|---|---|
| **S** Responsabilidad única | `Orden` protege sus reglas; `Dinero` solo hace aritmética; `RegistrarOrdenService` solo orquesta; `OrdenMapper` solo traduce a JPA. Cada servicio separa su configuración en `ConfiguracionDominio` (beans de negocio), `ConfiguracionMensajeria` (Kafka/RabbitMQ) y, si tiene web, `ConfiguracionWeb`/`ConfiguracionWebSocket`. En los workers, `ReglaTarea` decide, `EjecutorTarea` publica y el consumidor solo escucha su cola. `"pending"` vive en `ProgresoOrden`, el único que lo usa. En el front, cada módulo de `frontend/js/` tiene una tarea (`config`, `api`, `canal`, `tablero`…) | Que un cambio en la base de datos, en Kafka o en la página obligue a tocar las reglas de negocio |
| **O** Abierto/cerrado | `orden/dominio/descuentos/` y `orden/dominio/impuestos/`: un descuento nuevo es una clase nueva y un `@Bean` en `ConfiguracionDominio`. `TipoTarea` conoce la cola y el topic de cada tarea: un worker nuevo es un valor del enum, una `ReglaTarea` y un consumidor de ~10 líneas, sin modificar `EjecutorTarea` ni a los demás workers. Un consumidor nuevo de Kafka es un grupo más, sin tocar al productor | Editar `Orden` o el código común cada vez que cambia una regla. No hay ningún `if`/`switch` por tipo de estrategia ni por worker |
| **L** Sustitución de Liskov | Las 4 estrategias cumplen el contrato documentado en sus interfaces (no `null`, no negativos, misma moneda); `CalculoTotalTest` las intercambia. `ReglaPago`, `ReglaInventario` y `ReglaNotificacion` son intercambiables dentro de `EjecutorTarea` (`EjecutorTareaTest` le pasa una regla mínima escrita como lambda). Todos los consumidores tratan los estados igual a través de `EstadoTarea` (`esExito()`, `tipo()`) | Un total corrupto o un worker que se comporte distinto por depender de una implementación concreta |
| **I** Segregación de interfaces | Puertos pequeños: `OrdenRepository` (2 métodos), `BandejaSalida` (1), `AuditoriaRepository` (1), `ReglaTarea` (1). `common-mensajeria` declara Kafka y RabbitMQ como opcionales: el dashboard y analytics no reciben RabbitMQ | Que un adaptador, un mock o un servicio tenga que depender de métodos o librerías que no usa |
| **D** Inversión de dependencias | Los casos de uso dependen de puertos: `OrdenRepository` en `orden/dominio/puertos/` y `BandejaSalida`/`AuditoriaRepository` en `orden/aplicacion/puertos/`; los adaptadores de `infraestructura/` los implementan. `EjecutorTarea` recibe la `ReglaTarea` por constructor. Inyección **solo por constructor** (ningún `@Autowired` en atributos). El `DominioPuroTest` de cada servicio con `dominio/` verifica que no importa Spring, Jakarta, Kafka ni RabbitMQ | Que el dominio dependa de la tecnología y no se pueda probar sin base de datos ni broker |

## Patrones del enunciado

| Patrón | Clases | Prueba que lo demuestra |
|---|---|---|
| Strategy | `EstrategiaDescuento` (`DescuentoPorMonto`, `SinDescuento`), `EstrategiaImpuesto` (`IvaColombia`, `SinImpuesto`) | `CalculoTotalTest` |
| Builder | `OrdenBuilder` (el constructor de `Orden` es de paquete) | `OrdenBuilderTest` |
| Repository | Puerto `OrdenRepository`, adaptador `OrdenRepositoryJpa` | `RegistrarOrdenServiceTest` (con mock) |
| Entidad rica y value objects | `Orden` (sin setters), records `Dinero`, `OrdenId`, `ItemOrden` con Fail-Fast | `OrdenTest`, `DineroTest`, `ItemOrdenTest`, `OrdenMapperTest` |

Diagrama de clases: [diagramas/clases-dominio.md](diagramas/clases-dominio.md).

## Otros patrones usados

| Patrón | Dónde | Para qué |
|---|---|---|
| Outbox (bandeja de salida) | `BandejaSalida`, `BandejaSalidaJpa`, `PublicadorBandejaSalida` (`orden/infraestructura/bandejasalida/`) | Que guardar la orden y publicar su evento no pueda quedar a medias |
| Consumidor idempotente | `RegistrarResultadoService`, `TableroOrdenes`, `AgregadorVentas`, auditoría con `UNIQUE` | Que un mensaje repetido no cambie el resultado |
| Puertos y adaptadores | `dominio/puertos/` y `aplicacion/puertos/` ↔ `infraestructura/<tecnología>/` | Probar el dominio y los casos de uso sin infraestructura |
| Composición en los workers | `EjecutorTarea` (`mensajeria/tareas/`) recibe la `ReglaTarea` y el topic de cada worker; `ReglaPago`, `ReglaInventario` y `ReglaNotificacion` cumplen el mismo contrato (`common/contratos/ReglaTarea`) | No copiar el mismo consumidor en tres servicios: un worker nuevo solo aporta su regla, su cola y su topic |
| Enum con comportamiento | `EstadoTarea` (sabe su `TipoTarea` y si es éxito; `exigirTipo` rechaza un estado que llega por el topic equivocado) y `TipoTarea` (sabe su cola y su topic) | Sustituir textos sueltos y `if` por tipos: un estado mal escrito ya no cancela una orden |
| Template method | `ConsumidorQueReleeTodo` (`mensajeria/kafka/`): fija la relectura desde el inicio; analytics y el dashboard solo declaran sus `@KafkaListener` | Reconstruir el estado en memoria al arrancar sin repetir el código |
| Configuración base reutilizable | `ConfiguracionKafkaBase`, `ConfiguracionRabbitBase`, `TopologiaWorker` importadas con `@Import` desde la `ConfiguracionMensajeria` de cada servicio | Un solo `DefaultErrorHandler`, un solo conversor JSON y una sola topología de fallidos |
| Datos de prueba compartidos | `DatosDePrueba` y `VerificadorDominioPuro` en el test-jar de `common` (`scope test`) | No construir a mano los mismos contratos en cada módulo de pruebas |
