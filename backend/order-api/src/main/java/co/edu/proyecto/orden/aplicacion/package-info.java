/**
 * Capa de aplicación: casos de uso que coordinan el dominio con los puertos, y las interfaces de esos
 * puertos ({@code puertos/}). Aquí va lo que traduce o coordina sin decidir reglas de negocio; no conoce
 * ninguna tecnología concreta (la única anotación de Spring permitida es {@code @Transactional}).
 *
 * <p>RegistrarOrdenService y RegistrarResultadoService. Sus puertos propios (BandejaSalida, AuditoriaRepository)
 * están en {@code aplicacion/puertos/}: son necesidades del caso de uso, no reglas del negocio.
 */
package co.edu.proyecto.orden.aplicacion;
