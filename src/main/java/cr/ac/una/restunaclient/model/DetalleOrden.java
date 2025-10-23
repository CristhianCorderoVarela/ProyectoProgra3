package cr.ac.una.restunaclient.model;

import java.math.BigDecimal;

/**
 * Modelo de DetalleOrden para el cliente JavaFX
 * Representa un producto dentro de una orden
 */
public class DetalleOrden {
    
    private Long id;
    private Long ordenId;
    private Long productoId;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private Long version;
    
    // Referencia al producto
    private Producto producto;
    
    public DetalleOrden() {
        this.cantidad = 1;
        this.precioUnitario = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
    }

    public DetalleOrden(Producto producto, Integer cantidad) {
        this();
        this.producto = producto;
        this.productoId = producto.getId();
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio();
        calcularSubtotal();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }
    
    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }
    
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { 
        this.cantidad = cantidad;
        calcularSubtotal();
    }
    
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { 
        this.precioUnitario = precioUnitario;
        calcularSubtotal();
    }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) { 
        this.producto = producto;
        if (producto != null) {
            this.productoId = producto.getId();
            this.precioUnitario = producto.getPrecio();
            calcularSubtotal();
        }
    }

    /**
     * Calcula el subtotal (cantidad * precio unitario)
     */
    public void calcularSubtotal() {
        if (this.cantidad != null && this.precioUnitario != null) {
            this.subtotal = this.precioUnitario.multiply(BigDecimal.valueOf(this.cantidad));
        }
    }

    @Override
    public String toString() {
        return producto != null ? producto.getNombre() : "Detalle #" + id;
    }
}