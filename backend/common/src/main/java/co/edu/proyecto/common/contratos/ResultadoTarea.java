package co.edu.proyecto.common.contratos;

import java.time.Instant;

/**
 * Resultado que cada worker publica en su topic de Kafka (billing, inventory o notification).
 * En el JSON, {@code status} sigue siendo un texto ({@code "PAYMENT_SUCCESS"}…): es el nombre del {@link EstadoTarea}.
 */
public record ResultadoTarea(String orderId, EstadoTarea status, String detalle, Instant ocurridoEn) {

    public ResultadoTarea {
        Validaciones.textoObligatorio(orderId, "orderId");
        Validaciones.obligatorio(status, "status");
        Validaciones.obligatorio(ocurridoEn, "ocurridoEn");
    }
}
