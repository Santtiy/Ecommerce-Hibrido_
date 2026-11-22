// Órdenes enviadas desde este navegador: el WebSocket solo trae { payment, stock, email }, así que el cliente y
// el total de la respuesta 202 se recuerdan aquí. Se guardan en localStorage solo por comodidad: si el navegador
// no lo permite, la página funciona igual y simplemente no los recuerda al recargar.

const CLAVE = 'ecommerce-hibrido.enviadas';
const MAXIMO = 300;

function leer() {
  try { return JSON.parse(localStorage.getItem(CLAVE)) ?? {}; } catch { return {}; }
}

const enviadas = leer();

/** datos: { cliente, total, moneda }. */
export function guardarEnviada(orderId, datos) {
  enviadas[orderId] = { ...datos, enviadaEn: Date.now() };
  const ids = Object.keys(enviadas);
  if (ids.length > MAXIMO) {
    ids.sort((a, b) => enviadas[a].enviadaEn - enviadas[b].enviadaEn)
      .slice(0, ids.length - MAXIMO)
      .forEach((id) => delete enviadas[id]);
  }
  try { localStorage.setItem(CLAVE, JSON.stringify(enviadas)); } catch { /* sin almacenamiento: solo en memoria */ }
}

export function datosEnviada(orderId) {
  return enviadas[orderId] ?? null;
}
