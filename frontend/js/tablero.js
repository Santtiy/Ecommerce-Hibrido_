// Vista de las órdenes en vivo: indicador de conexión y tabla que se actualiza de forma incremental.

import { ESTADOS_EXITO, ESTADOS_FALLO, URL_WS, SEGUNDOS_REINTENTO } from './config.js';
import { formatoHora } from './formato.js';

const $ = (id) => document.getElementById(id);

function claseEstado(estado) {
  if (ESTADOS_EXITO.includes(estado)) return 'ok';
  if (ESTADOS_FALLO.includes(estado)) return 'fallo';
  return 'pendiente';
}

function pintarEstado(td, estado) {
  if (td.dataset.valor === estado) return false;
  td.dataset.valor = estado;
  const etiqueta = document.createElement('span');
  etiqueta.className = 'estado ' + claseEstado(estado);
  etiqueta.textContent = estado;
  td.replaceChildren(etiqueta);
  return true;
}

function destacar(fila) {
  fila.classList.add('destacada');
  void fila.offsetWidth;
  requestAnimationFrame(() => fila.classList.remove('destacada'));
}

function crearFila(filas, orderId) {
  const fila = document.createElement('tr');
  fila.dataset.orden = orderId;
  const celdaId = document.createElement('td');
  celdaId.className = 'orden';
  celdaId.textContent = orderId.slice(0, 8);
  celdaId.title = orderId;
  const celdaHora = document.createElement('td');
  celdaHora.className = 'hora';
  fila.append(celdaId, document.createElement('td'), document.createElement('td'), document.createElement('td'), celdaHora);
  filas.prepend(fila);
  $('contador').textContent = filas.rows.length;
  return fila;
}

export function actualizarFila(orderId, estado) {
  const filas = $('filas');
  $('sin-ordenes')?.remove();
  const fila = filas.querySelector('tr[data-orden="' + CSS.escape(orderId) + '"]') ?? crearFila(filas, orderId);
  const cambios = [
    pintarEstado(fila.cells[1], estado.payment),
    pintarEstado(fila.cells[2], estado.stock),
    pintarEstado(fila.cells[3], estado.email),
  ];
  if (cambios.some(Boolean)) {
    fila.cells[4].textContent = formatoHora.format(new Date());
    destacar(fila);
  }
}

export function mostrarConexion(conectado) {
  $('conexion').classList.toggle('conectado', conectado);
  $('texto-conexion').textContent = conectado ? 'conectado' : 'desconectado';
  $('conexion').title = conectado ? URL_WS : 'Reintentando cada ' + SEGUNDOS_REINTENTO + ' s';
}
