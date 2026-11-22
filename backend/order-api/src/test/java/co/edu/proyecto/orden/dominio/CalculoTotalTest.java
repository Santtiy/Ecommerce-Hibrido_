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
    void porDebajoDelUmbralSoloSeAplicaElIva() {
        // Arrange
        Orden orden = ordenDe("90000");

        // Act
        Dinero total = orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());

        // Assert
        assertThat(total).isEqualTo(Dinero.de("107100", "COP"));
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
