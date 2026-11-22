package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemOrdenTest {

    @Test
    void rechazaCantidadCero() {
        // Act + Assert
        assertThatThrownBy(() -> new ItemOrden("SKU-1", "Libro", 0, Dinero.de("1000", "COP")))
                .isInstanceOf(OrdenInvalidaException.class);
    }

    @Test
    void rechazaPrecioCero() {
        // Act + Assert
        assertThatThrownBy(() -> new ItemOrden("SKU-1", "Libro", 1, Dinero.cero("COP")))
                .isInstanceOf(OrdenInvalidaException.class);
    }

    @Test
    void elSubtotalEsPrecioPorCantidad() {
        // Arrange
        ItemOrden item = new ItemOrden("SKU-1", "Libro", 3, Dinero.de("2000", "COP"));

        // Act
        Dinero subtotal = item.subtotal();

        // Assert
        assertThat(subtotal).isEqualTo(Dinero.de("6000", "COP"));
    }
}
