package co.edu.proyecto.analytics.dominio;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.OrdenCreada;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgregadorVentasTest {

    private static OrdenCreada orden(String id, String total) {
        ItemEvento item = new ItemEvento("SKU-1", "Libro", 1, new BigDecimal(total));
        return new OrdenCreada(id, "Ana", List.of(item), new BigDecimal(total), "COP", Instant.now());
    }

    @Test
    void dosOrdenesDistintasSeSuman() {
        AgregadorVentas agregador = new AgregadorVentas();

        agregador.registrar(orden("o-1", "1000"));
        agregador.registrar(orden("o-2", "2500"));

        assertThat(agregador.totalAcumulado()).isEqualByComparingTo("3500");
        assertThat(agregador.cantidadOrdenes()).isEqualTo(2);
    }

    @Test
    void laMismaOrdenDosVecesSeCuentaUnaSola() {
        AgregadorVentas agregador = new AgregadorVentas();

        agregador.registrar(orden("o-1", "1000"));
        agregador.registrar(orden("o-1", "1000"));

        assertThat(agregador.totalAcumulado()).isEqualByComparingTo("1000");
    }
}
