package co.edu.proyecto.inventory.dominio;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.AHORA;
import static co.edu.proyecto.common.pruebas.DatosDePrueba.itemEvento;
import static co.edu.proyecto.common.pruebas.DatosDePrueba.procesarOrden;
import static org.assertj.core.api.Assertions.assertThat;

class ReglaInventarioTest {

    private final ReglaInventario regla = new ReglaInventario();

    /** Un ítem SKU-i por cada cantidad, en ese orden. */
    private static ProcesarOrden ordenCon(int... cantidades) {
        List<ItemEvento> items = new ArrayList<>();
        for (int i = 0; i < cantidades.length; i++) {
            items.add(itemEvento("SKU-" + i, "Producto " + i, cantidades[i], "1000"));
        }
        return procesarOrden(items);
    }

    @Test
    void diezUnidadesSeReservan() {
        // Arrange
        ProcesarOrden comando = ordenCon(10);

        // Act
        ResultadoTarea resultado = regla.evaluar(comando, AHORA);

        // Assert
        assertThat(resultado.status()).isEqualTo(EstadoTarea.STOCK_RESERVED);
    }

    @Test
    void unItemExcedidoAunqueNoSeaElPrimeroDejaSinStock() {
        // Arrange
        ProcesarOrden comando = ordenCon(1, 50);

        // Act
        ResultadoTarea resultado = regla.evaluar(comando, AHORA);

        // Assert
        assertThat(resultado.status()).isEqualTo(EstadoTarea.STOCK_OUT_OF_STOCK);
        assertThat(resultado.detalle()).contains("SKU-1").contains("50");
    }
}
