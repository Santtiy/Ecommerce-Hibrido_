// Órdenes enviadas desde este navegador: el WebSocket solo trae { payment, stock, email }, así que el cliente, el total
// y los productos de la respuesta 202 se recuerdan aquí. La tienda escribe y el panel de administración lee (mismo
// origen, mismo localStorage); el evento 'storage' avisa al panel de cada orden nueva. Es solo una comodidad: si el
// navegador no permite almacenamiento, todo funciona igual y simplemente no se recuerda al recargar.

import { leerGuardado, guardar } from './almacenamiento.js';

const CLAVE = 'ecommerce-hibrido.enviadas';
const MAXIMO = 300;

let enviadas = leerGuardado(CLAVE, {});

/** datos: { cliente, total, moneda, productos: [{ sku, nombre, cantidad }] }. */
export function guardarEnviada(orderId, datos) {
  enviadas[orderId] = { ...datos, enviadaEn: Date.now() };
  const ids = Object.keys(enviadas);
  if (ids.length > MAXIMO) {
    ids.sort((a, b) => enviadas[a].enviadaEn - enviadas[b].enviadaEn)
      .slice(0, ids.length - MAXIMO)
      .forEach((id) => delete enviadas[id]);
  }
  guardar(CLAVE, enviadas);
}

export function datosEnviada(orderId) {
  return enviadas[orderId] ?? null;
}

/** Órdenes de este navegador, de la más reciente a la más antigua: [{ orderId, ...datos }]. */
export function enviadasRecientes() {
  return Object.entries(enviadas)
    .map(([orderId, datos]) => ({ orderId, ...datos }))
    .sort((a, b) => b.enviadaEn - a.enviadaEn);
}

/** Llama a alCambiar(idsNuevos) cuando otra pestaña registra órdenes. */
export function escucharEnviadas(alCambiar) {
  window.addEventListener('storage', (evento) => {
    if (evento.key !== CLAVE) return;
    const antes = enviadas;
    enviadas = leerGuardado(CLAVE, {});
    alCambiar(Object.keys(enviadas).filter((id) => !(id in antes)));
  });
}
