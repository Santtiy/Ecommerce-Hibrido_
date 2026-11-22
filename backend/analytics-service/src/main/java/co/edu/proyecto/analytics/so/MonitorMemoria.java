package co.edu.proyecto.analytics.so;

import org.springframework.stereotype.Component;

/** Gestor de memoria visto desde la JVM: consulta el consumo y permite reservar un bloque a propósito. */
@Component
public class MonitorMemoria {

    private static final long BYTES_POR_MB = 1024 * 1024;

    private final Runtime runtime = Runtime.getRuntime();

    public long memoriaUsadaMb() {
        return (runtime.totalMemory() - runtime.freeMemory()) / BYTES_POR_MB;
    }

    public long memoriaMaximaMb() {
        return runtime.maxMemory() / BYTES_POR_MB;
    }

    /** Dentro de un contenedor devuelve el límite del cgroup, no los núcleos reales de la máquina. */
    public int procesadoresDisponibles() {
        return runtime.availableProcessors();
    }

    public byte[] reservarBloque(int megas) {
        if (megas <= 0) {
            throw new IllegalArgumentException("Los megas deben ser positivos: " + megas);
        }
        return new byte[Math.toIntExact(megas * BYTES_POR_MB)];
    }

    public String reportar() {
        return "memoria usada=" + memoriaUsadaMb() + " MB, máxima=" + memoriaMaximaMb()
                + " MB, procesadores=" + procesadoresDisponibles();
    }
}
