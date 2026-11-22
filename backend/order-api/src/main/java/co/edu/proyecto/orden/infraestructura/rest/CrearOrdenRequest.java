package co.edu.proyecto.orden.infraestructura.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Cuerpo de POST /order. Es un record: inmutable y validado antes de llegar al caso de uso. */
public record CrearOrdenRequest(@NotBlank String cliente, @NotEmpty List<@Valid ItemRequest> items) {

    public CrearOrdenRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
