package co.edu.proyecto.inventory.configuracion;

import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.inventory.dominio.ReglaInventario;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Crea el topic de resultados de este worker y registra su regla de dominio. */
@Configuration
public class ConfiguracionKafka {

    @Bean
    public NewTopic topicoResultados() {
        return TopicBuilder.name(Topicos.EVENTOS_INVENTARIO).partitions(1).replicas(1).build();
    }

    @Bean
    public ReglaInventario regla() {
        return new ReglaInventario();
    }
}
