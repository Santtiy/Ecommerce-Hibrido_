package co.edu.proyecto.processor.infraestructura.kafka;

import co.edu.proyecto.common.contratos.OrdenCreada;
import co.edu.proyecto.processor.aplicacion.TraductorEventoAComando;
import co.edu.proyecto.processor.infraestructura.rabbitmq.PublicadorTareas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;

import static co.edu.proyecto.common.pruebas.DatosDePrueba.AHORA;
import static co.edu.proyecto.common.pruebas.DatosDePrueba.ordenCreada;
import static co.edu.proyecto.common.pruebas.DatosDePrueba.procesarOrden;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsumidorNuevasOrdenesTest {

    @Mock
    private PublicadorTareas publicador;

    @Test
    void cadaEventoSePublicaUnaSolaVezComoComando() {
        // Arrange
        TraductorEventoAComando traductor = new TraductorEventoAComando(Clock.fixed(AHORA, ZoneOffset.UTC));
        ConsumidorNuevasOrdenes consumidor = new ConsumidorNuevasOrdenes(traductor, publicador);
        OrdenCreada evento = ordenCreada("o-1", "17850");

        // Act
        consumidor.alRecibir(evento);

        // Assert
        verify(publicador, times(1)).publicar(procesarOrden("17850"));
    }
}
