// Vista del resumen: cuántas órdenes hay de cada resultado, en los botones que filtran la tabla.
// Se calcula con lo que ya envía el WebSocket.

import { porId } from '../dom.js';
import { formatoNumero } from '../formato.js';

const n = (valor) => formatoNumero.format(valor);

/** conteos: { todas, completada, fallo, proceso, falloPago, falloStock } (ver tablero.refrescar). */
export function pintarResumen(conteos) {
  for (const clave of ['todas', 'completada', 'fallo', 'proceso']) porId('cuenta-' + clave).textContent = n(conteos[clave]);
  // Una orden puede fallar en pago y en stock a la vez: los dos números pueden sumar más que el total.
  porId('detalle-fallo').textContent = `(${n(conteos.falloPago)} de pago, ${n(conteos.falloStock)} de stock)`;
}
