package co.edu.proyecto.orden.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Solo los orígenes configurados (el panel del dashboard) pueden llamar a POST /order desde el navegador. */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    public ConfiguracionWeb(@Value("${app.origenes-permitidos}") String[] origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/order").allowedOrigins(origenesPermitidos).allowedMethods("POST");
    }
}
