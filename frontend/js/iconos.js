// Iconos de la interfaz (carrito, favoritos, cantidades, estados), de Lucide (https://lucide.dev, licencia ISC; lucide-static v0.460.0), incrustados para no depender de un CDN.
// Todos comparten la misma rejilla de 24 px y el mismo trazo de 2 px.

const ICONOS = {
  envio: [['path', { d: 'M10 17h4V5H2v12h3m9-9h4l4 4v5h-3' }],
    ['circle', { cx: 7.5, cy: 17.5, r: 2.5 }], ['circle', { cx: 16.5, cy: 17.5, r: 2.5 }]],
  regalo: [['rect', { x: 3, y: 8, width: 18, height: 4, rx: 1 }], ['path', { d: 'M5 12v8h14v-8M12 8v12M12 8H7.5A2.5 2.5 0 1 1 10 5.5L12 8Zm0 0h4.5A2.5 2.5 0 1 0 14 5.5L12 8Z' }]],
  bolsa: [['path', { d: 'm5 7-2 14h18L19 7ZM8 7V6a4 4 0 0 1 8 0v1' }]],
  carrito: [['circle', { cx: 8, cy: 21, r: 1 }], ['circle', { cx: 19, cy: 21, r: 1 }],
    ['path', { d: 'M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12' }]],
  corazon: [['path', { d: 'M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z' }]],
  mas: [['path', { d: 'M5 12h14' }], ['path', { d: 'M12 5v14' }]],
  menos: [['path', { d: 'M5 12h14' }]],
  quitar: [['path', { d: 'M18 6 6 18' }], ['path', { d: 'm6 6 12 12' }]],
  hecho: [['path', { d: 'M20 6 9 17l-5-5' }]],
  alerta: [['circle', { cx: 12, cy: 12, r: 10 }], ['line', { x1: 12, x2: 12, y1: 8, y2: 12 }], ['line', { x1: 12, x2: 12.01, y1: 16, y2: 16 }]],
};

const NS = 'http://www.w3.org/2000/svg';

/** Devuelve un <svg> decorativo (aria-hidden): el texto del control es quien lo nombra. */
export function icono(nombre, clase = 'icono') {
  const svg = document.createElementNS(NS, 'svg');
  for (const [atributo, valor] of [['viewBox', '0 0 24 24'], ['fill', 'none'], ['stroke', 'currentColor'],
    ['stroke-width', '2'], ['stroke-linecap', 'round'], ['stroke-linejoin', 'round'], ['aria-hidden', 'true'], ['class', clase]]) {
    svg.setAttribute(atributo, valor);
  }
  for (const [etiqueta, atributos] of ICONOS[nombre]) {
    const forma = document.createElementNS(NS, etiqueta);
    for (const [atributo, valor] of Object.entries(atributos)) forma.setAttribute(atributo, String(valor));
    svg.append(forma);
  }
  return svg;
}
