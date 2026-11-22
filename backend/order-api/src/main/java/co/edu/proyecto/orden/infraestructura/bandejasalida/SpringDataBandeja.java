package co.edu.proyecto.orden.infraestructura.bandejasalida;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Acceso a bandeja_salida. Solo lo usan BandejaSalidaJpa (escribe) y PublicadorBandejaSalida (lee en orden de llegada). */
interface SpringDataBandeja extends JpaRepository<MensajePendienteEntity, Long> {

    List<MensajePendienteEntity> findTop50ByOrderByIdAsc();
}
