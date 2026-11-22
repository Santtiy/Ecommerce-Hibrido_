package co.edu.proyecto.common.pruebas;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Recorre una carpeta de código fuente y devuelve los archivos .java que importan algo prohibido.
 * Lo usa el DominioPuroTest de cada módulo para comprobar que su paquete dominio es Java puro
 * (sustituye a ArchUnit, ver docs/decisiones.md D-04).
 */
public final class VerificadorDominioPuro {

    /** Lo que el dominio de ningún servicio puede importar. */
    public static final List<String> FRAMEWORKS = List.of(
            "org.springframework", "jakarta", "org.apache.kafka", "com.rabbitmq");

    private VerificadorDominioPuro() {
    }

    /**
     * @param carpeta          carpeta del paquete dominio, relativa al módulo (Maven ejecuta las pruebas desde allí)
     * @param importsProhibidos prefijos de paquete que no pueden aparecer en un {@code import}
     * @return las rutas de los archivos que incumplen; vacía si el dominio es puro
     * @throws IllegalArgumentException si la carpeta no existe (evita que la prueba pase sin revisar nada)
     */
    public static List<String> infractores(Path carpeta, List<String> importsProhibidos) {
        if (!Files.isDirectory(carpeta)) {
            throw new IllegalArgumentException("No existe la carpeta del dominio: " + carpeta.toAbsolutePath());
        }
        try (Stream<Path> archivos = Files.walk(carpeta)) {
            return archivos
                    .filter(archivo -> archivo.toString().endsWith(".java"))
                    .filter(archivo -> importaAlgunoDe(archivo, importsProhibidos))
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo recorrer " + carpeta, e);
        }
    }

    public static List<String> infractores(Path carpeta) {
        return infractores(carpeta, FRAMEWORKS);
    }

    private static boolean importaAlgunoDe(Path archivo, List<String> importsProhibidos) {
        try {
            String contenido = Files.readString(archivo);
            return importsProhibidos.stream().anyMatch(prefijo -> contenido.contains("import " + prefijo)
                    || contenido.contains("import static " + prefijo));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo, e);
        }
    }
}
