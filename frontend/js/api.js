// Cliente HTTP de order-api: envía la orden y devuelve el código y el cuerpo de la respuesta.
// Si no hay conexión, fetch lanza el error y quien llama decide cómo mostrarlo.

import { URL_API } from './config.js';

export async function enviarOrden(orden) {
  const respuesta = await fetch(URL_API, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(orden),
  });
  let cuerpo = null;
  try { cuerpo = await respuesta.json(); } catch { /* respuesta sin JSON */ }
  return { status: respuesta.status, ok: respuesta.ok, cuerpo };
}
