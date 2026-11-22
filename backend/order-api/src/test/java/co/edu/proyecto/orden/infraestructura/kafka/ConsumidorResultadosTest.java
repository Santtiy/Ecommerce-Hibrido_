package co.edu.proyecto.orden.infraestructura.kafka;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.TipoTarea;
import co.edu.proyecto.orden.aplicacion.RegistrarResultadoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.resultadoTarea;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ConsumidorResultadosTest {

    @Mock
    private RegistrarResultadoService registrarResultado;
    @InjectMocks
    private ConsumidorResultados consumidor;

    @Test
    void elTopicDePagosSeRegistraComoPago() {
        // Arrange
        ResultadoTarea resultado = resultadoTarea("o-1", EstadoTarea.PAYMENT_FAILED);

        // Act
        consumidor.alRecibirPago(resultado);

        // Assert
        verify(registrarResultado).registrar(TipoTarea.PAGO, resultado);
        verifyNoMoreInteractions(registrarResultado);
    }

    @Test
    void elTopicDeInventarioSeRegistraComoInventario() {
        // Arrange
        ResultadoTarea resultado = resultadoTarea("o-1", EstadoTarea.STOCK_RESERVED);

        // Act
        consumidor.alRecibirInventario(resultado);

        // Assert
        verify(registrarResultado).registrar(TipoTarea.INVENTARIO, resultado);
        verifyNoMoreInteractions(registrarResultado);
    }

    @Test
    void elTopicDeNotificacionesSeRegistraComoNotificacion() {
        // Arrange
        ResultadoTarea resultado = resultadoTarea("o-1", EstadoTarea.EMAIL_SENT);

        // Act
        consumidor.alRecibirNotificacion(resultado);

        // Assert
        verify(registrarResultado).registrar(TipoTarea.NOTIFICACION, resultado);
        verifyNoMoreInteractions(registrarResultado);
    }
}
