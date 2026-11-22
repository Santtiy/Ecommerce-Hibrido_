package co.edu.proyecto.mensajeria.tareas;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ReglaTarea;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.nombres.Topicos;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.procesarOrden;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjecutorTareaTest {

    /** Hora del reloj del ejecutor: es la que debe salir en ocurridoEn. */
    private static final Instant AHORA = Instant.parse("2026-11-21T15:00:00Z");

    /** Igual que el ObjectMapper de Spring Boot: fechas en ISO-8601, no como números. */
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /** Regla mínima que cumple el contrato de ReglaTarea: mismo orderId y nunca null. */
    private final ReglaTarea regla = (comando, ahora) ->
            new ResultadoTarea(comando.orderId(), EstadoTarea.PAYMENT_SUCCESS, "Pago aprobado", ahora);

    @Mock
    private KafkaTemplate<String, String> kafka;

    private EjecutorTarea ejecutor() {
        return new EjecutorTarea(regla, Topicos.EVENTOS_PAGO, kafka, json, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static ProcesarOrden comando() {
        return procesarOrden("500000");
    }

    @Test
    void publicaElResultadoEnSuTopicConElOrderIdComoClaveYElJsonDelResultado() throws Exception {
        // Arrange
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        String esperado = json.writeValueAsString(
                new ResultadoTarea("o-1", EstadoTarea.PAYMENT_SUCCESS, "Pago aprobado", AHORA));

        // Act
        ejecutor().ejecutar(comando());

        // Assert
        verify(kafka).send(Topicos.EVENTOS_PAGO, "o-1", esperado);
        assertThat(esperado).contains("\"status\":\"PAYMENT_SUCCESS\"", "\"ocurridoEn\":\"2026-11-21T15:00:00Z\"");
    }

    @Test
    void siKafkaNoConfirmaLaExcepcionSePropagaParaQueRabbitMqReintente() {
        // Arrange
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new KafkaException("broker caído")));

        // Act + Assert
        assertThatThrownBy(() -> ejecutor().ejecutar(comando()))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(KafkaException.class);
    }

    @Test
    void siElEnvioFallaAntesDeSalirLaExcepcionTambienSePropaga() {
        // Arrange
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new KafkaException("sin metadatos"));

        // Act + Assert
        assertThatThrownBy(() -> ejecutor().ejecutar(comando()))
                .isInstanceOf(KafkaException.class)
                .hasMessageContaining("sin metadatos");
    }
}
