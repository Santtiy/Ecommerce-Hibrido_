package co.edu.proyecto.orden.configuracion;

import co.edu.proyecto.orden.aplicacion.RegistrarOrdenService;
import co.edu.proyecto.orden.aplicacion.RegistrarResultadoService;
import co.edu.proyecto.orden.aplicacion.puertos.AuditoriaRepository;
import co.edu.proyecto.orden.aplicacion.puertos.BandejaSalida;
import co.edu.proyecto.orden.dominio.descuentos.DescuentoPorMonto;
import co.edu.proyecto.orden.dominio.descuentos.EstrategiaDescuento;
import co.edu.proyecto.orden.dominio.impuestos.EstrategiaImpuesto;
import co.edu.proyecto.orden.dominio.impuestos.IvaColombia;
import co.edu.proyecto.orden.dominio.puertos.OrdenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Arma los casos de uso con inyección por constructor. Aquí se elige qué estrategia de descuento
 * e impuesto se usa: cambiarla no obliga a tocar el dominio ni los servicios.
 */
@Configuration
public class ConfiguracionDominio {

    @Bean
    public EstrategiaDescuento estrategiaDescuento() {
        return new DescuentoPorMonto();
    }

    @Bean
    public EstrategiaImpuesto estrategiaImpuesto() {
        return new IvaColombia();
    }

    @Bean
    public RegistrarOrdenService registrarOrdenService(OrdenRepository ordenes, BandejaSalida bandejaSalida,
                                                       EstrategiaDescuento descuento, EstrategiaImpuesto impuesto) {
        return new RegistrarOrdenService(ordenes, bandejaSalida, descuento, impuesto);
    }

    @Bean
    public RegistrarResultadoService registrarResultadoService(OrdenRepository ordenes, AuditoriaRepository auditoria) {
        return new RegistrarResultadoService(ordenes, auditoria);
    }
}
