package co.edu.proyecto.common.contratos;

import java.time.Instant;

/** Resultado que cada worker publica en su topic de Kafka (billing, inventory o notification). */
public record ResultadoTarea(String orderId, String status, String detalle, Instant ocurridoEn) {

    public ResultadoTarea {
        Validaciones.textoObligatorio(orderId, "orderId");
        Validaciones.textoObligatorio(status, "status");
        Validaciones.obligatorio(ocurridoEn, "ocurridoEn");
    }
}
