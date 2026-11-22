/**
 * Capa de aplicación: casos de uso que coordinan el dominio con los puertos, y las interfaces de esos
 * puertos ({@code puertos/}). Aquí va lo que traduce o coordina sin decidir reglas de negocio; no conoce
 * ninguna tecnología concreta (la única anotación de Spring permitida es {@code @Transactional}).
 *
 * <p>TraductorEventoAComando: convierte el evento OrdenCreada en el comando ProcesarOrden (traduce, no decide).
 */
package co.edu.proyecto.processor.aplicacion;
