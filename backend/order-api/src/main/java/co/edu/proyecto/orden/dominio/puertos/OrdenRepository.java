package co.edu.proyecto.orden.dominio.puertos;

import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;

import java.util.Optional;

/** Puerto Repository: el dominio pide guardar y buscar órdenes sin saber que existe JPA. */
public interface OrdenRepository {

    void guardar(Orden orden);

    Optional<Orden> buscarPorId(OrdenId id);
}
