/**
 * Capa de dominio: reglas de negocio puras (entidades, value objects, reglas, estrategias).
 * Es Java puro: no importa Spring, Jakarta, JPA, Kafka ni RabbitMQ, así se prueba sin levantar nada.
 *
 * <p>TableroOrdenes y ProgresoOrden: estado {payment, stock, email} de cada orden, con "pending".
 */
package co.edu.proyecto.dashboard.dominio;
