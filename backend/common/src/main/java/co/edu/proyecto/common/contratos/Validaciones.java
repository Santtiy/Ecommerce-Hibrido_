package co.edu.proyecto.common.contratos;

import java.math.BigDecimal;

/** Comprobaciones Fail-Fast compartidas por los constructores compactos de los contratos. */
final class Validaciones {

    private Validaciones() {
    }

    static String textoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
        return valor;
    }

    static <T> T obligatorio(T valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo '" + campo + "' es obligatorio");
        }
        return valor;
    }

    static BigDecimal positivo(BigDecimal valor, String campo) {
        if (obligatorio(valor, campo).signum() <= 0) {
            throw new IllegalArgumentException("El campo '" + campo + "' debe ser mayor que cero");
        }
        return valor;
    }

    static BigDecimal noNegativo(BigDecimal valor, String campo) {
        if (obligatorio(valor, campo).signum() < 0) {
            throw new IllegalArgumentException("El campo '" + campo + "' no puede ser negativo");
        }
        return valor;
    }
}
