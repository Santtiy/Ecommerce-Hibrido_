// Panel de administración, "Última petición a order-api": la que acaba de hacer la tienda (en otra pestaña), con su
// código HTTP y los cuerpos JSON enviado y recibido.

import { TEXTO_HTTP } from '../config.js';
import { porId, crear, bloqueJson } from '../dom.js';
import { formatoPesos, formatoHora } from '../formato.js';

function dato(lista, termino, ...valor) {
  const dd = crear('dd');
  dd.append(...valor);
  lista.append(crear('dt', '', termino), dd);
}

/** peticion: { url, enviado, status, recibido, en } o { url, enviado, error, en }. opciones.alVerOrden(orderId). */
export function pintarPeticion(peticion, opciones = {}) {
  const panel = porId('peticion');
  const contenido = porId('contenido-peticion');
  if (!peticion) {
    panel.className = 'panel peticion';
    contenido.replaceChildren(crear('p', 'vacio', 'Cuando la tienda envíe una orden, aquí aparecen la petición y la respuesta de order-api.'));
    return;
  }
  const { url, enviado, status, recibido, error, en } = peticion;
  panel.className = 'panel peticion ' + (status === 202 ? 'ok' : 'fallo');

  const estado = crear('p', 'estado-http');
  estado.append(crear('span', 'numero', error ? 'Sin respuesta' : String(status)), crear('span', 'texto', TEXTO_HTTP[status] ?? ''));
  const nodos = [estado, crear('p', 'meta', `POST ${new URL(url).pathname} · ${formatoHora.format(new Date(en))}`)];

  if (error) {
    nodos.push(crear('p', 'mensaje', 'No se pudo conectar con order-api: ' + error));
  } else if (status === 202 && recibido) {
    const lista = crear('dl', 'datos');
    const ver = crear('button', 'enlace', recibido.orderId.slice(0, 8));
    ver.type = 'button';
    ver.title = 'Ver su recorrido';
    ver.addEventListener('click', () => opciones.alVerOrden?.(recibido.orderId));
    dato(lista, 'orderId', ver);
    dato(lista, 'Total', crear('strong', '', `${formatoPesos.format(recibido.total)} ${recibido.moneda ?? ''}`));
    dato(lista, 'Estado inicial', crear('code', '', recibido.estado));
    nodos.push(lista);
  } else if (recibido) {
    nodos.push(crear('p', 'mensaje', recibido.mensaje ?? 'Error'));
    if (Array.isArray(recibido.detalles) && recibido.detalles.length > 0) {
      const lista = crear('ul', 'detalles');
      recibido.detalles.forEach((detalle) => lista.append(crear('li', '', detalle)));
      nodos.push(lista);
    }
  }
  const cuerpos = crear('div', 'cuerpos');
  cuerpos.append(bloqueJson('Cuerpo enviado', enviado));
  if (recibido) cuerpos.append(bloqueJson('Cuerpo recibido', recibido));
  nodos.push(cuerpos);
  contenido.replaceChildren(...nodos);
}
