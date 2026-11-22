package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/** Única forma de crear una orden nueva: impide que exista a medio construir (RN-01). */
public class OrdenBuilder {

    private final Clock reloj;
    private final List<ItemOrden> items = new ArrayList<>();
    private String cliente;

    public OrdenBuilder() {
        this(Clock.systemUTC());
    }

    /** Permite fijar la hora en las pruebas. */
    public OrdenBuilder(Clock reloj) {
        this.reloj = reloj;
    }

    public OrdenBuilder cliente(String cliente) {
        this.cliente = cliente;
        return this;
    }

    public OrdenBuilder agregarItem(ItemOrden item) {
        items.add(item);
        return this;
    }

    public Orden construir() {
        if (cliente == null || cliente.isBlank()) {
            throw new OrdenInvalidaException("El cliente de la orden es obligatorio");
        }
        if (items.isEmpty()) {
            throw new OrdenInvalidaException("La orden debe tener al menos un ítem");
        }
        return new Orden(OrdenId.nuevo(), cliente, items, reloj.instant());
    }
}
