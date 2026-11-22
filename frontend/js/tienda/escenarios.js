// Casos de la exposición académica. Se configuran aparte del flujo normal de compra.

import { porId } from '../dom.js';
import { llenarCarrito } from './carrito.js';
import { ponerCliente, ocultarRechazo } from './confirmar.js';

const CLIENTE_DEMO = 'Cliente Demo';

/**
 * Escenarios de la exposición.
 * - Válidos: llenan el carrito con productos del catálogo y llevan a confirmar la compra.
 * - Inválidos: se envían tal cual a order-api, sin pasar por la tienda, para mostrar su validación (400).
 *   La tienda nunca deja armar estos pedidos.
 */
const ESCENARIOS = {
  'normal': { carrito: [{ sku: 'LIB-001', cantidad: 1 }] },
  'pago': { carrito: [{ sku: 'TV-001', cantidad: 1 }] },
  'stock': { carrito: [{ sku: 'USB-001', cantidad: 15 }] },
  // Solo el segundo ítem pasa de 10 unidades: RN-12 revisa todos los ítems, no solo el primero. Total 396.270 (< 1.000.000).
  'varios': { carrito: [{ sku: 'LIB-001', cantidad: 1 }, { sku: 'USB-001', cantidad: 11 }] },
  'precio-cero': { invalido: [{ sku: 'LIB-001', nombre: 'Libro', cantidad: 1, precioUnitario: 0 }] },
  'sin-items': { invalido: [] },
  'cantidad-cero': { invalido: [{ sku: 'LIB-001', nombre: 'Libro', cantidad: 0, precioUnitario: 150000 }] },
};

export function configurarEscenarios({ alConfirmar, alEnviar }) {
  document.querySelectorAll('[data-escenario]').forEach((boton) => {
    boton.addEventListener('click', () => {
      const escenario = ESCENARIOS[boton.dataset.escenario];
      ponerCliente(CLIENTE_DEMO);
      if (escenario.carrito) {
        ocultarRechazo();
        llenarCarrito(escenario.carrito);
        alConfirmar();
      } else {
        alEnviar({ cliente: CLIENTE_DEMO, items: escenario.invalido });
      }
    });
  });

  const escenarios = porId('escenarios');
  try {
    escenarios.open = localStorage.getItem('ecommerce-hibrido.escenarios') === 'abierto';
  } catch { /* Cerrado por defecto. */ }
  escenarios.addEventListener('toggle', () => {
    try {
      localStorage.setItem('ecommerce-hibrido.escenarios', escenarios.open ? 'abierto' : 'cerrado');
    } catch { /* Sin almacenamiento. */ }
  });
}
