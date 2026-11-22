package co.edu.proyecto.orden.infraestructura.persistencia.auditoria;

import co.edu.proyecto.orden.aplicacion.puertos.AuditoriaRepository;
import co.edu.proyecto.orden.aplicacion.puertos.RegistroAuditoria;
import org.springframework.stereotype.Repository;

/** Adaptador del puerto AuditoriaRepository sobre PostgreSQL. */
@Repository
public class AuditoriaRepositoryJpa implements AuditoriaRepository {

    private final SpringDataAuditoria springData;

    public AuditoriaRepositoryJpa(SpringDataAuditoria springData) {
        this.springData = springData;
    }

    @Override
    public void registrarSiNoExiste(RegistroAuditoria registro) {
        springData.insertarSiNoExiste(registro.ordenId(), registro.origen(), registro.status(),
                registro.detalle(), registro.ocurridoEn());
    }
}
