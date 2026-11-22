// Panel de administración, "Recorrido de la orden": el camino de una orden por la arquitectura, salto a salto, con el
// estado real de cada worker. Lo anterior a los workers se da por hecho en cuanto llega su primer resultado
// (si un worker respondió, la orden ya pasó por Kafka, order-processor y RabbitMQ).

import { porId, crear } from '../dom.js';
import { ETAPAS } from '../config.js';
import { claseEstado } from '../estados.js';
import { datosEnviada } from '../registro.js';
import { formatoPesos } from '../formato.js';
import { icono } from '../iconos.js';


const RAMAS = {
  payment: { cola: 'billing_queue', topico: 'billing_events', hace: 'Aplica RN-11: rechaza el pago si el total supera 1.000.000.' },
  stock: { cola: 'inventory_queue', topico: 'inventory_events', hace: 'Aplica RN-12: sin stock si un ítem pide más de 10 unidades.' },
  email: { cola: 'notification_queue', topico: 'notification_events', hace: 'Envía el correo de confirmación.' },
};

/** Texto con fragmentos de código: partes = ['Lee ', ['billing_queue'], ' y publica…']. */
function texto(clase, partes, etiqueta = 'p') {
  const p = crear(etiqueta, clase);
  partes.forEach((parte) => p.append(Array.isArray(parte) ? crear('code', '', parte[0]) : parte));
  return p;
}

function marca(estado) {
  const span = crear('span', 'marca');
  if (estado === 'hecho') span.append(icono('hecho'));
  if (estado === 'fallo') span.append(icono('quitar'));
  return span;
}

function salto(estado, servicio, canal, que) {
  const li = crear('li', 'salto ' + estado);
  const cuerpo = crear('div');
  const titulo = crear('p', 'servicio', servicio);
  if (canal) titulo.append(' ', texto('canal', canal, 'span'));
  cuerpo.append(titulo, texto('que', que));
  li.append(marca(estado), cuerpo);
  return { li, cuerpo };
}

function ramas(progreso) {
  const lista = crear('ul', 'ramas');
  ETAPAS.forEach(({ campo, worker }) => {
    const estado = progreso[campo] ?? 'pending';
    const { cola, topico, hace } = RAMAS[campo];
    const li = crear('li', 'rama ' + claseEstado(estado));
    const cabecera = crear('p', 'servicio', worker + ' ');
    cabecera.append(crear('span', 'estado ' + claseEstado(estado), estado));
    li.append(cabecera, texto('que', ['Lee ', [cola], ' y publica en ', [topico], '. ' + hace]));
    lista.append(li);
  });
  return lista;
}

/** Pinta el recorrido de orderId con su progreso { payment, stock, email }; sin orden, el estado vacío. */
export function pintarRecorrido(orderId, progreso) {
  const contenido = porId('contenido-recorrido');
  if (!orderId || !progreso) {
    contenido.replaceChildren(crear('p', 'vacio', 'Elige una orden de la tabla para ver por dónde pasó y qué decidió cada worker.'));
    return;
  }
  const clases = ETAPAS.map(({ campo }) => claseEstado(progreso[campo]));
  const empezo = clases.some((clase) => clase !== 'pendiente');
  const previo = empezo ? 'hecho' : 'en-curso';
  const terminaron = clases.every((clase) => clase !== 'pendiente');
  const pago = claseEstado(progreso.payment);
  let textoPago = ['Audita cada resultado; la orden sigue ', ['CREADA'], ' hasta que llegue el del pago.'];
  if (pago === 'ok') textoPago = ['Audita los resultados y aplica el del pago: la orden queda ', ['PAGADA'], ' en PostgreSQL.'];
  if (pago === 'fallo') textoPago = ['Audita los resultados y aplica el del pago: la orden queda ', ['CANCELADA'], ' en PostgreSQL.'];

  const datos = datosEnviada(orderId);
  const meta = crear('p', 'meta');
  meta.append(crear('code', 'id-completo', orderId));
  if (datos) meta.append(crear('span', '', ` · ${datos.cliente} · ${formatoPesos.format(datos.total)}`));

  const lista = crear('ol', 'recorrido');
  lista.append(
    salto('hecho', 'order-api', null,
      ['Valida la orden, la guarda en PostgreSQL junto con su evento en la bandeja de salida y responde ', ['202'], '.']).li,
    salto(previo, 'Kafka', ['topic ', ['new_orders']],
      ['La bandeja de salida publica el evento ', ['OrdenCreada'], '. También lo lee analytics-service para sumar las ventas.']).li,
    salto(previo, 'order-processor', null, ['Traduce el evento en el comando ', ['ProcesarOrden'], '.']).li,
    salto(previo, 'RabbitMQ', ['exchange fanout ', ['order_tasks']],
      ['Copia el comando a las tres colas: los workers trabajan a la vez.']).li);

  const paralelo = salto(terminaron ? 'hecho' : 'en-curso', 'Los tres workers', null,
    ['Cada uno procesa su copia y publica su resultado en Kafka.']);
  paralelo.cuerpo.append(ramas(progreso));
  lista.append(paralelo.li);

  lista.append(
    salto(terminaron ? 'hecho' : 'en-curso', 'dashboard-aggregator', null,
      ['Junta los tres resultados en ', ['{ payment, stock, email }'], ' y los envía por WebSocket a este panel.']).li,
    salto(pago === 'pendiente' ? 'en-curso' : 'hecho', 'order-api', null,
      textoPago).li);

  contenido.replaceChildren(meta, lista);
}
