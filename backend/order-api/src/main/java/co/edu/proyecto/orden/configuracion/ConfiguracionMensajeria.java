package co.edu.proyecto.orden.configuracion;

import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.mensajeria.kafka.ConfiguracionKafkaBase;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Mensajería de order-api: solo Kafka. Crea new_orders (lo publica la bandeja de salida) y usa los reintentos
 * comunes para el consumidor de resultados. Serializadores y tipos se configuran en application.yml, no aquí.
 */
@Configuration
@Import(ConfiguracionKafkaBase.class)
public class ConfiguracionMensajeria {

    private static final int PARTICIONES_NUEVAS_ORDENES = 3;

    /** 3 particiones con clave orderId: los eventos de una misma orden conservan su orden. */
    @Bean
    public NewTopic nuevasOrdenes() {
        return TopicBuilder.name(Topicos.NUEVAS_ORDENES).partitions(PARTICIONES_NUEVAS_ORDENES).replicas(1).build();
    }
}
