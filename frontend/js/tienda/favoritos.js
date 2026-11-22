// Favoritos: los productos que el cliente marca con el corazón. Se recuerdan en localStorage solo por comodidad; si el
// navegador no lo permite, funcionan durante la visita. Sin DOM: las vistas se suscriben con alCambiarFavoritos.

import { leerGuardado, guardar } from '../almacenamiento.js';

const CLAVE = 'ecommerce-hibrido.favoritos';
const oyentes = [];
const favoritos = new Set(leerGuardado(CLAVE, []));

export function alCambiarFavoritos(oyente) {
  oyentes.push(oyente);
}

export function esFavorito(sku) {
  return favoritos.has(sku);
}

export function cantidadFavoritos() {
  return favoritos.size;
}

export function alternarFavorito(sku) {
  if (favoritos.has(sku)) favoritos.delete(sku); else favoritos.add(sku);
  guardar(CLAVE, [...favoritos]);
  oyentes.forEach((oyente) => oyente());
}
