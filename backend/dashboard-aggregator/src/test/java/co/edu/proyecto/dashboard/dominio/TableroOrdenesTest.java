package co.edu.proyecto.dashboard.dominio;

import co.edu.proyecto.common.nombres.EstadosTarea;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TableroOrdenesTest {

    @Test
    void conElPrimerResultadoLasOtrasEtapasSiguenPendientes() {
        TableroOrdenes tablero = new TableroOrdenes();

        ProgresoOrden progreso = tablero.aplicar("o-123", Etapa.PAGO, EstadosTarea.PAGO_EXITOSO);

        assertThat(progreso).isEqualTo(new ProgresoOrden("PAYMENT_SUCCESS", "pending", "pending"));
    }

    @Test
    void elSegundoResultadoConservaElPrimero() {
        TableroOrdenes tablero = new TableroOrdenes();
        tablero.aplicar("o-123", Etapa.PAGO, EstadosTarea.PAGO_EXITOSO);

        ProgresoOrden progreso = tablero.aplicar("o-123", Etapa.STOCK, EstadosTarea.STOCK_RESERVADO);

        assertThat(progreso).isEqualTo(new ProgresoOrden("PAYMENT_SUCCESS", "STOCK_RESERVED", "pending"));
    }

    @Test
    void aplicarDosVecesElMismoResultadoNoCambiaNada() {
        TableroOrdenes tablero = new TableroOrdenes();
        ProgresoOrden primera = tablero.aplicar("o-123", Etapa.CORREO, EstadosTarea.CORREO_ENVIADO);

        ProgresoOrden segunda = tablero.aplicar("o-123", Etapa.CORREO, EstadosTarea.CORREO_ENVIADO);

        assertThat(segunda).isEqualTo(primera);
    }

    @Test
    void alPasarDelMaximoSeOlvidaLaOrdenMasAntigua() {
        TableroOrdenes tablero = new TableroOrdenes();
        for (int i = 0; i <= TableroOrdenes.MAXIMO_ORDENES; i++) {
            tablero.aplicar("o-" + i, Etapa.PAGO, EstadosTarea.PAGO_EXITOSO);
        }

        assertThat(tablero.todas()).hasSize(TableroOrdenes.MAXIMO_ORDENES).doesNotContainKey("o-0");
    }
}
