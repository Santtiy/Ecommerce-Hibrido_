package co.edu.proyecto.orden.infraestructura.bandejasalida;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Fila de bandeja_salida: un evento ya serializado a JSON que todavía no se ha publicado en Kafka. */
@Entity
@Table(name = "bandeja_salida")
public class MensajePendienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String topico;
    private String clave;
    private String contenido;
    private Instant creadoEn;

    protected MensajePendienteEntity() {
    }

    MensajePendienteEntity(String topico, String clave, String contenido, Instant creadoEn) {
        this.topico = topico;
        this.clave = clave;
        this.contenido = contenido;
        this.creadoEn = creadoEn;
    }

    Long getId() {
        return id;
    }

    String getTopico() {
        return topico;
    }

    String getClave() {
        return clave;
    }

    String getContenido() {
        return contenido;
    }
}
