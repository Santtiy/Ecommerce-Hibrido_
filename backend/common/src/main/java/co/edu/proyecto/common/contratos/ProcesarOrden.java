package co.edu.proyecto.common.contratos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Comando que viaja por RabbitMQ (exchange order_tasks): "procesa esta orden". */
public record ProcesarOrden(String orderId, List<ItemEvento> items, BigDecimal total, Instant solicitadoEn) {

    public ProcesarOrden {
        Validaciones.textoObligatorio(orderId, "orderId");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Un comando de orden debe tener al menos un ítem");
        }
        items = List.copyOf(items);
        Validaciones.noNegativo(total, "total");
        Validaciones.obligatorio(solicitadoEn, "solicitadoEn");
    }
}
