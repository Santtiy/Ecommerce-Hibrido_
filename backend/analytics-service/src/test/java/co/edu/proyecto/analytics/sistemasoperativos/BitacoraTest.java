package co.edu.proyecto.analytics.sistemasoperativos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BitacoraTest {

    @TempDir
    Path carpeta;

    @Test
    void alCrearseVaciaUnArchivoExistente() throws IOException {
        // Arrange
        Path archivo = carpeta.resolve("ordenes.log");
        Files.writeString(archivo, "contenido viejo");

        // Act
        new Bitacora(archivo.toString());

        // Assert
        assertThat(Files.size(archivo)).isZero();
    }

    @Test
    void elTamanoDelArchivoEsLaSumaDeLosBytesEscritos() {
        // Arrange
        Bitacora bitacora = new Bitacora(carpeta.resolve("ordenes.log").toString());

        // Act
        OperacionES primera = bitacora.escribir("o-1;1000");
        OperacionES segunda = bitacora.escribir("o-2;2500");

        // Assert
        assertThat(bitacora.tamanoBytes()).isEqualTo(primera.bytes() + segunda.bytes());
    }
}
