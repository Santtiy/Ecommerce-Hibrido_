/**
 * Capa de infraestructura: adaptadores (REST, JPA, Kafka, RabbitMQ, WebSocket, archivos), agrupados por
 * funcionalidad: cada uno en el subpaquete de su tecnología ({@code kafka/}, {@code rabbitmq/}, {@code websocket/},
 * {@code rest/}, {@code persistencia/}). Implementan los puertos o llaman a los casos de uso; no deciden reglas.
 *
 * <p>{@code kafka/} lee new_orders; {@code rabbitmq/} publica en el fanout order_tasks.
 */
package co.edu.proyecto.processor.infraestructura;
