package co.edu.proyecto.common.nombres;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TipoTareaTest {

    @Test
    void losNombresSonLosQueGuardaLaAuditoria() {
        // Act + Assert: auditoria_ordenes.origen ya tiene filas con estos textos
        assertThat(TipoTarea.values()).extracting(Enum::name)
                .containsExactly("PAGO", "INVENTARIO", "NOTIFICACION");
    }

    @Test
    void cadaTipoConoceSuColaYSuTopic() {
        // Act + Assert
        assertThat(TipoTarea.PAGO.cola()).isEqualTo("billing_queue");
        assertThat(TipoTarea.PAGO.topico()).isEqualTo("billing_events");
        assertThat(TipoTarea.INVENTARIO.cola()).isEqualTo("inventory_queue");
        assertThat(TipoTarea.INVENTARIO.topico()).isEqualTo("inventory_events");
        assertThat(TipoTarea.NOTIFICACION.cola()).isEqualTo("notification_queue");
        assertThat(TipoTarea.NOTIFICACION.topico()).isEqualTo("notification_events");
    }
}
