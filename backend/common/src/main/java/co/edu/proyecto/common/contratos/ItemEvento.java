package co.edu.proyecto.common.contratos;

import java.math.BigDecimal;

/** Un ítem de la orden tal como viaja en los mensajes: el inventario necesita sku y cantidad de cada uno. */
public record ItemEvento(String sku, String nombre, int cantidad, BigDecimal precioUnitario) {

    public ItemEvento {
        Validaciones.textoObligatorio(sku, "sku");
        Validaciones.textoObligatorio(nombre, "nombre");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero: " + cantidad);
        }
        Validaciones.positivo(precioUnitario, "precioUnitario");
    }
}
