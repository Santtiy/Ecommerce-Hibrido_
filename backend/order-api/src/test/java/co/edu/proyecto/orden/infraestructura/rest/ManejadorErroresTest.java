package co.edu.proyecto.orden.infraestructura.rest;

import co.edu.proyecto.orden.dominio.OrdenId;
import co.edu.proyecto.orden.dominio.excepciones.EstadoInvalidoException;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import co.edu.proyecto.orden.dominio.excepciones.OrdenNoEncontradaException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El manejador es una clase normal: se prueba llamando a sus métodos, sin levantar Spring. */
class ManejadorErroresTest {

    private final ManejadorErrores manejador = new ManejadorErrores();

    @Test
    void camposInvalidosRespondenCuatrocientosConCadaCampoYSuMensaje() {
        // Arrange
        BeanPropertyBindingResult errores = new BeanPropertyBindingResult(new Object(), "crearOrdenRequest");
        errores.addError(new FieldError("crearOrdenRequest", "items[0].cantidad", "debe ser mayor que o igual a 1"));
        MethodArgumentNotValidException excepcion = new MethodArgumentNotValidException((MethodParameter) null, errores);

        // Act
        ResponseEntity<RespuestaError> respuesta = manejador.camposInvalidos(excepcion);

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().detalles()).containsExactly("items[0].cantidad: debe ser mayor que o igual a 1");
    }

    @Test
    void unaReglaDeNegocioRotaRespondeCuatrocientosConSuMensaje() {
        // Act
        ResponseEntity<RespuestaError> respuesta =
                manejador.reglaDeNegocio(new OrdenInvalidaException("La orden debe tener al menos un ítem"));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().mensaje()).isEqualTo("La orden debe tener al menos un ítem");
    }

    @Test
    void unaOrdenQueNoExisteRespondeCuatrocientosCuatro() {
        // Act
        ResponseEntity<RespuestaError> respuesta =
                manejador.ordenNoEncontrada(new OrdenNoEncontradaException(new OrdenId(UUID.randomUUID())));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unaTransicionNoPermitidaRespondeCuatrocientosNueve() {
        // Act
        ResponseEntity<RespuestaError> respuesta =
                manejador.estadoInvalido(new EstadoInvalidoException("Una orden pagada no puede cancelarse"));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().mensaje()).isEqualTo("Una orden pagada no puede cancelarse");
    }

    @Test
    void unErrorInesperadoRespondeQuinientosSinMostrarElDetalleInterno() {
        // Act
        ResponseEntity<RespuestaError> respuesta =
                manejador.inesperado(new IllegalStateException("conexión a jdbc:postgresql://db rechazada"));

        // Assert
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().mensaje()).isEqualTo("Error interno del servidor");
        assertThat(respuesta.getBody().detalles()).isEmpty();
    }
}
