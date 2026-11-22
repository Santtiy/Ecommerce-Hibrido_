package co.edu.proyecto.processor.infraestructura.kafka;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.nombres.GruposConsumidores;
import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.processor.aplicacion.TraductorEventoAComando;
import co.edu.proyecto.processor.infraestructura.rabbitmq.PublicadorTareas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Lee cada OrdenCreada de Kafka (grupo processor-group), la traduce a comando y la manda a RabbitMQ. */
@Component
public class ConsumidorNuevasOrdenes {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorNuevasOrdenes.class);

    private final TraductorEventoAComando traductor;
    private final PublicadorTareas publicador;

    public ConsumidorNuevasOrdenes(TraductorEventoAComando traductor, PublicadorTareas publicador) {
        this.traductor = traductor;
        this.publicador = publicador;
    }

    @KafkaListener(topics = Topicos.NUEVAS_ORDENES, groupId = GruposConsumidores.PROCESSOR)
    public void alRecibir(OrdenCreada evento) {
        publicador.publicar(traductor.traducir(evento));
        log.info("Orden {} enviada a RabbitMQ como comando", evento.orderId());
    }
}
