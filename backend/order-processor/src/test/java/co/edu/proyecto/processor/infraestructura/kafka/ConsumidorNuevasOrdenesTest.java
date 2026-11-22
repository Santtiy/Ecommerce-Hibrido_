package co.edu.proyecto.processor.infraestructura.kafka;

import co.edu.proyecto.common.contratos.ItemEvento;
import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.common.contratos.ProcesarOrden;
import co.edu.proyecto.processor.aplicacion.TraductorEventoAComando;
import co.edu.proyecto.processor.infraestructura.rabbitmq.PublicadorTareas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsumidorNuevasOrdenesTest {

    private static final Instant AHORA = Instant.parse("2026-11-19T15:00:00Z");

    @Mock
    private PublicadorTareas publicador;

    @Test
    void cadaEventoSePublicaUnaSolaVezComoComando() {
        TraductorEventoAComando traductor = new TraductorEventoAComando(Clock.fixed(AHORA, ZoneOffset.UTC));
        ConsumidorNuevasOrdenes consumidor = new ConsumidorNuevasOrdenes(traductor, publicador);
        List<ItemEvento> items = List.of(new ItemEvento("SKU-1", "Libro", 1, new BigDecimal("15000")));
        OrdenCreada evento = new OrdenCreada("o-1", "Ana", items, new BigDecimal("17850"), "COP", AHORA);

        consumidor.alRecibir(evento);

        verify(publicador, times(1)).publicar(new ProcesarOrden("o-1", items, new BigDecimal("17850"), AHORA));
    }
}
