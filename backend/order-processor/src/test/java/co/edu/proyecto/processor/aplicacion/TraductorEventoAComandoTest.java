package co.edu.proyecto.processor.aplicacion;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.itemEvento;
import static co.edu.proyecto.common.pruebas.DatosDePrueba.ordenCreada;
import static org.assertj.core.api.Assertions.assertThat;

class TraductorEventoAComandoTest {

    /** Hora del reloj del traductor: distinta de la de creación para comprobar que se usa el reloj. */
    private static final Instant AHORA = Instant.parse("2026-11-19T16:30:00Z");

    @Test
    void elComandoCopiaLaOrdenConTodosSusItemsYLaHoraDelReloj() {
        // Arrange
        List<ItemEvento> items = List.of(itemEvento("SKU-1", "Libro", 1, "15000"), itemEvento("SKU-2", "USB", 50, "20000"));
        OrdenCreada evento = ordenCreada("o-1", items, "1000000");
        TraductorEventoAComando traductor = new TraductorEventoAComando(Clock.fixed(AHORA, ZoneOffset.UTC));

        // Act
        ProcesarOrden comando = traductor.traducir(evento);

        // Assert
        assertThat(comando.orderId()).isEqualTo("o-1");
        assertThat(comando.items()).isEqualTo(items);
        assertThat(comando.total()).isEqualByComparingTo("1000000");
        assertThat(comando.solicitadoEn()).isEqualTo(AHORA);
    }
}
