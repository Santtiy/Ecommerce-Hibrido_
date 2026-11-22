package co.edu.proyecto.dashboard.dominio;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.nombres.TipoTarea;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableroOrdenesTest {

    @Test
    void conElPrimerResultadoLasOtrasEtapasSiguenPendientes() {
        // Arrange
        TableroOrdenes tablero = new TableroOrdenes();

        // Act
        ProgresoOrden progreso = tablero.aplicar("o-123", TipoTarea.PAGO, EstadoTarea.PAYMENT_SUCCESS);

        // Assert
        assertThat(progreso).isEqualTo(new ProgresoOrden("PAYMENT_SUCCESS", "pending", "pending"));
    }

    @Test
    void elSegundoResultadoConservaElPrimero() {
        // Arrange
        TableroOrdenes tablero = new TableroOrdenes();
        tablero.aplicar("o-123", TipoTarea.PAGO, EstadoTarea.PAYMENT_SUCCESS);

        // Act
        ProgresoOrden progreso = tablero.aplicar("o-123", TipoTarea.INVENTARIO, EstadoTarea.STOCK_RESERVED);

        // Assert
        assertThat(progreso).isEqualTo(new ProgresoOrden("PAYMENT_SUCCESS", "STOCK_RESERVED", "pending"));
    }

    @Test
    void aplicarDosVecesElMismoResultadoNoCambiaNada() {
        // Arrange
        TableroOrdenes tablero = new TableroOrdenes();
        ProgresoOrden primera = tablero.aplicar("o-123", TipoTarea.NOTIFICACION, EstadoTarea.EMAIL_SENT);

        // Act
        ProgresoOrden segunda = tablero.aplicar("o-123", TipoTarea.NOTIFICACION, EstadoTarea.EMAIL_SENT);

        // Assert
        assertThat(segunda).isEqualTo(primera);
    }

    @Test
    void alPasarDelMaximoSeOlvidaLaOrdenMasAntigua() {
        // Arrange
        TableroOrdenes tablero = new TableroOrdenes();

        // Act
        for (int i = 0; i <= TableroOrdenes.MAXIMO_ORDENES; i++) {
            tablero.aplicar("o-" + i, TipoTarea.PAGO, EstadoTarea.PAYMENT_SUCCESS);
        }

        // Assert
        assertThat(tablero.todas()).hasSize(TableroOrdenes.MAXIMO_ORDENES).doesNotContainKey("o-0");
    }

    @Test
    void unEstadoDeOtroTipoDeTareaSeRechazaYNoCambiaElTablero() {
        // Arrange
        TableroOrdenes tablero = new TableroOrdenes();

        // Act + Assert
        assertThatThrownBy(() -> tablero.aplicar("o-123", TipoTarea.PAGO, EstadoTarea.STOCK_RESERVED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STOCK_RESERVED");
        assertThat(tablero.todas()).isEmpty();
    }
}
