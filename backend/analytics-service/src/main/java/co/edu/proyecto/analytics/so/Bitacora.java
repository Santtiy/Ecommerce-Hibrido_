package co.edu.proyecto.analytics.so;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Gestor de archivos y E/S: escribe una línea por orden y mide cada escritura en microsegundos.
 * Al arrancar vacía el archivo, porque el consumidor relee todo el topic y lo vuelve a escribir completo.
 */
@Component
public class Bitacora {

    private final Path archivo;

    public Bitacora(@Value("${analytics.archivo-bitacora}") String ruta) {
        this.archivo = Path.of(ruta);
        try {
            Files.createDirectories(archivo.toAbsolutePath().getParent());
            Files.writeString(archivo, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo preparar la bitácora " + archivo, e);
        }
    }

    /** synchronized: tres hilos consumidores escriben a la vez y sin esto las líneas se mezclarían. */
    public synchronized OperacionES escribir(String linea) {
        byte[] bytes = (linea + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        long inicio = System.nanoTime();
        try {
            Files.write(archivo, bytes, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir en " + archivo, e);
        }
        long microsegundos = (System.nanoTime() - inicio) / 1_000;
        return new OperacionES(bytes.length, microsegundos, Thread.currentThread().getName());
    }

    public long tamanoBytes() {
        try {
            return Files.size(archivo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
