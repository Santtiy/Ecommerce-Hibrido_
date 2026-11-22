// Paso 2, confirmar: resumen del pedido, nombre del cliente y, si order-api rechaza la petición, el motivo.

import { porId, crear, bloqueJson } from '../dom.js';
import { URL_API, TEXTO_HTTP } from '../config.js';
import { formatoPesos, formatoNumero } from '../formato.js';
import { icono } from '../iconos.js';
import { lineasDelCarrito, itemsDeLaOrden, carritoVacio } from './carrito.js';
import { fotoProducto } from './controles.js';
import { crearResumenPedido } from './resumen-pedido.js';

export function pintarResumen() {
  const contenido = porId('resumen-pedido');
  if (carritoVacio()) {
    contenido.replaceChildren(crear('p', 'vacio', 'No hay productos en el carrito.'));
    porId('confirmar').disabled = true;
    return;
  }
  const lista = crear('ul', 'lineas-resumen');
  lineasDelCarrito().forEach(({ producto, cantidad }) => {
    const li = crear('li');
    li.append(fotoProducto(producto, 'miniatura'), crear('span', 'nombre', producto.nombre),
      crear('span', 'cantidad', '× ' + formatoNumero.format(cantidad)),
      crear('span', 'importe', formatoPesos.format(producto.precio * cantidad)));
    lista.append(li);
  });
  contenido.replaceChildren(lista, crearResumenPedido(itemsDeLaOrden()));
  porId('confirmar').disabled = false;
}

/** Valida el nombre (CrearOrdenRequest exige un cliente). Devuelve el nombre o null. */
export function leerCliente() {
  const campo = porId('cliente');
  const cliente = campo.value.trim();
  porId('error-cliente').hidden = cliente !== '';
  if (cliente) {
    campo.removeAttribute('aria-invalid');
  } else {
    campo.setAttribute('aria-invalid', 'true');
    campo.focus();
  }
  return cliente || null;
}

export function ponerCliente(nombre) {
  porId('cliente').value = nombre;
  porId('cliente').removeAttribute('aria-invalid');
  porId('error-cliente').hidden = true;
}

/** Muestra por qué order-api no aceptó el pedido: { status, cuerpo } o, sin respuesta, el error de red. */
export function mostrarRechazo(enviado, { status, cuerpo, error }) {
  const caja = porId('rechazo');
  const titulo = crear('p', 'titulo-rechazo');
  titulo.append(icono('alerta'), crear('span', '',
    error ? 'No se pudo conectar con order-api' : `order-api rechazó el pedido: ${status} ${TEXTO_HTTP[status] ?? ''}`));
  const nodos = [titulo];
  if (error) {
    nodos.push(crear('p', '', `Comprueba que order-api esté arrancado en ${URL_API}. Detalle: ${error}`));
  } else if (cuerpo) {
    nodos.push(crear('p', '', cuerpo.mensaje ?? 'Error'));
    if (Array.isArray(cuerpo.detalles) && cuerpo.detalles.length > 0) {
      const lista = crear('ul', 'detalles');
      cuerpo.detalles.forEach((detalle) => lista.append(crear('li', '', detalle)));
      nodos.push(lista);
    }
  }
  const cuerpos = crear('div', 'cuerpos');
  cuerpos.append(bloqueJson('Cuerpo enviado', enviado));
  if (cuerpo) cuerpos.append(bloqueJson('Cuerpo recibido', cuerpo));
  nodos.push(cuerpos);
  caja.replaceChildren(...nodos);
  caja.hidden = false;
}

export function ocultarRechazo() {
  porId('rechazo').hidden = true;
}
