/**
 * Capa de infraestructura: adaptadores (REST, JPA, Kafka, RabbitMQ, WebSocket, archivos), agrupados por
 * funcionalidad: cada uno en el subpaquete de su tecnología ({@code kafka/}, {@code rabbitmq/}, {@code websocket/},
 * {@code rest/}, {@code persistencia/}). Implementan los puertos o llaman a los casos de uso; no deciden reglas.
 *
 * <p>{@code rest/} POST /order; {@code persistencia/orden/} y {@code persistencia/auditoria/} JPA;
 * {@code kafka/} resultados de los workers. {@code bandejasalida/} es la excepción: el patrón outbox usa JPA y Kafka
 * a la vez y se deja junto para no partir una misma funcionalidad.
 */
package co.edu.proyecto.orden.infraestructura;
