package co.edu.proyecto.orden.infraestructura.rest;

import co.edu.proyecto.orden.dominio.Orden;

import java.math.BigDecimal;

/** Respuesta de POST /order: la orden quedó aceptada; el resto del proceso es asíncrono. */
public record RespuestaOrden(String orderId, String estado, BigDecimal total, String moneda) {

    static RespuestaOrden desde(Orden orden) {
        return new RespuestaOrden(orden.id().toString(), orden.estado().name(),
                orden.total().monto(), orden.total().moneda());
    }
}
