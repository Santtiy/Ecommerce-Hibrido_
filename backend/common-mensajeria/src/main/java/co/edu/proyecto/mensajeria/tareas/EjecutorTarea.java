package co.edu.proyecto.mensajeria.tareas;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ReglaTarea;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Procesamiento común de los tres workers: pausa simulada, evaluación de la regla y publicación del resultado
 * en el topic del worker. Cada worker lo crea en su {@code ConfiguracionMensajeria} con su propia regla y su topic
 * (composición, no herencia), y su consumidor de RabbitMQ solo le entrega el comando.
 *
 * <p>El ack a RabbitMQ ocurre cuando el listener vuelve de {@link #ejecutar(ProcesarOrden)} sin excepción, es decir,
 * DESPUÉS de que Kafka confirma el envío: si Kafka falla, la excepción llega al listener, RabbitMQ reintenta el
 * mensaje (y tras 3 intentos lo manda a {@code failed_tasks}) y no se pierde ningún resultado.
 *
 * <p>No es un {@code @Component}: depende de la interfaz {@link ReglaTarea} y recibe todo por constructor.
 */
public class EjecutorTarea {

    private static final Logger log = LoggerFactory.getLogger(EjecutorTarea.class);
    /** Pausa simulada para que el panel muestre el avance de cada etapa. */
    private static final long PAUSA_MINIMA_MS = 200;
    private static final long PAUSA_MAXIMA_MS = 500;
    private static final long ESPERA_KAFKA_SEGUNDOS = 10;

    private final ReglaTarea regla;
    private final String topico;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final Clock reloj;

    public EjecutorTarea(ReglaTarea regla, String topico, KafkaTemplate<String, String> kafka,
                         ObjectMapper json, Clock reloj) {
        this.regla = regla;
        this.topico = topico;
        this.kafka = kafka;
        this.json = json;
        this.reloj = reloj;
    }

    /**
     * Procesa un comando y espera a que Kafka confirme el resultado (clave = orderId, valor = JSON).
     *
     * @throws Exception si Kafka no confirma a tiempo o falla: debe propagarse para que RabbitMQ reintente
     */
    public void ejecutar(ProcesarOrden comando) throws Exception {
        simularTrabajo();
        ResultadoTarea resultado = regla.evaluar(comando, Instant.now(reloj));
        kafka.send(topico, comando.orderId(), json.writeValueAsString(resultado))
                .get(ESPERA_KAFKA_SEGUNDOS, TimeUnit.SECONDS);
        log.info("Orden {}: {}", comando.orderId(), resultado.status());
    }

    private void simularTrabajo() throws InterruptedException {
        Thread.sleep(ThreadLocalRandom.current().nextLong(PAUSA_MINIMA_MS, PAUSA_MAXIMA_MS + 1));
    }
}
