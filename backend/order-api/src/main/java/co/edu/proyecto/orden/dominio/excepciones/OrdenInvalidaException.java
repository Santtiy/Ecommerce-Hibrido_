package co.edu.proyecto.orden.dominio.excepciones;

/** Datos que impiden crear o usar una orden (sin ítems, cantidad 0, monto negativo...). */
public class OrdenInvalidaException extends DominioException {

    public OrdenInvalidaException(String mensaje) {
        super(mensaje);
    }
}
