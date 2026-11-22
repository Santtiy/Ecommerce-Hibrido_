package co.edu.proyecto.orden.arquitectura;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba de forma automática que el paquete dominio es Java puro:
 * ningún archivo importa Spring, Jakarta (JPA, validación) ni Kafka.
 */
class DominioPuroTest {

    private static final Path DOMINIO = Path.of("src/main/java/co/edu/proyecto/orden/dominio");
    private static final List<String> IMPORTS_PROHIBIDOS =
            List.of("import org.springframework", "import jakarta", "import org.apache.kafka");

    @Test
    void elDominioNoDependeDeNingunFramework() throws IOException {
        try (Stream<Path> archivos = Files.walk(DOMINIO)) {
            List<String> infractores = archivos
                    .filter(archivo -> archivo.toString().endsWith(".java"))
                    .filter(DominioPuroTest::tieneImportProhibido)
                    .map(Path::toString)
                    .toList();

            assertThat(infractores).isEmpty();
        }
    }

    private static boolean tieneImportProhibido(Path archivo) {
        try {
            String contenido = Files.readString(archivo);
            return IMPORTS_PROHIBIDOS.stream().anyMatch(contenido::contains);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + archivo, e);
        }
    }
}
