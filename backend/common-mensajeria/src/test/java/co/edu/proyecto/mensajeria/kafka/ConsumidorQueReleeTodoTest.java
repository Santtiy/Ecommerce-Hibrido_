package co.edu.proyecto.mensajeria.kafka;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConsumidorQueReleeTodoTest {

    @Test
    void alRecibirParticionesLasRebobinaTodasAlInicio() {
        // Arrange
        ConsumidorQueReleeTodo consumidor = new ConsumidorQueReleeTodo() { };
        ConsumerSeekCallback callback = mock(ConsumerSeekCallback.class);
        TopicPartition p0 = new TopicPartition("billing_events", 0);
        TopicPartition p1 = new TopicPartition("inventory_events", 0);

        // Act
        consumidor.onPartitionsAssigned(Map.of(p0, 7L, p1, 3L), callback);

        // Assert
        verify(callback).seekToBeginning(Set.of(p0, p1));
    }
}
