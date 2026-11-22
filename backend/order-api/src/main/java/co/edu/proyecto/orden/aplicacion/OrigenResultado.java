package co.edu.proyecto.orden.aplicacion;

/** Qué worker produjo un resultado. Solo el pago cambia el estado de la orden (RN-16). */
public enum OrigenResultado {
    PAGO,
    INVENTARIO,
    NOTIFICACION
}
