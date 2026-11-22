package co.edu.proyecto.analytics.configuracion;

import co.edu.proyecto.analytics.aplicacion.AgregadorVentas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Reintentos del consumidor de Kafka y el agregador de ventas. El deserializador está en application.yml. */
@Configuration
public class ConfiguracionKafka {

    private static final long PAUSA_ENTRE_INTENTOS_MS = 1000;
    private static final long REINTENTOS = 2; // 3 intentos en total

    @Bean
    public DefaultErrorHandler manejadorErroresKafka() {
        return new DefaultErrorHandler(new FixedBackOff(PAUSA_ENTRE_INTENTOS_MS, REINTENTOS));
    }

    @Bean
    public AgregadorVentas agregadorVentas() {
        return new AgregadorVentas();
    }
}
