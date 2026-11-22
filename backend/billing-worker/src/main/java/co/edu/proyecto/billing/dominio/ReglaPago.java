package co.edu.proyecto.billing.dominio;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ReglaTarea;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.contratos.EstadoTarea;

import java.math.BigDecimal;
import java.time.Instant;

/** RN-11: el pago falla si el total de la orden supera 1.000.000. */
public class ReglaPago implements ReglaTarea {

    private static final BigDecimal LIMITE_PAGO = new BigDecimal("1000000");

    @Override
    public ResultadoTarea evaluar(ProcesarOrden comando, Instant ahora) {
        if (comando.total().compareTo(LIMITE_PAGO) > 0) {
            String detalle = "Total " + comando.total().toPlainString() + " supera el límite de " + LIMITE_PAGO.toPlainString();
            return new ResultadoTarea(comando.orderId(), EstadoTarea.PAYMENT_FAILED, detalle, ahora);
        }
        return new ResultadoTarea(comando.orderId(), EstadoTarea.PAYMENT_SUCCESS, "Pago aprobado", ahora);
    }
}
