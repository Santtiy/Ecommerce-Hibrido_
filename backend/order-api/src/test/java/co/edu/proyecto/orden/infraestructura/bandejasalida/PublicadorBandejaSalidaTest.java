package co.edu.proyecto.orden.infraestructura.bandejasalida;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicadorBandejaSalidaTest {

    @Mock
    private SpringDataBandeja bandeja;
    @Mock
    private KafkaTemplate<String, String> kafka;
    @InjectMocks
    private PublicadorBandejaSalida publicador;

    private static MensajePendienteEntity mensaje(String clave) {
        MensajePendienteEntity mensaje = mock(MensajePendienteEntity.class);
        when(mensaje.getTopico()).thenReturn("new_orders");
        when(mensaje.getClave()).thenReturn(clave);
        when(mensaje.getContenido()).thenReturn("{}");
        return mensaje;
    }

    @Test
    void siKafkaConfirmaSeBorraElMensaje() {
        // Arrange
        MensajePendienteEntity pendiente = mensaje("o-1");
        when(bandeja.findTop50ByOrderByIdAsc()).thenReturn(List.of(pendiente));
        when(kafka.send("new_orders", "o-1", "{}"))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // Act
        publicador.publicarPendientes();

        // Assert
        verify(bandeja).delete(pendiente);
    }

    @Test
    void siKafkaFallaNoSeBorraNadaNiSeSigueConElSiguiente() {
        // Arrange
        MensajePendienteEntity primero = mensaje("o-1");
        MensajePendienteEntity segundo = mock(MensajePendienteEntity.class);
        when(bandeja.findTop50ByOrderByIdAsc()).thenReturn(List.of(primero, segundo));
        when(kafka.send("new_orders", "o-1", "{}"))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka caído")));

        // Act
        publicador.publicarPendientes();

        // Assert
        verify(bandeja, never()).delete(any());
        verify(kafka, never()).send("new_orders", null, null);
    }
}
