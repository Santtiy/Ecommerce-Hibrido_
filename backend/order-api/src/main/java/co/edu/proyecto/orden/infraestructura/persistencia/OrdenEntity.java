package co.edu.proyecto.orden.infraestructura.persistencia;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fila de la tabla ordenes. Es una clase de infraestructura con setters: el dominio nunca la ve,
 * así la entidad rica Orden no tiene que abrir su encapsulamiento para JPA.
 */
@Entity
@Table(name = "ordenes")
public class OrdenEntity {

    @Id
    private UUID id;
    private String cliente;
    private String estado;
    @Column(name = "total_monto")
    private BigDecimal totalMonto;
    @Column(name = "total_moneda")
    private String totalMoneda;
    @Column(name = "creada_en", updatable = false)
    private Instant creadaEn;
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemOrdenEntity> items = new ArrayList<>();

    protected OrdenEntity() {
    }

    OrdenEntity(UUID id, String cliente, Instant creadaEn) {
        this.id = id;
        this.cliente = cliente;
        this.creadaEn = creadaEn;
    }

    void actualizar(String estado, BigDecimal totalMonto, String totalMoneda) {
        this.estado = estado;
        this.totalMonto = totalMonto;
        this.totalMoneda = totalMoneda;
    }

    void agregarItem(ItemOrdenEntity item) {
        item.asignarOrden(this);
        items.add(item);
    }

    UUID getId() {
        return id;
    }

    String getCliente() {
        return cliente;
    }

    String getEstado() {
        return estado;
    }

    BigDecimal getTotalMonto() {
        return totalMonto;
    }

    String getTotalMoneda() {
        return totalMoneda;
    }

    Instant getCreadaEn() {
        return creadaEn;
    }

    List<ItemOrdenEntity> getItems() {
        return items;
    }
}
