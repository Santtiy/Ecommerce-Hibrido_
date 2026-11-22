package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;

import java.util.UUID;

/** Identificador único de una orden (RN-08). */
public record OrdenId(UUID valor) {

    public OrdenId {
        if (valor == null) {
            throw new OrdenInvalidaException("El identificador de la orden es obligatorio");
        }
    }

    public static OrdenId nuevo() {
        return new OrdenId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
