package co.edu.proyecto.dashboard.configuracion;

import co.edu.proyecto.dashboard.dominio.TableroOrdenes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra el tablero en memoria (orderId → {payment, stock, email}) que comparten el consumidor y el WebSocket. */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public TableroOrdenes tableroOrdenes() {
        return new TableroOrdenes();
    }
}
