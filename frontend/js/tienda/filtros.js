// Qué muestra la vitrina: búsqueda, categoría, orden y "solo favoritos". Sin DOM.

import { CATALOGO, CATEGORIAS } from './productos.js';
import { esFavorito } from './favoritos.js';

const estado = { texto: '', categoria: 'todas', orden: 'relevancia', soloFavoritos: false };
const oyentes = [];

/** Sin tildes ni mayúsculas: "audifonos" encuentra "Audífonos". */
const normalizar = (texto) => texto.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();

export function filtroActual() {
  return { ...estado };
}

export function cambiarFiltro(cambios) {
  Object.assign(estado, cambios);
  oyentes.forEach((oyente) => oyente());
}

export function alCambiarFiltro(oyente) {
  oyentes.push(oyente);
}

export function quitarFiltros() {
  cambiarFiltro({ texto: '', categoria: 'todas', soloFavoritos: false });
}

function coincideBusqueda(producto) {
  const texto = normalizar(estado.texto.trim());
  return !texto || normalizar(`${producto.nombre} ${producto.detalle} ${producto.descripcion} ${producto.sku}`).includes(texto);
}

const pasaFiltrosComunes = (producto) => coincideBusqueda(producto) && (!estado.soloFavoritos || esFavorito(producto.sku));

const ORDENES = {
  'precio-asc': (a, b) => a.precio - b.precio,
  'precio-desc': (a, b) => b.precio - a.precio,
  'nombre': (a, b) => a.nombre.localeCompare(b.nombre, 'es'),
};

/** Productos que se ven con el filtro actual, en el orden elegido ('relevancia' = el del catálogo). */
export function productosVisibles() {
  const lista = CATALOGO.filter((producto) =>
    pasaFiltrosComunes(producto) && (estado.categoria === 'todas' || producto.categoria === estado.categoria));
  return ORDENES[estado.orden] ? lista.sort(ORDENES[estado.orden]) : lista;
}

/** Cuántos productos habría en cada categoría con la búsqueda y los favoritos actuales: { todas, computacion, … }. */
export function conteoPorCategoria() {
  const visibles = CATALOGO.filter(pasaFiltrosComunes);
  const conteo = { todas: visibles.length };
  CATEGORIAS.forEach(({ id }) => { conteo[id] = visibles.filter((producto) => producto.categoria === id).length; });
  return conteo;
}
