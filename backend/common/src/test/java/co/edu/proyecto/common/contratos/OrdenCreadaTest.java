package co.edu.proyecto.common.contratos;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.itemEvento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdenCreadaTest {

    @Test
    void rechazaListaDeItemsVacia() {
        // Act + Assert
        assertThatThrownBy(() -> new OrdenCreada("o-1", "Ana", List.of(), BigDecimal.TEN, "COP", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laListaDeItemsNoCambiaSiSeModificaLaOriginal() {
        // Arrange
        List<ItemEvento> items = new ArrayList<>(List.of(itemEvento()));
        OrdenCreada evento = new OrdenCreada("o-1", "Ana", items, BigDecimal.TEN, "COP", Instant.now());

        // Act
        items.add(itemEvento());

        // Assert
        assertThat(evento.items()).hasSize(1);
    }
}
