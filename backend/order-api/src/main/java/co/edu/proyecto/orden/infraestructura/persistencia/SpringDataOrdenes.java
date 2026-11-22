package co.edu.proyecto.orden.infraestructura.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Repositorio que Spring Data implementa solo. Únicamente lo usa OrdenRepositoryJpa. */
interface SpringDataOrdenes extends JpaRepository<OrdenEntity, UUID> {
}
