package co.edu.proyecto.orden.aplicacion;

import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.EstadosTarea;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    private static ResultadoTarea resultado(Orden orden, String status) {
        return new ResultadoTarea(orden.id().toString(), status, "detalle", Instant.now());
    }

    @Test
    void unPagoExitosoDejaLaOrdenPagadaYLaGuarda() {
        Orden orden = ordenCreada();
        when(ordenes.buscarPorId(orden.id())).thenReturn(Optional.of(orden));

        servicio.registrar(OrigenResultado.PAGO, resultado(orden, EstadosTarea.PAGO_EXITOSO));

        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGADA);
        verify(ordenes).guardar(orden);
    }

    @Test
    void unPagoFallidoCancelaLaOrden() {
        Orden orden = ordenCreada();
        when(ordenes.buscarPorId(orden.id())).thenReturn(Optional.of(orden));

        servicio.registrar(OrigenResultado.PAGO, resultado(orden, EstadosTarea.PAGO_FALLIDO));

        assertThat(orden.estado()).isEqualTo(EstadoOrden.CANCELADA);
    }

    @Test
    void unPagoRepetidoNoVuelveAGuardarLaOrden() {
        Orden orden = ordenCreada();
        orden.confirmarPago();
        when(ordenes.buscarPorId(orden.id())).thenReturn(Optional.of(orden));

        servicio.registrar(OrigenResultado.PAGO, resultado(orden, EstadosTarea.PAGO_EXITOSO));

        verify(ordenes, never()).guardar(any());
    }

    @Test
    void unResultadoDeInventarioSoloSeAudita() {
        Orden orden = ordenCreada();

        servicio.registrar(OrigenResultado.INVENTARIO, resultado(orden, EstadosTarea.STOCK_RESERVADO));

        verify(auditoria, times(1)).registrarSiNoExiste(any(RegistroAuditoria.class));
        verify(ordenes, never()).buscarPorId(any());
    }
}
