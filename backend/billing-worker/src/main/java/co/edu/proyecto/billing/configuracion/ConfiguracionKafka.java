package co.edu.proyecto.billing.configuracion;

import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.billing.dominio.ReglaPago;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Crea el topic de resultados de este worker y registra su regla de dominio. */
@Configuration
public class ConfiguracionKafka {

    @Bean
    public NewTopic topicoResultados() {
        return TopicBuilder.name(Topicos.EVENTOS_PAGO).partitions(1).replicas(1).build();
    }

    @Bean
    public ReglaPago regla() {
        return new ReglaPago();
    }
}

