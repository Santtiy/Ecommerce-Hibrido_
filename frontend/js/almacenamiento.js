// El almacenamiento del navegador es opcional. Si falla, la app puede seguir usando su estado en memoria.

export function leerGuardado(clave, porDefecto) {
  try {
    return JSON.parse(localStorage.getItem(clave)) ?? porDefecto;
  } catch {
    return porDefecto;
  }
}

export function guardar(clave, valor) {
  try {
    localStorage.setItem(clave, JSON.stringify(valor));
  } catch { /* Sin permiso o sin espacio: no interrumpir la compra. */ }
}
