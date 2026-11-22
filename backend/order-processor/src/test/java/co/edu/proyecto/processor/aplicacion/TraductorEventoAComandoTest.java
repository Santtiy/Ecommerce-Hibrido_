package co.edu.proyecto.processor.aplicacion;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TraductorEventoAComandoTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    @Test
    void elComandoCopiaLaOrdenConTodosSusItemsYLaHoraDelReloj() {
        List<ItemEvento> items = List.of(
                new ItemEvento("SKU-1", "Libro", 1, new BigDecimal("15000")),
                new ItemEvento("SKU-2", "USB", 50, new BigDecimal("20000")));
        OrdenCreada evento = new OrdenCreada("o-1", "Ana", items, new BigDecimal("1000000"), "COP", AHORA);
        TraductorEventoAComando traductor = new TraductorEventoAComando(Clock.fixed(AHORA, ZoneOffset.UTC));

        ProcesarOrden comando = traductor.traducir(evento);

        assertThat(comando.orderId()).isEqualTo("o-1");
        assertThat(comando.items()).isEqualTo(items);
        assertThat(comando.total()).isEqualByComparingTo("1000000");
        assertThat(comando.solicitadoEn()).isEqualTo(AHORA);
    }
}
