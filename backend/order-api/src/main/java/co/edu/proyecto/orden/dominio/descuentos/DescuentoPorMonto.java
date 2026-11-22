package co.edu.proyecto.orden.dominio.descuentos;

import co.edu.proyecto.orden.dominio.Dinero;

/** RN-14: 10 % de descuento desde un subtotal de 100.000, inclusive. */
public class DescuentoPorMonto implements EstrategiaDescuento {

    private static final String UMBRAL = "100000";
    private static final int PORCENTAJE = 10;

    @Override
    public Dinero aplicar(Dinero subtotal) {
        Dinero umbral = Dinero.de(UMBRAL, subtotal.moneda());
        if (umbral.esMayorQue(subtotal)) {
            return subtotal;
        }
        return subtotal.restar(subtotal.porcentaje(PORCENTAJE));
    }
}
