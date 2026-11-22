package co.edu.proyecto.mensajeria.rabbitmq;

import co.edu.proyecto.common.nombres.Exchanges;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lo que necesita todo servicio que use RabbitMQ (order-processor y los tres workers): el formato JSON de los
 * mensajes y el exchange fanout {@code order_tasks}. Se importa con {@code @Import}.
 * Varios servicios declaran la misma topología a propósito: así el sistema arranca en cualquier orden
 * (RabbitMQ ignora una declaración repetida si los argumentos coinciden).
 */
@Configuration
public class ConfiguracionRabbitBase {

    /** Fanout durable que copia cada comando {@code ProcesarOrden} a las tres colas de trabajo. */
    @Bean
    public FanoutExchange exchangeTareas() {
        return new FanoutExchange(Exchanges.TAREAS, true, false);
    }

    /** Spring Boot aplica este conversor solo a RabbitTemplate y a los listeners: los mensajes viajan como JSON. */
    @Bean
    public Jackson2JsonMessageConverter conversorJson(ObjectMapper json) {
        return new Jackson2JsonMessageConverter(json);
    }
}
