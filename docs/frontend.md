# Frontend: estructura y mantenimiento

La tienda y el panel usan HTML, CSS y módulos JavaScript nativos. No necesitan npm, un bundler, dependencias de
ejecución ni otro servidor. Maven copia `frontend/` dentro del JAR de `dashboard-aggregator`, como antes.

## Dónde cambiar cada cosa

| Archivo o carpeta | Responsabilidad |
|---|---|
| `frontend/index.html`, `frontend/admin.html` | Estructura y textos de las dos páginas |
| `frontend/css/base.css` | Colores, tipografía y controles comunes |
| `frontend/css/tienda.css`, `frontend/css/admin.css` | Diseño y adaptación a pantallas de cada página |
| `frontend/js/dom.js`, `frontend/js/iconos.js` | Elementos e iconos compartidos; los datos se insertan como texto |
| `frontend/js/formato.js` | Formatos de moneda, números y hora |
| `frontend/js/config.js` | Direcciones, reglas copiadas del backend y límites del navegador |
| `frontend/js/calculo.js`, `frontend/js/estados.js` | Cálculos y clasificación de estados, sin DOM |
| `frontend/js/api.js`, `frontend/js/canal.js` | HTTP y WebSocket |
| `frontend/js/almacenamiento.js` | Lectura y escritura opcional de JSON en `localStorage` |
| `frontend/js/registro.js`, `frontend/js/peticiones.js` | Pedidos del navegador y última petición compartida entre pestañas |
| `frontend/js/tienda/app.js`, `frontend/js/admin/app.js` | Conexión de eventos, vistas y estado de cada página |
| `frontend/js/tienda/productos.js` | Catálogo, categorías y créditos de las fotos |
| `frontend/js/tienda/carrito.js`, `favoritos.js`, `filtros.js` | Estado de la tienda, sin DOM |
| `frontend/js/tienda/controles.js` | Fotos, favoritos, cantidades, botón de compra y aviso al agregar |
| `frontend/js/tienda/resumen-pedido.js` | Promoción, totales y avisos compartidos por carrito y confirmación |
| `frontend/js/tienda/vitrina.js`, `detalle.js`, `panel-carrito.js`, `confirmar.js`, `seguimiento.js` | Una vista por archivo |
| `frontend/js/tienda/escenarios.js` | Casos de la exposición, separados del catálogo y del flujo normal |
| `frontend/js/admin/` | Tabla, conteos, recorrido y petición del panel |

## Mantenerlo simple y pequeño

- Escribir funciones cortas, nombres en español y comentarios que expliquen decisiones.
- Compartir solo lo que ya se usa en varias vistas. Mantener los cálculos separados del DOM.
- Conservar el código legible, sin minificar el código fuente ni generar copias de distribución.
- Actualizar los controles de compra sin retirar del documento las tarjetas y fotos del catálogo.
- Mantener un solo canal WebSocket por página; no consultar el servidor con intervalos adicionales.
- Conservar como máximo 1.000 órdenes en memoria por página y 300 pedidos en el historial local.
- Usar fotos JPEG de 768 × 576 px, calidad 75, con carga diferida y dimensiones declaradas. Los créditos y
  licencias deben conservarse. Evitar guardar originales, copias o herramientas de desarrollo en `frontend/`.
- No agregar paquetes de ejecución. Las fuentes externas y los iconos SVG existentes se conservan.

Los cambios de JavaScript se ejecutan en el navegador. La reducción de las fotos ahorra espacio en los archivos
del servidor y bytes de descarga; los límites de órdenes reducen el crecimiento de memoria del navegador.

## Comprobaciones de desarrollo

Con Node.js 22 o posterior, desde la raíz:

```powershell
node --test tests/frontend.test.mjs
```

Las pruebas usan solo Node.js y un DOM simulado: comprueban cálculos, catálogo, filtros, almacenamiento, contratos
HTTP, escenarios, tabla y referencias de los archivos. También comprueban un presupuesto de 750 KiB para todo
`frontend/` y 100 KiB por foto. No sustituyen una revisión visual en navegador.

Las pruebas y esta guía quedan fuera de `frontend/`, por lo que no se empaquetan como recursos estáticos.
Después de editar, vuelve a empaquetar `dashboard-aggregator` para que su JAR incluya los archivos actualizados.
