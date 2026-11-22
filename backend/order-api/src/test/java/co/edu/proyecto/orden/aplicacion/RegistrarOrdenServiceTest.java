package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.descuentos.DescuentoPorMonto;
import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import co.edu.proyecto.orden.dominio.impuestos.IvaColombia;
import co.edu.proyecto.orden.dominio.puertos.BandejaSalida;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RegistrarOrdenServiceTest {

    private static final ItemOrden LIBRO = new ItemOrden("SKU-1", "Libro", 1, Dinero.de("150000", "COP"));
    private static final ItemOrden USB = new ItemOrden("SKU-2", "Memoria USB", 50, Dinero.de("20000", "COP"));

    @Mock
    private OrdenRepository ordenes;
    @Mock
    private BandejaSalida bandejaSalida;

    private RegistrarOrdenService servicio;

    /** Las estrategias son reales (deterministas); solo se simulan los puertos. */
    @BeforeEach
    void crearServicio() {
        servicio = new RegistrarOrdenService(ordenes, bandejaSalida, new DescuentoPorMonto(), new IvaColombia());
    }

    @Test
    void guardaLaOrdenYDespuesEncolaSuEvento() {
        Orden orden = servicio.registrar("Ana Pérez", List.of(LIBRO));

        InOrder secuencia = inOrder(ordenes, bandejaSalida);
        secuencia.verify(ordenes).guardar(orden);
        secuencia.verify(bandejaSalida).encolar(any(OrdenCreada.class));
    }

    @Test
    void elEventoLlevaTodosLosItemsYLaFechaDeCreacion() {
        Orden orden = servicio.registrar("Ana Pérez", List.of(LIBRO, USB));

        ArgumentCaptor<OrdenCreada> evento = ArgumentCaptor.forClass(OrdenCreada.class);
        verify(bandejaSalida).encolar(evento.capture());
        assertThat(evento.getValue().items()).hasSize(2);
        assertThat(evento.getValue().creadaEn()).isEqualTo(orden.creadaEn());
    }

    @Test
    void siGuardarFallaNoSeEncolaNingunEvento() {
        doThrow(new IllegalStateException("BD caída")).when(ordenes).guardar(any());

        assertThatThrownBy(() -> servicio.registrar("Ana Pérez", List.of(LIBRO)))
                .isInstanceOf(IllegalStateException.class);
        verify(bandejaSalida, never()).encolar(any());
    }

    @Test
    void unaOrdenSinItemsNoTocaNiLaBaseDeDatosNiLaBandeja() {
        assertThatThrownBy(() -> servicio.registrar("Ana Pérez", List.of()))
                .isInstanceOf(OrdenInvalidaException.class);
        verifyNoInteractions(ordenes, bandejaSalida);
    }
}
