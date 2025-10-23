package cr.ac.una.restunaclient.model;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo de Mesa para el cliente JavaFX
 * Representa una mesa dentro de un salón del restaurante
 * 
 * IMPORTANTE: No incluir referencia al objeto Salon completo
 * para evitar referencias circulares en JSON
 */
public class Mesa {
    
    private Long id;
    
    @SerializedName("salon")
    private SalonReference salon; // Solo para deserialización del backend
    
    private String identificador;
    private Double posicionX;
    private Double posicionY;
    private String estado; // LIBRE, OCUPADA
    private Long version;
    
    // Clase interna para manejar la referencia mínima al salón
    public static class SalonReference {
        private Long id;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
    }
    
    /**
     * Constructor por defecto
     */
    public Mesa() {
        this.estado = "LIBRE";
        this.posicionX = 0.0;
        this.posicionY = 0.0;
    }
    
    /**
     * Constructor con parámetros básicos
     */
    public Mesa(String identificador, Double posicionX, Double posicionY) {
        this();
        this.identificador = identificador;
        this.posicionX = posicionX;
        this.posicionY = posicionY;
    }
    
    // ==================== GETTERS Y SETTERS ====================
    
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }
    
    public Long getSalonId() {
        return salon != null ? salon.getId() : null;
    }
    
    public void setSalonId(Long salonId) {
        if (this.salon == null) {
            this.salon = new SalonReference();
        }
        this.salon.setId(salonId);
    }
    
    public String getIdentificador() { 
        return identificador; 
    }
    
    public void setIdentificador(String identificador) { 
        this.identificador = identificador; 
    }
    
    public Double getPosicionX() { 
        return posicionX != null ? posicionX : 0.0; 
    }
    
    public void setPosicionX(Double posicionX) { 
        this.posicionX = posicionX; 
    }
    
    public Double getPosicionY() { 
        return posicionY != null ? posicionY : 0.0; 
    }
    
    public void setPosicionY(Double posicionY) { 
        this.posicionY = posicionY; 
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
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Verifica si la mesa está libre
     */
    public boolean isLibre() { 
        return "LIBRE".equals(this.estado); 
    }
    
    /**
     * Verifica si la mesa está ocupada
     */
    public boolean isOcupada() { 
        return "OCUPADA".equals(this.estado); 
    }
    
    /**
     * Marca la mesa como ocupada
     */
    public void ocupar() { 
        this.estado = "OCUPADA"; 
    }
    
    /**
     * Marca la mesa como libre
     */
    public void liberar() { 
        this.estado = "LIBRE"; 
    }
    
    /**
     * Actualiza la posición de la mesa (para drag & drop)
     */
    public void actualizarPosicion(Double x, Double y) {
        this.posicionX = x;
        this.posicionY = y;
    }
    
    /**
     * Crea una copia simple de la mesa para enviar al backend
     * (solo con los campos necesarios para actualizar)
     */
    public Mesa toUpdateDTO() {
        Mesa dto = new Mesa();
        dto.setId(this.id);
        dto.setPosicionX(this.posicionX);
        dto.setPosicionY(this.posicionY);
        return dto;
    }
    
    @Override
    public String toString() {
        return identificador;
    }
    
    @Override
    public int hashCode() {
        return (id != null ? id.hashCode() : 0);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Mesa other = (Mesa) obj;
        return id != null && id.equals(other.id);
    }
}