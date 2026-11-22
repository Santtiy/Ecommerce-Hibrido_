import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { pathToFileURL } from 'node:url';
import { execFileSync } from 'node:child_process';
import { Elemento, prepararDom } from './soporte/dom.mjs';

const archivos = carpeta => readdirSync(carpeta, { withFileTypes: true }).flatMap(entrada =>
  entrada.isDirectory() ? archivos(resolve(carpeta, entrada.name)) : [resolve(carpeta, entrada.name)]);
const html = ['frontend/index.html', 'frontend/admin.html'].map(ruta => readFileSync(ruta, 'utf8'));
const ids = html.flatMap(pagina => [...pagina.matchAll(/\bid="([^"]+)"/g)].map(coincidencia => coincidencia[1]));
const { elementos, grupos } = prepararDom([...new Set(ids)]);
const guardados = new Map();
globalThis.localStorage = { getItem: clave => guardados.get(clave) ?? null, setItem: (clave, valor) => guardados.set(clave, valor) };
globalThis.window = { location: { hostname: 'prueba.local' }, addEventListener() {} };
globalThis.requestAnimationFrame = accion => accion();

const carrito = await import('../frontend/js/tienda/carrito.js');
const filtros = await import('../frontend/js/tienda/filtros.js');
const favoritos = await import('../frontend/js/tienda/favoritos.js');
const { CATALOGO, CATEGORIAS } = await import('../frontend/js/tienda/productos.js');
const { estimar, predecir } = await import('../frontend/js/calculo.js');
const { claseEstado, resultadoOrden } = await import('../frontend/js/estados.js');
const { crearResumenPedido } = await import('../frontend/js/tienda/resumen-pedido.js');

test('recursos, imports y exportaciones son válidos; el frontend cabe en 750 KiB', async () => {
  const recursos = archivos('frontend');
  assert.ok(recursos.reduce((total, ruta) => total + statSync(ruta).size, 0) < 750 * 1024);
  for (const ruta of recursos.filter(ruta => ruta.endsWith('.jpg'))) assert.ok(statSync(ruta).size < 100 * 1024);
  const modulos = recursos.filter(ruta => ruta.endsWith('.js'));
  for (const ruta of modulos) {
    execFileSync(process.execPath, ['--check', ruta], { stdio: 'pipe' });
    const codigo = readFileSync(ruta, 'utf8');
    for (const [, nombres, origen] of codigo.matchAll(/import\s+\{([^}]+)\}\s+from\s+'([^']+)'/g)) {
      const dependencia = resolve(dirname(ruta), origen);
      assert.ok(existsSync(dependencia), origen);
      const exportaciones = await import(pathToFileURL(dependencia));
      for (const nombre of nombres.split(',').map(nombre => nombre.trim())) assert.ok(nombre in exportaciones, `${ruta}: ${nombre}`);
    }
    for (const [, id] of codigo.matchAll(/porId\('([^']+)'\)/g)) assert.ok(ids.includes(id) || id === 'titulo-detalle', `${ruta}: ${id}`);
    assert.doesNotMatch(codigo, /innerHTML\s*=/);
  }
  for (const pagina of html) {
    const presentes = [...pagina.matchAll(/\bid="([^"]+)"/g)].map(coincidencia => coincidencia[1]);
    assert.equal(presentes.length, new Set(presentes).size);
    for (const [, recurso] of pagina.matchAll(/(?:href|src)="([^"#]+)"/g)) {
      if (!recurso.includes(':')) assert.ok(existsSync(resolve('frontend', recurso)), recurso);
    }
  }
  for (const producto of CATALOGO) {
    assert.ok(existsSync(resolve('frontend', producto.foto.src)));
    assert.ok(CATEGORIAS.some(categoria => categoria.id === producto.categoria));
    assert.ok(producto.foto.autor && producto.foto.licencia && producto.foto.fuente);
  }
});

test('subtotal, descuento e IVA conservan el umbral estricto y los centavos', () => {
  assert.deepEqual(estimar([]), { subtotal: 0, descuento: 0, iva: 0, total: 0 });
  assert.deepEqual(estimar([{ cantidad: 1, precioUnitario: 150000 }]), { subtotal: 150000, descuento: 15000, iva: 25650, total: 160650 });
  assert.equal(estimar([{ cantidad: 5, precioUnitario: 20000 }]).descuento, 0);
  assert.equal(estimar([{ cantidad: 1, precioUnitario: 100000.01 }]).descuento, 10000);
  assert.equal(estimar([{ cantidad: 3, precioUnitario: 59900 }]).total, 192458.7);
  const items = [{ nombre: 'Libro', cantidad: 1 }, { nombre: 'USB', cantidad: 11 }];
  assert.equal(predecir(items, 1000000).pagoRechazado, false);
  assert.equal(predecir(items, 1000000.01).pagoRechazado, true);
  assert.equal(predecir(items, 0).itemSinStock.nombre, 'USB');
});

test('el resumen conserva promoción, envío gratis y avisos de pago y stock', () => {
  const limite = crearResumenPedido([{ cantidad: 1, precioUnitario: 100000 }]);
  assert.match(limite.textContent, /Supera/);
  assert.ok(limite.querySelector('progress'));
  assert.equal(limite.querySelector('.descuento'), null);
  const activo = crearResumenPedido([{ cantidad: 1, precioUnitario: 150000 }]);
  assert.match(activo.textContent, /Ahorras/);
  assert.match(activo.textContent, /Gratis/);
  assert.equal(activo.querySelector('progress'), null);
  const rechazado = crearResumenPedido([{ nombre: 'TV', cantidad: 11, precioUnitario: 1500000 }]);
  assert.match(rechazado.textContent, /RN-11/);
  assert.match(rechazado.textContent, /RN-12/);
  assert.equal(crearResumenPedido([]).querySelector('.totales'), null);
});

test('el carrito conserva cantidades, límites y el contrato de los ítems', () => {
  carrito.vaciarCarrito();
  carrito.ponerCantidad('USB-001', 5);
  assert.equal(estimar(carrito.itemsDeLaOrden()).descuento, 0);
  carrito.sumarCantidad('USB-001', 1);
  assert.equal(estimar(carrito.itemsDeLaOrden()).descuento, 12000);
  assert.deepEqual(carrito.itemsDeLaOrden()[0], { sku: 'USB-001', nombre: 'Memoria USB 4 GB', cantidad: 6, precioUnitario: 20000 });
  carrito.ponerCantidad('USB-001', 150);
  assert.equal(carrito.cantidadDe('USB-001'), 99);
  carrito.ponerCantidad('USB-001', -1);
  assert.equal(carrito.carritoVacio(), true);
});

test('búsqueda sin tildes, categorías, favoritos y orden siguen funcionando', () => {
  filtros.quitarFiltros();
  filtros.cambiarFiltro({ texto: 'AUDIFONOS' });
  assert.equal(filtros.productosVisibles()[0].sku, 'AUD-001');
  filtros.quitarFiltros();
  filtros.cambiarFiltro({ categoria: 'audio' });
  assert.equal(filtros.productosVisibles().length, 2);
  filtros.quitarFiltros();
  favoritos.alternarFavorito('USB-001');
  filtros.cambiarFiltro({ soloFavoritos: true });
  assert.deepEqual(filtros.productosVisibles().map(producto => producto.sku), ['USB-001']);
  favoritos.alternarFavorito('USB-001');
  filtros.quitarFiltros();
  filtros.cambiarFiltro({ orden: 'precio-asc' });
  assert.equal(filtros.productosVisibles()[0].sku, 'USB-001');
  filtros.cambiarFiltro({ orden: 'relevancia' });
  assert.equal(filtros.productosVisibles().length, 12);
});

test('una cantidad cambia sus controles y conserva las tarjetas y fotos del catálogo', async () => {
  const { crearVitrina, actualizarProductos } = await import('../frontend/js/tienda/vitrina.js');
  crearVitrina();
  const tarjetas = [...elementos.get('vitrina').children];
  const fotos = tarjetas.map(tarjeta => tarjeta.querySelector('img'));
  document.activeElement = tarjetas[0].querySelector('a');
  carrito.ponerCantidad('USB-001', 1);
  actualizarProductos();
  assert.deepEqual(elementos.get('vitrina').children, tarjetas);
  assert.deepEqual(tarjetas.map(tarjeta => tarjeta.querySelector('img')), fotos);
  assert.equal(document.activeElement, tarjetas[0].querySelector('a'));
  assert.equal(fotos[0].loading, 'lazy');
  assert.equal(fotos[0].width, 768);
  assert.equal(fotos[0].height, 576);
  document.activeElement = null;
  carrito.vaciarCarrito();
});

test('los siete escenarios siguen confirmando casos válidos y enviando inválidos', async () => {
  const { configurarEscenarios } = await import('../frontend/js/tienda/escenarios.js');
  const casos = ['normal', 'pago', 'stock', 'varios', 'precio-cero', 'sin-items', 'cantidad-cero'];
  const botones = casos.map(caso => { const boton = new Elemento('button'); boton.dataset.escenario = caso; return boton; });
  grupos.set('[data-escenario]', botones);
  let confirmaciones = 0;
  const enviados = [];
  configurarEscenarios({ alConfirmar: () => confirmaciones++, alEnviar: orden => enviados.push(orden) });
  for (const boton of botones) await boton.emitir('click');
  assert.equal(confirmaciones, 4);
  assert.equal(enviados.length, 3);
  assert.equal(enviados[0].items[0].precioUnitario, 0);
  assert.deepEqual(enviados[1].items, []);
  assert.equal(enviados[2].items[0].cantidad, 0);
  assert.equal(elementos.get('cliente').value, 'Cliente Demo');
  carrito.vaciarCarrito();
});

test('almacenamiento corrupto o sin espacio no interrumpe la app; historial limitado a 300', async () => {
  const { guardar, leerGuardado } = await import('../frontend/js/almacenamiento.js');
  const { guardarEnviada, enviadasRecientes, datosEnviada } = await import('../frontend/js/registro.js');
  guardados.set('corrupto', '{');
  assert.deepEqual(leerGuardado('corrupto', []), []);
  const almacenamiento = globalThis.localStorage;
  globalThis.localStorage = { getItem() { throw Error('Sin permiso'); }, setItem() { throw Error('Sin espacio'); } };
  try {
    assert.equal(leerGuardado('clave', null), null);
    assert.doesNotThrow(() => guardar('clave', {}));
    favoritos.alternarFavorito('USB-001');
    assert.equal(favoritos.esFavorito('USB-001'), true);
    favoritos.alternarFavorito('USB-001');
  } finally { globalThis.localStorage = almacenamiento; }
  for (let numero = 0; numero < 301; numero++) guardarEnviada('historial-' + numero, { cliente: 'Prueba', total: 20000, productos: [] });
  assert.equal(enviadasRecientes().length, 300);
  assert.equal(datosEnviada('historial-0'), null);
});

test('HTTP conserva el contrato POST /order y distingue respuestas de fallos de red', async () => {
  const { enviarOrden } = await import('../frontend/js/api.js');
  const orden = { cliente: 'Prueba', items: [{ sku: 'USB-001', nombre: 'USB', cantidad: 1, precioUnitario: 20000 }] };
  globalThis.fetch = async (url, opciones) => {
    assert.equal(url, 'http://prueba.local:8080/order');
    assert.equal(opciones.method, 'POST');
    assert.deepEqual(JSON.parse(opciones.body), orden);
    return { status: 202, ok: true, json: async () => ({ orderId: 'aceptada' }) };
  };
  assert.equal((await enviarOrden(orden)).cuerpo.orderId, 'aceptada');
  globalThis.fetch = async () => ({ status: 400, ok: false, json: async () => ({ mensaje: 'Inválida' }) });
  assert.equal((await enviarOrden(orden)).status, 400);
  globalThis.fetch = async () => { throw Error('Sin conexión'); };
  await assert.rejects(enviarOrden(orden), /Sin conexión/);
});

test('el panel clasifica estados, filtra y conserva como máximo 1.000 órdenes', async () => {
  const tablero = await import('../frontend/js/admin/tablero.js');
  const pendiente = { payment: 'pending', stock: 'pending', email: 'pending' };
  const completa = { payment: 'PAYMENT_SUCCESS', stock: 'STOCK_RESERVED', email: 'EMAIL_SENT' };
  assert.equal(claseEstado('desconocido'), 'pendiente');
  assert.equal(resultadoOrden(completa), 'completada');
  assert.equal(resultadoOrden({ ...completa, stock: 'STOCK_OUT_OF_STOCK' }), 'fallo');
  tablero.actualizarOrden('orden-0', pendiente, true);
  let seleccion = 'orden-0';
  tablero.alSeleccionarOrden(id => { seleccion = id; });
  tablero.seleccionarOrden('orden-0');
  for (let numero = 1; numero <= 1000; numero++) tablero.actualizarOrden('orden-' + numero, completa, true);
  assert.equal(tablero.refrescar().todas, 1000);
  assert.equal(elementos.get('filas').children.length, 1000);
  assert.equal(tablero.existeOrden('orden-0'), false);
  assert.equal(seleccion, null);
  tablero.filtrar({ resultado: 'fallo', texto: '' });
  assert.equal(tablero.estaVisible('orden-1000'), false);
  tablero.filtrar({ resultado: 'todas', texto: 'orden-1000' });
  assert.equal(tablero.estaVisible('orden-1000'), true);
  assert.equal(tablero.estaVisible('orden-999'), false);
});

test('la tienda arranca una vez y permite buscar, ver una ficha, confirmar y seguir un pedido', async () => {
  const dom = prepararDom([...new Set(ids)]);
  const nodo = id => dom.elementos.get(id);
  const eventos = new Map();
  const conexiones = [];
  globalThis.window = {
    location: { hostname: 'prueba.local', hash: '', pathname: '/', search: '' },
    scrollY: 0,
    scrollTo({ top }) { this.scrollY = top; },
    addEventListener(nombre, accion) { eventos.set(nombre, accion); },
  };
  globalThis.history = { replaceState() { window.location.hash = ''; } };
  globalThis.WebSocket = class {
    constructor(url) { this.url = url; conexiones.push(this); }
  };
  dom.grupos.set('.saltar-contenido', [new Elemento('a')]);
  dom.grupos.set('[data-paso]', ['productos', 'confirmar', 'seguimiento'].map(paso => {
    const boton = new Elemento('button');
    boton.dataset.paso = paso;
    return boton;
  }));
  nodo('detalle-producto').hidden = true;
  nodo('rechazo').hidden = true;
  carrito.vaciarCarrito();
  filtros.quitarFiltros();
  await import('../frontend/js/tienda/app.js');
  assert.equal(conexiones.length, 1);
  assert.equal(nodo('vitrina').children.length, 12);
  assert.equal(nodo('continuar').disabled, true);

  nodo('buscar').value = 'audifonos';
  await nodo('buscar').emitir('input');
  assert.equal(nodo('vitrina').children.length, 1);
  assert.equal(nodo('promociones').hidden, true);
  await nodo('limpiar-filtros').emitir('click');
  assert.equal(nodo('vitrina').children.length, 12);
  assert.equal(nodo('promociones').hidden, false);
  window.location.hash = '#producto/USB-001';
  eventos.get('hashchange')();
  assert.equal(nodo('detalle-producto').hidden, false);
  assert.equal(document.getElementById('titulo-detalle').textContent, 'Memoria USB 4 GB');

  carrito.ponerCantidad('USB-001', 6);
  await nodo('continuar').emitir('click');
  assert.equal(nodo('paso-productos').hidden, true);
  assert.equal(nodo('paso-confirmar').hidden, false);
  assert.match(nodo('resumen-pedido').textContent, /Ahorras/);
  let peticiones = 0;
  globalThis.fetch = async () => {
    peticiones++;
    return { status: 400, ok: false, json: async () => ({ mensaje: 'Orden inválida' }) };
  };
  nodo('cliente').value = '';
  await nodo('form-confirmar').emitir('submit');
  assert.equal(peticiones, 0);
  assert.equal(nodo('error-cliente').hidden, false);
  nodo('cliente').value = 'Cliente de prueba';
  await nodo('form-confirmar').emitir('submit');
  await new Promise(setImmediate);
  assert.equal(peticiones, 1);
  assert.equal(nodo('rechazo').hidden, false);
  assert.equal(carrito.carritoVacio(), false);

  globalThis.fetch = async () => {
    peticiones++;
    return { status: 202, ok: true, json: async () => ({ orderId: 'pedido-integracion', total: 128520, moneda: 'COP' }) };
  };
  await nodo('form-confirmar').emitir('submit');
  await new Promise(setImmediate);
  assert.equal(peticiones, 2);
  assert.equal(nodo('paso-seguimiento').hidden, false);
  assert.equal(carrito.carritoVacio(), true);
  conexiones[0].onmessage({ data: JSON.stringify({ 'pedido-integracion': { payment: 'PAYMENT_SUCCESS', stock: 'STOCK_RESERVED', email: 'EMAIL_SENT' } }) });
  assert.match(nodo('pedido-actual').textContent, /Pedido completado/);
});
