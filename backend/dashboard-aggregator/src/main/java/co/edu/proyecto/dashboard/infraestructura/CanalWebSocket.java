package co.edu.proyecto.dashboard.infraestructura;

import co.edu.proyecto.dashboard.dominio.ProgresoOrden;
import co.edu.proyecto.dashboard.dominio.TableroOrdenes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket /ws/ordenes. Al conectarse, el navegador recibe todas las órdenes;
 * después, solo la orden que cambió: { "<orderId>": { payment, stock, email } }.
 */
@Component
public class CanalWebSocket extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CanalWebSocket.class);
    private static final int TIEMPO_MAXIMO_ENVIO_MS = 5_000;
    private static final int BUFFER_MAXIMO_BYTES = 512 * 1024;

    private final Map<String, WebSocketSession> sesiones = new ConcurrentHashMap<>();
    private final TableroOrdenes tablero;
    private final ObjectMapper json;

    public CanalWebSocket(TableroOrdenes tablero, ObjectMapper json) {
        this.tablero = tablero;
        this.json = json;
    }

    /** El decorador permite enviar desde varios hilos (los consumidores de Kafka) sin corromper la sesión. */
    @Override
    public void afterConnectionEstablished(WebSocketSession sesion) {
        WebSocketSession segura = new ConcurrentWebSocketSessionDecorator(sesion, TIEMPO_MAXIMO_ENVIO_MS, BUFFER_MAXIMO_BYTES);
        sesiones.put(sesion.getId(), segura);
        enviar(segura, tablero.todas());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession sesion, CloseStatus estado) {
        sesiones.remove(sesion.getId());
    }

    public void difundir(String orderId, ProgresoOrden progreso) {
        Map<String, ProgresoOrden> cambio = Map.of(orderId, progreso);
        sesiones.values().forEach(sesion -> enviar(sesion, cambio));
    }

    private void enviar(WebSocketSession sesion, Map<String, ProgresoOrden> contenido) {
        try {
            sesion.sendMessage(new TextMessage(json.writeValueAsString(contenido)));
        } catch (JsonProcessingException e) {
            log.error("No se pudo convertir el estado a JSON", e);
        } catch (IOException | RuntimeException e) {
            log.warn("Se descarta la sesión {}: {}", sesion.getId(), e.getMessage());
            sesiones.remove(sesion.getId());
        }
    }
}
