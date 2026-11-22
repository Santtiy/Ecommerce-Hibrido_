package co.edu.proyecto.analytics.sistemasoperativos;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonitorMemoriaTest {

    private final MonitorMemoria monitor = new MonitorMemoria();

    @Test
    void reservarCeroMegasNoSePermite() {
        // Act + Assert
        assertThatThrownBy(() -> monitor.reservarBloque(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reservarUnMegaDevuelveUnBloqueDeUnMega() {
        // Act
        byte[] bloque = monitor.reservarBloque(1);

        // Assert
        assertThat(bloque).hasSize(1024 * 1024);
    }

    @Test
    void laMemoriaUsadaNoSuperaLaMaxima() {
        // Act
        long usada = monitor.memoriaUsadaMb();
        long maxima = monitor.memoriaMaximaMb();

        // Assert
        assertThat(usada).isLessThanOrEqualTo(maxima);
    }
}
