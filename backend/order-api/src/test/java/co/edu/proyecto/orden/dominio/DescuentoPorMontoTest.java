package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.descuentos.DescuentoPorMonto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DescuentoPorMontoTest {

    private final DescuentoPorMonto descuento = new DescuentoPorMonto();

    @Test
    void cienMilExactosNoTienenDescuento() {
        Dinero resultado = descuento.aplicar(Dinero.de("100000", "COP"));

        assertThat(resultado).isEqualTo(Dinero.de("100000", "COP"));
    }

    @Test
    void porEncimaDeCienMilSeDescuentaElDiezPorCiento() {
        Dinero resultado = descuento.aplicar(Dinero.de("200000", "COP"));

        assertThat(resultado).isEqualTo(Dinero.de("180000", "COP"));
    }
}
