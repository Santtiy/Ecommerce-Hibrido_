package co.edu.proyecto.orden.dominio.excepciones;

/** Transición de estado no permitida (RN-06), por ejemplo cancelar una orden pagada (RN-07). */
public class EstadoInvalidoException extends DominioException {

    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
