package co.edu.proyecto.analytics.so;

/** Resultado medido de una escritura en disco: cuántos bytes, cuánto tardó y qué hilo la hizo. */
public record OperacionES(long bytes, long microsegundos, String hilo) {
}
