// Paso 1, panel del carrito: líneas con su cantidad, totales, avisos de las reglas y el botón Continuar.
// También pinta el resumen del encabezado y la barra inferior del móvil.

import { porId, crear, repintarConservandoFoco } from '../dom.js';
import { formatoPesos, formatoNumero } from '../formato.js';
import { estimar, precioConIva } from '../calculo.js';
import { icono } from '../iconos.js';
import { lineasDelCarrito, itemsDeLaOrden, unidadesEnCarrito, ponerCantidad, carritoVacio } from './carrito.js';
import { fotoProducto, selectorCantidad } from './controles.js';
import { crearResumenPedido } from './resumen-pedido.js';


function lineaCarrito({ producto, cantidad }) {
  const li = crear('li', 'linea');
  const quitar = crear('button', 'boton-icono quitar');
  quitar.type = 'button';
  quitar.dataset.sku = producto.sku;
  quitar.dataset.accion = 'quitar';
  quitar.setAttribute('aria-label', `Quitar ${producto.nombre} del carrito`);
  quitar.title = 'Quitar del carrito';
  quitar.append(icono('quitar'));
  quitar.addEventListener('click', () => ponerCantidad(producto.sku, 0));
  li.append(
    fotoProducto(producto, 'miniatura'),
    crear('span', 'nombre', producto.nombre),
    crear('span', 'importe', formatoPesos.format(precioConIva(producto.precio * cantidad))),
    selectorCantidad(producto),
    quitar);
  return li;
}

export function pintarCarrito() {
  const items = itemsDeLaOrden();
  const contenido = porId('contenido-carrito');
  repintarConservandoFoco(contenido, () => {
    if (carritoVacio()) {
      const vacio = crear('div', 'carrito-vacio');
      vacio.append(icono('bolsa'), crear('strong', '', 'Aquí empieza tu próxima compra'),
        crear('p', '', 'Agrega lo que te guste y revisa tu pedido aquí.'), crear('span', 'envio-gratis', 'Envío gratis desde el primer producto'));
      contenido.replaceChildren(vacio, crearResumenPedido(items));
      return;
    }
    const lista = crear('ul', 'lineas');
    lista.append(...lineasDelCarrito().map(lineaCarrito));
    contenido.replaceChildren(lista, crearResumenPedido(items));
  });
  porId('continuar').disabled = carritoVacio();

  // Encabezado y barra del móvil: unidades y total.
  const unidades = unidadesEnCarrito();
  porId('unidades-carrito').textContent = formatoNumero.format(unidades);
  const total = formatoPesos.format(estimar(items).total);
  const resumen = unidades === 0 ? 'Vacío' : `${formatoNumero.format(unidades)} ${unidades === 1 ? 'unidad' : 'unidades'} · ${total}`;
  porId('resumen-encabezado').textContent = resumen;
  porId('ir-carrito').setAttribute('aria-label', 'Ver carrito: ' + resumen);
  porId('resumen-movil').textContent = resumen;
}
