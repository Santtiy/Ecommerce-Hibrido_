package co.edu.proyecto.dashboard.infraestructura;

import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.GruposConsumidores;
import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.dashboard.dominio.Etapa;
import co.edu.proyecto.dashboard.dominio.ProgresoOrden;
import co.edu.proyecto.dashboard.dominio.TableroOrdenes;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Escucha los tres topics de resultados (grupo dashboard-group), actualiza el tablero y avisa al navegador.
 * Al arrancar relee los topics desde el inicio para reconstruir el tablero tras un reinicio.
 */
@Component
public class ConsumidorResultados extends AbstractConsumerSeekAware {

    private final TableroOrdenes tablero;
    private final CanalWebSocket canal;

    public ConsumidorResultados(TableroOrdenes tablero, CanalWebSocket canal) {
        this.tablero = tablero;
        this.canal = canal;
    }

    @Override
    public void onPartitionsAssigned(Map<TopicPartition, Long> asignaciones, ConsumerSeekCallback callback) {
        super.onPartitionsAssigned(asignaciones, callback);
        callback.seekToBeginning(asignaciones.keySet());
    }

    @KafkaListener(topics = Topicos.EVENTOS_PAGO, groupId = GruposConsumidores.DASHBOARD)
    public void alRecibirPago(ResultadoTarea resultado) {
        aplicar(Etapa.PAGO, resultado);
    }

    @KafkaListener(topics = Topicos.EVENTOS_INVENTARIO, groupId = GruposConsumidores.DASHBOARD)
    public void alRecibirInventario(ResultadoTarea resultado) {
        aplicar(Etapa.STOCK, resultado);
    }

    @KafkaListener(topics = Topicos.EVENTOS_NOTIFICACION, groupId = GruposConsumidores.DASHBOARD)
    public void alRecibirNotificacion(ResultadoTarea resultado) {
        aplicar(Etapa.CORREO, resultado);
    }

    private void aplicar(Etapa etapa, ResultadoTarea resultado) {
        ProgresoOrden progreso = tablero.aplicar(resultado.orderId(), etapa, resultado.status());
        canal.difundir(resultado.orderId(), progreso);
    }
}
