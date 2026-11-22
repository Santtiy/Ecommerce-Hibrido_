// Paso 1, catálogo: categorías, orden, resultado de la búsqueda y la vitrina de tarjetas. Las tarjetas se crean una
// vez; al cambiar el filtro solo se reordenan o se ocultan, y al cambiar el carrito o los favoritos solo se repinta
// su zona de compra y su corazón.

import { porId, crear } from '../dom.js';
import { REGLAS } from '../config.js';
import { formatoPesos, formatoNumero } from '../formato.js';
import { CATALOGO, CATEGORIAS, nombreCategoria, enlaceProducto } from './productos.js';
import { cantidadDe } from './carrito.js';
import { cantidadFavoritos } from './favoritos.js';
import { filtroActual, cambiarFiltro, productosVisibles, conteoPorCategoria } from './filtros.js';
import { fotoProducto, botonFavorito, pintarFavorito, pintarCompra } from './controles.js';

const tarjetas = new Map(); // sku → { producto, tarjeta, compra, aviso, favorito }

function crearTarjeta(producto) {
  const tarjeta = crear('article', 'producto');
  tarjeta.setAttribute('aria-labelledby', 'producto-' + producto.sku);

  // La foto y el nombre llevan a la página del producto; la foto no es otra parada del tabulador.
  const marco = crear('div', 'marco-foto');
  const enlaceFoto = crear('a', 'enlace-foto');
  enlaceFoto.href = enlaceProducto(producto.sku);
  enlaceFoto.tabIndex = -1;
  enlaceFoto.setAttribute('aria-hidden', 'true');
  enlaceFoto.append(fotoProducto(producto, 'foto-producto'));
  const favorito = botonFavorito(producto);
  marco.append(enlaceFoto, favorito);

  const titulo = crear('h3');
  titulo.id = 'producto-' + producto.sku;
  const enlace = crear('a', '', producto.nombre);
  enlace.href = enlaceProducto(producto.sku);
  titulo.append(enlace);

  const compra = crear('div', 'compra');
  const aviso = crear('p', 'aviso-stock', `Más de ${REGLAS.maximoUnidades} unidades: no habrá stock (RN-12).`);
  const cuerpo = crear('div', 'cuerpo-producto');
  cuerpo.append(crear('p', 'categoria-producto', nombreCategoria(producto.categoria)), titulo,
    crear('p', 'detalle', producto.detalle), crear('p', 'precio', formatoPesos.format(producto.precio)),
    crear('p', 'envio-producto', 'Envío gratis'), compra, aviso);
  tarjeta.append(marco, cuerpo);
  tarjetas.set(producto.sku, { producto, tarjeta, compra, aviso, favorito });
}

const categorias = new Map(); // id → { boton, conteo }

/** Botones de categoría: se crean una vez (así no pierden el foco al pulsarlos) y luego solo se actualizan. */
function crearCategorias() {
  [{ id: 'todas', nombre: 'Todas' }, ...CATEGORIAS].forEach(({ id, nombre }) => {
    const boton = crear('button', '', nombre + ' ');
    boton.type = 'button';
    const conteo = crear('span', 'conteo');
    boton.append(conteo);
    boton.addEventListener('click', () => cambiarFiltro({ categoria: id }));
    categorias.set(id, { boton, conteo });
  });
  porId('categorias').replaceChildren(...[...categorias.values()].map(({ boton }) => boton));
}

function pintarCategorias() {
  const conteo = conteoPorCategoria();
  const { categoria } = filtroActual();
  categorias.forEach(({ boton, conteo: numero }, id) => {
    boton.setAttribute('aria-pressed', String(id === categoria));
    numero.textContent = formatoNumero.format(conteo[id]);
  });
}

function pintarResultado(cantidad) {
  const { texto, categoria, soloFavoritos } = filtroActual();
  const n = formatoNumero.format(cantidad);
  const productos = cantidad === 1 ? 'producto' : 'productos';
  let resumen = `${n} ${productos}`;
  if (texto.trim()) resumen = `${n} ${cantidad === 1 ? 'resultado' : 'resultados'} para «${texto.trim()}»`;
  if (soloFavoritos) resumen += ' en tus favoritos';
  porId('resultado-busqueda').textContent = resumen;
  porId('limpiar-filtros').hidden = !(texto.trim() || categoria !== 'todas' || soloFavoritos);

  const vacio = cantidad === 0;
  porId('sin-resultados').hidden = !vacio;
  if (vacio) {
    porId('texto-sin-resultados').textContent = soloFavoritos && cantidadFavoritos() === 0
      ? 'Aún no tienes favoritos. Toca el corazón de un producto para guardarlo aquí.'
      : 'No encontramos productos con ese filtro. Prueba con otra palabra o mira todo el catálogo.';
  }
}

export function crearVitrina() {
  CATALOGO.forEach(crearTarjeta);
  crearCategorias();
  actualizarProductos();
  actualizarVitrina();
}

/** Solo cambia la lista cuando se busca, se ordena o se filtra. */
export function actualizarVitrina() {
  const visibles = productosVisibles();
  porId('vitrina').replaceChildren(...visibles.map(({ sku }) => tarjetas.get(sku).tarjeta));
  pintarCategorias();
  pintarResultado(visibles.length);
}

/** Actualiza cantidades y corazones sin retirar las tarjetas ni sus fotos del documento. */
export function actualizarProductos() {
  tarjetas.forEach(({ producto, compra, aviso, favorito }) => {
    pintarCompra(producto, compra);
    aviso.hidden = cantidadDe(producto.sku) <= REGLAS.maximoUnidades;
    pintarFavorito(favorito);
  });
}

/** Créditos de las fotos: autor, licencia y fuente de cada una (las CC BY y CC BY-SA lo exigen). */
export function pintarCreditos(lista) {
  lista.replaceChildren(...CATALOGO.map(({ nombre, foto }) => {
    const li = crear('li');
    const fuente = crear('a', '', 'Wikimedia Commons');
    fuente.href = foto.fuente;
    fuente.rel = 'noopener';
    fuente.target = '_blank';
    li.append(crear('strong', '', nombre), `: foto de ${foto.autor}, `);
    if (foto.urlLicencia) {
      const licencia = crear('a', '', foto.licencia);
      licencia.href = foto.urlLicencia;
      licencia.rel = 'license noopener';
      licencia.target = '_blank';
      li.append(licencia);
    } else {
      li.append(foto.licencia);
    }
    li.append(', vía ', fuente, '.');
    return li;
  }));
}
