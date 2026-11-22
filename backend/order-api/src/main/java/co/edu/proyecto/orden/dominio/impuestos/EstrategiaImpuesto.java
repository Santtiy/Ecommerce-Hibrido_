package co.edu.proyecto.orden.dominio.impuestos;

import co.edu.proyecto.orden.dominio.Dinero;

/** Strategy de impuesto. Contrato: nunca devuelve null ni un valor negativo, y no cambia la moneda. */
public interface EstrategiaImpuesto {

    Dinero aplicar(Dinero base);
}
