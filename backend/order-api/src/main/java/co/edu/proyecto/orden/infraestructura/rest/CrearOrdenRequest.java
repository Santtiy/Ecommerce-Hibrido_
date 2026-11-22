package co.edu.proyecto.orden.infraestructura.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Cuerpo de POST /order. Los precios unitarios son bases sin IVA; la orden aplica la estrategia de impuesto. */
public record CrearOrdenRequest(@NotBlank String cliente, @NotEmpty List<@Valid ItemRequest> items) {

    public CrearOrdenRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
