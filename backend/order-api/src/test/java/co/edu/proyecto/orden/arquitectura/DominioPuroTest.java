package co.edu.proyecto.orden.arquitectura;

import co.edu.proyecto.common.pruebas.VerificadorDominioPuro;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba de forma automática que el paquete dominio es Java puro:
 * ningún archivo importa Spring, Jakarta (JPA, validación), Kafka ni RabbitMQ.
 */
class DominioPuroTest {

    @Test
    void elDominioNoDependeDeNingunFramework() {
        // Arrange
        Path dominio = Path.of("src/main/java/co/edu/proyecto/orden/dominio");

        // Act + Assert
        assertThat(VerificadorDominioPuro.infractores(dominio)).isEmpty();
    }
}
