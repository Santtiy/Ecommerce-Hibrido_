// Configuración común de la tienda y del panel: direcciones de los servicios, estados del enunciado y reglas.
// El host sale de la página (en la EC2 es la IP pública), nunca se escribe a mano.

const HOST = window.location.hostname;

export const URL_API = `http://${HOST}:8080/order`;
export const URL_WS = `ws://${HOST}:8081/ws/ordenes`;
export const SEGUNDOS_REINTENTO = 3;
export const MAXIMO_ORDENES = 1000; // Mismo límite de órdenes en memoria que el dashboard del servidor.
export const PROGRESO_PENDIENTE = { payment: 'pending', stock: 'pending', email: 'pending' };
export const TEXTO_HTTP = { 202: 'Accepted', 400: 'Bad Request', 404: 'Not Found', 409: 'Conflict', 500: 'Internal Server Error' };

// Estados que publican los workers (EstadoTarea en common); cualquier otro valor, como "pending", es pendiente.
export const ESTADOS_EXITO = ['PAYMENT_SUCCESS', 'STOCK_RESERVED', 'EMAIL_SENT'];
export const ESTADOS_FALLO = ['PAYMENT_FAILED', 'STOCK_OUT_OF_STOCK'];

// Las tres etapas de { payment, stock, email } (ProgresoOrden en dashboard-aggregator), en el orden de la tabla.
export const ETAPAS = [
  { campo: 'payment', nombre: 'Pago',   worker: 'billing-worker' },
  { campo: 'stock',   nombre: 'Stock',  worker: 'inventory-worker' },
  { campo: 'email',   nombre: 'Correo', worker: 'notification-worker' },
];

// Copia de las reglas del backend, solo para estimar y avisar en el navegador (la decisión la toma el servidor).
export const REGLAS = {
  umbralDescuento: 100000,    // RN-14, DescuentoPorMonto: 10 % si el subtotal lo supera
  porcentajeDescuento: 10,
  porcentajeIva: 19,          // IvaColombia
  limitePago: 1000000,        // RN-11, ReglaPago: el pago falla si el total lo supera
  maximoUnidades: 10,         // RN-12, ReglaInventario: sin stock si un ítem pide más
};

/** Páginas del frontend: la tienda (la app) y el panel de administración. */
export const PAGINA_ADMIN = '/admin.html';
