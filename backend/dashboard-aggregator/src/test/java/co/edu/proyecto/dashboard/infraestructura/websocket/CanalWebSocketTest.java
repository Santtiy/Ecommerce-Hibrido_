package co.edu.proyecto.dashboard.infraestructura.websocket;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.nombres.TipoTarea;
import co.edu.proyecto.dashboard.dominio.ProgresoOrden;
import co.edu.proyecto.dashboard.dominio.TableroOrdenes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanalWebSocketTest {

    private static final TypeReference<Map<String, ProgresoOrden>> CONTENIDO = new TypeReference<>() { };

    private final ObjectMapper json = new ObjectMapper();

    /** Las sesiones (navegadores) son dobles de la interfaz WebSocketSession de Spring. */
    @Mock
    private WebSocketSession navegador;
    @Mock
    private WebSocketSession otroNavegador;

    private TableroOrdenes tablero;
    private CanalWebSocket canal;

    @BeforeEach
    void crearCanal() {
        tablero = new TableroOrdenes();
        canal = new CanalWebSocket(tablero, json);
        when(navegador.getId()).thenReturn("s-1");
    }

    @Test
    void alConectarseElNavegadorRecibeElTableroCompleto() throws IOException {
        // Arrange
        tablero.aplicar("o-1", TipoTarea.PAGO, EstadoTarea.PAYMENT_SUCCESS);
        tablero.aplicar("o-2", TipoTarea.INVENTARIO, EstadoTarea.STOCK_RESERVED);

        // Act
        canal.afterConnectionEstablished(navegador);

        // Assert
        Map<String, ProgresoOrden> recibido = mensajesRecibidos(navegador, 1).get(0);
        assertThat(recibido).containsOnlyKeys("o-1", "o-2").isEqualTo(tablero.todas());
    }

    @Test
    void difundirEnviaSoloLaOrdenQueCambio() throws IOException {
        // Arrange
        tablero.aplicar("o-1", TipoTarea.PAGO, EstadoTarea.PAYMENT_SUCCESS);
        canal.afterConnectionEstablished(navegador);
        ProgresoOrden progreso = tablero.aplicar("o-2", TipoTarea.NOTIFICACION, EstadoTarea.EMAIL_SENT);

        // Act
        canal.difundir("o-2", progreso);

        // Assert
        Map<String, ProgresoOrden> cambio = mensajesRecibidos(navegador, 2).get(1);
        assertThat(cambio).isEqualTo(Map.of("o-2", new ProgresoOrden("pending", "pending", "EMAIL_SENT")));
    }

    @Test
    void unaSesionQueFallaSeDescartaYLasDemasSiguenRecibiendo() throws IOException {
        // Arrange: el primer navegador recibe el tablero al conectarse y después se cae
        when(otroNavegador.getId()).thenReturn("s-2");
        doNothing().doThrow(new IOException("conexión cerrada")).when(navegador).sendMessage(any());
        canal.afterConnectionEstablished(navegador);
        canal.afterConnectionEstablished(otroNavegador);
        ProgresoOrden progreso = ProgresoOrden.inicial().conPago(EstadoTarea.PAYMENT_FAILED);

        // Act
        canal.difundir("o-1", progreso);
        canal.difundir("o-2", progreso);

        // Assert: al caído no se le vuelve a intentar; el otro recibe el tablero (vacío) y los dos cambios
        verify(navegador, times(2)).sendMessage(any());
        assertThat(mensajesRecibidos(otroNavegador, 3))
                .containsExactly(Map.of(), Map.of("o-1", progreso), Map.of("o-2", progreso));
    }

    @Test
    void unaSesionCerradaYaNoRecibeCambios() throws IOException {
        // Arrange
        canal.afterConnectionEstablished(navegador);
        canal.afterConnectionClosed(navegador, CloseStatus.NORMAL);

        // Act
        canal.difundir("o-1", ProgresoOrden.inicial());

        // Assert: solo recibió el tablero al conectarse
        verify(navegador, times(1)).sendMessage(any());
    }

    /** Lee cada TextMessage enviado a la sesión como lo lee el navegador: orderId → { payment, stock, email }. */
    private List<Map<String, ProgresoOrden>> mensajesRecibidos(WebSocketSession sesion, int cantidad) throws IOException {
        ArgumentCaptor<TextMessage> mensajes = ArgumentCaptor.forClass(TextMessage.class);
        verify(sesion, times(cantidad)).sendMessage(mensajes.capture());
        return mensajes.getAllValues().stream().map(this::leer).toList();
    }

    private Map<String, ProgresoOrden> leer(TextMessage mensaje) {
        try {
            return json.readValue(mensaje.getPayload(), CONTENIDO);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
