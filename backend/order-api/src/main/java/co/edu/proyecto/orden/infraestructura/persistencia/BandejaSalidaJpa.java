package co.edu.proyecto.orden.infraestructura.persistencia;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.orden.dominio.puertos.BandejaSalida;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Adaptador del puerto BandejaSalida: guarda el evento como JSON en la tabla bandeja_salida,
 * dentro de la transacción que ya abrió el caso de uso.
 */
@Component
public class BandejaSalidaJpa implements BandejaSalida {

    private final SpringDataBandeja bandeja;
    private final ObjectMapper json;

    public BandejaSalidaJpa(SpringDataBandeja bandeja, ObjectMapper json) {
        this.bandeja = bandeja;
        this.json = json;
    }

    @Override
    public void encolar(OrdenCreada evento) {
        bandeja.save(new MensajePendienteEntity(Topicos.NUEVAS_ORDENES, evento.orderId(), aJson(evento), Instant.now()));
    }

    private String aJson(OrdenCreada evento) {
        try {
            return json.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo convertir el evento a JSON", e);
        }
    }
}
