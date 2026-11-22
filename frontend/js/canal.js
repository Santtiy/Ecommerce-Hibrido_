// Canal WebSocket con dashboard-aggregator: se reconecta solo cuando se cierra.
// Cada mensaje es un objeto { orderId: { payment, stock, email } } con las órdenes que cambiaron.

import { URL_WS, SEGUNDOS_REINTENTO } from './config.js';

export function conectarCanal(oyentes) {
  const { alCambiarConexion, alRecibirOrden } = oyentes;
  const ws = new WebSocket(URL_WS);
  ws.onopen = () => alCambiarConexion(true);
  ws.onmessage = (evento) => {
    const datos = JSON.parse(evento.data);
    for (const [orderId, estado] of Object.entries(datos)) alRecibirOrden(orderId, estado);
  };
  ws.onclose = () => {
    alCambiarConexion(false);
    setTimeout(() => conectarCanal(oyentes), SEGUNDOS_REINTENTO * 1000);
  };
}
