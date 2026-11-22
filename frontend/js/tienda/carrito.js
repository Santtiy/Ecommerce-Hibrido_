// Estado del carrito: sku → cantidad. Sin DOM; las vistas se suscriben con alCambiarCarrito.

import { buscarProducto } from './productos.js';

const MAXIMO_POR_PRODUCTO = 99;
const lineas = new Map();
const oyentes = [];

const avisar = () => oyentes.forEach((oyente) => oyente());

export function alCambiarCarrito(oyente) {
  oyentes.push(oyente);
}

export function cantidadDe(sku) {
  return lineas.get(sku) ?? 0;
}

/** Fija la cantidad de un producto; 0 o menos lo quita del carrito. */
export function ponerCantidad(sku, cantidad) {
  const n = Math.min(Math.max(Math.trunc(cantidad) || 0, 0), MAXIMO_POR_PRODUCTO);
  if (n === 0) lineas.delete(sku); else lineas.set(sku, n);
  avisar();
}

export function sumarCantidad(sku, diferencia) {
  ponerCantidad(sku, cantidadDe(sku) + diferencia);
}

/** Reemplaza el contenido del carrito (escenarios de la exposición). items: [{ sku, cantidad }]. */
export function llenarCarrito(items) {
  lineas.clear();
  items.forEach(({ sku, cantidad }) => lineas.set(sku, cantidad));
  avisar();
}

export function vaciarCarrito() {
  lineas.clear();
  avisar();
}

export function carritoVacio() {
  return lineas.size === 0;
}

export function unidadesEnCarrito() {
  let total = 0;
  for (const cantidad of lineas.values()) total += cantidad;
  return total;
}

/** [{ producto, cantidad }] en el orden en que se agregaron. */
export function lineasDelCarrito() {
  return [...lineas].map(([sku, cantidad]) => ({ producto: buscarProducto(sku), cantidad }));
}

/** El carrito con la forma de los ítems de POST /order. */
export function itemsDeLaOrden() {
  return lineasDelCarrito().map(({ producto, cantidad }) =>
    ({ sku: producto.sku, nombre: producto.nombre, cantidad, precioUnitario: producto.precio }));
}
