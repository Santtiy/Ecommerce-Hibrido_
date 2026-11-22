// Clasificación de los estados que envía el WebSocket. Funciones puras, sin DOM.

import { ESTADOS_EXITO, ESTADOS_FALLO, ETAPAS } from './config.js';

/** 'ok', 'fallo' o 'pendiente' para un valor de payment, stock o email. */
export function claseEstado(estado) {
  if (ESTADOS_EXITO.includes(estado)) return 'ok';
  if (ESTADOS_FALLO.includes(estado)) return 'fallo';
  return 'pendiente';
}

/**
 * Resultado de una orden en el dashboard: 'fallo' si alguna etapa falló, 'completada' si las tres terminaron
 * bien y 'proceso' mientras falte alguna. (No es EstadoOrden de order-api: allí solo cuenta el pago.)
 */
export function resultadoOrden(progreso) {
  const clases = ETAPAS.map((etapa) => claseEstado(progreso[etapa.campo]));
  if (clases.includes('fallo')) return 'fallo';
  if (clases.every((clase) => clase === 'ok')) return 'completada';
  return 'proceso';
}

/** Cuántas de las tres etapas ya tienen resultado. */
export function etapasTerminadas(progreso) {
  return ETAPAS.filter((etapa) => claseEstado(progreso[etapa.campo]) !== 'pendiente').length;
}

export const NOMBRE_RESULTADO = { completada: 'Completada', fallo: 'Con fallo', proceso: 'En proceso' };
