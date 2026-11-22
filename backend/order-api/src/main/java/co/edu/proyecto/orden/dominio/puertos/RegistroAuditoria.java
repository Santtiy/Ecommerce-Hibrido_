package co.edu.proyecto.orden.dominio.puertos;

import java.time.Instant;
import java.util.UUID;

/** Una fila de auditoría: qué resultado mandó cada worker para una orden. */
public record RegistroAuditoria(UUID ordenId, String origen, String status, String detalle, Instant ocurridoEn) {
}
