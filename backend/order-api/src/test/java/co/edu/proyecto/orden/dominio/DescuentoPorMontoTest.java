package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.descuentos.DescuentoPorMonto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DescuentoPorMontoTest {

    private final DescuentoPorMonto descuento = new DescuentoPorMonto();

    @Test
    void cienMilExactosTienenDiezMilDeDescuento() {
        // Arrange
        Dinero subtotal = Dinero.de("100000", "COP");

        // Act
        Dinero resultado = descuento.aplicar(subtotal);

        // Assert
        assertThat(resultado).isEqualTo(Dinero.de("90000", "COP"));
    }

    @Test
    void porDebajoDeCienMilNoSeAplicaDescuento() {
        // Arrange
        Dinero subtotal = Dinero.de("99999.99", "COP");

        // Act
        Dinero resultado = descuento.aplicar(subtotal);

        // Assert
        assertThat(resultado).isEqualTo(subtotal);
    }

    @Test
    void porEncimaDeCienMilSeDescuentaElDiezPorCiento() {
        // Arrange
        Dinero subtotal = Dinero.de("200000", "COP");

        // Act
        Dinero resultado = descuento.aplicar(subtotal);

        // Assert
        assertThat(resultado).isEqualTo(Dinero.de("180000", "COP"));
    }
}
