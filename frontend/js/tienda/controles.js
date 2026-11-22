// Controles de producto compartidos por el catálogo, la ficha y el carrito.

import { porId, crear, repintarConservandoFoco } from '../dom.js';
import { formatoNumero } from '../formato.js';
import { icono } from '../iconos.js';
import { cantidadDe, sumarCantidad, ponerCantidad } from './carrito.js';
import { esFavorito, alternarFavorito } from './favoritos.js';

/**
 * Foto del producto. El texto alternativo va vacío porque el nombre del producto siempre está escrito al lado:
 * un lector de pantalla lo leería dos veces. width y height reservan el espacio y evitan saltos al cargar.
 */
export function fotoProducto(producto, clase) {
  const foto = document.createElement('img');
  foto.className = clase;
  foto.src = producto.foto.src;
  foto.width = 768;
  foto.height = 576;
  foto.alt = '';
  foto.loading = 'lazy';
  foto.decoding = 'async';
  return foto;
}

/**
 * Botón de favorito (corazón). Es un interruptor: la etiqueta no cambia y aria-pressed dice si está marcado.
 * conTexto: versión con la palabra "Favorito" (página de producto); sin texto, el corazón sobre la foto.
 */
export function botonFavorito(producto, conTexto = false) {
  const boton = crear('button', conTexto ? 'boton-secundario boton-favorito-texto' : 'boton-favorito');
  boton.type = 'button';
  boton.dataset.sku = producto.sku;
  boton.dataset.accion = 'favorito';
  boton.append(icono('corazon'));
  if (conTexto) boton.append(crear('span', '', 'Favorito'));
  else boton.setAttribute('aria-label', `Favorito: ${producto.nombre}`);
  boton.addEventListener('click', (evento) => {
    evento.preventDefault();
    evento.stopPropagation();
    alternarFavorito(producto.sku);
  });
  pintarFavorito(boton);
  return boton;
}

export function pintarFavorito(boton) {
  boton.setAttribute('aria-pressed', String(esFavorito(boton.dataset.sku)));
}

function botonIcono(nombreIcono, etiqueta, datos) {
  const boton = crear('button', 'boton-icono');
  boton.type = 'button';
  boton.setAttribute('aria-label', etiqueta);
  boton.title = etiqueta;
  Object.assign(boton.dataset, datos);
  boton.append(icono(nombreIcono));
  return boton;
}

/** Selector [− n +] de un producto del carrito. Lleva data-sku y data-accion para recuperar el foco al repintar. */
export function selectorCantidad(producto) {
  const contenedor = crear('div', 'selector-cantidad');
  const menos = botonIcono('menos', `Quitar una unidad de ${producto.nombre}`, { sku: producto.sku, accion: 'menos' });
  const mas = botonIcono('mas', `Agregar una unidad de ${producto.nombre}`, { sku: producto.sku, accion: 'mas' });
  menos.addEventListener('click', () => sumarCantidad(producto.sku, -1));
  mas.addEventListener('click', () => sumarCantidad(producto.sku, 1));
  const cantidad = crear('output', 'cantidad', formatoNumero.format(cantidadDe(producto.sku)));
  cantidad.setAttribute('aria-label', `Cantidad de ${producto.nombre}`);
  contenedor.append(menos, cantidad, mas);
  return contenedor;
}

/** Zona de compra: "Agregar" si no está en el carrito; si ya está, el selector de cantidad. */
export function pintarCompra(producto, compra, { primario = false } = {}) {
  const cantidad = cantidadDe(producto.sku);
  repintarConservandoFoco(compra, () => {
    if (cantidad > 0) {
      compra.replaceChildren(selectorCantidad(producto));
      return;
    }
    const agregar = crear('button', primario ? 'boton-primario boton-agregar-grande' : 'boton-agregar');
    agregar.type = 'button';
    agregar.dataset.sku = producto.sku;
    agregar.dataset.accion = 'agregar';
    agregar.append(icono('carrito'), crear('span', '', primario ? 'Agregar al carrito' : 'Agregar'));
    agregar.setAttribute('aria-label', `Agregar ${producto.nombre} al carrito`);
    agregar.addEventListener('click', () => {
      ponerCantidad(producto.sku, 1);
      avisarAgregado(producto);
      // El botón desaparece al agregar: el foco pasa al + del selector para seguir sumando con el teclado.
      compra.querySelector('[data-accion="mas"]')?.focus();
    });
    compra.replaceChildren(agregar);
  });
}

const SEGUNDOS_VISIBLE = 4;
let temporizador = null;
let alVerCarrito = () => {};

export function configurarAviso({ verCarrito }) {
  alVerCarrito = verCarrito;
}

function avisarAgregado(producto) {
  const caja = porId('aviso-carrito');
  const texto = crear('p', 'texto-aviso');
  texto.append(crear('strong', '', 'Agregado al carrito'), crear('span', '', producto.nombre));
  const ver = crear('button', 'boton-secundario', 'Ver carrito');
  ver.type = 'button';
  ver.addEventListener('click', () => { caja.hidden = true; alVerCarrito(); });
  caja.replaceChildren(fotoProducto(producto, 'miniatura'), texto, ver);
  caja.hidden = false;
  clearTimeout(temporizador);
  temporizador = setTimeout(() => { caja.hidden = true; }, SEGUNDOS_VISIBLE * 1000);
}
