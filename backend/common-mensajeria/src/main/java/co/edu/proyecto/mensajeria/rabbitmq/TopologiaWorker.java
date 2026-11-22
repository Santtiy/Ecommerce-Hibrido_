package co.edu.proyecto.mensajeria.rabbitmq;

import co.edu.proyecto.common.nombres.Colas;
import co.edu.proyecto.common.nombres.Exchanges;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología común de los workers: el exchange y la cola de fallidos ({@code order_tasks_dlx} y
 * {@code failed_tasks}), y las fábricas para declarar la cola de trabajo de cada worker con su dead-letter
 * exchange y su enlace al fanout. Cada worker la importa junto con {@link ConfiguracionRabbitBase} y declara
 * como beans solo su propia cola y su enlace. Reintentos, prefetch y concurrencia están en application.yml.
 */
@Configuration
public class TopologiaWorker {

    @Bean
    public FanoutExchange exchangeTareasFallidas() {
        return new FanoutExchange(Exchanges.TAREAS_FALLIDAS, true, false);
    }

    @Bean
    public Queue colaFallidas() {
        return QueueBuilder.durable(Colas.FALLIDAS).build();
    }

    @Bean
    public Binding enlaceFallidas() {
        return BindingBuilder.bind(colaFallidas()).to(exchangeTareasFallidas());
    }

    /**
     * Cola de trabajo durable de un worker. Tras agotar los reintentos, RabbitMQ manda el mensaje al
     * exchange de fallidos.
     */
    public static Queue colaDeTrabajo(String nombre) {
        return QueueBuilder.durable(nombre).deadLetterExchange(Exchanges.TAREAS_FALLIDAS).build();
    }

    /** Enlaza la cola de trabajo al fanout {@code order_tasks}: así recibe una copia de cada comando. */
    public static Binding enlaceAlFanout(Queue cola) {
        return BindingBuilder.bind(cola).to(new FanoutExchange(Exchanges.TAREAS, true, false));
    }
}
