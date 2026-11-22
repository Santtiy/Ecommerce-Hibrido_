// Precios finales y estimación del pedido, con la misma aritmética que order-api.
// El IVA se incluye antes de evaluar el descuento; el resumen lo desglosa sin volver a sumarlo.

import { REGLAS } from './config.js';

const centavos = (pesos) => Math.round(pesos * 100);
const porcentaje = (monto, pct) => Math.round(monto * pct / 100);
const incluirIva = (monto) => monto + porcentaje(monto, REGLAS.porcentajeIva);

/** Precio anunciado al cliente, con el IVA común del catálogo. El precio base se conserva para POST /order. */
export function precioConIva(precioBase) {
  return incluirIva(centavos(precioBase)) / 100;
}

/** Subtotal y total incluyen IVA. iva es la parte del total que corresponde al impuesto, después del descuento. */
export function estimar(items) {
  const base = items.reduce((suma, it) => suma + centavos((it.cantidad ?? 0) * (it.precioUnitario ?? 0)), 0);
  const subtotal = incluirIva(base);
  const descuento = subtotal >= centavos(REGLAS.umbralDescuento) ? porcentaje(subtotal, REGLAS.porcentajeDescuento) : 0;
  const total = subtotal - descuento;
  const iva = Math.round(total * REGLAS.porcentajeIva / (100 + REGLAS.porcentajeIva));
  return { subtotal: subtotal / 100, descuento: descuento / 100, iva: iva / 100, total: total / 100 };
}

/** Qué decidirán los workers según RN-11 (pago) y RN-12 (stock): { pagoRechazado, itemSinStock }. */
export function predecir(items, total) {
  return {
    pagoRechazado: total > REGLAS.limitePago,
    itemSinStock: items.find((it) => (it.cantidad ?? 0) > REGLAS.maximoUnidades) ?? null,
  };
}
