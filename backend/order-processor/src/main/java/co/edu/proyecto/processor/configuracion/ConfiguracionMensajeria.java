package co.edu.proyecto.processor.configuracion;

import co.edu.proyecto.mensajeria.kafka.ConfiguracionKafkaBase;
import co.edu.proyecto.mensajeria.rabbitmq.ConfiguracionRabbitBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Mensajería de order-processor, el puente entre Kafka y RabbitMQ: los reintentos del consumidor de new_orders y,
 * del lado de RabbitMQ, el fanout {@code order_tasks} con mensajes en JSON. No declara nada propio: cada worker
 * declara su cola. El deserializador de Kafka se configura en application.yml.
 */
@Configuration
@Import({ConfiguracionKafkaBase.class, ConfiguracionRabbitBase.class})
public class ConfiguracionMensajeria {
}
