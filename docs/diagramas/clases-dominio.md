# Clases del dominio de order-api

Paquete `co.edu.proyecto.orden.dominio` (Java puro: lo comprueba `DominioPuroTest`), más los casos de uso de
`aplicacion/` y los adaptadores de `infraestructura/` que implementan los puertos.

```mermaid
classDiagram
    direction LR

    class Orden {
        -OrdenId id
        -String cliente
        -List~ItemOrden~ items
        -Instant creadaEn
        -EstadoOrden estado
        -Dinero total
        ~Orden(OrdenId, String, List~ItemOrden~, Instant)
        +reconstituir(...)$ Orden
        +calcularTotal(EstrategiaDescuento, EstrategiaImpuesto) Dinero
        +confirmarPago()
        +cancelar()
        +estaCreada() boolean
    }
    class EstadoOrden {
        <<enumeration>>
        CREADA
        PAGADA
        CANCELADA
    }
    class OrdenId {
        <<record>>
        UUID valor
        +nuevo()$ OrdenId
    }
    class ItemOrden {
        <<record>>
        String sku
        String nombre
        int cantidad
        Dinero precioUnitario
        +subtotal() Dinero
    }
    class Dinero {
        <<record>>
        BigDecimal monto
        String moneda
        +de(String, String)$ Dinero
        +cero(String)$ Dinero
        +sumar(Dinero) Dinero
        +restar(Dinero) Dinero
        +multiplicar(int) Dinero
        +porcentaje(int) Dinero
        +esMayorQue(Dinero) boolean
        +esPositivo() boolean
    }
    class OrdenBuilder {
        -Clock reloj
        -String cliente
        -List~ItemOrden~ items
        +cliente(String) OrdenBuilder
        +agregarItem(ItemOrden) OrdenBuilder
        +construir() Orden
    }

    class EstrategiaDescuento {
        <<interface>>
        +aplicar(Dinero subtotal) Dinero
    }
    class DescuentoPorMonto
    class SinDescuento
    class EstrategiaImpuesto {
        <<interface>>
        +aplicar(Dinero base) Dinero
    }
    class IvaColombia
    class SinImpuesto

    class OrdenRepository {
        <<interface>>
        +guardar(Orden)
        +buscarPorId(OrdenId) Optional~Orden~
    }

    class RegistrarOrdenService {
        +registrar(String cliente, List~ItemOrden~) Orden
    }
    class RegistrarResultadoService {
        +registrar(TipoTarea, ResultadoTarea)
    }
    class BandejaSalida {
        <<interface>>
        +encolar(OrdenCreada)
    }
    class AuditoriaRepository {
        <<interface>>
        +registrarSiNoExiste(RegistroAuditoria)
    }
    class OrdenRepositoryJpa
    class BandejaSalidaJpa
    class AuditoriaRepositoryJpa

    Orden *-- "1..*" ItemOrden
    Orden *-- OrdenId
    Orden --> EstadoOrden
    Orden --> Dinero : total
    ItemOrden --> Dinero : precioUnitario
    OrdenBuilder ..> Orden : construye
    Orden ..> EstrategiaDescuento : usa
    Orden ..> EstrategiaImpuesto : usa
    EstrategiaDescuento <|.. DescuentoPorMonto
    EstrategiaDescuento <|.. SinDescuento
    EstrategiaImpuesto <|.. IvaColombia
    EstrategiaImpuesto <|.. SinImpuesto

    RegistrarOrdenService --> OrdenRepository
    RegistrarOrdenService --> BandejaSalida
    RegistrarOrdenService --> EstrategiaDescuento
    RegistrarOrdenService --> EstrategiaImpuesto
    RegistrarOrdenService ..> OrdenBuilder
    RegistrarResultadoService --> OrdenRepository
    RegistrarResultadoService --> AuditoriaRepository

    OrdenRepository <|.. OrdenRepositoryJpa
    BandejaSalida <|.. BandejaSalidaJpa
    AuditoriaRepository <|.. AuditoriaRepositoryJpa
```

| Elemento | Requisito | Detalle |
|---|---|---|
| `Orden` | Entidad rica | Sin setters; el constructor es de paquete (solo lo usa `OrdenBuilder`); `reconstituir` es para el mapper de JPA |
| `Dinero`, `OrdenId`, `ItemOrden` | Records con Fail-Fast | Validan en el constructor compacto y lanzan `OrdenInvalidaException` |
| `EstrategiaDescuento`, `EstrategiaImpuesto` | Strategy | `DescuentoPorMonto`: 10 % si el subtotal supera 100.000; `IvaColombia`: 19 %. Se eligen en `ConfiguracionDominio` |
| `OrdenBuilder` | Builder | Exige cliente y al menos un ítem; recibe un `Clock` para poder probar la fecha |
| `OrdenRepository` | Repository | Puerto en `dominio/puertos/`; `BandejaSalida` y `AuditoriaRepository` están en `aplicacion/puertos/` |
