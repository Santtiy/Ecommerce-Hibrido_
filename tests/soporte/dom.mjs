// DOM mínimo para comprobar las vistas sin instalar un navegador ni dependencias de pruebas.

export class Elemento {
  constructor(etiqueta) {
    this.tagName = etiqueta;
    this.children = [];
    this.dataset = {};
    this.attributes = {};
    this.className = '';
    this.listeners = {};
    this.hidden = false;
  }

  get textContent() { return this.children.map(nodo => nodo.textContent ?? nodo).join(''); }
  set textContent(texto) { this.replaceChildren(String(texto)); }
  get cells() { return this.children.filter(nodo => nodo.tagName === 'td'); }
  get classList() {
    return {
      add: nombre => this.classList.toggle(nombre, true),
      remove: nombre => this.classList.toggle(nombre, false),
      toggle: (nombre, activo) => {
        const clases = new Set(this.className.split(' ').filter(Boolean));
        if (activo) clases.add(nombre); else clases.delete(nombre);
        this.className = [...clases].join(' ');
      },
    };
  }

  append(...nodos) {
    for (const nodo of nodos) {
      if (nodo.tagName === '#fragment') { this.append(...[...nodo.children]); continue; }
      if (typeof nodo === 'object') { nodo.remove(); nodo.parent = this; }
      this.children.push(nodo);
    }
  }
  prepend(nodo) { nodo.remove(); nodo.parent = this; this.children.unshift(nodo); }
  replaceChildren(...nodos) {
    for (const hijo of this.children) if (typeof hijo === 'object') hijo.parent = null;
    this.children = [];
    this.append(...nodos);
  }
  remove() {
    if (this.parent) this.parent.children.splice(this.parent.children.indexOf(this), 1);
    this.parent = null;
  }
  setAttribute(clave, valor) {
    this.attributes[clave] = String(valor);
    if (clave === 'class') this.className = valor;
  }
  removeAttribute(clave) { delete this.attributes[clave]; }
  contains(nodo) { return this === nodo || this.children.some(hijo => hijo.contains?.(nodo)); }
  focus() { document.activeElement = this; }
  scrollIntoView() {}
  addEventListener(evento, accion) { (this.listeners[evento] ??= []).push(accion); }
  async emitir(evento) {
    for (const accion of this.listeners[evento] ?? []) await accion({ preventDefault() {}, stopPropagation() {} });
  }
  querySelector(selector) { return this.querySelectorAll(selector)[0] ?? null; }
  querySelectorAll(selector) {
    const coincide = nodo => {
      if (selector.startsWith('.')) return nodo.className.split(' ').includes(selector.slice(1));
      if (!selector.startsWith('[')) return nodo.tagName === selector;
      return [...selector.matchAll(/\[data-([\w-]+)="([^"]+)"\]/g)].every(([, clave, valor]) => nodo.dataset[clave] === valor);
    };
    return this.children.flatMap(nodo => typeof nodo === 'object'
      ? [...(coincide(nodo) ? [nodo] : []), ...nodo.querySelectorAll(selector)] : []);
  }
}

export function prepararDom(ids) {
  const elementos = new Map(ids.map(id => [id, new Elemento('div')]));
  const grupos = new Map();
  const creados = [];
  const crear = etiqueta => {
    const elemento = new Elemento(etiqueta);
    creados.push(elemento);
    return elemento;
  };
  globalThis.document = {
    activeElement: null,
    createElement: crear,
    createElementNS: (_, etiqueta) => crear(etiqueta),
    createDocumentFragment: () => new Elemento('#fragment'),
    getElementById: id => elementos.get(id) ?? creados.findLast(elemento => elemento.id === id),
    querySelector: selector => grupos.get(selector)?.[0] ?? null,
    querySelectorAll: selector => grupos.get(selector) ?? [],
  };
  globalThis.CSS = { escape: texto => texto };
  return { elementos, grupos };
}
