package co.edu.proyecto.analytics.dominio;

import org.junit.jupiter.api.Test;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.ordenCreada;
import static org.assertj.core.api.Assertions.assertThat;

class AgregadorVentasTest {

    @Test
    void dosOrdenesDistintasSeSuman() {
        // Arrange
        AgregadorVentas agregador = new AgregadorVentas();

        // Act
        agregador.registrar(ordenCreada("o-1", "1000"));
        agregador.registrar(ordenCreada("o-2", "2500"));

        // Assert
        assertThat(agregador.totalAcumulado()).isEqualByComparingTo("3500");
        assertThat(agregador.cantidadOrdenes()).isEqualTo(2);
    }

    @Test
    void laMismaOrdenDosVecesSeCuentaUnaSola() {
        // Arrange
        AgregadorVentas agregador = new AgregadorVentas();

        // Act
        agregador.registrar(ordenCreada("o-1", "1000"));
        agregador.registrar(ordenCreada("o-1", "1000"));

        // Assert
        assertThat(agregador.totalAcumulado()).isEqualByComparingTo("1000");
    }

    @Test
    void releerElTopicDesdeElInicioNoDuplicaNiElTotalNiLaCantidad() {
        // Arrange: el servicio ya contó dos órdenes y, al reiniciar, Kafka se las vuelve a entregar (D-07)
        AgregadorVentas agregador = new AgregadorVentas();
        agregador.registrar(ordenCreada("o-1", "1000"));
        agregador.registrar(ordenCreada("o-2", "2500"));

        // Act
        agregador.registrar(ordenCreada("o-1", "1000"));
        agregador.registrar(ordenCreada("o-2", "2500"));
        agregador.registrar(ordenCreada("o-3", "500"));

        // Assert
        assertThat(agregador.totalAcumulado()).isEqualByComparingTo("4000");
        assertThat(agregador.cantidadOrdenes()).isEqualTo(3);
    }
}
