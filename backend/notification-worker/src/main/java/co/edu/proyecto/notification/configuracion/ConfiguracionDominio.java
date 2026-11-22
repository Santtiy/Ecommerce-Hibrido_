package co.edu.proyecto.notification.configuracion;

import co.edu.proyecto.notification.dominio.ReglaNotificacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra la regla de dominio de este worker (simular el correo de confirmación); el consumidor la recibe por constructor. */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public ReglaNotificacion regla() {
        return new ReglaNotificacion();
    }
}
