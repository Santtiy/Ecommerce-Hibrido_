package co.edu.proyecto.orden.dominio;

/** Estados válidos de una orden (RN-13). Las transiciones las controla Orden. */
public enum EstadoOrden {
    CREADA,
    PAGADA,
    CANCELADA
}
