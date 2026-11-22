package co.edu.proyecto.dashboard.dominio;

import co.edu.proyecto.common.nombres.EstadosTarea;

/**
 * Progreso de una orden tal como lo pide el enunciado: { payment, stock, email }.
 * Es inmutable: cada cambio devuelve un progreso nuevo. (Se llama así para no confundirlo con EstadoOrden de order-api.)
 */
public record ProgresoOrden(String payment, String stock, String email) {

    public static ProgresoOrden inicial() {
        return new ProgresoOrden(EstadosTarea.PENDIENTE, EstadosTarea.PENDIENTE, EstadosTarea.PENDIENTE);
    }

    public ProgresoOrden conPago(String status) {
        return new ProgresoOrden(status, stock, email);
    }

    public ProgresoOrden conStock(String status) {
        return new ProgresoOrden(payment, status, email);
    }

    public ProgresoOrden conEmail(String status) {
        return new ProgresoOrden(payment, stock, status);
    }
}
