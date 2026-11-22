// Paso 1, página de producto (#producto/SKU): migas de pan, foto grande, descripción, características, compra,
// favorito, condiciones (las reglas reales del sistema) y "También te puede interesar".

import { porId, crear } from '../dom.js';
import { REGLAS } from '../config.js';
import { estimar, precioConIva } from '../calculo.js';
import { formatoPesos, formatoNumero } from '../formato.js';
import { CATALOGO, nombreCategoria, enlaceProducto } from './productos.js';
import { cantidadDe } from './carrito.js';
import { fotoProducto, botonFavorito, pintarFavorito, pintarCompra } from './controles.js';

const MAXIMO_RELACIONADOS = 4;
let actual = null; // { producto, compra, favorito, enCarrito }

function migas(producto, alElegirCategoria) {
  const nav = crear('nav', 'migas');
  nav.setAttribute('aria-label', 'Ruta');
  const lista = crear('ol');
  const inicio = crear('a', '', 'Productos');
  inicio.href = '#';
  const categoria = crear('a', '', nombreCategoria(producto.categoria));
  categoria.href = '#';
  categoria.addEventListener('click', () => alElegirCategoria(producto.categoria));
  const aqui = crear('li', '', producto.nombre);
  aqui.setAttribute('aria-current', 'page');
  [inicio, categoria].forEach((enlace) => { const li = crear('li'); li.append(enlace); lista.append(li); });
  lista.append(aqui);
  nav.append(lista);
  return nav;
}

/** Las condiciones son las reglas reales del sistema (RN-11, RN-12, RN-14) y el envío, que order-api no cobra. */
function condiciones(producto) {
  const lista = crear('ul', 'condiciones');
  lista.append(
    crear('li', '', 'Envío gratis.'),
    crear('li', '', `${REGLAS.porcentajeDescuento} % de descuento desde ${formatoPesos.format(REGLAS.umbralDescuento)} de subtotal con IVA incluido (RN-14).`),
    crear('li', '', `Máximo ${REGLAS.maximoUnidades} unidades por producto; con más no habrá stock (RN-12).`));
  if (estimar([{ cantidad: 1, precioUnitario: producto.precio }]).total > REGLAS.limitePago) {
    lista.append(crear('li', 'alerta',
      `Este producto supera por sí solo el límite de pago de ${formatoPesos.format(REGLAS.limitePago)}: el pago se rechazará (RN-11).`));
  }
  return lista;
}

/** Hasta cuatro productos: primero los de la misma categoría, luego el resto por cercanía de precio. */
function relacionados(producto) {
  const otros = CATALOGO.filter((p) => p.sku !== producto.sku);
  const misma = otros.filter((p) => p.categoria === producto.categoria);
  const resto = otros.filter((p) => p.categoria !== producto.categoria)
    .sort((a, b) => Math.abs(a.precio - producto.precio) - Math.abs(b.precio - producto.precio));
  const elegidos = [...misma, ...resto].slice(0, MAXIMO_RELACIONADOS);

  const seccion = crear('section', 'relacionados');
  seccion.setAttribute('aria-labelledby', 'titulo-relacionados');
  const titulo = crear('h3', '', 'También te puede interesar');
  titulo.id = 'titulo-relacionados';
  const lista = crear('ul', 'mini-vitrina');
  elegidos.forEach((p) => {
    const enlace = crear('a', 'mini-producto');
    enlace.href = enlaceProducto(p.sku);
    enlace.append(fotoProducto(p, 'foto-mini'), crear('span', 'nombre', p.nombre), crear('span', 'precio', formatoPesos.format(precioConIva(p.precio))));
    const li = crear('li');
    li.append(enlace);
    lista.append(li);
  });
  seccion.append(titulo, lista);
  return seccion;
}

export function mostrarDetalle(producto, { alElegirCategoria }) {
  const foto = fotoProducto(producto, 'foto-ficha');
  foto.loading = 'eager';

  const titulo = crear('h2', '', producto.nombre);
  titulo.id = 'titulo-detalle';
  titulo.tabIndex = -1;
  const caracteristicas = crear('ul', 'caracteristicas');
  producto.caracteristicas.forEach((texto) => caracteristicas.append(crear('li', '', texto)));
  const compra = crear('div', 'compra compra-detalle');
  const enCarrito = crear('p', 'en-carrito');
  const favorito = botonFavorito(producto, true);

  const info = crear('div', 'ficha-info');
  info.append(crear('p', 'sku', producto.sku), titulo, crear('p', 'precio-grande', formatoPesos.format(precioConIva(producto.precio))),
    crear('p', 'meta', 'IVA 19 % incluido'),
    crear('p', 'descripcion', producto.descripcion), caracteristicas, enCarrito, compra, favorito, condiciones(producto));
  const marco = crear('div', 'ficha-foto');
  marco.append(foto);
  const ficha = crear('div', 'ficha');
  ficha.append(marco, info);

  actual = { producto, compra, favorito, enCarrito };
  porId('detalle-producto').replaceChildren(migas(producto, alElegirCategoria), ficha, relacionados(producto));
  actualizarDetalle();
}

/** Repinta lo que cambia con el carrito y los favoritos. */
export function actualizarDetalle() {
  if (!actual) return;
  const { producto, compra, favorito, enCarrito } = actual;
  pintarCompra(producto, compra, { primario: true });
  pintarFavorito(favorito);
  const cantidad = cantidadDe(producto.sku);
  enCarrito.hidden = cantidad === 0;
  enCarrito.textContent = `En tu carrito: ${formatoNumero.format(cantidad)} ${cantidad === 1 ? 'unidad' : 'unidades'}`;
  enCarrito.classList.toggle('excede', cantidad > REGLAS.maximoUnidades);
}

export function cerrarDetalle() {
  actual = null;
}
