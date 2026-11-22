package co.edu.proyecto.common.nombres;

/**
 * Grupos de consumidores de Kafka. Cada grupo recibe su propia copia de cada evento;
 * dos servicios en el mismo grupo se repartirían los eventos en lugar de recibirlos todos.
 */
public final class GruposConsumidores {

    public static final String ANALYTICS = "analytics-group";
    public static final String PROCESSOR = "processor-group";
    public static final String DASHBOARD = "dashboard-group";
    public static final String ORDER_API = "order-api-group";

    private GruposConsumidores() {
    }
}
