package co.edu.proyecto.inventory.dominio;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.EstadosTarea;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaInventarioTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    private final ReglaInventario regla = new ReglaInventario();

    private static ProcesarOrden ordenCon(int... cantidades) {
        List<ItemEvento> items = new ArrayList<>();
        for (int i = 0; i < cantidades.length; i++) {
            items.add(new ItemEvento("SKU-" + i, "Producto " + i, cantidades[i], new BigDecimal("1000")));
        }
        return new ProcesarOrden("o-1", items, new BigDecimal("1000"), AHORA);
    }

    @Test
    void diezUnidadesSeReservan() {
        ResultadoTarea resultado = regla.evaluar(ordenCon(10), AHORA);

        assertThat(resultado.status()).isEqualTo(EstadosTarea.STOCK_RESERVADO);
    }

    @Test
    void unItemExcedidoAunqueNoSeaElPrimeroDejaSinStock() {
        ResultadoTarea resultado = regla.evaluar(ordenCon(1, 50), AHORA);

        assertThat(resultado.status()).isEqualTo(EstadosTarea.SIN_STOCK);
        assertThat(resultado.detalle()).contains("SKU-1").contains("50");
    }
}
