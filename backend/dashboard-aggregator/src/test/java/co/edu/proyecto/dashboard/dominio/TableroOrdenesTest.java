package co.edu.proyecto.dashboard.dominio;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.nombres.TipoTarea;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableroOrdenesTest {

    @Test
    void conElPrimerResultadoLasOtrasEtapasSiguenPendientes() {
        TableroOrdenes tablero = new TableroOrdenes();

        ProgresoOrden progreso = tablero.aplicar("o-123", TipoTarea.PAGO, EstadoTarea.PAYMENT_SUCCESS);

        assertThat(progreso).isEqualTo(new ProgresoOrden("PAYMENT_SUCCESS", "pending", "pending"));
    }

    @Test
    void elSegundoResultadoConservaElPrimero() {
        TableroOrdenes tablero = new TableroOrdenes();
        tablero.aplicar("o-123", TipoTarea.PAGO, EstadoTarea.PAYMENT_SUCCESS);

        ProgresoOrden progreso = tablero.aplicar("o-123", TipoTarea.INVENTARIO, EstadoTarea.STOCK_RESERVED);

        assertThat(progreso).isEqualTo(new ProgresoOrden("PAYMENT_SUCCESS", "STOCK_RESERVED", "pending"));
    }

    @Test
    void aplicarDosVecesElMismoResultadoNoCambiaNada() {
        TableroOrdenes tablero = new TableroOrdenes();
        ProgresoOrden primera = tablero.aplicar("o-123", TipoTarea.NOTIFICACION, EstadoTarea.EMAIL_SENT);

        ProgresoOrden segunda = tablero.aplicar("o-123", TipoTarea.NOTIFICACION, EstadoTarea.EMAIL_SENT);

        assertThat(segunda).isEqualTo(primera);
    }

    @Test
    void alPasarDelMaximoSeOlvidaLaOrdenMasAntigua() {
        TableroOrdenes tablero = new TableroOrdenes();
        for (int i = 0; i <= TableroOrdenes.MAXIMO_ORDENES; i++) {
            tablero.aplicar("o-" + i, TipoTarea.PAGO, EstadoTarea.PAYMENT_SUCCESS);
        }

        assertThat(tablero.todas()).hasSize(TableroOrdenes.MAXIMO_ORDENES).doesNotContainKey("o-0");
    }

    @Test
    void unEstadoDeOtroTipoDeTareaSeRechazaYNoCambiaElTablero() {
        // Arrange
        TableroOrdenes tablero = new TableroOrdenes();

        // Act y Assert
        assertThatThrownBy(() -> tablero.aplicar("o-123", TipoTarea.PAGO, EstadoTarea.STOCK_RESERVED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STOCK_RESERVED");
        assertThat(tablero.todas()).isEmpty();
    }
}
