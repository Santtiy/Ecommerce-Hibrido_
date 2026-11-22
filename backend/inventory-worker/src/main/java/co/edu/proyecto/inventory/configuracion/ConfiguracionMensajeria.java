package co.edu.proyecto.inventory.configuracion;

import co.edu.proyecto.common.nombres.Colas;
import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.mensajeria.rabbitmq.ConfiguracionRabbitBase;
import co.edu.proyecto.mensajeria.rabbitmq.TopologiaWorker;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Mensajería de inventory-worker. Lo común (JSON, fanout {@code order_tasks} y cola de fallidos) viene de
 * common-mensajeria; aquí solo se declara lo propio: su cola de trabajo enlazada al fanout y su topic de resultados.
 * Varios servicios declaran la misma topología a propósito: así el sistema arranca en cualquier orden.
 */
@Configuration
@Import({ConfiguracionRabbitBase.class, TopologiaWorker.class})
public class ConfiguracionMensajeria {

    /** Topic donde este worker publica sus resultados. */
    @Bean
    public NewTopic topicoResultados() {
        return TopicBuilder.name(Topicos.EVENTOS_INVENTARIO).partitions(1).replicas(1).build();
    }

    @Bean
    public Queue cola() {
        return TopologiaWorker.colaDeTrabajo(Colas.INVENTARIO);
    }

    @Bean
    public Binding enlaceCola() {
        return TopologiaWorker.enlaceAlFanout(cola());
    }
}
