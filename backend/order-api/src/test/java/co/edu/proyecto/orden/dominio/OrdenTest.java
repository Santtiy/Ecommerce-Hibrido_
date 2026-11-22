package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.descuentos.SinDescuento;
import co.edu.proyecto.orden.dominio.excepciones.EstadoInvalidoException;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import co.edu.proyecto.orden.dominio.impuestos.SinImpuesto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdenTest {

    private static final ItemOrden LIBRO = new ItemOrden("SKU-1", "Libro", 1, Dinero.de("50000", "COP"));

    private static Orden ordenNueva() {
        return new OrdenBuilder().cliente("Ana Pérez").agregarItem(LIBRO).construir();
    }

    @Test
    void confirmarPagoLaDejaPagada() {
        // Arrange
        Orden orden = ordenNueva();

        // Act
        orden.confirmarPago();

        // Assert
        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGADA);
    }

    @Test
    void cancelarLaDejaCancelada() {
        // Arrange
        Orden orden = ordenNueva();

        // Act
        orden.cancelar();

        // Assert
        assertThat(orden.estado()).isEqualTo(EstadoOrden.CANCELADA);
    }

    @Test
    void unaOrdenPagadaNoSePuedeCancelar() {
        // Arrange
        Orden orden = ordenNueva();
        orden.confirmarPago();

        // Act + Assert
        assertThatThrownBy(orden::cancelar)
                .isInstanceOf(EstadoInvalidoException.class)
                .hasMessage("Una orden pagada no puede cancelarse");
        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGADA);
    }

    @Test
    void noSePuedePagarDosVeces() {
        // Arrange
        Orden orden = ordenNueva();
        orden.confirmarPago();

        // Act + Assert
        assertThatThrownBy(orden::confirmarPago).isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void noSePuedeCalcularElTotalDeUnaOrdenCancelada() {
        // Arrange
        Orden orden = ordenNueva();
        orden.cancelar();

        // Act + Assert
        assertThatThrownBy(() -> orden.calcularTotal(new SinDescuento(), new SinImpuesto()))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void reconstituirRecuperaElEstadoYElTotalGuardados() {
        // Arrange
        OrdenId id = OrdenId.nuevo();
        Dinero total = Dinero.de("59500", "COP");
        Instant creadaEn = Instant.parse("2026-11-19T15:00:00Z");

        // Act
        Orden orden = Orden.reconstituir(id, "Ana Pérez", List.of(LIBRO), EstadoOrden.PAGADA, total, creadaEn);

        // Assert
        assertThat(orden.id()).isEqualTo(id);
        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGADA);
        assertThat(orden.total()).isEqualTo(total);
        assertThat(orden.creadaEn()).isEqualTo(creadaEn);
    }

    @Test
    void reconstituirSinItemsSeRechaza() {
        // Act + Assert
        assertThatThrownBy(() -> Orden.reconstituir(OrdenId.nuevo(), "Ana Pérez", List.of(),
                EstadoOrden.CREADA, Dinero.cero("COP"), Instant.now()))
                .isInstanceOf(OrdenInvalidaException.class);
    }

    @Test
    void laListaDeItemsNoSePuedeModificarDesdeFuera() {
        // Arrange
        Orden orden = ordenNueva();

        // Act + Assert
        assertThatThrownBy(() -> orden.items().add(LIBRO)).isInstanceOf(UnsupportedOperationException.class);
    }
}
