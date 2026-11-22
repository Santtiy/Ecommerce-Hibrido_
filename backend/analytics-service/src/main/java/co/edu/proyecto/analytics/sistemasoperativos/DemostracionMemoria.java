package co.edu.proyecto.analytics.sistemasoperativos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Al arrancar, mide la memoria, reserva un bloque y vuelve a medir: muestra la memoria manipulada desde el código. */
@Component
public class DemostracionMemoria implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemostracionMemoria.class);
    private static final int MEGAS_DEMOSTRACION = 8;

    private final MonitorMemoria monitor;

    public DemostracionMemoria(MonitorMemoria monitor) {
        this.monitor = monitor;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        log.info("[SO] Antes de reservar: {}", monitor.reportar());
        byte[] bloque = monitor.reservarBloque(MEGAS_DEMOSTRACION);
        log.info("[SO] Con {} MB reservados ({} bytes): {}", MEGAS_DEMOSTRACION, bloque.length, monitor.reportar());
        bloque = null; // se suelta la referencia: el recolector de basura podrá liberar el bloque
        log.info("[SO] Bloque liberado para el recolector: {}", monitor.reportar());
    }
}
