package co.edu.proyecto.orden.infraestructura.persistencia;

import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.EstadoOrden;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;
import org.springframework.stereotype.Component;

import java.util.List;

/** Traduce en los dos sentidos entre la entidad de dominio Orden y las filas de JPA. */
@Component
public class OrdenMapper {

    public OrdenEntity aEntidad(Orden orden) {
        OrdenEntity entidad = new OrdenEntity(orden.id().valor(), orden.cliente(), orden.creadaEn());
        entidad.actualizar(orden.estado().name(), orden.total().monto(), orden.total().moneda());
        for (ItemOrden item : orden.items()) {
            entidad.agregarItem(new ItemOrdenEntity(item.sku(), item.nombre(), item.cantidad(),
                    item.precioUnitario().monto(), item.precioUnitario().moneda()));
        }
        return entidad;
    }

    public Orden aDominio(OrdenEntity entidad) {
        List<ItemOrden> items = entidad.getItems().stream()
                .map(item -> new ItemOrden(item.getSku(), item.getNombre(), item.getCantidad(),
                        new Dinero(item.getPrecioMonto(), item.getPrecioMoneda())))
                .toList();
        return Orden.reconstituir(new OrdenId(entidad.getId()), entidad.getCliente(), items,
                EstadoOrden.valueOf(entidad.getEstado()),
                new Dinero(entidad.getTotalMonto(), entidad.getTotalMoneda()), entidad.getCreadaEn());
    }

    /** Copia a una fila ya guardada solo lo que puede cambiar: estado y total. */
    public void actualizar(OrdenEntity entidad, Orden orden) {
        entidad.actualizar(orden.estado().name(), orden.total().monto(), orden.total().moneda());
    }
}
