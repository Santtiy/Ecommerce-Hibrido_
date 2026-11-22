package co.edu.proyecto.orden.aplicacion.puertos;

/** Puerto de auditoría. Registrar dos veces el mismo resultado no crea una fila nueva (idempotente). */
public interface AuditoriaRepository {

    void registrarSiNoExiste(RegistroAuditoria registro);
}
