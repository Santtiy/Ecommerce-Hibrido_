package co.edu.proyecto.orden.infraestructura.persistencia.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

/** Acceso a auditoria_ordenes. ON CONFLICT DO NOTHING hace que un resultado repetido no falle ni se duplique. */
interface SpringDataAuditoria extends JpaRepository<AuditoriaEntity, Long> {

    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO auditoria_ordenes (orden_id, origen, status, detalle, ocurrido_en, registrado_en)
            VALUES (:ordenId, :origen, :status, :detalle, :ocurridoEn, now())
            ON CONFLICT (orden_id, origen, status) DO NOTHING
            """)
    void insertarSiNoExiste(UUID ordenId, String origen, String status, String detalle, Instant ocurridoEn);
}
