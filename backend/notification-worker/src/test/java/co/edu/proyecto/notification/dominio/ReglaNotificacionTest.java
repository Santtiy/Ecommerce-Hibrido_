package co.edu.proyecto.notification.dominio;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import org.junit.jupiter.api.Test;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.AHORA;
import static co.edu.proyecto.common.pruebas.DatosDePrueba.procesarOrden;
import static org.assertj.core.api.Assertions.assertThat;

class ReglaNotificacionTest {

    @Test
    void siempreEnviaElCorreo() {
        // Arrange
        ProcesarOrden comando = procesarOrden("15000");

        // Act
        ResultadoTarea resultado = new ReglaNotificacion().evaluar(comando, AHORA);

        // Assert
        assertThat(resultado.status()).isEqualTo(EstadoTarea.EMAIL_SENT);
    }
}
