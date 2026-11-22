package co.edu.proyecto.common.nombres;

/** Nombres de los exchanges de RabbitMQ. */
public final class Exchanges {

    /** Fanout que copia cada comando a las tres colas de trabajo. */
    public static final String TAREAS = "order_tasks";
    /** Fanout que recibe los mensajes que fallaron tres veces (dead-letter exchange). */
    public static final String TAREAS_FALLIDAS = "order_tasks_dlx";

    private Exchanges() {
    }
}
