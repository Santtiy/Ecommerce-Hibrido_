package co.edu.proyecto.inventory.arquitectura;

import co.edu.proyecto.common.pruebas.VerificadorDominioPuro;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** El dominio del worker de inventario es Java puro: no importa Spring, Jakarta, Kafka ni RabbitMQ. */
class DominioPuroTest {

    @Test
    void elDominioNoDependeDeNingunFramework() {
        // Arrange
        Path dominio = Path.of("src/main/java/co/edu/proyecto/inventory/dominio");

        // Act + Assert
        assertThat(VerificadorDominioPuro.infractores(dominio)).isEmpty();
    }
}
