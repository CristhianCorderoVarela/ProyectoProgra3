package cr.ac.una.restunaclient.model;

import java.time.LocalDate;

/**
 * Modelo de Usuario para el cliente JavaFX
 * Representa los datos del usuario que vienen del backend REST
 * 
 * @author Tu Nombre
 */
public class Usuario {
    
    private Long id;
    private String nombre;
    private String usuario;
    private String clave;
    private String rol; // ADMINISTRATIVO, CAJERO, SALONERO
    private String estado; // A=Activo, I=Inactivo
    private LocalDate fechaCreacion;
    private Long version;
    
    // Campos auxiliares del JSON (que vienen del backend)
    private Boolean activo;
    private Boolean administrativo;
    private Boolean cajero;
    private Boolean salonero;
    
    public Usuario() {
    }

    public Usuario(Long id, String nombre, String usuario, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.usuario = usuario;
        this.rol = rol;
    }

    // Getters y Setters
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

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getAdministrativo() {
        return administrativo;
    }

    public void setAdministrativo(Boolean administrativo) {
        this.administrativo = administrativo;
    }

    public Boolean getCajero() {
        return cajero;
    }

    public void setCajero(Boolean cajero) {
        this.cajero = cajero;
    }

    public Boolean getSalonero() {
        return salonero;
    }

    public void setSalonero(Boolean salonero) {
        this.salonero = salonero;
    }

    // Métodos auxiliares
    public boolean isActivo() {
        return "A".equals(estado);
    }
    
    public boolean isAdministrativo() {
        return "ADMINISTRATIVO".equals(rol);
    }
    
    public boolean isCajero() {
        return "CAJERO".equals(rol);
    }
    
    public boolean isSalonero() {
        return "SALONERO".equals(rol);
    }

    @Override
    public String toString() {
        return nombre + " (" + usuario + ")";
    }
}