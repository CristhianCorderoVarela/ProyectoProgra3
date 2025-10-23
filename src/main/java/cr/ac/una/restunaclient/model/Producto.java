package cr.ac.una.restunaclient.model;

import java.math.BigDecimal;

/**
 * Modelo de Producto para el cliente JavaFX
 * Representa un producto del menú del restaurante
 * 
 * @author RestUNA Team
 */
public class Producto {
    
    private Long id;
    private Long grupoId;
    private String nombre;
    private String nombreCorto;
    private BigDecimal precio;
    private String menuRapido; // S=Sí, N=No
    private Long totalVentas;
    private String estado; // A=Activo, I=Inactivo
    private Long version;
    
    // Referencia al grupo (opcional)
    private GrupoProducto grupo;
    
    /**
     * Constructor por defecto
     */
    public Producto() {
        this.estado = "A";
        this.menuRapido = "N";
        this.totalVentas = 0L;
        this.precio = BigDecimal.ZERO;
    }

    /**
     * Constructor con parámetros básicos
     */
    public Producto(Long id, String nombre, BigDecimal precio) {
        this();
        this.id = id;
        this.nombre = nombre;
        this.precio = precio;
    }

    /**
     * Constructor completo
     */
    public Producto(String nombre, String nombreCorto, BigDecimal precio, GrupoProducto grupo) {
        this();
        this.nombre = nombre;
        this.nombreCorto = nombreCorto;
        this.precio = precio;
        this.grupo = grupo;
        if (grupo != null) {
            this.grupoId = grupo.getId();
        }
    }

    // ==================== GETTERS Y SETTERS ====================
    
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public Long getGrupoId() { 
        return grupoId; 
    }
    
    public void setGrupoId(Long grupoId) { 
        this.grupoId = grupoId; 
    }
    
    public String getNombre() { 
        return nombre; 
    }
    
    public void setNombre(String nombre) { 
        this.nombre = nombre; 
    }
    
    public String getNombreCorto() { 
        return nombreCorto; 
    }
    
    public void setNombreCorto(String nombreCorto) { 
        this.nombreCorto = nombreCorto; 
    }
    
    public BigDecimal getPrecio() { 
        return precio; 
    }
    
    public void setPrecio(BigDecimal precio) { 
        this.precio = precio; 
    }
    
    public String getMenuRapido() { 
        return menuRapido; 
    }
    
    public void setMenuRapido(String menuRapido) { 
        this.menuRapido = menuRapido; 
    }
    
    public Long getTotalVentas() { 
        return totalVentas; 
    }
    
    public void setTotalVentas(Long totalVentas) { 
        this.totalVentas = totalVentas; 
    }
    
    public String getEstado() { 
        return estado; 
    }
    
    public void setEstado(String estado) { 
        this.estado = estado; 
    }
    
    public Long getVersion() { 
        return version; 
    }
    
    public void setVersion(Long version) { 
        this.version = version; 
    }
    
    public GrupoProducto getGrupo() { 
        return grupo; 
    }
    
    public void setGrupo(GrupoProducto grupo) { 
        this.grupo = grupo;
        if (grupo != null) {
            this.grupoId = grupo.getId();
        }
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Verifica si el producto está activo
     */
    public boolean isActivo() { 
        return "A".equals(this.estado); 
    }
    
    /**
     * Verifica si el producto está en el menú rápido
     */
    public boolean isMenuRapido() { 
        return "S".equals(this.menuRapido); 
    }

    /**
     * Activa el producto
     */
    public void activar() {
        this.estado = "A";
    }

    /**
     * Desactiva el producto
     */
    public void desactivar() {
        this.estado = "I";
    }

    /**
     * Incluye el producto en el menú rápido
     */
    public void incluirEnMenuRapido() {
        this.menuRapido = "S";
    }

    /**
     * Excluye el producto del menú rápido
     */
    public void excluirDeMenuRapido() {
        this.menuRapido = "N";
    }

    /**
     * Incrementa el contador de ventas
     */
    public void incrementarVentas() {
        if (this.totalVentas == null) {
            this.totalVentas = 0L;
        }
        this.totalVentas++;
        
        // También incrementar en el grupo si existe
        if (this.grupo != null) {
            this.grupo.incrementarVentas();
        }
    }

    /**
     * Obtiene el nombre a mostrar (corto si existe, sino el completo)
     */
    public String getNombreDisplay() {
        return nombreCorto != null && !nombreCorto.isEmpty() ? nombreCorto : nombre;
    }

    /**
     * Obtiene el precio formateado como String
     */
    public String getPrecioFormateado() {
        if (precio == null) {
            return "₡0.00";
        }
        return String.format("₡%.2f", precio);
    }

    /**
     * Verifica si el producto tiene un precio válido
     */
    public boolean tienePrecioValido() {
        return precio != null && precio.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Obtiene el nombre del grupo (si tiene)
     */
    public String getNombreGrupo() {
        return grupo != null ? grupo.getNombre() : "Sin grupo";
    }

    @Override
    public String toString() {
        return getNombreDisplay();
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Producto other = (Producto) obj;
        return id != null && id.equals(other.id);
    }
}