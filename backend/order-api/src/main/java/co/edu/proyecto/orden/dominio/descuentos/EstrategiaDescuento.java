package co.edu.proyecto.orden.dominio.descuentos;

import co.edu.proyecto.orden.dominio.Dinero;

/**
 * Strategy de descuento. Contrato: nunca devuelve null ni un valor negativo, y no cambia la moneda.
 * Para una regla nueva (por ejemplo Black Friday) basta con crear otra implementación.
 */
public interface EstrategiaDescuento {

    Dinero aplicar(Dinero subtotal);
}
