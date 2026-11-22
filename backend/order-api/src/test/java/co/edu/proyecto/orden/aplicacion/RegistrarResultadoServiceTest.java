package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.TipoTarea;
import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.EstadoOrden;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenBuilder;
import co.edu.proyecto.orden.dominio.puertos.AuditoriaRepository;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import co.edu.proyecto.orden.dominio.puertos.RegistroAuditoria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrarResultadoServiceTest {

    @Mock
    private OrdenRepository ordenes;
    @Mock
    private AuditoriaRepository auditoria;
    @InjectMocks
    private RegistrarResultadoService servicio;

    private static Orden ordenCreada() {
        ItemOrden libro = new ItemOrden("SKU-1", "Libro", 1, Dinero.de("50000", "COP"));
        return new OrdenBuilder().cliente("Ana Pérez").agregarItem(libro).construir();
    }

    private static ResultadoTarea resultado(Orden orden, EstadoTarea status) {
        return new ResultadoTarea(orden.id().toString(), status, "detalle", Instant.now());
    }

    @Test
    void unPagoExitosoDejaLaOrdenPagadaYLaGuarda() {
        Orden orden = ordenCreada();
        when(ordenes.buscarPorId(orden.id())).thenReturn(Optional.of(orden));

        servicio.registrar(TipoTarea.PAGO, resultado(orden, EstadoTarea.PAYMENT_SUCCESS));

        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGADA);
        verify(ordenes).guardar(orden);
    }

    @Test
    void unPagoFallidoCancelaLaOrden() {
        Orden orden = ordenCreada();
        when(ordenes.buscarPorId(orden.id())).thenReturn(Optional.of(orden));

        servicio.registrar(TipoTarea.PAGO, resultado(orden, EstadoTarea.PAYMENT_FAILED));

        assertThat(orden.estado()).isEqualTo(EstadoOrden.CANCELADA);
    }

    @Test
    void unPagoRepetidoNoVuelveAGuardarLaOrden() {
        Orden orden = ordenCreada();
        orden.confirmarPago();
        when(ordenes.buscarPorId(orden.id())).thenReturn(Optional.of(orden));

        servicio.registrar(TipoTarea.PAGO, resultado(orden, EstadoTarea.PAYMENT_SUCCESS));

        verify(ordenes, never()).guardar(any());
    }

    @Test
    void unResultadoDeInventarioSoloSeAudita() {
        Orden orden = ordenCreada();

        servicio.registrar(TipoTarea.INVENTARIO, resultado(orden, EstadoTarea.STOCK_RESERVED));

        verify(auditoria, times(1)).registrarSiNoExiste(any(RegistroAuditoria.class));
        verify(ordenes, never()).buscarPorId(any());
    }

    @Test
    void unaAuditoriaDePagoGuardaElOrigenYElEstadoComoTexto() {
        // Arrange
        Orden orden = ordenCreada();
        when(ordenes.buscarPorId(orden.id())).thenReturn(Optional.of(orden));
        ArgumentCaptor<RegistroAuditoria> registro = ArgumentCaptor.forClass(RegistroAuditoria.class);

        // Act
        servicio.registrar(TipoTarea.PAGO, resultado(orden, EstadoTarea.PAYMENT_SUCCESS));

        // Assert
        verify(auditoria).registrarSiNoExiste(registro.capture());
        assertThat(registro.getValue().origen()).isEqualTo("PAGO");
        assertThat(registro.getValue().status()).isEqualTo("PAYMENT_SUCCESS");
    }

    @Test
    void unEstadoQueNoEsDePagoLlegandoComoPagoSeRechazaYNoCancelaLaOrden() {
        // Arrange
        Orden orden = ordenCreada();

        // Act y Assert
        assertThatThrownBy(() -> servicio.registrar(TipoTarea.PAGO, resultado(orden, EstadoTarea.STOCK_OUT_OF_STOCK)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STOCK_OUT_OF_STOCK")
                .hasMessageContaining("PAGO");
        assertThat(orden.estado()).isEqualTo(EstadoOrden.CREADA);
        verifyNoInteractions(ordenes, auditoria);
    }
}
