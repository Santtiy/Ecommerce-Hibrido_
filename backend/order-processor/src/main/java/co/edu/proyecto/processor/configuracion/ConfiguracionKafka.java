package co.edu.proyecto.processor.configuracion;

import co.edu.proyecto.processor.aplicacion.TraductorEventoAComando;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Clock;

/** Reintentos del consumidor de Kafka y el traductor. El deserializador se configura en application.yml. */
@Configuration
public class ConfiguracionKafka {

    private static final long PAUSA_ENTRE_INTENTOS_MS = 1000;
    private static final long REINTENTOS = 2; // 3 intentos en total

    @Bean
    public DefaultErrorHandler manejadorErroresKafka() {
        return new DefaultErrorHandler(new FixedBackOff(PAUSA_ENTRE_INTENTOS_MS, REINTENTOS));
    }

    @Bean
    public TraductorEventoAComando traductor() {
        return new TraductorEventoAComando(Clock.systemUTC());
    }
}
