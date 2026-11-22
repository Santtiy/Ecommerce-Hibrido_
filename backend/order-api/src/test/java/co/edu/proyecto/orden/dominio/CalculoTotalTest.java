package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.descuentos.DescuentoPorMonto;
import co.edu.proyecto.orden.dominio.descuentos.SinDescuento;
import co.edu.proyecto.orden.dominio.impuestos.IvaColombia;
import co.edu.proyecto.orden.dominio.impuestos.SinImpuesto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculoTotalTest {

    private static Orden ordenDe(String precio) {
        ItemOrden item = new ItemOrden("SKU-1", "Monitor", 1, Dinero.de(precio, "COP"));
        return new OrdenBuilder().cliente("Ana Pérez").agregarItem(item).construir();
    }

    @Test
    void conDescuentoEIvaCientoCincuentaMilDaCientoSesentaMilSeiscientosCincuenta() {
        // Arrange
        Orden orden = ordenDe("150000");

        // Act
        Dinero total = orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());

        // Assert
        assertThat(total).isEqualTo(Dinero.de("160650", "COP"));
    }

    @Test
    void cienMilDeBaseIncluyenIvaYDescuentoEnElTotal() {
        // Arrange
        Orden orden = ordenDe("100000");

        // Act
        Dinero total = orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());

        // Assert
        assertThat(total).isEqualTo(Dinero.de("107100", "COP"));
    }

    @Test
    void porDebajoDelUmbralConIvaNoSeAplicaDescuento() {
        // Arrange
        Orden orden = ordenDe("80000");

        // Act
        Dinero total = orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());

        // Assert
        assertThat(total).isEqualTo(Dinero.de("95200", "COP"));
    }

    @Test
    void cienMilAnunciadosConIvaQuedanEnNoventaMilFinales() {
        // Arrange
        Orden orden = ordenDe("84033.61"); // Con IVA: 100.000,00.

        // Act
        Dinero total = orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());

        // Assert
        assertThat(total).isEqualTo(Dinero.de("90000", "COP"));
    }

    @Test
    void debajoDeCienMilAnunciadosNoSeActivaElDescuento() {
        // Arrange
        Orden orden = ordenDe("84033.60"); // Con IVA: 99.999,98.

        // Act
        Dinero total = orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());

        // Assert
        assertThat(total).isEqualTo(Dinero.de("99999.98", "COP"));
    }

    @Test
    void laEstrategiaDeIvaSeAplicaTambienSinDescuento() {
        // Arrange
        Orden orden = ordenDe("100000");

        // Act
        Dinero total = orden.calcularTotal(new SinDescuento(), new IvaColombia());

        // Assert
        assertThat(total).isEqualTo(Dinero.de("119000", "COP"));
    }

    @Test
    void sinDescuentoNiImpuestoElTotalEsElSubtotal() {
        // Arrange
        Orden orden = ordenDe("90000");

        // Act
        Dinero total = orden.calcularTotal(new SinDescuento(), new SinImpuesto());

        // Assert
        assertThat(total).isEqualTo(Dinero.de("90000", "COP"));
    }
}
