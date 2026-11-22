// Formatos de presentación compartidos por las vistas.

export const formatoPesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 });

export const formatoNumero = new Intl.NumberFormat('es-CO');

export const formatoHora = new Intl.DateTimeFormat('es-CO', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });

/** Crea un elemento con clase y texto opcionales (evita innerHTML con datos del servidor o del usuario). */
export function crear(etiqueta, clase, texto) {
  const elemento = document.createElement(etiqueta);
  if (clase) elemento.className = clase;
  if (texto !== undefined) elemento.textContent = texto;
  return elemento;
}
