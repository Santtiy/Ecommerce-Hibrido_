package co.edu.proyecto.orden.dominio.impuestos;

import co.edu.proyecto.orden.dominio.Dinero;

/** IVA general de Colombia: suma el 19 % a la base. */
public class IvaColombia implements EstrategiaImpuesto {

    private static final int TASA_IVA = 19;

    @Override
    public Dinero aplicar(Dinero base) {
        return base.sumar(base.porcentaje(TASA_IVA));
    }
}
