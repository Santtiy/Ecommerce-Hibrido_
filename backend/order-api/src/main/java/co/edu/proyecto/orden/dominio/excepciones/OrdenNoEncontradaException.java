package co.edu.proyecto.orden.dominio.excepciones;

import co.edu.proyecto.orden.dominio.OrdenId;

/** Se pidió una orden que no está guardada. No son datos inválidos: la orden simplemente no existe. */
public class OrdenNoEncontradaException extends DominioException {

    public OrdenNoEncontradaException(OrdenId id) {
        super("No existe la orden " + id);
    }
}
