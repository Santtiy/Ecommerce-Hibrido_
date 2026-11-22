// Categorías y catálogo de la tienda. Sin DOM ni estado de la compra.

// Fotos reales con licencia libre, tomadas de Wikimedia Commons y llevadas a 768x576 (img/productos/, ver CREDITOS.md).
// Las CC BY y CC BY-SA exigen citar al autor y la licencia: la tienda lo hace en "Créditos de las fotos".
// Las descripciones solo dicen lo que se ve en la foto: nada de datos que la tienda no podría respaldar.
const COMMONS = 'https://commons.wikimedia.org/wiki/File:';
const DOMINIO_PUBLICO = { licencia: 'Dominio público', urlLicencia: '' };

export const CATEGORIAS = [
  { id: 'computacion', nombre: 'Computación' },
  { id: 'audio', nombre: 'Audio' },
  { id: 'celulares', nombre: 'Celulares y relojes' },
  { id: 'video', nombre: 'TV y fotografía' },
  { id: 'libros', nombre: 'Libros' },
];

/** Catálogo de la tienda. El precio va en pesos; order-api le asigna la moneda (COP). */
export const CATALOGO = [
  { sku: 'LIB-001', categoria: 'libros', nombre: 'Libro: Said bin Sultan', detalle: 'Tapa dura', precio: 150000,
    descripcion: 'Libro de tapa dura forrada en tela verde, con el título y un emblema grabados en dorado en la portada.',
    caracteristicas: ['Tapa dura forrada en tela', 'Título grabado en dorado'],
    foto: { src: 'img/productos/libro.jpg', autor: 'Cees de Jonge', licencia: 'CC BY-SA 4.0',
      urlLicencia: 'https://creativecommons.org/licenses/by-sa/4.0/',
      fuente: COMMONS + 'Boek_getiteld_Said_bin_Sultan,_uit_bibliotheek_Said-Ruete_(kaft).jpg' } },
  { sku: 'TV-001', categoria: 'video', nombre: 'Televisor 50"', detalle: 'Pantalla LED, Smart TV', precio: 1500000,
    descripcion: 'Televisor de pantalla plana LED de 50 pulgadas con funciones Smart TV y base de mesa.',
    caracteristicas: ['Pantalla LED de 50"', 'Smart TV', 'Base de mesa incluida'],
    foto: { src: 'img/productos/televisor.jpg', autor: 'LG전자 (LG Electronics)', licencia: 'CC BY 2.0',
      urlLicencia: 'https://creativecommons.org/licenses/by/2.0/', fuente: COMMONS + 'LG_smart_TV.jpg' } },
  { sku: 'USB-001', categoria: 'computacion', nombre: 'Memoria USB 4 GB', detalle: 'Conector deslizable', precio: 20000,
    descripcion: 'Memoria USB de 4 GB con conector deslizable que se guarda dentro del cuerpo y argolla para llavero.',
    caracteristicas: ['Capacidad de 4 GB', 'Conector deslizable', 'Argolla para llavero'],
    foto: { src: 'img/productos/memoria-usb.jpg', autor: 'Evan-Amos', ...DOMINIO_PUBLICO,
      fuente: COMMONS + 'SanDisk-Cruzer-USB-4GB-ThumbDrive.jpg' } },
  { sku: 'AUD-001', categoria: 'audio', nombre: 'Audífonos con cancelación de ruido', detalle: 'Plegables, con estuche', precio: 189900,
    descripcion: 'Audífonos over-ear plegables con cancelación de ruido, cable con control y estuche rígido para llevarlos.',
    caracteristicas: ['Cancelación de ruido', 'Diseño plegable', 'Estuche rígido incluido'],
    foto: { src: 'img/productos/audifonos.jpg', autor: 'Florian Fuchs', licencia: 'CC BY-SA 3.0',
      urlLicencia: 'https://creativecommons.org/licenses/by-sa/3.0/',
      fuente: COMMONS + 'Bose_QuietComfort_25_Acoustic_Noise_Cancelling_Headphones_with_Carry_Case.jpg' } },
  { sku: 'TEC-001', categoria: 'computacion', nombre: 'Teclado mecánico', detalle: 'Retroiluminado, para juegos', precio: 249000,
    descripcion: 'Teclado mecánico compacto, sin teclado numérico, con retroiluminación de colores en cada tecla.',
    caracteristicas: ['Teclas mecánicas', 'Formato compacto sin teclado numérico', 'Retroiluminación de colores'],
    foto: { src: 'img/productos/teclado.jpg', autor: 'Morn', licencia: 'CC BY-SA 4.0',
      urlLicencia: 'https://creativecommons.org/licenses/by-sa/4.0/', fuente: COMMONS + 'Lingbao_JIGUANSHI_Mechanical_Gaming_Keyboard.jpg' } },
  { sku: 'MOU-001', categoria: 'computacion', nombre: 'Mouse inalámbrico', detalle: 'Con receptor USB', precio: 59900,
    descripcion: 'Mouse inalámbrico de forma ergonómica, con rueda de desplazamiento y receptor USB para el computador.',
    caracteristicas: ['Inalámbrico', 'Receptor USB incluido', 'Rueda de desplazamiento'],
    foto: { src: 'img/productos/mouse.jpg', autor: 'Evan-Amos', ...DOMINIO_PUBLICO,
      fuente: COMMONS + 'Microsoft-wireless-mouse.jpg' } },
  { sku: 'CEL-001', categoria: 'celulares', nombre: 'Smartphone', detalle: 'Pantalla táctil de 3,5"', precio: 899000,
    descripcion: 'Smartphone con pantalla táctil de 3,5 pulgadas, cuerpo de vidrio y marco metálico.',
    caracteristicas: ['Pantalla táctil de 3,5"', 'Cuerpo de vidrio', 'Marco metálico'],
    foto: { src: 'img/productos/celular.jpg', autor: 'Yutaka Tsutano (pantalla editada por Kyro)',
      licencia: 'CC BY 2.0', urlLicencia: 'https://creativecommons.org/licenses/by/2.0/', fuente: COMMONS + 'IPhone_4_-_noscreen.jpg' } },
  { sku: 'POR-001', categoria: 'computacion', nombre: 'Portátil 13"', detalle: 'Ultradelgado, en aluminio', precio: 2899000,
    descripcion: 'Portátil ultradelgado de 13 pulgadas con cuerpo de aluminio, teclado completo y panel táctil amplio.',
    caracteristicas: ['Pantalla de 13"', 'Cuerpo de aluminio', 'Panel táctil amplio'],
    foto: { src: 'img/productos/portatil.jpg', autor: 'Sam Lionheart (iFixit)', licencia: 'CC BY-SA 3.0',
      urlLicencia: 'https://creativecommons.org/licenses/by-sa/3.0/', fuente: COMMONS + 'Macbook_Air.jpg' } },
  { sku: 'TAB-001', categoria: 'computacion', nombre: 'Tableta gráfica', detalle: 'Con lápiz, para dibujo digital', precio: 389000,
    descripcion: 'Tableta gráfica para dibujo digital, con lápiz, teclas de acceso rápido y anillo táctil a un costado.',
    caracteristicas: ['Lápiz incluido', 'Teclas de acceso rápido', 'Anillo táctil'],
    foto: { src: 'img/productos/tableta.jpg', autor: 'Joe Ravi', licencia: 'CC BY-SA 3.0',
      urlLicencia: 'https://creativecommons.org/licenses/by-sa/3.0/', fuente: COMMONS + 'Wacom_Intuos4_Pen_Tablet.jpg' } },
  { sku: 'REL-001', categoria: 'celulares', nombre: 'Reloj inteligente', detalle: 'Pantalla redonda, correa de cuero', precio: 459000,
    descripcion: 'Reloj inteligente con pantalla redonda, bisel metálico y correa de cuero.',
    caracteristicas: ['Pantalla redonda', 'Bisel metálico', 'Correa de cuero'],
    foto: { src: 'img/productos/reloj.jpg', autor: 'LG Electronics (Flickr: lge)', licencia: 'CC BY 2.0',
      urlLicencia: 'https://creativecommons.org/licenses/by/2.0/', fuente: COMMONS + 'LG_G_Watch_R.jpg' } },
  { sku: 'CAM-001', categoria: 'video', nombre: 'Cámara réflex', detalle: 'Con objetivo incluido', precio: 2499000,
    descripcion: 'Cámara réflex digital con su objetivo incluido, lista para usar.',
    caracteristicas: ['Cámara réflex digital', 'Objetivo incluido'],
    foto: { src: 'img/productos/camara.jpg', autor: 'Bernie', ...DOMINIO_PUBLICO,
      fuente: COMMONS + 'Nikon_D7000_Digital_SLR_Camera_05.jpg' } },
  { sku: 'PAR-001', categoria: 'audio', nombre: 'Parlante Bluetooth', detalle: 'Portátil y recargable', precio: 299900,
    descripcion: 'Parlante Bluetooth portátil y recargable, de forma cilíndrica, recubierto en tela y con correa para llevarlo.',
    caracteristicas: ['Conexión Bluetooth', 'Batería recargable', 'Correa para llevarlo'],
    foto: { src: 'img/productos/parlante.jpg', autor: 'Trougnouf', licencia: 'CC BY 4.0',
      urlLicencia: 'https://creativecommons.org/licenses/by/4.0/', fuente: COMMONS + 'JBL_Flip_3_bluetooth_speaker_(DSCF2653).jpg' } },
];

export function buscarProducto(sku) {
  return CATALOGO.find((producto) => producto.sku === sku) ?? null;
}

export function nombreCategoria(id) {
  return CATEGORIAS.find((categoria) => categoria.id === id)?.nombre ?? '';
}

export const enlaceProducto = (sku) => '#producto/' + sku;
