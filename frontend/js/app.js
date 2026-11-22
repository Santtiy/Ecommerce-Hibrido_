// Punto de entrada: une la configuración, el cliente HTTP, el canal WebSocket y las vistas.

import { EJEMPLOS, CLIENTE_EJEMPLO } from './config.js';
import { enviarOrden } from './api.js';
import { conectarCanal } from './canal.js';
import { guardarEnviada } from './registro.js';
import * as tablero from './tablero.js';
import { pintarResumen } from './resumen.js';
import { anadirFila, leerOrden, rellenar, validarFormulario } from './formulario.js';
import { mostrarEnviando, mostrarResultado, mostrarSinConexion } from './respuesta.js';

const $ = (id) => document.getElementById(id);
let filtroActual = 'todas';

const refrescar = () => pintarResumen(tablero.refrescar());

/** validar: false solo para los ejemplos inválidos, que existen para ver la validación del servidor (400). */
async function enviar({ validar = true } = {}) {
  if (validar && !validarFormulario()) return;
  const orden = leerOrden();
  const boton = $('enviar');
  boton.disabled = true;
  boton.textContent = 'Enviando…';
  mostrarEnviando();
  try {
    const respuesta = await enviarOrden(orden);
    mostrarResultado(orden, respuesta, { alVerOrden: verOrden });
    if (respuesta.status === 202 && respuesta.cuerpo?.orderId) {
      const { orderId, total, moneda } = respuesta.cuerpo;
      guardarEnviada(orderId, { cliente: orden.cliente, total, moneda });
      tablero.registrarEnviada(orderId);
      refrescar();
    }
  } catch (error) {
    mostrarSinConexion(error);
  } finally {
    boton.disabled = false;
    boton.textContent = 'Enviar orden';
  }
}

function establecerFiltro(resultado, texto = $('buscar').value) {
  filtroActual = resultado;
  document.querySelectorAll('[data-filtro]').forEach((boton) => {
    boton.setAttribute('aria-pressed', String(boton.dataset.filtro === resultado));
  });
  $('buscar').value = texto;
  tablero.filtrar({ resultado, texto });
}

function verOrden(orderId) {
  if (!tablero.estaVisible(orderId)) establecerFiltro('todas', '');
  tablero.irAOrden(orderId);
}

document.querySelectorAll('[data-ejemplo]').forEach((boton) => {
  boton.addEventListener('click', () => {
    const ejemplo = EJEMPLOS[boton.dataset.ejemplo];
    rellenar(CLIENTE_EJEMPLO, ejemplo.items);
    if (ejemplo.enviar) enviar({ validar: false });
  });
});

document.querySelectorAll('[data-filtro]').forEach((boton) => {
  boton.addEventListener('click', () => {
    const elegido = boton.dataset.filtro;
    establecerFiltro(elegido === filtroActual ? 'todas' : elegido);
  });
});

$('buscar').addEventListener('input', () => establecerFiltro(filtroActual));
$('formulario').addEventListener('submit', (evento) => { evento.preventDefault(); enviar(); });
$('anadir-fila').addEventListener('click', () => anadirFila().querySelector('.sku').focus());

anadirFila();
refrescar();
conectarCanal({
  alCambiarConexion: tablero.mostrarConexion,
  alRecibir: (cambios, inicial) => {
    for (const [orderId, progreso] of Object.entries(cambios)) tablero.actualizarOrden(orderId, progreso, inicial);
    tablero.anotarActualizacion();
    refrescar();
  },
});
