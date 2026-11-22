package co.edu.proyecto.dashboard.configuracion;

import co.edu.proyecto.mensajeria.kafka.ConfiguracionKafkaBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Mensajería del dashboard: solo Kafka (lee los tres topics de resultados). Usa los reintentos comunes;
 * el deserializador se configura en application.yml. El WebSocket está en {@link ConfiguracionWebSocket}.
 */
@Configuration
@Import(ConfiguracionKafkaBase.class)
public class ConfiguracionMensajeria {
}
