package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.descuentos.EstrategiaDescuento;
import co.edu.proyecto.orden.dominio.excepciones.EstadoInvalidoException;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import co.edu.proyecto.orden.dominio.impuestos.EstrategiaImpuesto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad rica: guarda su estado y solo lo cambia con métodos de negocio.
 * No tiene setters, así que ninguna parte del sistema puede saltarse sus reglas (RN-04, RN-06, RN-07, RN-15).
 */
public class Orden {

    private final OrdenId id;
    private final String cliente;
    private final List<ItemOrden> items;
    private final Instant creadaEn;
    private EstadoOrden estado;
    private Dinero total;

    /** Solo OrdenBuilder crea órdenes nuevas (por eso el constructor no es público). */
    Orden(OrdenId id, String cliente, List<ItemOrden> items, Instant creadaEn) {
        this.id = id;
        this.cliente = cliente;
        this.items = new ArrayList<>(items);
        this.creadaEn = creadaEn;
        this.estado = EstadoOrden.CREADA;
        this.total = Dinero.cero(items.get(0).precioUnitario().moneda());
    }

    /** Reconstruye una orden ya guardada. Solo debe usarlo el adaptador de persistencia. */
    public static Orden reconstituir(OrdenId id, String cliente, List<ItemOrden> items,
                                     EstadoOrden estado, Dinero total, Instant creadaEn) {
        if (id == null || cliente == null || items == null || items.isEmpty()
                || estado == null || total == null || creadaEn == null) {
            throw new OrdenInvalidaException("Datos incompletos para reconstruir la orden " + id);
        }
        Orden orden = new Orden(id, cliente, items, creadaEn);
        orden.estado = estado;
        orden.total = total;
        return orden;
    }

    /** RN-15: solo se agregan productos a una orden CREADA. */
    public void agregarProducto(ItemOrden item) {
        exigirEstado(EstadoOrden.CREADA, "agregar productos");
        if (item == null) {
            throw new OrdenInvalidaException("El ítem no puede ser nulo");
        }
        items.add(item);
    }

    /** RN-04: total = suma de subtotales, menos el descuento, más el impuesto (en ese orden). RN-15: solo en CREADA. */
    public Dinero calcularTotal(EstrategiaDescuento descuento, EstrategiaImpuesto impuesto) {
        exigirEstado(EstadoOrden.CREADA, "calcular el total");
        if (descuento == null || impuesto == null) {
            throw new OrdenInvalidaException("Las estrategias de descuento e impuesto son obligatorias");
        }
        Dinero conDescuento = descuento.aplicar(subtotal());
        this.total = impuesto.aplicar(conDescuento);
        return total;
    }

    /** CREADA → PAGADA. */
    public void confirmarPago() {
        exigirEstado(EstadoOrden.CREADA, "confirmar el pago");
        estado = EstadoOrden.PAGADA;
    }

    /** CREADA → CANCELADA. RN-07: una orden pagada no puede cancelarse. */
    public void cancelar() {
        if (estado == EstadoOrden.PAGADA) {
            throw new EstadoInvalidoException("Una orden pagada no puede cancelarse");
        }
        exigirEstado(EstadoOrden.CREADA, "cancelar");
        estado = EstadoOrden.CANCELADA;
    }

    public boolean estaCreada() {
        return estado == EstadoOrden.CREADA;
    }

    public OrdenId id() {
        return id;
    }

    public String cliente() {
        return cliente;
    }

    public EstadoOrden estado() {
        return estado;
    }

    public Dinero total() {
        return total;
    }

    public Instant creadaEn() {
        return creadaEn;
    }

    /** Copia inmutable: quien la reciba no puede cambiar los ítems de la orden. */
    public List<ItemOrden> items() {
        return List.copyOf(items);
    }

    private Dinero subtotal() {
        Dinero suma = Dinero.cero(total.moneda());
        for (ItemOrden item : items) {
            suma = suma.sumar(item.subtotal());
        }
        return suma;
    }

    private void exigirEstado(EstadoOrden esperado, String accion) {
        if (estado != esperado) {
            throw new EstadoInvalidoException(
                    "Solo se puede " + accion + " en una orden " + esperado + "; estado actual: " + estado);
        }
    }
}
