package co.edu.proyecto.dashboard.dominio;

import co.edu.proyecto.common.contratos.EstadoTarea;

/**
 * Progreso de una orden tal como lo pide el enunciado: { payment, stock, email }.
 * Cada campo es el texto que se envía al navegador: "pending" mientras el worker no responde y, después,
 * el nombre del {@link EstadoTarea} que publicó ({@code PAYMENT_SUCCESS}, {@code STOCK_RESERVED}…).
 * Es inmutable: cada cambio devuelve un progreso nuevo. (Se llama así para no confundirlo con EstadoOrden de order-api.)
 */
public record ProgresoOrden(String payment, String stock, String email) {

    /** Valor de una etapa sin resultado todavía. Solo lo usa el dashboard; no viaja por Kafka ni RabbitMQ. */
    private static final String PENDIENTE = "pending";

    public static ProgresoOrden inicial() {
        return new ProgresoOrden(PENDIENTE, PENDIENTE, PENDIENTE);
    }

    public ProgresoOrden conPago(EstadoTarea estado) {
        return new ProgresoOrden(estado.name(), stock, email);
    }

    public ProgresoOrden conStock(EstadoTarea estado) {
        return new ProgresoOrden(payment, estado.name(), email);
    }

    public ProgresoOrden conEmail(EstadoTarea estado) {
        return new ProgresoOrden(payment, stock, estado.name());
    }
}
