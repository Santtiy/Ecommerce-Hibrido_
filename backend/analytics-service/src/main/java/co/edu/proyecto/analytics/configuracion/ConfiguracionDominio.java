package co.edu.proyecto.analytics.configuracion;

import co.edu.proyecto.analytics.dominio.AgregadorVentas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra el agregador que acumula en memoria el total de ventas de las órdenes de new_orders. */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public AgregadorVentas agregadorVentas() {
        return new AgregadorVentas();
    }
}
