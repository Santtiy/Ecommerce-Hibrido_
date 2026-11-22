package co.edu.proyecto.dashboard.configuracion;

import co.edu.proyecto.dashboard.dominio.TableroOrdenes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Reintentos de los consumidores de Kafka y el tablero en memoria. El deserializador está en application.yml. */
@Configuration
public class ConfiguracionKafka {

    private static final long PAUSA_ENTRE_INTENTOS_MS = 1000;
    private static final long REINTENTOS = 2; // 3 intentos en total

    @Bean
    public DefaultErrorHandler manejadorErroresKafka() {
        return new DefaultErrorHandler(new FixedBackOff(PAUSA_ENTRE_INTENTOS_MS, REINTENTOS));
    }

    @Bean
    public TableroOrdenes tableroOrdenes() {
        return new TableroOrdenes();
    }
}
