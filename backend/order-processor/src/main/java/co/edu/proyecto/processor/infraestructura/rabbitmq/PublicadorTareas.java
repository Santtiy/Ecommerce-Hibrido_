package co.edu.proyecto.processor.infraestructura.rabbitmq;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.nombres.Exchanges;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publica el comando en el exchange fanout order_tasks, que lo copia a las tres colas de trabajo. */
@Component
public class PublicadorTareas {

    /** Un exchange fanout ignora la clave de enrutamiento: entrega a todas las colas enlazadas. */
    private static final String SIN_CLAVE = "";

    private final RabbitTemplate rabbit;

    public PublicadorTareas(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publicar(ProcesarOrden comando) {
        rabbit.convertAndSend(Exchanges.TAREAS, SIN_CLAVE, comando);
    }
}
