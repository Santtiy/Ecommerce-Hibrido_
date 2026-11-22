package co.edu.proyecto.processor.configuracion;

import co.edu.proyecto.processor.aplicacion.TraductorEventoAComando;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Registra el traductor evento → comando con el reloj del sistema en UTC (las pruebas usan un reloj fijo). */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public TraductorEventoAComando traductor() {
        return new TraductorEventoAComando(Clock.systemUTC());
    }
}
