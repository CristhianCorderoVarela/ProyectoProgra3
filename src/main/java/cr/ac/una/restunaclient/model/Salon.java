package cr.ac.una.restunaclient.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Salon para el cliente JavaFX
 */
public class Salon {
    
    private Long id;
    private String nombre;
    private String tipo; // SALON, BARRA
    private byte[] imagenMesa;
    private String tipoImagen;
    private String cobraServicio; // S=Sí, N=No
    private String estado; // A=Activo, I=Inactivo
    private Long version;
    
    // Lista de mesas del salón
    private List<Mesa> mesas;
    
    public Salon() {
        this.estado = "A";
        this.cobraServicio = "S";
        this.mesas = new ArrayList<>();
    }

    public Salon(Long id, String nombre, String tipo) {
        this();
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public byte[] getImagenMesa() { return imagenMesa; }
    public void setImagenMesa(byte[] imagenMesa) { this.imagenMesa = imagenMesa; }
    
    public String getTipoImagen() { return tipoImagen; }
    public void setTipoImagen(String tipoImagen) { this.tipoImagen = tipoImagen; }
    
    public String getCobraServicio() { return cobraServicio; }
    public void setCobraServicio(String cobraServicio) { this.cobraServicio = cobraServicio; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    public List<Mesa> getMesas() { return mesas; }
    public void setMesas(List<Mesa> mesas) { this.mesas = mesas; }

    // Métodos auxiliares
    public boolean isSalon() { return "SALON".equals(this.tipo); }
    public boolean isBarra() { return "BARRA".equals(this.tipo); }
    public boolean cobraServicio() { return "S".equals(this.cobraServicio); }
    public boolean isActivo() { return "A".equals(this.estado); }

    @Override
    public String toString() {
        return nombre;
    }
}