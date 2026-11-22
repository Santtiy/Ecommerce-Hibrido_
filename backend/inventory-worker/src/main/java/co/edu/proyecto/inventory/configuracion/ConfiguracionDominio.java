package co.edu.proyecto.inventory.configuracion;

import co.edu.proyecto.inventory.dominio.ReglaInventario;
import co.edu.proyecto.common.contratos.ReglaTarea;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra la regla de dominio de este worker (reservar stock o marcar sin stock).
 * Se expone como {@code ReglaTarea}: el EjecutorTarea la recibe por constructor sin conocer la clase concreta.
 */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public ReglaTarea regla() {
        return new ReglaInventario();
    }
}
