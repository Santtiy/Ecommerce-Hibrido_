package co.edu.proyecto.processor.configuracion;

import co.edu.proyecto.common.nombres.Exchanges;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declara el exchange fanout y hace que RabbitMQ use JSON (Spring Boot aplica el conversor solo). */
@Configuration
public class ConfiguracionRabbit {

    @Bean
    public FanoutExchange exchangeTareas() {
        return new FanoutExchange(Exchanges.TAREAS, true, false);
    }

    @Bean
    public Jackson2JsonMessageConverter conversorJson(ObjectMapper json) {
        return new Jackson2JsonMessageConverter(json);
    }
}
