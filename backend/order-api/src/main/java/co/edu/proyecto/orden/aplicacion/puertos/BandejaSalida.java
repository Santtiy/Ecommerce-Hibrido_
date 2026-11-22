package co.edu.proyecto.orden.aplicacion.puertos;

import co.edu.proyecto.common.contratos.OrdenCreada;

/**
 * Puerto de la bandeja de salida (patrón outbox): deja el evento guardado en la misma transacción
 * que la orden. Otro componente lo publica después en Kafka.
 */
public interface BandejaSalida {

    void encolar(OrdenCreada evento);
}
