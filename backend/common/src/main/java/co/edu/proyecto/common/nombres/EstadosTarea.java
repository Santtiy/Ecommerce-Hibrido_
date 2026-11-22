package co.edu.proyecto.common.nombres;

/** Valores posibles del campo status de ResultadoTarea, más el valor "pending" del dashboard. */
public final class EstadosTarea {

    public static final String PAGO_EXITOSO = "PAYMENT_SUCCESS";
    public static final String PAGO_FALLIDO = "PAYMENT_FAILED";
    public static final String STOCK_RESERVADO = "STOCK_RESERVED";
    public static final String SIN_STOCK = "STOCK_OUT_OF_STOCK";
    public static final String CORREO_ENVIADO = "EMAIL_SENT";
    public static final String PENDIENTE = "pending";

    private EstadosTarea() {
    }
}
