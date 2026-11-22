package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;

/** Línea de una orden. RN-02: la cantidad y el precio unitario son mayores que cero. */
public record ItemOrden(String sku, String nombre, int cantidad, Dinero precioUnitario) {

    public ItemOrden {
        if (sku == null || sku.isBlank()) {
            throw new OrdenInvalidaException("El sku del ítem es obligatorio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new OrdenInvalidaException("El nombre del ítem es obligatorio");
        }
        if (cantidad <= 0) {
            throw new OrdenInvalidaException("La cantidad debe ser mayor que cero: " + cantidad);
        }
        if (precioUnitario == null || !precioUnitario.esPositivo()) {
            throw new OrdenInvalidaException("El precio unitario debe ser mayor que cero");
        }
    }

    public Dinero subtotal() {
        return precioUnitario.multiplicar(cantidad);
    }
}
