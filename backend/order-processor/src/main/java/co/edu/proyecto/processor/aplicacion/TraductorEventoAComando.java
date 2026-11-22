package co.edu.proyecto.processor.aplicacion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;

import java.time.Clock;

/**
 * Convierte un hecho ("se creó la orden") en una orden de trabajo ("procesa esta orden").
 * Java puro: se prueba sin Spring.
 */
public class TraductorEventoAComando {

    private final Clock reloj;

    public TraductorEventoAComando(Clock reloj) {
        this.reloj = reloj;
    }

    public ProcesarOrden traducir(OrdenCreada evento) {
        return new ProcesarOrden(evento.orderId(), evento.items(), evento.total(), reloj.instant());
    }
}
