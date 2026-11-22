package co.edu.proyecto.orden.infraestructura.persistencia.orden;

import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenBuilder;
import co.edu.proyecto.orden.dominio.descuentos.DescuentoPorMonto;
import co.edu.proyecto.orden.dominio.impuestos.IvaColombia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdenRepositoryJpaTest {

    /** Solo se simula Spring Data; el mapper es el real. */
    @Mock
    private SpringDataOrdenes springData;

    private final OrdenMapper mapper = new OrdenMapper();
    private OrdenRepositoryJpa repositorio;

    @BeforeEach
    void crearRepositorio() {
        repositorio = new OrdenRepositoryJpa(springData, mapper);
    }

    private static Orden ordenConTotal() {
        Orden orden = new OrdenBuilder().cliente("Ana Pérez")
                .agregarItem(new ItemOrden("SKU-1", "Libro", 2, Dinero.de("60000", "COP")))
                .construir();
        orden.calcularTotal(new DescuentoPorMonto(), new IvaColombia());
        return orden;
    }

    @Test
    void unaOrdenNuevaSeInsertaConSusItems() {
        // Arrange
        Orden orden = ordenConTotal();
        when(springData.findById(orden.id().valor())).thenReturn(Optional.empty());
        ArgumentCaptor<OrdenEntity> fila = ArgumentCaptor.forClass(OrdenEntity.class);

        // Act
        repositorio.guardar(orden);

        // Assert
        verify(springData).save(fila.capture());
        assertThat(fila.getValue().getId()).isEqualTo(orden.id().valor());
        assertThat(fila.getValue().getEstado()).isEqualTo("CREADA");
        assertThat(fila.getValue().getItems()).hasSize(1);
    }

    @Test
    void unaOrdenYaGuardadaSoloActualizaEstadoYTotalSinInsertarOtraVez() {
        // Arrange
        Orden orden = ordenConTotal();
        OrdenEntity guardada = mapper.aEntidad(orden); // la fila tal como quedó al crear la orden
        when(springData.findById(orden.id().valor())).thenReturn(Optional.of(guardada));
        orden.confirmarPago();

        // Act
        repositorio.guardar(orden);

        // Assert: JPA escribe el cambio al confirmar la transacción, sin un save nuevo
        verify(springData, never()).save(any());
        assertThat(guardada.getEstado()).isEqualTo("PAGADA");
        assertThat(guardada.getTotalMonto()).isEqualByComparingTo(orden.total().monto());
        assertThat(guardada.getItems()).hasSize(1);
    }
}
