package co.edu.proyecto.orden.infraestructura.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Fila de la tabla items_orden. El objeto de valor Dinero se guarda en dos columnas (monto y moneda). */
@Entity
@Table(name = "items_orden")
public class ItemOrdenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "orden_id")
    private OrdenEntity orden;
    private String sku;
    private String nombre;
    private int cantidad;
    @Column(name = "precio_monto")
    private BigDecimal precioMonto;
    @Column(name = "precio_moneda")
    private String precioMoneda;

    protected ItemOrdenEntity() {
    }

    ItemOrdenEntity(String sku, String nombre, int cantidad, BigDecimal precioMonto, String precioMoneda) {
        this.sku = sku;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioMonto = precioMonto;
        this.precioMoneda = precioMoneda;
    }

    void asignarOrden(OrdenEntity orden) {
        this.orden = orden;
    }

    String getSku() {
        return sku;
    }

    String getNombre() {
        return nombre;
    }

    int getCantidad() {
        return cantidad;
    }

    BigDecimal getPrecioMonto() {
        return precioMonto;
    }

    String getPrecioMoneda() {
        return precioMoneda;
    }
}
