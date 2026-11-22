// Paso 3, seguimiento: el pedido actual con sus tres etapas en vivo y la lista de pedidos hechos desde este navegador.
// El progreso llega por el WebSocket del dashboard; aquí solo se pintan los pedidos propios.

import { porId, crear } from '../dom.js';
import { ETAPAS, PAGINA_ADMIN, REGLAS, MAXIMO_ORDENES, PROGRESO_PENDIENTE } from '../config.js';
import { claseEstado, resultadoOrden } from '../estados.js';
import { datosEnviada, enviadasRecientes } from '../registro.js';
import { formatoPesos, formatoNumero, formatoHora } from '../formato.js';
import { icono } from '../iconos.js';
import { buscarProducto } from './productos.js';
import { fotoProducto } from './controles.js';

// Cómo lo lee el cliente; el estado técnico (PAYMENT_SUCCESS…) se muestra al lado para la exposición.
const TEXTOS = {
  payment: { pendiente: 'Procesando el pago', ok: 'Pago aprobado', fallo: 'Pago rechazado',
    motivo: `El total supera ${formatoPesos.format(REGLAS.limitePago)} (RN-11).` },
  stock: { pendiente: 'Reservando los productos', ok: 'Productos reservados', fallo: 'Sin stock',
    motivo: `Algún producto pide más de ${REGLAS.maximoUnidades} unidades (RN-12).` },
  email: { pendiente: 'Enviando el correo de confirmación', ok: 'Correo de confirmación enviado', fallo: 'No se envió el correo',
    motivo: '' },
};
const TITULOS = { proceso: 'Estamos procesando tu pedido', completada: 'Pedido completado', fallo: 'El pedido tuvo un problema' };
const ETIQUETAS = { proceso: 'En proceso', completada: 'Completado', fallo: 'Con problemas' };

const progresos = new Map(); // orderId → { payment, stock, email }
let actual = null;

const progresoDe = (orderId) => progresos.get(orderId) ?? PROGRESO_PENDIENTE;

/** Cambios del WebSocket: { orderId: { payment, stock, email } }. Solo repinta si tocan un pedido propio. */
export function actualizarProgresos(cambios) {
  let propio = false;
  for (const [orderId, progreso] of Object.entries(cambios)) {
    progresos.set(orderId, progreso);
    if (progresos.size > MAXIMO_ORDENES) progresos.delete(progresos.keys().next().value);
    if (datosEnviada(orderId)) propio = true;
  }
  if (propio) pintarSeguimiento();
}

export function mostrarPedido(orderId) {
  actual = orderId;
  pintarSeguimiento();
}

export function hayPedidos() {
  return enviadasRecientes().length > 0;
}

export function mostrarConexion(conectado) {
  porId('aviso-conexion').hidden = conectado;
}

function etapa({ campo, worker }, progreso) {
  const estado = progreso[campo] ?? 'pending';
  const clase = claseEstado(estado);
  const textos = TEXTOS[campo];
  const li = crear('li', 'etapa ' + clase);
  const marca = crear('span', 'marca');
  if (clase === 'ok') marca.append(icono('hecho'));
  if (clase === 'fallo') marca.append(icono('quitar'));
  const cuerpo = crear('div');
  cuerpo.append(crear('p', 'etapa-titulo', textos[clase]));
  if (clase === 'fallo' && textos.motivo) cuerpo.append(crear('p', 'etapa-motivo', textos.motivo));
  const tecnico = crear('p', 'etapa-tecnico');
  tecnico.append(crear('span', '', worker + ' · '), crear('code', '', estado));
  cuerpo.append(tecnico);
  li.append(marca, cuerpo);
  return li;
}

function pintarActual() {
  const contenedor = porId('pedido-actual');
  const datos = actual ? datosEnviada(actual) : null;
  if (!datos) {
    contenedor.replaceChildren(crear('p', 'vacio', 'Aún no has hecho ningún pedido.'));
    return;
  }
  const progreso = progresoDe(actual);
  const resultado = resultadoOrden(progreso);
  const unidades = (datos.productos ?? []).reduce((suma, p) => suma + p.cantidad, 0);

  const cabecera = crear('div', 'cabecera-pedido');
  const numero = crear('p', 'numero-pedido');
  numero.append(crear('span', '', 'Pedido '), crear('code', '', actual.slice(0, 8)));
  numero.title = actual;
  cabecera.append(numero, crear('p', 'meta',
    `${datos.cliente} · ${formatoNumero.format(unidades)} ${unidades === 1 ? 'unidad' : 'unidades'} · recibido a las ${formatoHora.format(new Date(datos.enviadaEn))}`));

  const titulo = crear('p', 'titulo-estado ' + resultado, TITULOS[resultado]);
  const etapas = crear('ol', 'etapas');
  etapas.append(...ETAPAS.map((e) => etapa(e, progreso)));

  // Productos del pedido con su foto (los pedidos guardados antes de tener sku se muestran sin foto).
  const productos = crear('ul', 'productos-pedido');
  (datos.productos ?? []).forEach(({ sku, nombre, cantidad }) => {
    const li = crear('li');
    const producto = sku ? buscarProducto(sku) : null;
    if (producto) li.append(fotoProducto(producto, 'miniatura'));
    li.append(crear('span', 'nombre', nombre), crear('span', 'cantidad', '× ' + formatoNumero.format(cantidad)));
    productos.append(li);
  });

  const total = crear('p', 'total-pedido');
  total.append(crear('span', '', 'Total cobrado'), crear('strong', '', `${formatoPesos.format(datos.total)} ${datos.moneda ?? ''}`));

  const admin = crear('a', 'boton-secundario', 'Ver su recorrido en el panel de administración');
  admin.href = `${PAGINA_ADMIN}#orden=${actual}`;
  admin.target = 'panel-admin';
  const acciones = crear('div', 'acciones');
  acciones.append(admin);

  contenedor.replaceChildren(cabecera, titulo, etapas, productos, total, acciones);
}

function pintarLista() {
  const lista = porId('mis-pedidos');
  const pedidos = enviadasRecientes();
  porId('sin-pedidos').hidden = pedidos.length > 0;
  lista.replaceChildren(...pedidos.map((pedido) => {
    const resultado = resultadoOrden(progresoDe(pedido.orderId));
    const boton = crear('button', 'pedido');
    boton.type = 'button';
    if (pedido.orderId === actual) boton.setAttribute('aria-current', 'true');
    boton.addEventListener('click', () => mostrarPedido(pedido.orderId));
    boton.append(
      crear('code', 'id', pedido.orderId.slice(0, 8)),
      crear('span', 'hora', formatoHora.format(new Date(pedido.enviadaEn))),
      crear('span', 'importe', formatoPesos.format(pedido.total)),
      crear('span', 'etiqueta ' + resultado, ETIQUETAS[resultado]));
    const li = crear('li');
    li.append(boton);
    return li;
  }));
}

export function pintarSeguimiento() {
  if (!actual) actual = enviadasRecientes()[0]?.orderId ?? null;
  pintarActual();
  pintarLista();
}
