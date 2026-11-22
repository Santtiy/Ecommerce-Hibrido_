// Canal WebSocket con dashboard-aggregator: se reconecta solo cuando se cierra.
// Cada mensaje es un objeto { orderId: { payment, stock, email } }. El primero tras conectar trae todas las órdenes;
// los siguientes, solo la que cambió.

import { URL_WS, SEGUNDOS_REINTENTO } from './config.js';

export function conectarCanal(oyentes) {
  const { alCambiarConexion, alRecibir } = oyentes;
  const ws = new WebSocket(URL_WS);
  let primero = true;
  ws.onopen = () => alCambiarConexion(true);
  ws.onmessage = (evento) => {
    alRecibir(JSON.parse(evento.data), primero);
    primero = false;
  };
  ws.onclose = () => {
    alCambiarConexion(false);
    setTimeout(() => conectarCanal(oyentes), SEGUNDOS_REINTENTO * 1000);
  };
}
