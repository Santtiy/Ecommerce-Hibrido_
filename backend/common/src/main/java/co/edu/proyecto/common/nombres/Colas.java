package co.edu.proyecto.common.nombres;

/** Nombres de las colas de RabbitMQ. */
public final class Colas {

    public static final String PAGOS = "billing_queue";
    public static final String INVENTARIO = "inventory_queue";
    public static final String NOTIFICACIONES = "notification_queue";
    public static final String FALLIDAS = "failed_tasks";

    private Colas() {
    }
}
