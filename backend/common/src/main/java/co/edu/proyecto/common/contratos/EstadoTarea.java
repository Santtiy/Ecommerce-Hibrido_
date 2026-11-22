package co.edu.proyecto.common.contratos;

import co.edu.proyecto.common.nombres.TipoTarea;

/**
 * Resultado posible de una tarea: el campo {@code status} de {@link ResultadoTarea}.
 * El nombre de cada valor es el texto exacto que viaja en el JSON (Jackson serializa un enum por su nombre),
 * por eso no se deben renombrar. Un texto que no sea uno de estos valores no se puede leer, así que un estado
 * mal escrito nunca pasa como válido.
 *
 * <p>Cada estado sabe de qué tipo de tarea es y si es un éxito; así todos los consumidores lo interpretan igual.
 */
public enum EstadoTarea {

    PAYMENT_SUCCESS(TipoTarea.PAGO, true),
    PAYMENT_FAILED(TipoTarea.PAGO, false),
    STOCK_RESERVED(TipoTarea.INVENTARIO, true),
    STOCK_OUT_OF_STOCK(TipoTarea.INVENTARIO, false),
    EMAIL_SENT(TipoTarea.NOTIFICACION, true);

    private final TipoTarea tipo;
    private final boolean exito;

    EstadoTarea(TipoTarea tipo, boolean exito) {
        this.tipo = tipo;
        this.exito = exito;
    }

    /** Tipo de tarea (worker) que puede producir este estado. */
    public TipoTarea tipo() {
        return tipo;
    }

    /** {@code true} si la tarea se cumplió (pago aprobado, stock reservado, correo enviado). */
    public boolean esExito() {
        return exito;
    }

    /**
     * Comprueba que el estado corresponde al tipo de tarea por el que llegó (por ejemplo, que un resultado leído
     * de {@code billing_events} sea un estado de pago).
     *
     * @return este mismo estado, para encadenar
     * @throws IllegalArgumentException si el estado es de otro tipo de tarea
     */
    public EstadoTarea exigirTipo(TipoTarea esperado) {
        if (tipo != esperado) {
            throw new IllegalArgumentException(
                    "El estado " + name() + " es de " + tipo + " y llegó como resultado de " + esperado);
        }
        return this;
    }
}
