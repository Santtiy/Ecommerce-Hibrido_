package co.edu.proyecto.common.contratos;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdenCreadaTest {

    private static final ItemEvento LIBRO = new ItemEvento("SKU-1", "Libro", 1, new BigDecimal("15000"));

    @Test
    void rechazaListaDeItemsVacia() {
        assertThatThrownBy(() -> new OrdenCreada("o-1", "Ana", List.of(), BigDecimal.TEN, "COP", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laListaDeItemsNoCambiaSiSeModificaLaOriginal() {
        List<ItemEvento> items = new ArrayList<>(List.of(LIBRO));
        OrdenCreada evento = new OrdenCreada("o-1", "Ana", items, BigDecimal.TEN, "COP", Instant.now());

        items.add(LIBRO);

        assertThat(evento.items()).hasSize(1);
    }
}
