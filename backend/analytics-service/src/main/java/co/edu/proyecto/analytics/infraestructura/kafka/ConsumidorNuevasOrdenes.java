package co.edu.proyecto.analytics.infraestructura.kafka;

import co.edu.proyecto.analytics.dominio.AgregadorVentas;
import co.edu.proyecto.analytics.sistemasoperativos.Bitacora;
import co.edu.proyecto.analytics.sistemasoperativos.MonitorMemoria;
import co.edu.proyecto.analytics.sistemasoperativos.OperacionES;
import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.nombres.GruposConsumidores;
import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.mensajeria.kafka.ConsumidorQueReleeTodo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Segundo consumidor independiente de new_orders (grupo analytics-group): acumula ventas y
 * registra cada orden en la bitácora. Al arrancar relee el topic desde el inicio
 * para reconstruir el total (lo hace {@link ConsumidorQueReleeTodo}).
 */
@Component
public class ConsumidorNuevasOrdenes extends ConsumidorQueReleeTodo {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorNuevasOrdenes.class);

    private final AgregadorVentas agregador;
    private final Bitacora bitacora;
    private final MonitorMemoria monitor;

    public ConsumidorNuevasOrdenes(AgregadorVentas agregador, Bitacora bitacora, MonitorMemoria monitor) {
        this.agregador = agregador;
        this.bitacora = bitacora;
        this.monitor = monitor;
    }

    @KafkaListener(topics = Topicos.NUEVAS_ORDENES, groupId = GruposConsumidores.ANALYTICS)
    public void alRecibir(OrdenCreada evento) {
        agregador.registrar(evento);
        OperacionES escritura = bitacora.escribir(String.join(";", evento.orderId(),
                evento.total().toPlainString(), evento.creadaEn().toString()));
        log.info("Orden {} | órdenes={} | total acumulado={} {}", evento.orderId(),
                agregador.cantidadOrdenes(), agregador.totalAcumulado().toPlainString(), evento.moneda());
        log.info("[SO] escritura={} | archivo={} bytes | {}", escritura, bitacora.tamanoBytes(), monitor.reportar());
    }
}
