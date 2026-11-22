package co.edu.proyecto.mensajeria.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Reintentos comunes de todos los consumidores de Kafka. La importan con {@code @Import} los servicios que
 * escuchan topics (order-api, order-processor, analytics y dashboard); los workers solo producen y no la usan.
 * Serializadores, deserializadores y tipos se configuran en el application.yml de cada servicio.
 */
@Configuration
public class ConfiguracionKafkaBase {

    private static final long PAUSA_ENTRE_INTENTOS_MS = 1000;
    private static final long REINTENTOS = 2; // 1 intento + 2 reintentos = 3 intentos

    /**
     * Si un mensaje falla 3 veces se registra en el log y el consumidor sigue con el siguiente: un mensaje
     * dañado no bloquea la partición. Spring Boot lo asigna solo a la fábrica de listeners.
     */
    @Bean
    public DefaultErrorHandler manejadorErroresKafka() {
        return new DefaultErrorHandler(new FixedBackOff(PAUSA_ENTRE_INTENTOS_MS, REINTENTOS));
    }
}
