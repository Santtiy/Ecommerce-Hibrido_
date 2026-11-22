// Vista del formulario de la orden: filas de ítems, lectura de los datos, validación en línea, estimación del total
// y predicción de las reglas de negocio (RN-11, RN-12, RN-14).

import { REGLAS } from './config.js';
import { estimar, predecir } from './calculo.js';
import { formatoPesos, formatoNumero, crear } from './formato.js';

const $ = (id) => document.getElementById(id);
const filas = () => [...$('items').children];

// La validación se muestra después del primer intento de envío; desde entonces se actualiza al escribir.
let validarAlEscribir = false;

function numero(texto) {
  if (texto.trim() === '') return null;
  const n = Number(texto);
  return Number.isFinite(n) ? n : null;
}

function crearInput(clase, etiqueta, valor, atributos) {
  const input = document.createElement('input');
  input.className = clase;
  Object.assign(input, atributos);
  input.value = valor ?? '';
  input.setAttribute('aria-label', etiqueta);
  input.addEventListener('input', alCambiar);
  return input;
}

// Aspa dibujada con el mismo trazo que la lupa del buscador (2 px, extremos redondeados).
function iconoQuitar() {
  const ns = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(ns, 'svg');
  svg.setAttribute('viewBox', '0 0 16 16');
  svg.setAttribute('aria-hidden', 'true');
  const trazo = document.createElementNS(ns, 'path');
  trazo.setAttribute('d', 'M4 4l8 8M12 4l-8 8');
  for (const [nombre, valor] of [['fill', 'none'], ['stroke', 'currentColor'], ['stroke-width', '2'], ['stroke-linecap', 'round']]) {
    trazo.setAttribute(nombre, valor);
  }
  svg.append(trazo);
  return svg;
}

export function anadirFila(item = { sku: '', nombre: '', cantidad: 1, precioUnitario: '' }) {
  const fila = crear('li', 'item');
  const quitar = crear('button', 'quitar');
  quitar.append(iconoQuitar());
  quitar.type = 'button';
  quitar.title = 'Eliminar ítem';
  quitar.setAttribute('aria-label', 'Eliminar ítem');
  quitar.addEventListener('click', () => { fila.remove(); alCambiar(); });
  fila.append(
    crearInput('sku', 'SKU', item.sku, { type: 'text', placeholder: 'SKU', autocomplete: 'off' }),
    crearInput('nombre', 'Nombre', item.nombre, { type: 'text', placeholder: 'Producto', autocomplete: 'off' }),
    crearInput('cantidad', 'Cantidad', item.cantidad, { type: 'number', min: 1, step: 1, inputMode: 'numeric' }),
    crearInput('precio', 'Precio unitario', item.precioUnitario, { type: 'number', min: 0, step: 'any', placeholder: '0', inputMode: 'decimal' }),
    quitar);
  $('items').append(fila);
  alCambiar();
  return fila;
}

export function leerOrden() {
  const items = filas().map((fila) => ({
    sku: fila.querySelector('.sku').value.trim(),
    nombre: fila.querySelector('.nombre').value.trim(),
    cantidad: numero(fila.querySelector('.cantidad').value),
    precioUnitario: numero(fila.querySelector('.precio').value),
  }));
  return { cliente: $('cliente').value.trim(), items };
}

export function rellenar(cliente, items) {
  $('cliente').value = cliente;
  $('items').replaceChildren();
  validarAlEscribir = false;
  pintarErrores([]);
  items.forEach((item) => anadirFila(item));
  alCambiar();
}

/** Valida lo mismo que CrearOrdenRequest e ItemRequest en order-api. Devuelve true si se puede enviar. */
export function validarFormulario() {
  validarAlEscribir = true;
  const errores = buscarErrores();
  pintarErrores(errores);
  errores.find((error) => error.input)?.input.focus();
  return errores.length === 0;
}

function buscarErrores() {
  const errores = [];
  const { cliente, items } = leerOrden();
  if (!cliente) errores.push({ input: $('cliente'), mensaje: 'Escribe el nombre del cliente.' });
  if (items.length === 0) errores.push({ input: null, mensaje: 'Añade al menos un ítem.' });
  filas().forEach((fila, i) => {
    const item = items[i];
    const n = i + 1;
    if (!item.sku) errores.push({ input: fila.querySelector('.sku'), mensaje: `Ítem ${n}: falta el SKU.` });
    if (!item.nombre) errores.push({ input: fila.querySelector('.nombre'), mensaje: `Ítem ${n}: falta el nombre.` });
    if (item.cantidad === null || !Number.isInteger(item.cantidad) || item.cantidad < 1) {
      errores.push({ input: fila.querySelector('.cantidad'), mensaje: `Ítem ${n}: la cantidad debe ser un entero mayor que 0.` });
    }
    if (item.precioUnitario === null || item.precioUnitario <= 0) {
      errores.push({ input: fila.querySelector('.precio'), mensaje: `Ítem ${n}: el precio debe ser mayor que 0.` });
    }
  });
  return errores;
}

function pintarErrores(errores) {
  document.querySelectorAll('#formulario [aria-invalid]').forEach((input) => input.removeAttribute('aria-invalid'));
  errores.forEach((error) => error.input?.setAttribute('aria-invalid', 'true'));
  const caja = $('errores');
  caja.hidden = errores.length === 0;
  if (errores.length === 0) return caja.replaceChildren();
  const lista = crear('ul');
  errores.forEach((error) => lista.append(crear('li', '', error.mensaje)));
  caja.replaceChildren(crear('p', 'titulo-errores', 'Revisa antes de enviar'), lista);
}

function alCambiar() {
  recalcular();
  if (validarAlEscribir) pintarErrores(buscarErrores());
}

function recalcular() {
  const { items } = leerOrden();
  const estimacion = estimar(items);
  $('subtotal').textContent = formatoPesos.format(estimacion.subtotal);
  $('descuento').textContent = (estimacion.descuento > 0 ? '− ' : '') + formatoPesos.format(estimacion.descuento);
  $('iva').textContent = formatoPesos.format(estimacion.iva);
  $('total').textContent = formatoPesos.format(estimacion.total);
  filas().forEach((fila, i) => fila.classList.toggle('excede', (items[i].cantidad ?? 0) > REGLAS.maximoUnidades));
  pintarPrediccion(items, estimacion);
}

function aviso(tipo, regla, texto) {
  const li = crear('li', 'aviso ' + tipo);
  if (regla) li.append(crear('strong', '', regla), ' ');
  li.append(texto);
  return li;
}

function pintarPrediccion(items, estimacion) {
  const avisos = [];
  if (estimacion.subtotal > 0) {
    const { pagoRechazado, itemSinStock } = predecir(items, estimacion.total);
    if (estimacion.descuento > 0) {
      avisos.push(aviso('info', 'RN-14', `Se aplica un ${REGLAS.porcentajeDescuento} % de descuento porque el subtotal supera ${formatoPesos.format(REGLAS.umbralDescuento)}.`));
    }
    if (pagoRechazado) {
      avisos.push(aviso('fallo', 'RN-11', `El pago se rechazará: el total supera ${formatoPesos.format(REGLAS.limitePago)}.`));
    }
    if (itemSinStock) {
      const quien = itemSinStock.sku || 'Un ítem';
      avisos.push(aviso('fallo', 'RN-12', `No habrá stock: ${quien} pide ${formatoNumero.format(itemSinStock.cantidad)} unidades y el máximo es ${REGLAS.maximoUnidades}.`));
    }
    if (!pagoRechazado && !itemSinStock) {
      avisos.push(aviso('ok', '', 'Ninguna regla la rechaza: pago, stock y correo deberían salir bien.'));
    }
  }
  $('prediccion').replaceChildren(...avisos);
}
