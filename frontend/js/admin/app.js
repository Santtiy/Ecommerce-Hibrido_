// Punto de entrada del panel de administración: órdenes en vivo (WebSocket), filtros, recorrido de la orden elegida y
// la última petición que hizo la tienda. Las órdenes nuevas y las peticiones llegan desde la pestaña de la tienda por
// el evento 'storage' (mismo origen, mismo localStorage).

import { porId } from '../dom.js';
import { conectarCanal } from '../canal.js';
import { escucharEnviadas } from '../registro.js';
import { leerPeticion, escucharPeticiones } from '../peticiones.js';
import * as tablero from './tablero.js';
import { pintarResumen } from './resumen.js';
import { pintarPeticion } from './peticion.js';
import { pintarRecorrido } from './recorrido.js';

let filtroActual = 'todas';
let ordenPendiente = ordenDelEnlace(); // la del enlace #orden=… se elige en cuanto aparece en la tabla

const refrescar = () => pintarResumen(tablero.refrescar());

function ordenDelEnlace() {
  const coincidencia = window.location.hash.match(/^#orden=([\w-]+)$/);
  return coincidencia ? coincidencia[1] : null;
}

function establecerFiltro(resultado, texto = porId('buscar').value) {
  filtroActual = resultado;
  document.querySelectorAll('[data-filtro]').forEach((boton) => {
    boton.setAttribute('aria-pressed', String(boton.dataset.filtro === resultado));
  });
  porId('buscar').value = texto;
  tablero.filtrar({ resultado, texto });
}

function verOrden(orderId) {
  if (!tablero.existeOrden(orderId)) {
    ordenPendiente = orderId;
    return;
  }
  if (!tablero.estaVisible(orderId)) establecerFiltro('todas', '');
  tablero.seleccionarOrden(orderId);
  tablero.irAOrden(orderId);
}

const mostrarPeticion = (peticion) => pintarPeticion(peticion, { alVerOrden: verOrden });

document.querySelectorAll('[data-filtro]').forEach((boton) => {
  boton.addEventListener('click', () => {
    const elegido = boton.dataset.filtro;
    establecerFiltro(elegido === filtroActual ? 'todas' : elegido);
  });
});
porId('buscar').addEventListener('input', () => establecerFiltro(filtroActual));
window.addEventListener('hashchange', () => {
  const orderId = ordenDelEnlace();
  if (orderId) verOrden(orderId);
});

tablero.alSeleccionarOrden((orderId) => pintarRecorrido(orderId, tablero.progresoDe(orderId)));
function elegirPendienteSiYaLlego() {
  if (ordenPendiente && tablero.existeOrden(ordenPendiente)) {
    const orderId = ordenPendiente;
    ordenPendiente = null;
    verOrden(orderId);
  }
}

escucharEnviadas((nuevas) => {
  nuevas.forEach((orderId) => tablero.registrarEnviada(orderId));
  refrescar();
  elegirPendienteSiYaLlego();
});
escucharPeticiones(mostrarPeticion);

mostrarPeticion(leerPeticion());
pintarRecorrido(null);
refrescar();
conectarCanal({
  alCambiarConexion: tablero.mostrarConexion,
  alRecibir: (cambios, inicial) => {
    for (const [orderId, progreso] of Object.entries(cambios)) tablero.actualizarOrden(orderId, progreso, inicial);
    tablero.anotarActualizacion();
    refrescar();
    elegirPendienteSiYaLlego();
  },
});
