package co.edu.proyecto.orden.infraestructura.rest;

import java.util.List;

/** Cuerpo de las respuestas de error: un mensaje y, si aplica, los campos inválidos. */
public record RespuestaError(int codigo, String mensaje, List<String> detalles) {
}
