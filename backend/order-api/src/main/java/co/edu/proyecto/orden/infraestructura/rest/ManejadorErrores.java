package co.edu.proyecto.orden.infraestructura.rest;

import co.edu.proyecto.orden.dominio.excepciones.DominioException;
import co.edu.proyecto.orden.dominio.excepciones.EstadoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Traduce cada excepción a su código HTTP: 400 datos inválidos, 409 estado inválido, 500 lo inesperado. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> camposInvalidos(MethodArgumentNotValidException ex) {
        List<String> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "La solicitud tiene campos inválidos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> jsonInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo no es un JSON válido", List.of());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<RespuestaError> estadoInvalido(EstadoInvalidoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), List.of());
    }

    @ExceptionHandler(DominioException.class)
    public ResponseEntity<RespuestaError> reglaDeNegocio(DominioException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private ResponseEntity<RespuestaError> responder(HttpStatus estado, String mensaje, List<String> detalles) {
        return ResponseEntity.status(estado).body(new RespuestaError(estado.value(), mensaje, detalles));
    }
}
