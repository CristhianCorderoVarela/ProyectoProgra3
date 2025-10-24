package cr.ac.una.restunaclient.util;

import cr.ac.una.restunaclient.model.Usuario;
import java.util.HashMap;
import java.util.Map;

/**
 * Contexto global de la aplicación
 * Almacena información de la sesión actual (usuario logueado, configuraciones, etc.)
 * 
 * @author Tu Nombre
 */
public class AppContext {

    private static AppContext instance;
    private Usuario usuarioLogueado;
    private Map<String, Object> context;

    private AppContext() {
        context = new HashMap<>();
    }

    /**
     * Obtiene la instancia única del contexto (Singleton)
     */
    public static AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    /**
     * Establece el usuario logueado
     */
    public void setUsuarioLogueado(Usuario usuario) {
        this.usuarioLogueado = usuario;
    }

    /**
     * Obtiene el usuario logueado
     */
    public Usuario getUsuarioLogueado() {
        return usuarioLogueado;
    }

    /**
     * Verifica si hay un usuario logueado
     */
    public boolean isLoggedIn() {
        return usuarioLogueado != null;
    }

    /**
     * Cierra la sesión del usuario
     */
    public void logout() {
        usuarioLogueado = null;
        context.clear();
    }

    /**
     * Almacena un valor en el contexto
     */
    public void set(String key, Object value) {
        context.put(key, value);
    }

    /**
     * Obtiene un valor del contexto
     */
    public Object get(String key) {
        return context.get(key);
    }

    /**
     * Elimina un valor del contexto
     */
    public void remove(String key) {
        context.remove(key);
    }

    /**
     * Limpia todo el contexto
     */
    public void clear() {
        context.clear();
    }

    /**
     * Verifica si el usuario es administrador
     */
    public boolean isAdministrador() {
        return usuarioLogueado != null && "ADMINISTRATIVO".equals(usuarioLogueado.getRol());
    }

    /**
     * Verifica si el usuario es cajero
     */
    public boolean isCajero() {
        return usuarioLogueado != null &&
               ("CAJERO".equals(usuarioLogueado.getRol()) || isAdministrador());
    }

    /**
     * Verifica si el usuario es salonero
     */
    public boolean isSalonero() {
        return usuarioLogueado != null &&
               ("SALONERO".equals(usuarioLogueado.getRol()) || isCajero());
    }
}