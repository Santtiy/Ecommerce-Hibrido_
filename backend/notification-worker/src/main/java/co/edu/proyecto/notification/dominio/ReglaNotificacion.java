package co.edu.proyecto.notification.dominio;

import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.EstadosTarea;

import java.time.Instant;

/** RN-09: la notificación se envía siempre, en paralelo y sin depender del pago. */
public class ReglaNotificacion {

    public ResultadoTarea evaluar(ProcesarOrden comando, Instant ahora) {
        return new ResultadoTarea(comando.orderId(), EstadosTarea.CORREO_ENVIADO, "Correo de confirmación enviado", ahora);
    }
}
