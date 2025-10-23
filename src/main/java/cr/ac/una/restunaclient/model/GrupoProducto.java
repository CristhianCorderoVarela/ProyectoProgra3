package cr.ac.una.restunaclient.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de GrupoProducto para el cliente JavaFX
 * Representa categorías de productos (bebidas calientes, platos fuertes, etc.)
 * 
 * @author RestUNA Team
 */
public class GrupoProducto {
    
    private Long id;
    private String nombre;
    private String menuRapido; // S=Sí, N=No
    private Long totalVentas;
    private String estado; // A=Activo, I=Inactivo
    private Long version;
    
    // Lista de productos del grupo (opcional)
    private List<Producto> productos;
    
    /**
     * Constructor por defecto
     */
    public GrupoProducto() {
        this.estado = "A";
        this.menuRapido = "N";
        this.totalVentas = 0L;
        this.productos = new ArrayList<>();
    }

    /**
     * Constructor con parámetros básicos
     */
    public GrupoProducto(Long id, String nombre) {
        this();
        this.id = id;
        this.nombre = nombre;
    }

    /**
     * Constructor completo
     */
    public GrupoProducto(String nombre, String menuRapido) {
        this();
        this.nombre = nombre;
        this.menuRapido = menuRapido;
    }

    
    
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public String getNombre() { 
        return nombre; 
    }
    
    public void setNombre(String nombre) { 
        this.nombre = nombre; 
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
    
    public List<Producto> getProductos() { 
        return productos; 
    }
    
    public void setProductos(List<Producto> productos) { 
        this.productos = productos; 
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Verifica si el grupo está activo
     */
    public boolean isActivo() { 
        return "A".equals(this.estado); 
    }
    
    /**
     * Verifica si el grupo está en el menú rápido
     */
    public boolean isMenuRapido() { 
        return "S".equals(this.menuRapido); 
    }

    /**
     * Activa el grupo
     */
    public void activar() {
        this.estado = "A";
    }

    /**
     * Desactiva el grupo
     */
    public void desactivar() {
        this.estado = "I";
    }

    /**
     * Incluye el grupo en el menú rápido
     */
    public void incluirEnMenuRapido() {
        this.menuRapido = "S";
    }

    /**
     * Excluye el grupo del menú rápido
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
    }

    /**
     * Agrega un producto al grupo
     */
    public void agregarProducto(Producto producto) {
        if (this.productos == null) {
            this.productos = new ArrayList<>();
        }
        if (!this.productos.contains(producto)) {
            this.productos.add(producto);
            producto.setGrupo(this);
        }
    }

    /**
     * Remueve un producto del grupo
     */
    public void removerProducto(Producto producto) {
        if (this.productos != null) {
            this.productos.remove(producto);
        }
    }

    /**
     * Obtiene la cantidad de productos en el grupo
     */
    public int getCantidadProductos() {
        return productos != null ? productos.size() : 0;
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GrupoProducto other = (GrupoProducto) obj;
        return id != null && id.equals(other.id);
    }
}