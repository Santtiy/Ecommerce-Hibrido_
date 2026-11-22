package co.edu.proyecto.billing.configuracion;

import co.edu.proyecto.billing.dominio.ReglaPago;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra la regla de dominio de este worker (aprobar o rechazar el pago); el consumidor la recibe por constructor. */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public ReglaPago regla() {
        return new ReglaPago();
    }
}
