package co.edu.proyecto.orden.infraestructura;

import co.edu.proyecto.orden.dominio.Dinero;
import co.edu.proyecto.orden.dominio.ItemOrden;
import co.edu.proyecto.orden.dominio.Orden;
import co.edu.proyecto.orden.dominio.OrdenBuilder;
import co.edu.proyecto.orden.dominio.descuentos.DescuentoPorMonto;
import co.edu.proyecto.orden.dominio.impuestos.IvaColombia;
import co.edu.proyecto.orden.infraestructura.persistencia.OrdenMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrdenMapperTest {

    private final OrdenMapper mapper = new OrdenMapper();

    @Test
    void idaYVueltaConservaTodosLosDatos() {
        Orden original = new OrdenBuilder().cliente("Ana Pérez")
                .agregarItem(new ItemOrden("SKU-1", "Libro", 2, Dinero.de("60000", "COP")))
                .construir();
        original.calcularTotal(new DescuentoPorMonto(), new IvaColombia());
        original.confirmarPago();

        Orden copia = mapper.aDominio(mapper.aEntidad(original));

        assertThat(copia.id()).isEqualTo(original.id());
        assertThat(copia.cliente()).isEqualTo(original.cliente());
        assertThat(copia.estado()).isEqualTo(original.estado());
        assertThat(copia.total()).isEqualTo(original.total());
        assertThat(copia.items()).isEqualTo(original.items());
        assertThat(copia.creadaEn()).isEqualTo(original.creadaEn());
    }
}
