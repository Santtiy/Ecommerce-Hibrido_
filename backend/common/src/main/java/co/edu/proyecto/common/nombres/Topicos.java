package co.edu.proyecto.common.nombres;

/** Nombres de los topics de Kafka. Ningún servicio los escribe a mano. */
public final class Topicos {

    public static final String NUEVAS_ORDENES = "new_orders";
    public static final String EVENTOS_PAGO = "billing_events";
    public static final String EVENTOS_INVENTARIO = "inventory_events";
    public static final String EVENTOS_NOTIFICACION = "notification_events";

    private Topicos() {
    }
}
