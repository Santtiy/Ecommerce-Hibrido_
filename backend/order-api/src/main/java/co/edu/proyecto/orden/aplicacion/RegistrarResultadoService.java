package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.EstadosTarea;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import co.edu.proyecto.orden.dominio.puertos.AuditoriaRepository;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import co.edu.proyecto.orden.dominio.puertos.RegistroAuditoria;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso "aplicar el resultado de un worker": siempre lo audita y, si es del pago,
 * confirma o cancela la orden. Es idempotente: un resultado repetido no cambia nada (RN-10).
 */
public class RegistrarResultadoService {

    private final OrdenRepository ordenes;
    private final AuditoriaRepository auditoria;

    public RegistrarResultadoService(OrdenRepository ordenes, AuditoriaRepository auditoria) {
        this.ordenes = ordenes;
        this.auditoria = auditoria;
    }

    @Transactional
    public void registrar(OrigenResultado origen, ResultadoTarea resultado) {
        UUID ordenId = UUID.fromString(resultado.orderId());
        auditoria.registrarSiNoExiste(new RegistroAuditoria(
                ordenId, origen.name(), resultado.status(), resultado.detalle(), resultado.ocurridoEn()));
        if (origen == OrigenResultado.PAGO) {
            aplicarPago(new OrdenId(ordenId), resultado.status());
        }
    }

    private void aplicarPago(OrdenId id, String status) {
        Orden orden = ordenes.buscarPorId(id)
                .orElseThrow(() -> new OrdenInvalidaException("No existe la orden " + id));
        if (!orden.estaCreada()) {
            return; // ya se aplicó antes: el resultado está repetido
        }
        if (EstadosTarea.PAGO_EXITOSO.equals(status)) {
            orden.confirmarPago();
        } else {
            orden.cancelar();
        }
        ordenes.guardar(orden);
    }
}
