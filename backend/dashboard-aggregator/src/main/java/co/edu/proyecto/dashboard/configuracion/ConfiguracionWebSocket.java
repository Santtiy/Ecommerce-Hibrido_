package co.edu.proyecto.dashboard.configuracion;

import co.edu.proyecto.dashboard.infraestructura.CanalWebSocket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** Publica el WebSocket en /ws/ordenes y solo acepta conexiones de los orígenes configurados (nunca "*"). */
@Configuration
@EnableWebSocket
public class ConfiguracionWebSocket implements WebSocketConfigurer {

    private final CanalWebSocket canal;
    private final String[] origenesPermitidos;

    public ConfiguracionWebSocket(CanalWebSocket canal, @Value("${app.origenes-permitidos}") String[] origenesPermitidos) {
        this.canal = canal;
        this.origenesPermitidos = origenesPermitidos;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registro) {
        registro.addHandler(canal, "/ws/ordenes").setAllowedOrigins(origenesPermitidos);
    }
}
