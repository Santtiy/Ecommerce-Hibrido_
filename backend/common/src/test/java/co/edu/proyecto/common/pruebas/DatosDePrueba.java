package co.edu.proyecto.common.pruebas;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Datos de prueba compartidos por todos los módulos (se publican en el test-jar de common).
 * Así ninguna prueba vuelve a construir a mano los contratos que viajan por Kafka y RabbitMQ:
 * si un contrato cambia, solo se ajusta esta clase.
 */
public final class DatosDePrueba {

    /** Instante fijo para que las pruebas sean deterministas. */
    public static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");
    public static final String ORDEN_ID = "o-1";

    private DatosDePrueba() {
    }

    // ---------- ItemEvento ----------

    public static ItemEvento itemEvento(String sku, String nombre, int cantidad, String precioUnitario) {
        return new ItemEvento(sku, nombre, cantidad, new BigDecimal(precioUnitario));
    }

    /** Un ítem de una unidad con el precio dado. */
    public static ItemEvento itemEvento(String precioUnitario) {
        return itemEvento("SKU-1", "Libro", 1, precioUnitario);
    }

    /** Un libro de 15.000 COP. */
    public static ItemEvento itemEvento() {
        return itemEvento("15000");
    }

    // ---------- OrdenCreada (evento de new_orders) ----------

    public static OrdenCreada ordenCreada(String orderId, List<ItemEvento> items, String total) {
        return new OrdenCreada(orderId, "Ana", items, new BigDecimal(total), "COP", AHORA);
    }

    /** Orden de un solo ítem cuyo precio es el total. */
    public static OrdenCreada ordenCreada(String orderId, String total) {
        return ordenCreada(orderId, List.of(itemEvento(total)), total);
    }

    public static OrdenCreada ordenCreada() {
        return ordenCreada(ORDEN_ID, "15000");
    }

    // ---------- ProcesarOrden (comando de order_tasks) ----------

    public static ProcesarOrden procesarOrden(String orderId, List<ItemEvento> items, String total) {
        return new ProcesarOrden(orderId, items, new BigDecimal(total), AHORA);
    }

    /** Comando de un solo ítem cuyo precio es el total. */
    public static ProcesarOrden procesarOrden(String total) {
        return procesarOrden(ORDEN_ID, List.of(itemEvento(total)), total);
    }

    /** Comando con los ítems dados (el total no importa a quien lo use). */
    public static ProcesarOrden procesarOrden(List<ItemEvento> items) {
        return procesarOrden(ORDEN_ID, items, "1000");
    }

    // ---------- ResultadoTarea (eventos de billing/inventory/notification_events) ----------

    public static ResultadoTarea resultadoTarea(String orderId, EstadoTarea status) {
        return new ResultadoTarea(orderId, status, "detalle", AHORA);
    }
}
