package co.edu.proyecto.analytics.dominio;

import co.edu.proyecto.common.contratos.OrdenCreada;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Total de ventas acumulado en tiempo real. Es idempotente: una orden que llega dos veces
 * se cuenta una sola. Sus métodos son synchronized porque el consumidor usa varios hilos.
 */
public class AgregadorVentas {

    private final Set<String> ordenesContadas = new HashSet<>();
    private BigDecimal totalAcumulado = BigDecimal.ZERO;

    public synchronized void registrar(OrdenCreada evento) {
        if (ordenesContadas.add(evento.orderId())) {
            totalAcumulado = totalAcumulado.add(evento.total());
        }
    }

    public synchronized BigDecimal totalAcumulado() {
        return totalAcumulado;
    }

    public synchronized int cantidadOrdenes() {
        return ordenesContadas.size();
    }
}
