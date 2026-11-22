/**
 * Capa de dominio: reglas de negocio puras (entidades, value objects, reglas, estrategias).
 * Es Java puro: no importa Spring, Jakarta, JPA, Kafka ni RabbitMQ, así se prueba sin levantar nada.
 *
 * <p>Orden, Dinero, OrdenId, ItemOrden, OrdenBuilder, estrategias de descuento e impuesto, excepciones y el
 * puerto Repository {@code puertos/OrdenRepository}, que el enunciado pide como parte del dominio.
 */
package co.edu.proyecto.orden.dominio;
