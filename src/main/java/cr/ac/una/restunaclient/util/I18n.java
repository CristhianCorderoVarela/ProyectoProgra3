package cr.ac.una.restunaclient.util;
import java.util.Locale;
import java.util.ResourceBundle;
/**
 * Clase para manejar la internacionalización (i18n)
 * Permite cambiar entre español e inglés dinámicamente
 * 
 * @author Tu Nombre
 */
public class I18n {

    private static Locale currentLocale = new Locale("es", "CR"); // Español por defecto
    private static ResourceBundle bundle;

    static {
        loadBundle();
    }

    /**
     * Carga el bundle de recursos según el idioma actual
     */
    private static void loadBundle() {
        bundle = ResourceBundle.getBundle("cr.ac.una.restunaclient.i18n.messages", currentLocale);
    }

    /**
     * Obtiene un texto traducido por su clave
     * @param key Clave del texto (ej: "login.titulo")
     * @return Texto traducido
     */
    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "???" + key + "???";
        }
    }

    /**
     * Obtiene un texto traducido con parámetros
     * @param key Clave del texto
     * @param params Parámetros a reemplazar en el texto
     * @return Texto traducido con parámetros
     */
    public static String get(String key, Object... params) {
        try {
            return String.format(bundle.getString(key), params);
        } catch (Exception e) {
            return "???" + key + "???";
        }
    }

    /**
     * Cambia el idioma de la aplicación
     * @param locale Nuevo locale (ej: new Locale("es"), new Locale("en"))
     */
    public static void setLocale(Locale locale) {
        currentLocale = locale;
        loadBundle();
    }

    /**
     * Obtiene el locale actual
     * @return Locale actual
     */
    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    /**
     * Obtiene el idioma actual como String
     * @return "es" o "en"
     */
    public static String getCurrentLanguage() {
        return currentLocale.getLanguage();
    }

    /**
     * Verifica si el idioma actual es español
     * @return true si es español
     */
    public static boolean isSpanish() {
        return "es".equals(currentLocale.getLanguage());
    }

    /**
     * Verifica si el idioma actual es inglés
     * @return true si es inglés
     */
    public static boolean isEnglish() {
        return "en".equals(currentLocale.getLanguage());
    }
}