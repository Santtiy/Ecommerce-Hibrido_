// Punto de entrada de la tienda: catálogo (búsqueda, categorías, orden, favoritos y página de producto), pasos de la
// compra (productos → confirmar → seguimiento), escenarios de la exposición, envío a order-api y seguimiento por
// WebSocket. La página de producto tiene su propia dirección (#producto/SKU), así el botón Atrás vuelve al catálogo.

import { porId } from '../dom.js';
import { URL_API } from '../config.js';
import { enviarOrden } from '../api.js';
import { conectarCanal } from '../canal.js';
import { guardarEnviada } from '../registro.js';
import { guardarPeticion } from '../peticiones.js';
import { icono } from '../iconos.js';
import { buscarProducto } from './productos.js';
import { alCambiarCarrito, carritoVacio, itemsDeLaOrden, vaciarCarrito } from './carrito.js';
import { alCambiarFavoritos, cantidadFavoritos } from './favoritos.js';
import { alCambiarFiltro, cambiarFiltro, filtroActual, quitarFiltros } from './filtros.js';
import { crearVitrina, actualizarVitrina, actualizarProductos, pintarCreditos } from './vitrina.js';
import { mostrarDetalle, actualizarDetalle, cerrarDetalle } from './detalle.js';
import { configurarAviso } from './controles.js';
import { configurarEscenarios } from './escenarios.js';
import { pintarCarrito } from './panel-carrito.js';
import { pintarResumen, leerCliente, mostrarRechazo, ocultarRechazo } from './confirmar.js';
import { actualizarProgresos, mostrarPedido, hayPedidos, mostrarConexion, pintarSeguimiento } from './seguimiento.js';

const PASOS = ['productos', 'confirmar', 'seguimiento'];
let pasoActual = 'productos';
let scrollDelCatalogo = 0;

// ---------- Pasos ----------
function pasoDisponible(paso) {
  if (paso === 'confirmar') return !carritoVacio() || !porId('rechazo').hidden;
  if (paso === 'seguimiento') return hayPedidos();
  return true;
}

function pintarPasos() {
  document.querySelector('.saltar-contenido').href = '#titulo-' + pasoActual;
  document.querySelectorAll('[data-paso]').forEach((boton) => {
    const paso = boton.dataset.paso;
    boton.disabled = !pasoDisponible(paso);
    if (paso === pasoActual) boton.setAttribute('aria-current', 'step'); else boton.removeAttribute('aria-current');
    boton.classList.toggle('hecho', PASOS.indexOf(paso) < PASOS.indexOf(pasoActual));
  });
  porId('barra-movil').hidden = pasoActual !== 'productos' || carritoVacio();
}

/** Muestra un paso y lleva el foco a su título, como haría una app al cambiar de pantalla. */
function ir(paso, { enfocar = true } = {}) {
  if (paso !== 'productos' && skuDeLaRuta()) { quitarRuta(); aplicarRuta(); } // al volver a Productos se ve el catálogo
  pasoActual = paso;
  PASOS.forEach((p) => { porId('paso-' + p).hidden = p !== paso; });
  if (paso === 'confirmar') pintarResumen();
  if (paso === 'seguimiento') pintarSeguimiento();
  pintarPasos();
  window.scrollTo({ top: 0 });
  if (enfocar) porId('titulo-' + paso).focus({ preventScroll: true });
}

// ---------- Catálogo y página de producto (#producto/SKU) ----------
function skuDeLaRuta() {
  const coincidencia = window.location.hash.match(/^#producto\/([\w-]+)$/);
  return coincidencia ? coincidencia[1] : null;
}

function quitarRuta() {
  history.replaceState(null, '', window.location.pathname + window.location.search);
}

function aplicarRuta() {
  const producto = buscarProducto(skuDeLaRuta() ?? '');
  const enDetalle = !porId('detalle-producto').hidden;
  if (producto) {
    if (!enDetalle) scrollDelCatalogo = window.scrollY;
    if (pasoActual !== 'productos') ir('productos', { enfocar: false });
    porId('vista-catalogo').hidden = true;
    porId('promociones').hidden = true;
    porId('detalle-producto').hidden = false;
    mostrarDetalle(producto, { alElegirCategoria: (categoria) => cambiarFiltro({ categoria, texto: '', soloFavoritos: false }) });
    window.scrollTo({ top: 0 });
    porId('titulo-detalle').focus({ preventScroll: true });
  } else {
    cerrarDetalle();
    porId('detalle-producto').hidden = true;
    porId('vista-catalogo').hidden = false;
    pintarPromociones();
    if (enDetalle) window.scrollTo({ top: scrollDelCatalogo });
  }
}

/** Lleva al catálogo (sale de la página de producto y del paso en que esté). */
function verCatalogo() {
  if (skuDeLaRuta()) { quitarRuta(); aplicarRuta(); }
  if (pasoActual !== 'productos') ir('productos', { enfocar: false });
}

function pintarEncabezado() {
  const { texto, soloFavoritos } = filtroActual();
  const favoritos = cantidadFavoritos();
  if (porId('buscar').value !== texto) porId('buscar').value = texto;
  porId('ver-favoritos').setAttribute('aria-pressed', String(soloFavoritos));
  porId('cuenta-favoritos').textContent = String(favoritos);
  porId('ver-favoritos').setAttribute('aria-label', `Ver favoritos: ${favoritos} guardados`);
}

// Las promociones acompañan la entrada al catálogo; buscar o filtrar deja los productos en primer plano.
function pintarPromociones() {
  const { texto, categoria, soloFavoritos } = filtroActual();
  porId('promociones').hidden = Boolean(skuDeLaRuta() || texto.trim() || categoria !== 'todas' || soloFavoritos);
}

function enfocarCatalogo() {
  porId('titulo-productos').scrollIntoView({ block: 'start' });
  porId('titulo-productos').focus({ preventScroll: true });
}

// ---------- Envío a order-api ----------
async function enviar(orden) {
  const boton = porId('confirmar');
  boton.disabled = true;
  boton.textContent = 'Enviando…';
  ocultarRechazo();
  try {
    const { status, ok, cuerpo } = await enviarOrden(orden);
    guardarPeticion({ url: URL_API, enviado: orden, status, recibido: cuerpo });
    if (status === 202 && cuerpo?.orderId) {
      guardarEnviada(cuerpo.orderId, {
        cliente: orden.cliente, total: cuerpo.total, moneda: cuerpo.moneda,
        productos: orden.items.map(({ sku, nombre, cantidad }) => ({ sku, nombre, cantidad })),
      });
      vaciarCarrito();
      mostrarPedido(cuerpo.orderId);
      ir('seguimiento');
      return;
    }
    if (!ok) mostrarRechazo(orden, { status, cuerpo });
  } catch (error) {
    guardarPeticion({ url: URL_API, enviado: orden, error: error.message });
    mostrarRechazo(orden, { error: error.message });
  } finally {
    boton.textContent = 'Confirmar compra';
    boton.disabled = carritoVacio();
  }
  ir('confirmar');
}

function irAlCarrito() {
  ir('productos', { enfocar: false });
  porId('titulo-carrito').focus();
}

// ---------- Encabezado: búsqueda, favoritos y carrito ----------
porId('buscar').addEventListener('input', () => {
  verCatalogo();
  cambiarFiltro({ texto: porId('buscar').value });
});
porId('form-buscar').addEventListener('submit', (evento) => {
  evento.preventDefault();
  verCatalogo();
  porId('vitrina').querySelector('a')?.focus();
});
porId('ver-favoritos').addEventListener('click', () => {
  verCatalogo();
  cambiarFiltro({ soloFavoritos: !filtroActual().soloFavoritos });
});
porId('ir-carrito').addEventListener('click', irAlCarrito);
porId('orden').addEventListener('change', () => cambiarFiltro({ orden: porId('orden').value }));
porId('ver-todo').addEventListener('click', () => quitarFiltros());
porId('limpiar-filtros').addEventListener('click', () => quitarFiltros());
porId('explorar-productos').addEventListener('click', enfocarCatalogo);
porId('volver-catalogo').addEventListener('click', () => {
  verCatalogo();
  quitarFiltros();
  enfocarCatalogo();
});
window.addEventListener('hashchange', aplicarRuta);

// ---------- Pasos y compra ----------
document.querySelectorAll('[data-paso]').forEach((boton) => boton.addEventListener('click', () => {
  if (boton.dataset.paso === 'productos') verCatalogo();
  ir(boton.dataset.paso);
}));
document.querySelectorAll('[data-ir]').forEach((boton) => boton.addEventListener('click', () => ir(boton.dataset.ir)));
porId('continuar').addEventListener('click', () => ir('confirmar'));
porId('continuar-movil').addEventListener('click', () => ir('confirmar'));
porId('form-confirmar').addEventListener('submit', (evento) => {
  evento.preventDefault();
  const cliente = leerCliente();
  if (cliente && !carritoVacio()) enviar({ cliente, items: itemsDeLaOrden() });
});
porId('otro-pedido').addEventListener('click', () => { verCatalogo(); ir('productos'); });

// ---------- Estado compartido ----------
alCambiarCarrito(() => {
  actualizarProductos();
  actualizarDetalle();
  pintarCarrito();
  if (pasoActual === 'confirmar') pintarResumen();
  pintarPasos();
});
alCambiarFavoritos(() => {
  actualizarProductos();
  if (filtroActual().soloFavoritos) actualizarVitrina();
  actualizarDetalle();
  pintarEncabezado();
});
alCambiarFiltro(() => {
  actualizarVitrina();
  pintarEncabezado();
  pintarPromociones();
});

porId('ir-carrito').prepend(icono('carrito'));
porId('ver-favoritos').prepend(icono('corazon'));
document.querySelectorAll('[data-icono]').forEach((elemento) => elemento.append(icono(elemento.dataset.icono)));
configurarAviso({ verCarrito: irAlCarrito });
configurarEscenarios({ alConfirmar: () => ir('confirmar'), alEnviar: enviar });
crearVitrina();
pintarCreditos(porId('lista-creditos'));
pintarCarrito();
pintarEncabezado();
pintarPasos();
aplicarRuta();
conectarCanal({
  alCambiarConexion: mostrarConexion,
  alRecibir: (cambios) => {
    actualizarProgresos(cambios);
    pintarPasos();
  },
});
