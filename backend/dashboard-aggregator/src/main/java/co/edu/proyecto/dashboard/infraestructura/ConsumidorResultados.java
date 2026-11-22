package co.edu.proyecto.dashboard.infraestructura;

import co.edu.proyecto.common.contratos.ResultadoTarea;
import co.edu.proyecto.common.nombres.GruposConsumidores;
import co.edu.proyecto.common.nombres.TipoTarea;
import co.edu.proyecto.common.nombres.Topicos;
import co.edu.proyecto.dashboard.dominio.ProgresoOrden;
import co.edu.proyecto.dashboard.dominio.TableroOrdenes;
import co.edu.proyecto.mensajeria.kafka.ConsumidorQueReleeTodo;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Escucha los tres topics de resultados (grupo dashboard-group), actualiza el tablero y avisa al navegador.
 * Al arrancar relee los topics desde el inicio para reconstruir el tablero tras un reinicio
 * (lo hace {@link ConsumidorQueReleeTodo}).
 */
@Component
public class ConsumidorResultados extends ConsumidorQueReleeTodo {

    private final TableroOrdenes tablero;
    private final CanalWebSocket canal;

    public ConsumidorResultados(TableroOrdenes tablero, CanalWebSocket canal) {
        this.tablero = tablero;
        this.canal = canal;
    }

    @KafkaListener(topics = Topicos.EVENTOS_PAGO, groupId = GruposConsumidores.DASHBOARD)
    public void alRecibirPago(ResultadoTarea resultado) {
        aplicar(TipoTarea.PAGO, resultado);
    }

    @KafkaListener(topics = Topicos.EVENTOS_INVENTARIO, groupId = GruposConsumidores.DASHBOARD)
    public void alRecibirInventario(ResultadoTarea resultado) {
        aplicar(TipoTarea.INVENTARIO, resultado);
    }

    @KafkaListener(topics = Topicos.EVENTOS_NOTIFICACION, groupId = GruposConsumidores.DASHBOARD)
    public void alRecibirNotificacion(ResultadoTarea resultado) {
        aplicar(TipoTarea.NOTIFICACION, resultado);
    }

    private void aplicar(TipoTarea tipo, ResultadoTarea resultado) {
        ProgresoOrden progreso = tablero.aplicar(resultado.orderId(), tipo, resultado.status());
        canal.difundir(resultado.orderId(), progreso);
    }
}
