package co.edu.proyecto.orden.infraestructura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a bandeja_salida. El publicador la lee en orden de llegada. */
public interface SpringDataBandeja extends JpaRepository<MensajePendienteEntity, Long> {

    List<MensajePendienteEntity> findTop50ByOrderByIdAsc();
}
