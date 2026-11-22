/**
 * Capa de infraestructura: adaptadores (REST, JPA, Kafka, RabbitMQ, WebSocket, archivos), agrupados por
 * funcionalidad: cada uno en el subpaquete de su tecnología ({@code kafka/}, {@code rabbitmq/}, {@code websocket/},
 * {@code rest/}, {@code persistencia/}). Implementan los puertos o llaman a los casos de uso; no deciden reglas.
 *
 * <p>{@code rabbitmq/} escucha notification_queue y delega en EjecutorTarea.
 */
package co.edu.proyecto.notification.infraestructura;
