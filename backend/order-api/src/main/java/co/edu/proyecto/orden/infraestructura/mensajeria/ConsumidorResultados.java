package co.edu.proyecto.orden.infraestructura.mensajeria;

import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.GruposConsumidores;
import co.edu.proyecto.common.nombres.TipoTarea;
import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.orden.aplicacion.RegistrarResultadoService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Escucha los resultados de los tres workers y los entrega, con el {@link TipoTarea} de su topic, al caso de uso
 * que los audita y aplica. Un JSON con un {@code status} desconocido no llega aquí: falla al deserializarse y el
 * manejador de errores de Kafka lo registra y lo salta.
 */
@Component
public class ConsumidorResultados {

    private final RegistrarResultadoService registrarResultado;

    public ConsumidorResultados(RegistrarResultadoService registrarResultado) {
        this.registrarResultado = registrarResultado;
    }

    @KafkaListener(topics = Topicos.EVENTOS_PAGO, groupId = GruposConsumidores.ORDER_API)
    public void alRecibirPago(ResultadoTarea resultado) {
        registrarResultado.registrar(TipoTarea.PAGO, resultado);
    }

    @KafkaListener(topics = Topicos.EVENTOS_INVENTARIO, groupId = GruposConsumidores.ORDER_API)
    public void alRecibirInventario(ResultadoTarea resultado) {
        registrarResultado.registrar(TipoTarea.INVENTARIO, resultado);
    }

    @KafkaListener(topics = Topicos.EVENTOS_NOTIFICACION, groupId = GruposConsumidores.ORDER_API)
    public void alRecibirNotificacion(ResultadoTarea resultado) {
        registrarResultado.registrar(TipoTarea.NOTIFICACION, resultado);
    }
}
