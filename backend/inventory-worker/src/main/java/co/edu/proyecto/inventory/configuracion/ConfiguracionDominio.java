package co.edu.proyecto.inventory.configuracion;

import co.edu.proyecto.inventory.dominio.ReglaInventario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra la regla de dominio de este worker (reservar stock o marcar sin stock); el consumidor la recibe por constructor. */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public ReglaInventario regla() {
        return new ReglaInventario();
    }
}
