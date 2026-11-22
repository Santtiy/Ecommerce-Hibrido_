package co.edu.proyecto.orden.dominio.descuentos;

import co.edu.proyecto.orden.dominio.Dinero;

/** Estrategia nula: no aplica ningún descuento. */
public class SinDescuento implements EstrategiaDescuento {

    @Override
    public Dinero aplicar(Dinero subtotal) {
        return subtotal;
    }
}
