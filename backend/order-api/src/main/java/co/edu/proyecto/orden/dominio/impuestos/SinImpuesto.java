package co.edu.proyecto.orden.dominio.impuestos;

import co.edu.proyecto.orden.dominio.Dinero;

/** Estrategia nula: no aplica ningún impuesto. */
public class SinImpuesto implements EstrategiaImpuesto {

    @Override
    public Dinero aplicar(Dinero base) {
        return base;
    }
}
