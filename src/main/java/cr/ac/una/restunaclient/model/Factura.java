package cr.ac.una.restunaclient.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Factura para el cliente JavaFX
 */
public class Factura {
    
    private Long id;
    private Long ordenId;
    private Long clienteId;
    private Long usuarioId;
    private Long cierreCajaId;
    private LocalDateTime fechaHora;
    private BigDecimal subtotal;
    private BigDecimal impuestoVenta;
    private BigDecimal impuestoServicio;
    private BigDecimal descuento;
    private BigDecimal total;
    private BigDecimal montoEfectivo;
    private BigDecimal montoTarjeta;
    private BigDecimal vuelto;
    private String estado; // A=Activa, C=Cancelada
    private Long version;
    
    // Referencias opcionales
    private Orden orden;
    private Cliente cliente;
    private Usuario usuario;
    private List<DetalleFactura> detalles;
    
    public Factura() {
        this.estado = "A";
        this.fechaHora = LocalDateTime.now();
        this.subtotal = BigDecimal.ZERO;
        this.total = BigDecimal.ZERO;
        this.impuestoVenta = BigDecimal.ZERO;
        this.impuestoServicio = BigDecimal.ZERO;
        this.descuento = BigDecimal.ZERO;
        this.montoEfectivo = BigDecimal.ZERO;
        this.montoTarjeta = BigDecimal.ZERO;
        this.vuelto = BigDecimal.ZERO;
        this.detalles = new ArrayList<>();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }
    
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    
    public Long getCierreCajaId() { return cierreCajaId; }
    public void setCierreCajaId(Long cierreCajaId) { this.cierreCajaId = cierreCajaId; }
    
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getImpuestoVenta() { return impuestoVenta; }
    public void setImpuestoVenta(BigDecimal impuestoVenta) { this.impuestoVenta = impuestoVenta; }
    
    public BigDecimal getImpuestoServicio() { return impuestoServicio; }
    public void setImpuestoServicio(BigDecimal impuestoServicio) { this.impuestoServicio = impuestoServicio; }
    
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }
    
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    
    public BigDecimal getMontoEfectivo() { return montoEfectivo; }
    public void setMontoEfectivo(BigDecimal montoEfectivo) { this.montoEfectivo = montoEfectivo; }
    
    public BigDecimal getMontoTarjeta() { return montoTarjeta; }
    public void setMontoTarjeta(BigDecimal montoTarjeta) { this.montoTarjeta = montoTarjeta; }
    
    public BigDecimal getVuelto() { return vuelto; }
    public void setVuelto(BigDecimal vuelto) { this.vuelto = vuelto; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public Orden getOrden() { return orden; }
    public void setOrden(Orden orden) { this.orden = orden; }
    
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    
    public List<DetalleFactura> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleFactura> detalles) { this.detalles = detalles; }

    // Métodos auxiliares
    public boolean isActiva() { return "A".equals(this.estado); }
    public boolean isCancelada() { return "C".equals(this.estado); }

    @Override
    public String toString() {
        return "Factura #" + id;
    }
}