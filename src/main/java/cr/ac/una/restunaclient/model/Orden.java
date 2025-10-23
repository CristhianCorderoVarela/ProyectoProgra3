package cr.ac.una.restunaclient.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Orden para el cliente JavaFX
 */
public class Orden {
    
    private Long id;
    private Long mesaId;
    private Long usuarioId;
    private LocalDateTime fechaHora;
    private String estado; // ABIERTA, FACTURADA, CANCELADA
    private String observaciones;
    private Long version;
    
    // Referencias opcionales
    private Mesa mesa;
    private Usuario usuario;
    private List<DetalleOrden> detalles;
    
    public Orden() {
        this.estado = "ABIERTA";
        this.fechaHora = LocalDateTime.now();
        this.detalles = new ArrayList<>();
    }

    public Orden(Long id) {
        this();
        this.id = id;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getMesaId() { return mesaId; }
    public void setMesaId(Long mesaId) { this.mesaId = mesaId; }
    
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }
    
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    
    public List<DetalleOrden> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleOrden> detalles) { this.detalles = detalles; }

    // Métodos auxiliares
    public boolean isAbierta() { return "ABIERTA".equals(this.estado); }
    public boolean isFacturada() { return "FACTURADA".equals(this.estado); }
    public boolean isCancelada() { return "CANCELADA".equals(this.estado); }

    /**
     * Calcula el total de la orden sumando todos los detalles
     */
    public BigDecimal calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (detalles != null) {
            for (DetalleOrden detalle : detalles) {
                if (detalle.getSubtotal() != null) {
                    total = total.add(detalle.getSubtotal());
                }
            }
        }
        return total;
    }

    @Override
    public String toString() {
        return "Orden #" + id;
    }
}