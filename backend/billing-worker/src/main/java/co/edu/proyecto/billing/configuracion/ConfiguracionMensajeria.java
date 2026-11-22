package co.edu.proyecto.billing.configuracion;

import co.edu.proyecto.common.contratos.ReglaTarea;
import co.edu.proyecto.common.nombres.TipoTarea;
import co.edu.proyecto.mensajeria.rabbitmq.ConfiguracionRabbitBase;
import co.edu.proyecto.mensajeria.rabbitmq.TopologiaWorker;
import co.edu.proyecto.mensajeria.tareas.EjecutorTarea;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;

/**
 * Mensajería de billing-worker. Lo común (JSON, fanout {@code order_tasks} y cola de fallidos) viene de
 * common-mensajeria; aquí solo se declara lo propio: su cola de trabajo enlazada al fanout, su topic de resultados
 * y el {@link EjecutorTarea} que une su regla con ese topic.
 * Varios servicios declaran la misma topología a propósito: así el sistema arranca en cualquier orden.
 */
@Configuration
@Import({ConfiguracionRabbitBase.class, TopologiaWorker.class})
public class ConfiguracionMensajeria {

    /** Topic donde este worker publica sus resultados. */
    @Bean
    public NewTopic topicoResultados() {
        return TopicBuilder.name(TipoTarea.PAGO.topico()).partitions(1).replicas(1).build();
    }

    @Bean
    public Queue cola() {
        return TopologiaWorker.colaDeTrabajo(TipoTarea.PAGO.cola());
    }

    @Bean
    public Binding enlaceCola() {
        return TopologiaWorker.enlaceAlFanout(cola());
    }

    /**
     * Procesamiento común de los workers aplicado a este: la regla llega por su interfaz {@link ReglaTarea}
     * (desde ConfiguracionDominio) y el resultado va a su topic. KafkaTemplate y ObjectMapper los crea Spring Boot.
     */
    @Bean
    public EjecutorTarea ejecutorTarea(ReglaTarea regla, KafkaTemplate<String, String> kafka, ObjectMapper json) {
        return new EjecutorTarea(regla, TipoTarea.PAGO.topico(), kafka, json, Clock.systemUTC());
    }
}
