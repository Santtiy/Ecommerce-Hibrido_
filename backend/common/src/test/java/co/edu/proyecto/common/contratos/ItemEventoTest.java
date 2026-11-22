package co.edu.proyecto.common.contratos;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ItemEventoTest {

    @Test
    void rechazaCantidadCero() {
        assertThatThrownBy(() -> new ItemEvento("SKU-1", "Libro", 0, new BigDecimal("1000")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaPrecioCero() {
        assertThatThrownBy(() -> new ItemEvento("SKU-1", "Libro", 1, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
