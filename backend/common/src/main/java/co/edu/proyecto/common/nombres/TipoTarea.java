package co.edu.proyecto.common.nombres;

/**
 * Las tres tareas en que se reparte cada orden: una por worker. Cada tipo conoce su cola de RabbitMQ y su topic
 * de resultados en Kafka, así ningún servicio tiene que traducir un tipo a un nombre con {@code if} ni {@code switch}:
 * un tipo nuevo se agrega aquí, con sus nombres, sin tocar a quien los usa.
 *
 * <p>Su {@link #name()} es el texto que order-api guarda en {@code auditoria_ordenes.origen}
 * ({@code PAGO}, {@code INVENTARIO}, {@code NOTIFICACION}): no se debe renombrar ningún valor.
 *
 * <p>Las anotaciones ({@code @KafkaListener}, {@code @RabbitListener}) siguen usando las constantes de
 * {@link Topicos} y {@link Colas} porque Java exige ahí constantes de compilación.
 */
public enum TipoTarea {

    PAGO(Colas.PAGOS, Topicos.EVENTOS_PAGO),
    INVENTARIO(Colas.INVENTARIO, Topicos.EVENTOS_INVENTARIO),
    NOTIFICACION(Colas.NOTIFICACIONES, Topicos.EVENTOS_NOTIFICACION);

    private final String cola;
    private final String topico;

    TipoTarea(String cola, String topico) {
        this.cola = cola;
        this.topico = topico;
    }

    /** Cola de RabbitMQ de donde el worker de este tipo toma sus comandos. */
    public String cola() {
        return cola;
    }

    /** Topic de Kafka donde el worker de este tipo publica sus resultados. */
    public String topico() {
        return topico;
    }
}
