package co.edu.proyecto.dashboard.infraestructura.kafka;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.dashboard.dominio.ProgresoOrden;
import co.edu.proyecto.dashboard.dominio.TableroOrdenes;
import co.edu.proyecto.dashboard.infraestructura.websocket.CanalWebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.resultadoTarea;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ConsumidorResultadosTest {

    /** Solo se simula el canal (la salida al navegador); el tablero es el real porque es dominio puro. */
    @Mock
    private CanalWebSocket canal;

    private TableroOrdenes tablero;
    private ConsumidorResultados consumidor;

    @BeforeEach
    void crearConsumidor() {
        tablero = new TableroOrdenes();
        consumidor = new ConsumidorResultados(tablero, canal);
    }

    @Test
    void cadaTopicActualizaSuEtapaEnElTableroYDifundeLaOrden() {
        // Arrange
        ProgresoOrden alFinal = new ProgresoOrden("PAYMENT_SUCCESS", "STOCK_OUT_OF_STOCK", "EMAIL_SENT");

        // Act
        consumidor.alRecibirPago(resultadoTarea("o-1", EstadoTarea.PAYMENT_SUCCESS));
        consumidor.alRecibirInventario(resultadoTarea("o-1", EstadoTarea.STOCK_OUT_OF_STOCK));
        consumidor.alRecibirNotificacion(resultadoTarea("o-1", EstadoTarea.EMAIL_SENT));

        // Assert
        InOrder secuencia = inOrder(canal);
        secuencia.verify(canal).difundir("o-1", new ProgresoOrden("PAYMENT_SUCCESS", "pending", "pending"));
        secuencia.verify(canal).difundir("o-1", new ProgresoOrden("PAYMENT_SUCCESS", "STOCK_OUT_OF_STOCK", "pending"));
        secuencia.verify(canal).difundir("o-1", alFinal);
        assertThat(tablero.todas()).containsEntry("o-1", alFinal);
    }

    @Test
    void unEstadoQueNoEsDeSuTopicNoCambiaElTableroNiSeDifunde() {
        // Act + Assert
        assertThatThrownBy(() -> consumidor.alRecibirPago(resultadoTarea("o-1", EstadoTarea.EMAIL_SENT)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(tablero.todas()).isEmpty();
        verifyNoInteractions(canal);
    }
}
