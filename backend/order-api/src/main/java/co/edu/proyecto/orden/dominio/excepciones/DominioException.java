package co.edu.proyecto.orden.dominio.excepciones;

/** Raíz de las excepciones de negocio: el mensaje explica qué regla se rompió. */
public class DominioException extends RuntimeException {

    public DominioException(String mensaje) {
        super(mensaje);
    }
}
