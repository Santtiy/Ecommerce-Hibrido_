// Vista del formulario de la orden: filas de ítems, lectura de los datos y estimación del total.

import { ESTIMACION } from './config.js';
import { formatoPesos } from './formato.js';

const $ = (id) => document.getElementById(id);
const cuerpoItems = () => document.querySelector('#items tbody');

function numero(texto) {
  if (texto.trim() === '') return null;
  const n = Number(texto);
  return Number.isFinite(n) ? n : null;
}

function crearInput(tipo, clase, valor, extra = {}) {
  const input = document.createElement('input');
  input.type = tipo;
  input.className = clase;
  input.value = valor;
  Object.assign(input, extra);
  input.addEventListener('input', recalcular);
  const td = document.createElement('td');
  td.append(input);
  return td;
}

export function anadirFila(item = { sku: '', nombre: '', cantidad: 1, precioUnitario: '' }) {
  const fila = document.createElement('tr');
  const quitar = document.createElement('button');
  quitar.type = 'button';
  quitar.className = 'quitar';
  quitar.textContent = 'Eliminar';
  quitar.addEventListener('click', () => { fila.remove(); recalcular(); });
  const tdQuitar = document.createElement('td');
  tdQuitar.append(quitar);
  fila.append(
    crearInput('text', 'sku', item.sku, { placeholder: 'SKU', ariaLabel: 'SKU' }),
    crearInput('text', 'nombre', item.nombre, { placeholder: 'Nombre', ariaLabel: 'Nombre' }),
    crearInput('number', 'cantidad', item.cantidad, { min: 0, step: 1, ariaLabel: 'Cantidad' }),
    crearInput('number', 'precio', item.precioUnitario, { min: 0, step: 'any', placeholder: '0', ariaLabel: 'Precio unitario' }),
    tdQuitar);
  cuerpoItems().append(fila);
  recalcular();
}

export function leerOrden() {
  const items = [...cuerpoItems().rows].map((fila) => ({
    sku: fila.querySelector('.sku').value.trim(),
    nombre: fila.querySelector('.nombre').value.trim(),
    cantidad: numero(fila.querySelector('.cantidad').value),
    precioUnitario: numero(fila.querySelector('.precio').value),
  }));
  return { cliente: $('cliente').value.trim(), items };
}

function recalcular() {
  const centavos = (pesos) => Math.round(pesos * 100);
  const subtotal = leerOrden().items.reduce(
    (suma, it) => suma + centavos((it.cantidad ?? 0) * (it.precioUnitario ?? 0)), 0);
  const descuento = subtotal > centavos(ESTIMACION.umbralDescuento)
    ? Math.round(subtotal * ESTIMACION.porcentajeDescuento / 100) : 0;
  const base = subtotal - descuento;
  const total = base + Math.round(base * ESTIMACION.porcentajeIva / 100);
  $('subtotal').textContent = formatoPesos.format(subtotal / 100);
  $('descuento').textContent = (descuento > 0 ? '− ' : '') + formatoPesos.format(descuento / 100);
  $('total').textContent = formatoPesos.format(total / 100);
}

export function rellenar(cliente, items) {
  $('cliente').value = cliente;
  cuerpoItems().replaceChildren();
  items.forEach((item) => anadirFila(item));
  recalcular();
}
