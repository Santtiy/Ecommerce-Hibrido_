package co.edu.proyecto.orden.infraestructura.persistencia;

import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenId;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador del puerto OrdenRepository sobre PostgreSQL con Spring Data JPA. */
@Repository
public class OrdenRepositoryJpa implements OrdenRepository {

    private final SpringDataOrdenes springData;
    private final OrdenMapper mapper;

    public OrdenRepositoryJpa(SpringDataOrdenes springData, OrdenMapper mapper) {
        this.springData = springData;
        this.mapper = mapper;
    }

    /** Si la orden ya existe solo actualiza estado y total; si no, inserta la orden con sus ítems. */
    @Override
    public void guardar(Orden orden) {
        Optional<OrdenEntity> existente = springData.findById(orden.id().valor());
        if (existente.isPresent()) {
            mapper.actualizar(existente.get(), orden);
        } else {
            springData.save(mapper.aEntidad(orden));
        }
    }

    @Override
    public Optional<Orden> buscarPorId(OrdenId id) {
        return springData.findById(id.valor()).map(mapper::aDominio);
    }
}
