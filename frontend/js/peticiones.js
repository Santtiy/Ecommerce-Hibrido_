// Última petición a order-api: la tienda la guarda y el panel de administración la muestra, aunque estén en pestañas
// distintas. Las dos páginas son del mismo origen, así que comparten localStorage y el evento 'storage' avisa al
// panel en cuanto la tienda escribe. Si el navegador no permite almacenamiento, el panel simplemente no la ve.

import { leerGuardado, guardar } from './almacenamiento.js';

const CLAVE = 'ecommerce-hibrido.ultimaPeticion';

/** peticion: { url, enviado, status, recibido } o { url, enviado, error } si no hubo respuesta. */
export function guardarPeticion(peticion) {
  guardar(CLAVE, { ...peticion, en: Date.now() });
}

export function leerPeticion() {
  return leerGuardado(CLAVE, null);
}

/** Llama a alCambiar(peticion) cada vez que otra pestaña guarda una petición nueva. */
export function escucharPeticiones(alCambiar) {
  window.addEventListener('storage', (evento) => {
    if (evento.key === CLAVE) alCambiar(leerPeticion());
  });
}
