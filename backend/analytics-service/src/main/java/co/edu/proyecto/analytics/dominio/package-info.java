/**
 * Capa de dominio: reglas de negocio puras (entidades, value objects, reglas, estrategias).
 * Es Java puro: no importa Spring, Jakarta, JPA, Kafka ni RabbitMQ, así se prueba sin levantar nada.
 *
 * <p>AgregadorVentas: acumula el total de ventas de forma idempotente (regla de negocio pura).
 */
package co.edu.proyecto.analytics.dominio;
