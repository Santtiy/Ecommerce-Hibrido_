package co.edu.proyecto.analytics.so;

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
        Path archivo = carpeta.resolve("ordenes.log");
        Files.writeString(archivo, "contenido viejo");

        new Bitacora(archivo.toString());

        assertThat(Files.size(archivo)).isZero();
    }

    @Test
    void elTamanoDelArchivoEsLaSumaDeLosBytesEscritos() {
        Bitacora bitacora = new Bitacora(carpeta.resolve("ordenes.log").toString());

        OperacionES primera = bitacora.escribir("o-1;1000");
        OperacionES segunda = bitacora.escribir("o-2;2500");

        assertThat(bitacora.tamanoBytes()).isEqualTo(primera.bytes() + segunda.bytes());
    }
}
