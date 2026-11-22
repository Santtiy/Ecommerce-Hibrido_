package co.edu.proyecto.notification.infraestructura.rabbitmq;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.nombres.Colas;
import co.edu.proyecto.mensajeria.tareas.EjecutorTarea;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Escucha la cola {@code notification_queue} y entrega cada comando al {@link EjecutorTarea} de este worker, que aplica
 * la regla de notificación (ReglaNotificacion) y publica el resultado en Kafka. El ack ocurre al volver sin excepción (después de que Kafka confirma).
 */
@Component
public class ConsumidorNotificaciones {

    private final EjecutorTarea ejecutor;

    public ConsumidorNotificaciones(EjecutorTarea ejecutor) {
        this.ejecutor = ejecutor;
    }

    @RabbitListener(queues = Colas.NOTIFICACIONES)
    public void procesar(ProcesarOrden comando) throws Exception {
        ejecutor.ejecutar(comando);
    }
}
