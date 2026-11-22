// Ayudas de DOM compartidas por la tienda y el panel. Los datos se escriben como texto, nunca como HTML.

export const porId = (id) => document.getElementById(id);

export function crear(etiqueta, clase = '', texto) {
  const elemento = document.createElement(etiqueta);
  elemento.className = clase;
  if (texto !== undefined) elemento.textContent = texto;
  return elemento;
}

export function bloqueJson(titulo, objeto) {
  const detalles = crear('details');
  detalles.append(crear('summary', '', titulo), crear('pre', '', JSON.stringify(objeto, null, 2)));
  return detalles;
}

// Al cambiar la cantidad se reemplaza su control. Recuperamos el foco por producto y acción.
export function repintarConservandoFoco(contenedor, pintar) {
  const activo = document.activeElement;
  const clave = contenedor.contains(activo) && activo.dataset.accion ? activo.dataset : null;
  pintar();
  if (!clave) return;
  const producto = `[data-sku="${CSS.escape(clave.sku)}"]`;
  const accion = `[data-accion="${CSS.escape(clave.accion)}"]`;
  (contenedor.querySelector(producto + accion) ?? contenedor.querySelector(producto))?.focus();
}
