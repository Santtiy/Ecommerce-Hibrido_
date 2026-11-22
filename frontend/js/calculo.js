// Estimación del total y predicción de las reglas de negocio, con la misma aritmética que order-api
// (subtotal − descuento + IVA, en centavos y redondeando la mitad hacia arriba). Funciones puras, sin DOM.

import { REGLAS } from './config.js';

const centavos = (pesos) => Math.round(pesos * 100);
const porcentaje = (monto, pct) => Math.round(monto * pct / 100);

/** Devuelve { subtotal, descuento, iva, total } en pesos. Los valores vacíos o no numéricos cuentan como 0. */
export function estimar(items) {
  const subtotal = items.reduce((suma, it) => suma + centavos((it.cantidad ?? 0) * (it.precioUnitario ?? 0)), 0);
  const descuento = subtotal >= centavos(REGLAS.umbralDescuento) ? porcentaje(subtotal, REGLAS.porcentajeDescuento) : 0;
  const base = subtotal - descuento;
  const iva = porcentaje(base, REGLAS.porcentajeIva);
  return { subtotal: subtotal / 100, descuento: descuento / 100, iva: iva / 100, total: (base + iva) / 100 };
}

/** Qué decidirán los workers según RN-11 (pago) y RN-12 (stock): { pagoRechazado, itemSinStock }. */
export function predecir(items, total) {
  return {
    pagoRechazado: total > REGLAS.limitePago,
    itemSinStock: items.find((it) => (it.cantidad ?? 0) > REGLAS.maximoUnidades) ?? null,
  };
}
