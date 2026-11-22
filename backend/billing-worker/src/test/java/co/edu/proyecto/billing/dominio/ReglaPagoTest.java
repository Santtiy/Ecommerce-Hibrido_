package co.edu.proyecto.billing.dominio;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.contratos.EstadoTarea;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaPagoTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    private final ReglaPago regla = new ReglaPago();

    private static ProcesarOrden ordenPor(String total) {
        ItemEvento item = new ItemEvento("SKU-1", "Televisor", 1, new BigDecimal(total));
        return new ProcesarOrden("o-1", List.of(item), new BigDecimal(total), AHORA);
    }

    @Test
    void unMillonExactoSeAprueba() {
        ResultadoTarea resultado = regla.evaluar(ordenPor("1000000"), AHORA);

        assertThat(resultado.status()).isEqualTo(EstadoTarea.PAYMENT_SUCCESS);
    }

    @Test
    void masDeUnMillonSeRechazaYElDetalleLoExplica() {
        ResultadoTarea resultado = regla.evaluar(ordenPor("1000001"), AHORA);

        assertThat(resultado.status()).isEqualTo(EstadoTarea.PAYMENT_FAILED);
        assertThat(resultado.detalle()).contains("1000000");
    }
}
