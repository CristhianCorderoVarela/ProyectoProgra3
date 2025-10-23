package cr.ac.una.restunaclient.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de CierreCaja para el cliente JavaFX
 */
public class CierreCaja {
    
    private Long id;
    private Long usuarioId;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal efectivoDeclarado;
    private BigDecimal tarjetaDeclarado;
    private BigDecimal efectivoSistema;
    private BigDecimal tarjetaSistema;
    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaTarjeta;
    private String estado; // ABIERTO, CERRADO
    private Long version;
    
    // Referencia al usuario
    private Usuario usuario;
    
    public CierreCaja() {
        this.estado = "ABIERTO";
        this.fechaApertura = LocalDateTime.now();
        this.efectivoSistema = BigDecimal.ZERO;
        this.tarjetaSistema = BigDecimal.ZERO;
        this.diferenciaEfectivo = BigDecimal.ZERO;
        this.diferenciaTarjeta = BigDecimal.ZERO;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    
    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }
    
    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
    
    public BigDecimal getEfectivoDeclarado() { return efectivoDeclarado; }
    public void setEfectivoDeclarado(BigDecimal efectivoDeclarado) { 
        this.efectivoDeclarado = efectivoDeclarado; 
    }
    
    public BigDecimal getTarjetaDeclarado() { return tarjetaDeclarado; }
    public void setTarjetaDeclarado(BigDecimal tarjetaDeclarado) { 
        this.tarjetaDeclarado = tarjetaDeclarado; 
    }
    
    public BigDecimal getEfectivoSistema() { return efectivoSistema; }
    public void setEfectivoSistema(BigDecimal efectivoSistema) { 
        this.efectivoSistema = efectivoSistema; 
    }
    
    public BigDecimal getTarjetaSistema() { return tarjetaSistema; }
    public void setTarjetaSistema(BigDecimal tarjetaSistema) { 
        this.tarjetaSistema = tarjetaSistema; 
    }
    
    public BigDecimal getDiferenciaEfectivo() { return diferenciaEfectivo; }
    public void setDiferenciaEfectivo(BigDecimal diferenciaEfectivo) { 
        this.diferenciaEfectivo = diferenciaEfectivo; 
    }
    
    public BigDecimal getDiferenciaTarjeta() { return diferenciaTarjeta; }
    public void setDiferenciaTarjeta(BigDecimal diferenciaTarjeta) { 
        this.diferenciaTarjeta = diferenciaTarjeta; 
    }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    // Métodos auxiliares
    public boolean isAbierto() { return "ABIERTO".equals(this.estado); }
    public boolean isCerrado() { return "CERRADO".equals(this.estado); }

    /**
     * Calcula las diferencias entre lo declarado y lo registrado en el sistema
     */
    public void calcularDiferencias() {
        if (efectivoDeclarado != null && efectivoSistema != null) {
            this.diferenciaEfectivo = efectivoDeclarado.subtract(efectivoSistema);
        }
        if (tarjetaDeclarado != null && tarjetaSistema != null) {
            this.diferenciaTarjeta = tarjetaDeclarado.subtract(tarjetaSistema);
        }
    }

    @Override
    public String toString() {
        return "Cierre #" + id + " - " + estado;
    }
}