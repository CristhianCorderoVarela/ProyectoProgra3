package cr.ac.una.restunaclient.model;

import java.math.BigDecimal;

/**
 * Modelo de Parametros para el cliente JavaFX
 * Configuración general del sistema
 */
public class Parametros {
    
    private Long id;
    private String idioma; // es, en
    private BigDecimal porcImpuestoVenta;
    private BigDecimal porcImpuestoServicio;
    private BigDecimal porcDescuentoMaximo;
    private String nombreRestaurante;
    private String telefono1;
    private String telefono2;
    private String direccion;
    private String correoSistema;
    private String claveCorreoSistema;
    private Long version;
    
    public Parametros() {
        this.idioma = "es";
        this.porcImpuestoVenta = new BigDecimal("13.00");
        this.porcImpuestoServicio = new BigDecimal("10.00");
        this.porcDescuentoMaximo = new BigDecimal("10.00");
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }
    
    public BigDecimal getPorcImpuestoVenta() { return porcImpuestoVenta; }
    public void setPorcImpuestoVenta(BigDecimal porcImpuestoVenta) { 
        this.porcImpuestoVenta = porcImpuestoVenta; 
    }
    
    public BigDecimal getPorcImpuestoServicio() { return porcImpuestoServicio; }
    public void setPorcImpuestoServicio(BigDecimal porcImpuestoServicio) { 
        this.porcImpuestoServicio = porcImpuestoServicio; 
    }
    
    public BigDecimal getPorcDescuentoMaximo() { return porcDescuentoMaximo; }
    public void setPorcDescuentoMaximo(BigDecimal porcDescuentoMaximo) { 
        this.porcDescuentoMaximo = porcDescuentoMaximo; 
    }
    
    public String getNombreRestaurante() { return nombreRestaurante; }
    public void setNombreRestaurante(String nombreRestaurante) { 
        this.nombreRestaurante = nombreRestaurante; 
    }
    
    public String getTelefono1() { return telefono1; }
    public void setTelefono1(String telefono1) { this.telefono1 = telefono1; }
    
    public String getTelefono2() { return telefono2; }
    public void setTelefono2(String telefono2) { this.telefono2 = telefono2; }
    
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    
    public String getCorreoSistema() { return correoSistema; }
    public void setCorreoSistema(String correoSistema) { this.correoSistema = correoSistema; }
    
    public String getClaveCorreoSistema() { return claveCorreoSistema; }
    public void setClaveCorreoSistema(String claveCorreoSistema) { 
        this.claveCorreoSistema = claveCorreoSistema; 
    }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    /**
     * Calcula el impuesto de venta sobre un monto
     */
    public BigDecimal calcularImpuestoVenta(BigDecimal monto) {
        if (monto == null || porcImpuestoVenta == null) {
            return BigDecimal.ZERO;
        }
        return monto.multiply(porcImpuestoVenta).divide(new BigDecimal("100"));
    }

    /**
     * Calcula el impuesto de servicio sobre un monto
     */
    public BigDecimal calcularImpuestoServicio(BigDecimal monto) {
        if (monto == null || porcImpuestoServicio == null) {
            return BigDecimal.ZERO;
        }
        return monto.multiply(porcImpuestoServicio).divide(new BigDecimal("100"));
    }

    @Override
    public String toString() {
        return nombreRestaurante != null ? nombreRestaurante : "Parámetros";
    }
}