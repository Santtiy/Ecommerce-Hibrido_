// Vista del panel "Respuesta de order-api": muestra el envío, el resultado (202 o error) y los cuerpos JSON.

import { URL_API } from './config.js';
import { formatoPesos } from './formato.js';

const $ = (id) => document.getElementById(id);

function mostrar(tipo, nodos) {
  $('respuesta').className = 'tarjeta ' + tipo;
  $('respuesta-contenido').className = '';
  $('respuesta-contenido').replaceChildren(...nodos);
}

function parrafo(texto, clase) {
  const p = document.createElement('p');
  if (clase) p.className = clase;
  p.textContent = texto;
  return p;
}

function bloqueJson(titulo, objeto) {
  const detalles = document.createElement('details');
  const resumen = document.createElement('summary');
  resumen.textContent = titulo;
  const pre = document.createElement('pre');
  pre.textContent = JSON.stringify(objeto, null, 2);
  detalles.append(resumen, pre);
  return detalles;
}

export function mostrarEnviando() {
  mostrar('', [parrafo('Enviando a ' + URL_API + '…')]);
}

export function mostrarResultado(orden, { status, ok, cuerpo }) {
  const nodos = [parrafo('HTTP ' + status, 'codigo')];
  if (status === 202 && cuerpo) {
    nodos.push(parrafo('orderId: ' + cuerpo.orderId));
    nodos.push(parrafo('Total calculado por el servidor: ' + formatoPesos.format(cuerpo.total) + ' ' + (cuerpo.moneda ?? '')));
    nodos.push(parrafo('Estado: ' + cuerpo.estado + '. Siga su avance en la tabla de órdenes en vivo.'));
  } else if (cuerpo) {
    nodos.push(parrafo(cuerpo.mensaje ?? 'Error'));
    if (Array.isArray(cuerpo.detalles) && cuerpo.detalles.length > 0) {
      const lista = document.createElement('ul');
      cuerpo.detalles.forEach((campo) => {
        const li = document.createElement('li');
        li.textContent = campo;
        lista.append(li);
      });
      nodos.push(lista);
    }
  }
  nodos.push(bloqueJson('Cuerpo enviado', orden));
  if (cuerpo) nodos.push(bloqueJson('Cuerpo recibido', cuerpo));
  mostrar(ok ? 'ok' : 'fallo', nodos);
}

export function mostrarSinConexion(error) {
  mostrar('fallo', [
    parrafo('Sin respuesta', 'codigo'),
    parrafo('No se pudo conectar con order-api en ' + URL_API + '. Es posible que order-api no esté arrancado (ops\\arrancar.ps1).'),
    parrafo('Detalle técnico: ' + error.message),
  ]);
}
