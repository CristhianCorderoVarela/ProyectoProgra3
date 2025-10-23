package cr.ac.una.restunaclient.model;

import java.time.LocalDate;

/**
 * Modelo de Cliente para el cliente JavaFX
 */
public class Cliente {
    
    private Long id;
    private String nombre;
    private String correo;
    private String telefono;
    private String estado; // A=Activo, I=Inactivo
    private LocalDate fechaCreacion;
    private Long version;
    
    public Cliente() {
        this.estado = "A";
        this.fechaCreacion = LocalDate.now();
    }

    public Cliente(Long id, String nombre) {
        this();
        this.id = id;
        this.nombre = nombre;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    // Métodos auxiliares
    public boolean isActivo() { return "A".equals(this.estado); }

    @Override
    public String toString() {
        return nombre;
    }
}

