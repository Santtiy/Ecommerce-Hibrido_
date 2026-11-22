package co.edu.proyecto.common.pruebas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificadorDominioPuroTest {

    @TempDir
    Path dominio;

    @Test
    void unDominioQueSoloUsaJavaNoTieneInfractores() throws IOException {
        // Arrange
        Files.writeString(dominio.resolve("Regla.java"), "import java.util.List;\nclass Regla { }");

        // Act
        List<String> infractores = VerificadorDominioPuro.infractores(dominio);

        // Assert
        assertThat(infractores).isEmpty();
    }

    @Test
    void detectaUnImportProhibidoAunqueEsteEnUnSubpaquete() throws IOException {
        // Arrange
        Path subpaquete = Files.createDirectories(dominio.resolve("puertos"));
        Files.writeString(subpaquete.resolve("Repositorio.java"),
                "import org.springframework.stereotype.Component;\nclass Repositorio { }");
        Files.writeString(dominio.resolve("Valor.java"), "import static jakarta.persistence.EnumType.STRING;");

        // Act
        List<String> infractores = VerificadorDominioPuro.infractores(dominio);

        // Assert
        assertThat(infractores).hasSize(2)
                .anyMatch(ruta -> ruta.endsWith("Repositorio.java"))
                .anyMatch(ruta -> ruta.endsWith("Valor.java"));
    }

    @Test
    void unaCarpetaQueNoExisteFallaEnVezDePasarSinRevisarNada() {
        // Arrange
        Path inexistente = dominio.resolve("no-existe");

        // Act + Assert
        assertThatThrownBy(() -> VerificadorDominioPuro.infractores(inexistente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no-existe");
    }
}
