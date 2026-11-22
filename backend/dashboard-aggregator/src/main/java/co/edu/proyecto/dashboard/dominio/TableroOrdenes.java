package co.edu.proyecto.dashboard.dominio;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado agregado de las órdenes: orderId → { payment, stock, email }.
 * Aplicar dos veces el mismo resultado deja el mismo estado (idempotente).
 * Guarda como máximo MAXIMO_ORDENES; al pasarse, olvida la más antigua.
 * Sus métodos son synchronized porque los tres consumidores de Kafka escriben a la vez.
 */
public class TableroOrdenes {

    public static final int MAXIMO_ORDENES = 1000;

    private final Map<String, ProgresoOrden> ordenes = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ProgresoOrden> masAntigua) {
            return size() > MAXIMO_ORDENES;
        }
    };

    public synchronized ProgresoOrden aplicar(String orderId, Etapa etapa, String status) {
        ProgresoOrden actual = ordenes.getOrDefault(orderId, ProgresoOrden.inicial());
        ProgresoOrden nuevo = switch (etapa) {
            case PAGO -> actual.conPago(status);
            case STOCK -> actual.conStock(status);
            case CORREO -> actual.conEmail(status);
        };
        ordenes.put(orderId, nuevo);
        return nuevo;
    }

    public synchronized Map<String, ProgresoOrden> todas() {
        return Map.copyOf(ordenes);
    }
}
