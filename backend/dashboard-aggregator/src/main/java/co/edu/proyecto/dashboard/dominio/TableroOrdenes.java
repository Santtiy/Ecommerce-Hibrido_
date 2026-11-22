package co.edu.proyecto.dashboard.dominio;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.nombres.TipoTarea;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado agregado de las órdenes: orderId → { payment, stock, email }.
 * Aplicar dos veces el mismo resultado deja el mismo estado (idempotente).
 * Guarda como máximo MAXIMO_ORDENES; al pasarse, olvida la más antigua.
 * Sus métodos son synchronized porque los tres consumidores de Kafka escriben a la vez.
 * Un estado que no corresponde al tipo de tarea por el que llegó se rechaza, igual que en order-api.
 */
public class TableroOrdenes {

    public static final int MAXIMO_ORDENES = 1000;

    private final Map<String, ProgresoOrden> ordenes = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ProgresoOrden> masAntigua) {
            return size() > MAXIMO_ORDENES;
        }
    };

    /**
     * @throws IllegalArgumentException si {@code estado} no es del tipo {@code tipo}
     */
    public synchronized ProgresoOrden aplicar(String orderId, TipoTarea tipo, EstadoTarea estado) {
        estado.exigirTipo(tipo);
        ProgresoOrden actual = ordenes.getOrDefault(orderId, ProgresoOrden.inicial());
        ProgresoOrden nuevo = switch (tipo) {
            case PAGO -> actual.conPago(estado);
            case INVENTARIO -> actual.conStock(estado);
            case NOTIFICACION -> actual.conEmail(estado);
        };
        ordenes.put(orderId, nuevo);
        return nuevo;
    }

    public synchronized Map<String, ProgresoOrden> todas() {
        return Map.copyOf(ordenes);
    }
}
