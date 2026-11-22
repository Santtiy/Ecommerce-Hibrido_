package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.OrdenInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor monetario inmutable. RN-03: nunca es negativo y siempre tiene una moneda de 3 letras.
 * Cada operación devuelve un Dinero nuevo.
 */
public record Dinero(BigDecimal monto, String moneda) {

    private static final int DECIMALES = 2;
    private static final BigDecimal CIEN = new BigDecimal("100");

    public Dinero {
        if (monto == null || monto.signum() < 0) {
            throw new OrdenInvalidaException("El monto no puede ser nulo ni negativo: " + monto);
        }
        if (moneda == null || !moneda.matches("[A-Z]{3}")) {
            throw new OrdenInvalidaException("La moneda debe tener 3 letras mayúsculas: " + moneda);
        }
        monto = monto.setScale(DECIMALES, RoundingMode.HALF_UP);
    }

    public static Dinero de(String monto, String moneda) {
        return new Dinero(new BigDecimal(monto), moneda);
    }

    public static Dinero cero(String moneda) {
        return new Dinero(BigDecimal.ZERO, moneda);
    }

    public Dinero sumar(Dinero otro) {
        exigirMismaMoneda(otro);
        return new Dinero(monto.add(otro.monto), moneda);
    }

    public Dinero restar(Dinero otro) {
        exigirMismaMoneda(otro);
        return new Dinero(monto.subtract(otro.monto), moneda);
    }

    public Dinero multiplicar(int factor) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(factor)), moneda);
    }

    public Dinero porcentaje(int porcentaje) {
        return new Dinero(monto.multiply(BigDecimal.valueOf(porcentaje)).divide(CIEN, DECIMALES, RoundingMode.HALF_UP), moneda);
    }

    public boolean esMayorQue(Dinero otro) {
        exigirMismaMoneda(otro);
        return monto.compareTo(otro.monto) > 0;
    }

    public boolean esPositivo() {
        return monto.signum() > 0;
    }

    private void exigirMismaMoneda(Dinero otro) {
        if (!moneda.equals(otro.moneda)) {
            throw new OrdenInvalidaException("No se pueden operar monedas distintas: " + moneda + " y " + otro.moneda);
        }
    }
}
