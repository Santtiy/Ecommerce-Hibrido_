package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.orden.aplicacion.puertos.BandejaSalida;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenBuilder;
import co.edu.proyecto.orden.dominio.descuentos.EstrategiaDescuento;
import co.edu.proyecto.orden.dominio.impuestos.EstrategiaImpuesto;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso "registrar una orden": la construye, calcula su total, la guarda y deja su evento
 * en la bandeja de salida. Guardar y encolar ocurren en la misma transacción: o quedan los dos, o ninguno.
 */
public class RegistrarOrdenService {

    private final OrdenRepository ordenes;
    private final BandejaSalida bandejaSalida;
    private final EstrategiaDescuento descuento;
    private final EstrategiaImpuesto impuesto;

    public RegistrarOrdenService(OrdenRepository ordenes, BandejaSalida bandejaSalida,
                                 EstrategiaDescuento descuento, EstrategiaImpuesto impuesto) {
        this.ordenes = ordenes;
        this.bandejaSalida = bandejaSalida;
        this.descuento = descuento;
        this.impuesto = impuesto;
    }

    @Transactional
    public Orden registrar(String cliente, List<ItemOrden> items) {
        Orden orden = construirOrden(cliente, items);
        orden.calcularTotal(descuento, impuesto);
        ordenes.guardar(orden);
        bandejaSalida.encolar(aEvento(orden));
        return orden;
    }

    private Orden construirOrden(String cliente, List<ItemOrden> items) {
        OrdenBuilder builder = new OrdenBuilder().cliente(cliente);
        items.forEach(builder::agregarItem);
        return builder.construir();
    }

    /** El evento lleva todos los ítems y la fecha real de creación de la orden. */
    private OrdenCreada aEvento(Orden orden) {
        List<ItemEvento> items = orden.items().stream()
                .map(item -> new ItemEvento(item.sku(), item.nombre(), item.cantidad(), item.precioUnitario().monto()))
                .toList();
        return new OrdenCreada(orden.id().toString(), orden.cliente(), items,
                orden.total().monto(), orden.total().moneda(), orden.creadaEn());
    }
}
