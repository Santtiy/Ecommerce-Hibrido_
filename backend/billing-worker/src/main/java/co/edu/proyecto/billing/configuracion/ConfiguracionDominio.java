package co.edu.proyecto.billing.configuracion;

import co.edu.proyecto.billing.dominio.ReglaPago;
import co.edu.proyecto.common.contratos.ReglaTarea;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra la regla de dominio de este worker (aprobar o rechazar el pago).
 * Se expone como {@code ReglaTarea}: el EjecutorTarea la recibe por constructor sin conocer la clase concreta.
 */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public ReglaTarea regla() {
        return new ReglaPago();
    }
}
