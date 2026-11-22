// Panel de administración, tabla de órdenes en vivo: indicador de conexión, filas incrementales, filtro por resultado,
// búsqueda y selección de una orden (para ver su recorrido).

import { porId, crear } from '../dom.js';
import { ETAPAS, URL_WS, SEGUNDOS_REINTENTO, MAXIMO_ORDENES, PROGRESO_PENDIENTE } from '../config.js';
import { claseEstado, resultadoOrden, etapasTerminadas, NOMBRE_RESULTADO } from '../estados.js';
import { datosEnviada } from '../registro.js';
import { formatoHora, formatoPesos, formatoNumero } from '../formato.js';

const COLUMNA_RESULTADO = ETAPAS.length + 1;

// orderId → { fila, progreso, resultado }
const ordenes = new Map();
const filtro = { resultado: 'todas', texto: '' };
let seleccionada = null;
let alSeleccionar = () => {};

export function alSeleccionarOrden(oyente) {
  alSeleccionar = oyente;
}

function crearFila(orderId) {
  const fila = crear('tr');
  fila.dataset.orden = orderId;
  fila.addEventListener('click', () => seleccionarOrden(orderId));
  const celdaOrden = crear('td', 'col-orden');
  const id = crear('button', 'id', orderId.slice(0, 8));
  id.type = 'button';
  id.title = `Ver el recorrido de ${orderId}`;
  celdaOrden.append(id, crear('span', 'extra'));
  const etapas = ETAPAS.map((etapa) => {
    const celda = crear('td', 'col-etapa');
    celda.dataset.etiqueta = etapa.nombre;
    celda.title = etapa.worker;
    return celda;
  });
  // Resultado y, debajo, la hora del último cambio.
  const celdaResultado = crear('td', 'col-resultado');
  celdaResultado.append(crear('span', 'resultado'), crear('span', 'hora'));
  fila.append(celdaOrden, ...etapas, celdaResultado);
  porId('filas').prepend(fila);
  return fila;
}

function pintarCliente(fila, orderId) {
  const datos = datosEnviada(orderId);
  fila.cells[0].querySelector('.extra').textContent = datos ? `${datos.cliente} · ${formatoPesos.format(datos.total)}` : '';
}

function pintarEtapa(celda, estado) {
  if (celda.dataset.valor === estado) return false;
  celda.dataset.valor = estado;
  celda.replaceChildren(crear('span', 'estado ' + claseEstado(estado), estado));
  return true;
}

function pintarResultado(celda, progreso, resultado) {
  const texto = celda.querySelector('.resultado');
  texto.className = 'resultado ' + resultado;
  texto.textContent = resultado === 'proceso'
    ? `${NOMBRE_RESULTADO.proceso} (${etapasTerminadas(progreso)} de ${ETAPAS.length})`
    : NOMBRE_RESULTADO[resultado];
}

function destacar(fila) {
  fila.classList.add('destacada');
  void fila.offsetWidth;
  requestAnimationFrame(() => fila.classList.remove('destacada'));
}

function coincide(orderId, entrada) {
  if (filtro.resultado !== 'todas' && entrada.resultado !== filtro.resultado) return false;
  if (!filtro.texto) return true;
  const cliente = datosEnviada(orderId)?.cliente ?? '';
  return orderId.toLowerCase().includes(filtro.texto) || cliente.toLowerCase().includes(filtro.texto);
}

/**
 * Aplica un cambio que llegó por el WebSocket.
 * inicial: true en el primer mensaje tras conectar (el estado completo): no tiene hora real ni se destaca.
 */
export function actualizarOrden(orderId, progreso, inicial = false) {
  let entrada = ordenes.get(orderId);
  if (!entrada) {
    if (ordenes.size >= MAXIMO_ORDENES) quitarPrimeraOrden();
    entrada = { fila: crearFila(orderId) };
    ordenes.set(orderId, entrada);
    pintarCliente(entrada.fila, orderId);
  }
  const { fila } = entrada;
  const cambios = ETAPAS.map((etapa, i) => pintarEtapa(fila.cells[i + 1], progreso[etapa.campo] ?? 'pending'));
  entrada.progreso = progreso;
  entrada.resultado = resultadoOrden(progreso);
  fila.dataset.resultado = entrada.resultado;
  if (cambios.some(Boolean)) {
    const celda = fila.cells[COLUMNA_RESULTADO];
    pintarResultado(celda, progreso, entrada.resultado);
    const hora = celda.querySelector('.hora');
    if (inicial) {
      hora.textContent = 'al conectar';
      hora.title = 'Estado recibido al abrir el panel; la hora real del cambio no se conoce';
    } else {
      hora.textContent = formatoHora.format(new Date());
      hora.title = '';
      destacar(fila);
    }
    if (orderId === seleccionada) alSeleccionar(orderId);
  }
  fila.hidden = !coincide(orderId, entrada);
}

function quitarPrimeraOrden() {
  const orderId = ordenes.keys().next().value;
  ordenes.get(orderId).fila.remove();
  ordenes.delete(orderId);
  if (orderId === seleccionada) {
    seleccionada = null;
    alSeleccionar(null);
  }
}

/** Orden recién aceptada por order-api (202): aparece al instante, aunque los workers aún no hayan respondido. */
export function registrarEnviada(orderId) {
  const entrada = ordenes.get(orderId);
  if (!entrada) return actualizarOrden(orderId, PROGRESO_PENDIENTE);
  pintarCliente(entrada.fila, orderId);
  entrada.fila.hidden = !coincide(orderId, entrada);
  if (orderId === seleccionada) alSeleccionar(orderId);
}

export function existeOrden(orderId) {
  return ordenes.has(orderId);
}

export function progresoDe(orderId) {
  return ordenes.get(orderId)?.progreso ?? null;
}

/** Marca la fila y avisa para pintar su recorrido. Devuelve false si la orden aún no está en la tabla. */
export function seleccionarOrden(orderId) {
  if (!ordenes.has(orderId)) return false;
  ordenes.get(seleccionada)?.fila.removeAttribute('aria-selected');
  seleccionada = orderId;
  ordenes.get(orderId).fila.setAttribute('aria-selected', 'true');
  alSeleccionar(orderId);
  return true;
}

export function filtrar({ resultado, texto }) {
  filtro.resultado = resultado;
  filtro.texto = texto.trim().toLowerCase();
  for (const [orderId, entrada] of ordenes) entrada.fila.hidden = !coincide(orderId, entrada);
  pintarMostrando();
}

export function estaVisible(orderId) {
  const entrada = ordenes.get(orderId);
  return Boolean(entrada) && !entrada.fila.hidden;
}

export function irAOrden(orderId) {
  const fila = ordenes.get(orderId)?.fila;
  if (!fila) return;
  const suave = !window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  fila.scrollIntoView({ behavior: suave ? 'smooth' : 'auto', block: 'center' });
  destacar(fila);
}

function pintarMostrando() {
  const total = ordenes.size;
  let visibles = 0;
  for (const entrada of ordenes.values()) if (!entrada.fila.hidden) visibles++;
  porId('sin-ordenes').hidden = visibles > 0;
  porId('sin-ordenes').textContent = total === 0
    ? 'Todavía no hay órdenes. Haz una compra en la tienda o usa un escenario de la exposición.'
    : 'Ninguna orden coincide con el filtro o la búsqueda.';
  porId('mostrando').textContent = visibles === total ? ''
    : `Mostrando ${formatoNumero.format(visibles)} de ${formatoNumero.format(total)}`;
}

/** Actualiza el pie de la tabla y devuelve los conteos para el resumen. */
export function refrescar() {
  pintarMostrando();
  const conteos = { todas: ordenes.size, completada: 0, fallo: 0, proceso: 0, falloPago: 0, falloStock: 0 };
  for (const { progreso, resultado } of ordenes.values()) {
    conteos[resultado]++;
    if (claseEstado(progreso.payment) === 'fallo') conteos.falloPago++;
    if (claseEstado(progreso.stock) === 'fallo') conteos.falloStock++;
  }
  return conteos;
}

export function mostrarConexion(conectado) {
  const conexion = porId('conexion');
  conexion.classList.toggle('desconectado', !conectado);
  conexion.textContent = conectado
    ? 'Conectado al dashboard'
    : `Sin conexión con el dashboard. Se reintenta cada ${SEGUNDOS_REINTENTO} s.`;
  conexion.title = conectado ? URL_WS : '';
}

/** Cada mensaje del WebSocket deja la hora a la vista: es la prueba de que el panel está al día. */
export function anotarActualizacion() {
  porId('conexion').textContent = 'Conectado. Última actualización: ' + formatoHora.format(new Date());
}
