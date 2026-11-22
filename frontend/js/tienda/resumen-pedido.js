// Promoción, totales y avisos usados tanto en el carrito como en la confirmación.

import { REGLAS } from '../config.js';
import { estimar, predecir } from '../calculo.js';
import { crear } from '../dom.js';
import { formatoPesos, formatoNumero } from '../formato.js';
import { icono } from '../iconos.js';

export function crearResumenPedido(items) {
  const estimacion = estimar(items);
  const resumen = document.createDocumentFragment();
  resumen.append(beneficio(estimacion));
  if (items.length > 0) resumen.append(totales(estimacion), avisos(items, estimacion.total));
  return resumen;
}

function totales({ subtotal, descuento, iva, total }) {
  const lista = crear('dl', 'totales');
  const fila = (termino, valor, clase = '') => {
    lista.append(crear('dt', clase, termino), crear('dd', clase, valor));
  };
  fila('Subtotal', formatoPesos.format(subtotal));
  if (descuento > 0) {
    fila(`Descuento ${REGLAS.porcentajeDescuento} %`, '− ' + formatoPesos.format(descuento), 'descuento');
  }
  fila(`IVA ${REGLAS.porcentajeIva} %`, formatoPesos.format(iva));
  fila('Envío', 'Gratis', 'envio');
  fila('Total', formatoPesos.format(total), 'total');
  return lista;
}

function beneficio({ subtotal, descuento }) {
  const activo = descuento > 0;
  const caja = crear('div', 'beneficio-pedido' + (activo ? ' activo' : ''));
  const titulo = crear('p', 'beneficio-titulo');
  titulo.append(icono(activo ? 'hecho' : 'regalo'), crear('strong', '',
    activo ? `¡Tu ${REGLAS.porcentajeDescuento} % ya está activo!` : `Descuento del ${REGLAS.porcentajeDescuento} % pendiente`));

  let texto = `Supera ${formatoPesos.format(REGLAS.umbralDescuento)} de subtotal, antes de IVA, para activar el ${REGLAS.porcentajeDescuento} % de descuento.`;
  if (activo) {
    texto = `Ahorras ${formatoPesos.format(descuento)} en productos. El envío también es gratis.`;
  }
  caja.append(titulo, crear('p', 'beneficio-texto', texto));
  if (!activo) {
    const progreso = crear('progress');
    progreso.max = REGLAS.umbralDescuento;
    progreso.value = subtotal;
    progreso.setAttribute('aria-label', 'Subtotal para superar el umbral del descuento');
    caja.append(progreso, crear('small', '', `Subtotal actual: ${formatoPesos.format(subtotal)} · antes de IVA`));
  }
  return caja;
}

function avisos(items, total) {
  const { pagoRechazado, itemSinStock } = predecir(items, total);
  const lista = crear('ul', 'avisos');
  if (pagoRechazado) {
    lista.append(aviso('RN-11', `El pago se rechazará: el total supera ${formatoPesos.format(REGLAS.limitePago)}.`));
  }
  if (itemSinStock) {
    lista.append(aviso('RN-12', `No habrá stock: ${itemSinStock.nombre} pide ${formatoNumero.format(itemSinStock.cantidad)} unidades y el máximo es ${REGLAS.maximoUnidades}.`));
  }
  return lista;
}

function aviso(regla, texto) {
  const li = crear('li');
  li.append(icono('alerta'), crear('span', '', texto + ' '), crear('strong', '', regla));
  return li;
}
