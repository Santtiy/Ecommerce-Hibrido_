package co.edu.proyecto.inventory.configuracion;

import co.edu.proyecto.common.nombres.Colas;
import co.edu.proyecto.common.nombres.Exchanges;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología que necesita este worker: su cola enlazada al fanout de tareas y la cola de fallidos.
 * Varios servicios declaran lo mismo a propósito: así el sistema arranca en cualquier orden.
 * Reintentos, prefetch y concurrencia están en application.yml.
 */
@Configuration
public class ConfiguracionRabbit {

    @Bean
    public FanoutExchange exchangeTareas() {
        return new FanoutExchange(Exchanges.TAREAS, true, false);
    }

    @Bean
    public FanoutExchange exchangeTareasFallidas() {
        return new FanoutExchange(Exchanges.TAREAS_FALLIDAS, true, false);
    }

    /** Tras agotar los reintentos, RabbitMQ manda el mensaje al exchange de fallidos. */
    @Bean
    public Queue cola() {
        return QueueBuilder.durable(Colas.INVENTARIO).deadLetterExchange(Exchanges.TAREAS_FALLIDAS).build();
    }

    @Bean
    public Queue colaFallidas() {
        return QueueBuilder.durable(Colas.FALLIDAS).build();
    }

    @Bean
    public Binding enlaceCola() {
        return BindingBuilder.bind(cola()).to(exchangeTareas());
    }

    @Bean
    public Binding enlaceFallidas() {
        return BindingBuilder.bind(colaFallidas()).to(exchangeTareasFallidas());
    }

    /** Spring Boot aplica este conversor solo: los mensajes viajan como JSON. */
    @Bean
    public Jackson2JsonMessageConverter conversorJson(ObjectMapper json) {
        return new Jackson2JsonMessageConverter(json);
    }
}
