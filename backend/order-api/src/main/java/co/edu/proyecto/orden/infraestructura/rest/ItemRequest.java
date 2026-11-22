package co.edu.proyecto.orden.infraestructura.rest;

import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.ItemOrden;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Un ítem tal como llega en el JSON de POST /order. */
public record ItemRequest(
        @NotBlank String sku,
        @NotBlank String nombre,
        @Min(1) int cantidad,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal precioUnitario) {

    ItemOrden aDominio(String moneda) {
        return new ItemOrden(sku, nombre, cantidad, new Dinero(precioUnitario, moneda));
    }
}
