package co.edu.proyecto.orden.configuracion;

import co.edu.proyecto.common.nombres.Topicos;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka en order-api: crea new_orders y define los reintentos de los consumidores.
 * Serializadores y tipos se configuran en application.yml, no aquí.
 */
@Configuration
@EnableScheduling
public class ConfiguracionKafka {

    private static final int PARTICIONES_NUEVAS_ORDENES = 3;
    private static final long PAUSA_ENTRE_INTENTOS_MS = 1000;
    private static final long REINTENTOS = 2; // 1 intento + 2 reintentos = 3 intentos

    /** 3 particiones con clave orderId: los eventos de una misma orden conservan su orden. */
    @Bean
    public NewTopic nuevasOrdenes() {
        return TopicBuilder.name(Topicos.NUEVAS_ORDENES).partitions(PARTICIONES_NUEVAS_ORDENES).replicas(1).build();
    }

    /** Si un mensaje falla 3 veces se registra en el log y el consumidor sigue con el siguiente. */
    @Bean
    public DefaultErrorHandler manejadorErroresKafka() {
        return new DefaultErrorHandler(new FixedBackOff(PAUSA_ENTRE_INTENTOS_MS, REINTENTOS));
    }
}
