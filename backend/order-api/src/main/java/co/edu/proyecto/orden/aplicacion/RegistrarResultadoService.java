package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.TipoTarea;
import co.edu.proyecto.orden.aplicacion.puertos.AuditoriaRepository;
import co.edu.proyecto.orden.aplicacion.puertos.RegistroAuditoria;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;
import co.edu.proyecto.orden.dominio.excepciones.OrdenNoEncontradaException;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso "aplicar el resultado de un worker": siempre lo audita y, si es del pago,
 * confirma o cancela la orden (solo el pago cambia el estado de la orden, RN-16).
 * Es idempotente: un resultado repetido no cambia nada (RN-10).
 *
 * <p>Un estado que no corresponde al tipo de tarea por el que llegó (por ejemplo {@code STOCK_RESERVED} leído de
 * {@code billing_events}) se rechaza con una excepción antes de auditar, así un resultado mal enrutado nunca
 * cancela una orden.
 */
public class RegistrarResultadoService {

    private final OrdenRepository ordenes;
    private final AuditoriaRepository auditoria;

    public RegistrarResultadoService(OrdenRepository ordenes, AuditoriaRepository auditoria) {
        this.ordenes = ordenes;
        this.auditoria = auditoria;
    }

    /**
     * @param tipo      tarea (worker) de cuyo topic llegó el resultado; se audita como {@code origen}
     * @throws IllegalArgumentException si el estado del resultado no es del tipo {@code tipo}
     * @throws OrdenNoEncontradaException si es un resultado de pago de una orden que no existe
     */
    @Transactional
    public void registrar(TipoTarea tipo, ResultadoTarea resultado) {
        EstadoTarea estado = resultado.status().exigirTipo(tipo);
        UUID ordenId = UUID.fromString(resultado.orderId());
        auditoria.registrarSiNoExiste(new RegistroAuditoria(
                ordenId, tipo.name(), estado.name(), resultado.detalle(), resultado.ocurridoEn()));
        if (tipo == TipoTarea.PAGO) {
            aplicarPago(new OrdenId(ordenId), estado);
        }
    }

    /** {@code estado} ya es de tipo PAGO: PAYMENT_SUCCESS confirma la orden y PAYMENT_FAILED la cancela. */
    private void aplicarPago(OrdenId id, EstadoTarea estado) {
        Orden orden = ordenes.buscarPorId(id)
                .orElseThrow(() -> new OrdenNoEncontradaException(id));
        if (!orden.estaCreada()) {
            return; // ya se aplicó antes: el resultado está repetido
        }
        if (estado.esExito()) {
            orden.confirmarPago();
        } else {
            orden.cancelar();
        }
        ordenes.guardar(orden);
    }
}
