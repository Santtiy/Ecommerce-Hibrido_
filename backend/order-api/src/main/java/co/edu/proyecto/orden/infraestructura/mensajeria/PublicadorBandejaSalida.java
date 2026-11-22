package co.edu.proyecto.orden.infraestructura.mensajeria;

import co.edu.proyecto.orden.infraestructura.persistencia.MensajePendienteEntity;
import co.edu.proyecto.orden.infraestructura.persistencia.SpringDataBandeja;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Publica en Kafka los eventos de la bandeja de salida, en orden de llegada.
 * Un evento solo se borra cuando Kafka confirma que lo recibió; si Kafka falla, se reintenta en el siguiente ciclo.
 */
@Component
public class PublicadorBandejaSalida {

    private static final Logger log = LoggerFactory.getLogger(PublicadorBandejaSalida.class);
    private static final long ESPERA_CONFIRMACION_SEGUNDOS = 5;

    private final SpringDataBandeja bandeja;
    private final KafkaTemplate<String, String> kafka;

    public PublicadorBandejaSalida(SpringDataBandeja bandeja, KafkaTemplate<String, String> kafka) {
        this.bandeja = bandeja;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelay = 1000)
    public void publicarPendientes() {
        for (MensajePendienteEntity mensaje : bandeja.findTop50ByOrderByIdAsc()) {
            if (!publicar(mensaje)) {
                return; // se corta para no desordenar los eventos; se reintenta en 1 s
            }
            bandeja.delete(mensaje);
        }
    }

    private boolean publicar(MensajePendienteEntity mensaje) {
        try {
            kafka.send(mensaje.getTopico(), mensaje.getClave(), mensaje.getContenido())
                    .get(ESPERA_CONFIRMACION_SEGUNDOS, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("Kafka no confirmó el evento de la orden {}; se reintentará: {}", mensaje.getClave(), e.getMessage());
            return false;
        }
    }
}
