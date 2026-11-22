package co.edu.proyecto.orden.infraestructura.persistencia;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Fila de auditoria_ordenes. Se inserta con SQL nativo (ver SpringDataAuditoria); la clase existe para JPA. */
@Entity
@Table(name = "auditoria_ordenes")
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID ordenId;
    private String origen;
    private String status;
    private String detalle;
    private Instant ocurridoEn;
    private Instant registradoEn;

    protected AuditoriaEntity() {
    }
}
