// Formatos de presentación compartidos por las vistas.

const pesosEnteros = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 });
const pesosConCentavos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Pesos sin decimales si el monto es exacto ($ 160.650) y con dos si tiene centavos ($ 406.765,80). */
export const formatoPesos = {
  format(valor) {
    const tieneCentavos = Math.round(Number(valor) * 100) % 100 !== 0;
    return (tieneCentavos ? pesosConCentavos : pesosEnteros).format(valor);
  },
};

export const formatoNumero = new Intl.NumberFormat('es-CO');

export const formatoHora = new Intl.DateTimeFormat('es-CO', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
