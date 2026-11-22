package co.edu.proyecto.analytics.configuracion;

import co.edu.proyecto.mensajeria.kafka.ConfiguracionKafkaBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Mensajería de analytics: solo Kafka (lee new_orders directamente, sin pasar por RabbitMQ).
 * Usa los reintentos comunes; el deserializador se configura en application.yml.
 */
@Configuration
@Import(ConfiguracionKafkaBase.class)
public class ConfiguracionMensajeria {
}
