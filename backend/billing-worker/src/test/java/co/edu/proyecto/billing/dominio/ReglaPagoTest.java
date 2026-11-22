package co.edu.proyecto.billing.dominio;

import co.edu.proyecto.common.contratos.EstadoTarea;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.common.contratos.ResultadoTarea;
import org.junit.jupiter.api.Test;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.AHORA;
import static co.edu.proyecto.common.pruebas.DatosDePrueba.procesarOrden;
import static org.assertj.core.api.Assertions.assertThat;

class ReglaPagoTest {

    private final ReglaPago regla = new ReglaPago();

    @Test
    void unMillonExactoSeAprueba() {
        // Arrange
        ProcesarOrden comando = procesarOrden("1000000");

        // Act
        ResultadoTarea resultado = regla.evaluar(comando, AHORA);

        // Assert
        assertThat(resultado.status()).isEqualTo(EstadoTarea.PAYMENT_SUCCESS);
    }

    @Test
    void masDeUnMillonSeRechazaYElDetalleLoExplica() {
        // Arrange
        ProcesarOrden comando = procesarOrden("1000001");

        // Act
        ResultadoTarea resultado = regla.evaluar(comando, AHORA);

        // Assert
        assertThat(resultado.status()).isEqualTo(EstadoTarea.PAYMENT_FAILED);
        assertThat(resultado.detalle()).contains("1000000");
    }
}
