package co.edu.proyecto.orden.dominio.descuentos;

import co.edu.proyecto.orden.dominio.Dinero;

/** RN-14: 10 % de descuento si el subtotal supera 100.000. */
public class DescuentoPorMonto implements EstrategiaDescuento {

    private static final String UMBRAL = "100000";
    private static final int PORCENTAJE = 10;

    @Override
    public Dinero aplicar(Dinero subtotal) {
        Dinero umbral = Dinero.de(UMBRAL, subtotal.moneda());
        if (!subtotal.esMayorQue(umbral)) {
            return subtotal;
        }
        return subtotal.restar(subtotal.porcentaje(PORCENTAJE));
    }
}
