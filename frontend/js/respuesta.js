// Vista del panel "Respuesta de order-api": el envío en curso, el resultado (202 o error) y los cuerpos JSON.

import { URL_API } from './config.js';
import { estimar } from './calculo.js';
import { formatoPesos, crear } from './formato.js';

const $ = (id) => document.getElementById(id);
const TEXTO_HTTP = { 202: 'Accepted', 400: 'Bad Request', 404: 'Not Found', 409: 'Conflict', 500: 'Internal Server Error' };

function mostrar(tipo, nodos) {
  $('respuesta').className = 'panel respuesta ' + tipo;
  const contenido = crear('div', 'contenido-respuesta');
  contenido.id = 'respuesta-contenido';
  contenido.append(...nodos);
  $('respuesta-contenido').replaceWith(contenido);
}

function estadoHttp(numero, texto) {
  const p = crear('p', 'estado-http');
  p.append(crear('span', 'numero', numero), crear('span', 'texto', texto));
  return p;
}

function bloqueJson(titulo, objeto) {
  const detalles = crear('details');
  detalles.append(crear('summary', '', titulo), crear('pre', '', JSON.stringify(objeto, null, 2)));
  return detalles;
}

function dato(lista, termino, ...valor) {
  const dd = crear('dd');
  dd.append(...valor);
  lista.append(crear('dt', '', termino), dd);
}

function botonCopiar(texto) {
  const boton = crear('button', 'copiar', 'Copiar');
  boton.type = 'button';
  boton.addEventListener('click', async () => {
    try {
      await navigator.clipboard.writeText(texto);
      boton.textContent = 'Copiado';
    } catch {
      boton.textContent = 'No se pudo copiar';
    }
    setTimeout(() => { boton.textContent = 'Copiar'; }, 1500);
  });
  return boton;
}

export function mostrarEnviando() {
  mostrar('', [crear('p', 'enviando', 'Enviando a ' + URL_API + '…')]);
}

/** opciones.alVerOrden(orderId): lleva a la fila de la orden en la tabla. */
export function mostrarResultado(orden, { status, ok, cuerpo }, opciones = {}) {
  const nodos = [estadoHttp(String(status), TEXTO_HTTP[status] ?? '')];
  if (status === 202 && cuerpo) {
    const lista = crear('dl', 'datos');
    dato(lista, 'orderId', crear('code', 'id-orden', cuerpo.orderId), ...(navigator.clipboard ? [botonCopiar(cuerpo.orderId)] : []));
    dato(lista, 'Total del servidor', crear('strong', '', formatoPesos.format(cuerpo.total) + ' ' + (cuerpo.moneda ?? '')));
    const estimado = estimar(orden.items).total;
    const igual = Math.abs(estimado - Number(cuerpo.total)) < 0.005;
    dato(lista, 'Estimación local', crear('span', '', formatoPesos.format(estimado)),
      crear('span', 'comparacion ' + (igual ? 'ok' : 'fallo'), igual ? 'coincide' : 'no coincide'));
    dato(lista, 'Estado inicial', crear('code', '', cuerpo.estado));
    nodos.push(lista);
    if (opciones.alVerOrden) {
      const ver = crear('button', 'ver-orden', 'Ver en la tabla de órdenes');
      ver.type = 'button';
      ver.addEventListener('click', () => opciones.alVerOrden(cuerpo.orderId));
      nodos.push(ver);
    }
  } else if (cuerpo) {
    nodos.push(crear('p', 'mensaje', cuerpo.mensaje ?? 'Error'));
    if (Array.isArray(cuerpo.detalles) && cuerpo.detalles.length > 0) {
      const lista = crear('ul', 'detalles');
      cuerpo.detalles.forEach((campo) => lista.append(crear('li', '', campo)));
      nodos.push(lista);
    }
  }
  const cuerpos = crear('div', 'cuerpos');
  cuerpos.append(bloqueJson('Cuerpo enviado', orden));
  if (cuerpo) cuerpos.append(bloqueJson('Cuerpo recibido', cuerpo));
  nodos.push(cuerpos);
  mostrar(ok ? 'ok' : 'fallo', nodos);
}

export function mostrarSinConexion(error) {
  mostrar('fallo', [
    estadoHttp('Sin respuesta', ''),
    crear('p', 'mensaje', 'No se pudo conectar con order-api en ' + URL_API + '. Comprueba que esté arrancado y vuelve a enviar.'),
    crear('p', 'tecnico', 'Detalle técnico: ' + error.message),
  ]);
}
