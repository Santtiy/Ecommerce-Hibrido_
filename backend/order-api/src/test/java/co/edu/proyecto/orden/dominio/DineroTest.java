package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DineroTest {

    @Test
    void rechazaUnMontoNegativo() {
        // Act + Assert
        assertThatThrownBy(() -> Dinero.de("-1", "COP")).isInstanceOf(OrdenInvalidaException.class);
    }

    @Test
    void normalizaElMontoADosDecimales() {
        // Act
        Dinero dinero = Dinero.de("1500", "COP");

        // Assert
        assertThat(dinero.monto()).isEqualTo(new BigDecimal("1500.00"));
    }

    @Test
    void sumarMonedasDistintasLanzaExcepcion() {
        // Arrange
        Dinero pesos = Dinero.de("1000", "COP");
        Dinero dolares = Dinero.de("1", "USD");

        // Act + Assert
        assertThatThrownBy(() -> pesos.sumar(dolares)).isInstanceOf(OrdenInvalidaException.class);
    }

    @Test
    void elDiezPorCientoDeCienMilEsDiezMil() {
        // Arrange
        Dinero cienMil = Dinero.de("100000", "COP");

        // Act
        Dinero resultado = cienMil.porcentaje(10);

        // Assert
        assertThat(resultado).isEqualTo(Dinero.de("10000", "COP"));
    }
}
