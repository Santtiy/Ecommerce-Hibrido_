package co.edu.proyecto.common.contratos;

import co.edu.proyecto.common.nombres.TipoTarea;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstadoTareaTest {

    @Test
    void losNombresSonElTextoExactoQueViajaEnElJson() {
        // Act
        String[] nombres = Arrays.stream(EstadoTarea.values()).map(Enum::name).toArray(String[]::new);

        // Assert
        assertThat(nombres).containsExactly(
                "PAYMENT_SUCCESS", "PAYMENT_FAILED", "STOCK_RESERVED", "STOCK_OUT_OF_STOCK", "EMAIL_SENT");
    }

    @Test
    void cadaEstadoSabeSuTipoYSiEsUnExito() {
        // Assert
        assertThat(EstadoTarea.PAYMENT_SUCCESS.tipo()).isEqualTo(TipoTarea.PAGO);
        assertThat(EstadoTarea.PAYMENT_SUCCESS.esExito()).isTrue();
        assertThat(EstadoTarea.PAYMENT_FAILED.tipo()).isEqualTo(TipoTarea.PAGO);
        assertThat(EstadoTarea.PAYMENT_FAILED.esExito()).isFalse();
        assertThat(EstadoTarea.STOCK_RESERVED.tipo()).isEqualTo(TipoTarea.INVENTARIO);
        assertThat(EstadoTarea.STOCK_RESERVED.esExito()).isTrue();
        assertThat(EstadoTarea.STOCK_OUT_OF_STOCK.tipo()).isEqualTo(TipoTarea.INVENTARIO);
        assertThat(EstadoTarea.STOCK_OUT_OF_STOCK.esExito()).isFalse();
        assertThat(EstadoTarea.EMAIL_SENT.tipo()).isEqualTo(TipoTarea.NOTIFICACION);
        assertThat(EstadoTarea.EMAIL_SENT.esExito()).isTrue();
    }

    @Test
    void exigirTipoDevuelveElEstadoSiCoincide() {
        // Act
        EstadoTarea estado = EstadoTarea.PAYMENT_FAILED.exigirTipo(TipoTarea.PAGO);

        // Assert
        assertThat(estado).isEqualTo(EstadoTarea.PAYMENT_FAILED);
    }

    @Test
    void exigirTipoRechazaUnEstadoDeOtraTarea() {
        // Act y Assert
        assertThatThrownBy(() -> EstadoTarea.EMAIL_SENT.exigirTipo(TipoTarea.PAGO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EMAIL_SENT")
                .hasMessageContaining("NOTIFICACION");
    }
}
