package co.edu.proyecto.analytics.sistemasoperativos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonitorMemoriaTest {

    private final MonitorMemoria monitor = new MonitorMemoria();

    @Test
    void reservarCeroMegasNoSePermite() {
        assertThatThrownBy(() -> monitor.reservarBloque(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reservarUnMegaDevuelveUnBloqueDeUnMega() {
        byte[] bloque = monitor.reservarBloque(1);

        assertThat(bloque).hasSize(1024 * 1024);
    }

    @Test
    void laMemoriaUsadaNoSuperaLaMaxima() {
        assertThat(monitor.memoriaUsadaMb()).isLessThanOrEqualTo(monitor.memoriaMaximaMb());
    }
}
