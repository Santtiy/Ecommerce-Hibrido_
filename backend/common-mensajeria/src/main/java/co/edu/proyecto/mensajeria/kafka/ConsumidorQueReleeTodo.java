package co.edu.proyecto.mensajeria.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;

import java.util.Map;

/**
 * Base de los consumidores de Kafka cuyo estado vive en memoria (analytics y dashboard): cada vez que reciben
 * sus particiones, releen el topic desde el inicio y así reconstruyen el estado tras un reinicio.
 * Es seguro porque los dos son idempotentes. No tiene lógica de negocio: quien la extiende solo declara sus
 * {@code @KafkaListener}.
 */
public abstract class ConsumidorQueReleeTodo extends AbstractConsumerSeekAware {

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> asignaciones, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(asignaciones, callback);
        callback.seekToBeginning(asignaciones.keySet());
    }
}
