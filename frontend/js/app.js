// Punto de entrada: une la configuración, el cliente HTTP, el canal WebSocket y las vistas.

import { EJEMPLOS, CLIENTE_EJEMPLO } from './config.js';
import { enviarOrden } from './api.js';
import { conectarCanal } from './canal.js';
import { actualizarFila, mostrarConexion } from './tablero.js';
import { anadirFila, leerOrden, rellenar } from './formulario.js';
import { mostrarEnviando, mostrarResultado, mostrarSinConexion } from './respuesta.js';

const $ = (id) => document.getElementById(id);

async function enviar() {
  const orden = leerOrden();
  const boton = $('enviar');
  boton.disabled = true;
  mostrarEnviando();
  try {
    mostrarResultado(orden, await enviarOrden(orden));
  } catch (error) {
    mostrarSinConexion(error);
  } finally {
    boton.disabled = false;
  }
}

document.querySelectorAll('[data-ejemplo]').forEach((boton) => {
  boton.addEventListener('click', () => {
    const ejemplo = EJEMPLOS[boton.dataset.ejemplo];
    rellenar(CLIENTE_EJEMPLO, ejemplo.items);
    if (ejemplo.enviar) enviar();
  });
});

$('formulario').addEventListener('submit', (evento) => { evento.preventDefault(); enviar(); });
$('anadir-fila').addEventListener('click', () => anadirFila());

anadirFila();
conectarCanal({ alCambiarConexion: mostrarConexion, alRecibirOrden: actualizarFila });
