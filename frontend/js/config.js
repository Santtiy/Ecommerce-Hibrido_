// Configuración del dashboard: direcciones de los servicios, estados del enunciado y órdenes de ejemplo.
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

// Estimación del total en el navegador (el total real lo calcula order-api).
export const ESTIMACION = { umbralDescuento: 100000, porcentajeDescuento: 10, porcentajeIva: 19 };

export const CLIENTE_EJEMPLO = 'Cliente Demo';

// enviar: true manda la orden al pulsar el botón (casos inválidos que devuelven 400).
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
