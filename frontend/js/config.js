// Configuración del dashboard: direcciones de los servicios, estados del enunciado, reglas y órdenes de ejemplo.
// El host sale de la página (en la EC2 es la IP pública), nunca se escribe a mano.

const HOST = window.location.hostname;

export const PUERTO_API = 8080;
export const PUERTO_DASHBOARD = 8081;
export const URL_API = `http://${HOST}:${PUERTO_API}/order`;
export const URL_WS = `ws://${HOST}:${PUERTO_DASHBOARD}/ws/ordenes`;
export const SEGUNDOS_REINTENTO = 3;

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

export const CLIENTE_EJEMPLO = 'Cliente Demo';

// enviar: true manda la orden al pulsar el botón, sin validar en el navegador (casos que devuelven 400).
export const EJEMPLOS = {
  'normal':        { enviar: false, items: [{ sku: 'LIB-001', nombre: 'Libro', cantidad: 1, precioUnitario: 150000 }] },
  'pago':          { enviar: false, items: [{ sku: 'TV-001', nombre: 'Televisor', cantidad: 1, precioUnitario: 1500000 }] },
  'stock':         { enviar: false, items: [{ sku: 'USB-001', nombre: 'Memoria USB', cantidad: 15, precioUnitario: 20000 }] },
  'varios':        { enviar: false, items: [{ sku: 'LIB-001', nombre: 'Libro', cantidad: 1, precioUnitario: 15000 },
                                            { sku: 'USB-001', nombre: 'Memoria USB', cantidad: 50, precioUnitario: 20000 }] },
  'precio-cero':   { enviar: true,  items: [{ sku: 'LIB-001', nombre: 'Libro', cantidad: 1, precioUnitario: 0 }] },
  'sin-items':     { enviar: true,  items: [] },
  'cantidad-cero': { enviar: true,  items: [{ sku: 'LIB-001', nombre: 'Libro', cantidad: 0, precioUnitario: 150000 }] },
};
