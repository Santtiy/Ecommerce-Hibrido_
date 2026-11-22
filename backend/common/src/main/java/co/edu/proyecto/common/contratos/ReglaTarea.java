package co.edu.proyecto.common.contratos;

import java.time.Instant;

/**
 * Regla de negocio de un worker: decide el resultado de una tarea a partir del comando recibido.
 * La implementan ReglaPago, ReglaInventario y ReglaNotificacion; el procesamiento común de los workers
 * depende de esta interfaz y no de ninguna regla concreta, así un worker nuevo solo aporta su regla.
 *
 * <p>Contrato que toda implementación debe cumplir (lo da por hecho quien la usa):
 * <ul>
 *   <li>nunca devuelve {@code null}: siempre hay un resultado, aprobado o rechazado;</li>
 *   <li>el {@code orderId} del resultado es el del comando;</li>
 *   <li>el {@link EstadoTarea} del resultado es del {@code TipoTarea} de su worker (order-api y el dashboard
 *       rechazan un estado que llegue por el topic de otra tarea);</li>
 *   <li>no tiene efectos secundarios: solo evalúa, el envío lo hace quien la llama.</li>
 * </ul>
 */
public interface ReglaTarea {

    /**
     * @param comando orden a evaluar
     * @param ahora   instante del resultado (se recibe para que la regla sea determinista en las pruebas)
     */
    ResultadoTarea evaluar(ProcesarOrden comando, Instant ahora);
}
