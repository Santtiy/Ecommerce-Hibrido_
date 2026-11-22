package co.edu.proyecto.inventory.infraestructura;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.Colas;
import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.inventory.dominio.ReglaInventario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Toma cada comando de su cola, aplica la regla y publica el resultado en Kafka.
 * El ack a RabbitMQ ocurre al terminar el método sin excepción, es decir, DESPUÉS de que Kafka confirma:
 * si Kafka falla, el mensaje se reintenta y no se pierde ningún resultado.
 */
@Component
public class ConsumidorInventario {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorInventario.class);
    /** Pausa simulada para que el panel muestre el avance de cada etapa. */
    private static final long PAUSA_MINIMA_MS = 200;
    private static final long PAUSA_MAXIMA_MS = 500;
    private static final long ESPERA_KAFKA_SEGUNDOS = 10;

    private final ReglaInventario regla;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public ConsumidorInventario(ReglaInventario regla, KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.regla = regla;
        this.kafka = kafka;
        this.json = json;
    }

    @RabbitListener(queues = Colas.INVENTARIO)
    public void procesar(ProcesarOrden comando) throws Exception {
        simularTrabajo();
        ResultadoTarea resultado = regla.evaluar(comando, Instant.now());
        kafka.send(Topicos.EVENTOS_INVENTARIO, comando.orderId(), json.writeValueAsString(resultado))
                .get(ESPERA_KAFKA_SEGUNDOS, TimeUnit.SECONDS);
        log.info("Orden {}: {}", comando.orderId(), resultado.status());
    }

    private void simularTrabajo() throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextLong(PAUSA_MINIMA_MS, PAUSA_MAXIMA_MS + 1));
    }
}
