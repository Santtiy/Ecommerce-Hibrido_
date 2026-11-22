package co.edu.proyecto.inventory.dominio;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ReglaTarea;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.EstadosTarea;

import java.time.Instant;
import java.util.Optional;

/** RN-12: la reserva falla si algún ítem de la orden pide más de 10 unidades. */
public class ReglaInventario implements ReglaTarea {

    private static final int MAXIMO_UNIDADES_POR_ITEM = 10;

    @Override
    public ResultadoTarea evaluar(ProcesarOrden comando, Instant ahora) {
        Optional<ItemEvento> excedido = comando.items().stream()
                .filter(item -> item.cantidad() > MAXIMO_UNIDADES_POR_ITEM)
                .findFirst();
        if (excedido.isPresent()) {
            String detalle = "El ítem " + excedido.get().sku() + " pide " + excedido.get().cantidad()
                    + " unidades (máximo " + MAXIMO_UNIDADES_POR_ITEM + ")";
            return new ResultadoTarea(comando.orderId(), EstadosTarea.SIN_STOCK, detalle, ahora);
        }
        return new ResultadoTarea(comando.orderId(), EstadosTarea.STOCK_RESERVADO, "Stock reservado", ahora);
    }
}
