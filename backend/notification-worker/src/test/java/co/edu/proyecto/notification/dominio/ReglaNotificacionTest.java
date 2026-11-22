package co.edu.proyecto.notification.dominio;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.EstadosTarea;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglaNotificacionTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    @Test
    void siempreEnviaElCorreo() {
        ItemEvento item = new ItemEvento("SKU-1", "Libro", 1, new BigDecimal("15000"));
        ProcesarOrden comando = new ProcesarOrden("o-1", List.of(item), new BigDecimal("15000"), AHORA);

        ResultadoTarea resultado = new ReglaNotificacion().evaluar(comando, AHORA);

        assertThat(resultado.status()).isEqualTo(EstadosTarea.CORREO_ENVIADO);
    }
}
