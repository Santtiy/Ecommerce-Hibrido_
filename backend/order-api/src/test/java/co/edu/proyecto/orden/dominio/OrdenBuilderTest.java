package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdenBuilderTest {

    private static final ItemOrden LIBRO = new ItemOrden("SKU-1", "Libro", 1, Dinero.de("50000", "COP"));

    @Test
    void sinItemsNoSePuedeConstruir() {
        OrdenBuilder builder = new OrdenBuilder().cliente("Ana Pérez");

        assertThatThrownBy(builder::construir).isInstanceOf(OrdenInvalidaException.class);
    }

    @Test
    void sinClienteNoSePuedeConstruir() {
        OrdenBuilder builder = new OrdenBuilder().agregarItem(LIBRO);

        assertThatThrownBy(builder::construir).isInstanceOf(OrdenInvalidaException.class);
    }

    @Test
    void unaOrdenNuevaNaceCreada() {
        Orden orden = new OrdenBuilder().cliente("Ana Pérez").agregarItem(LIBRO).construir();

        assertThat(orden.estado()).isEqualTo(EstadoOrden.CREADA);
    }
}
