package cl.duoc.farmaexpress.catalog.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "MEDICAMENTOS", uniqueConstraints = @UniqueConstraint(name = "UK_MEDICAMENTOS_SKU", columnNames = "SKU"))
public class Medicamento {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medicamento_seq")
    @SequenceGenerator(name = "medicamento_seq", sequenceName = "MEDICAMENTO_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "SKU", nullable = false, length = 100)
    private String sku;

    @Column(name = "NOMBRE", nullable = false, length = 200)
    private String nombre;

    @Column(name = "PRECIO", nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "STOCK", nullable = false)
    private Integer stock;

    public Medicamento() {}
    public Long getId() { return id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
