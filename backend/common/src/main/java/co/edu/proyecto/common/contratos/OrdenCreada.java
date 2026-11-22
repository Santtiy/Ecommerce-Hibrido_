package co.edu.proyecto.common.contratos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Evento publicado en Kafka (new_orders) cuando una orden queda registrada.
 * Lleva la lista completa de ítems y la fecha en que se creó la orden.
 */
public record OrdenCreada(String orderId, String cliente, List<ItemEvento> items,
                          BigDecimal total, String moneda, Instant creadaEn) {

    public OrdenCreada {
        Validaciones.textoObligatorio(orderId, "orderId");
        Validaciones.textoObligatorio(cliente, "cliente");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Una orden creada debe tener al menos un ítem");
        }
        items = List.copyOf(items);
        Validaciones.noNegativo(total, "total");
        Validaciones.textoObligatorio(moneda, "moneda");
        Validaciones.obligatorio(creadaEn, "creadaEn");
    }
}
