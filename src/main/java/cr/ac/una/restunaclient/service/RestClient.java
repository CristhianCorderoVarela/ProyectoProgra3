package cr.ac.una.restunaclient.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Cliente REST para comunicarse con el backend WsRestUNA
 * 
 * @author Tu Nombre
 */
public class RestClient {
    
    private static final String BASE_URL = "http://localhost:8080/WsRestUNA/api";
    private static final Gson gson;
    
    static {
        // Configurar Gson con adaptadores para LocalDate y LocalDateTime
        gson = new GsonBuilder()
                // SERIALIZER para LocalDate (Object -> JSON)
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                // DESERIALIZER para LocalDate (JSON -> Object)
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, context) ->
                        LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                // SERIALIZER para LocalDateTime (Object -> JSON)
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                // DESERIALIZER para LocalDateTime (JSON -> Object)
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                        LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();
    }
    
    /**
     * Realiza una petición GET
     * @param endpoint Endpoint (ej: "/usuarios")
     * @return Respuesta como String JSON
     */
    public static String get(String endpoint) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + endpoint);
            request.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
    
    /**
     * Realiza una petición POST
     * @param endpoint Endpoint (ej: "/usuarios/login")
     * @param body Cuerpo de la petición (puede ser Map o cualquier objeto)
     * @return Respuesta como String JSON
     */
    public static String post(String endpoint, Object body) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL + endpoint);
            request.setHeader("Content-Type", "application/json");
            
            String jsonBody = gson.toJson(body);
            request.setEntity(new StringEntity(jsonBody));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
    
    /**
     * Realiza una petición PUT
     * @param endpoint Endpoint (ej: "/usuarios/1")
     * @param body Cuerpo de la petición
     * @return Respuesta como String JSON
     */
    public static String put(String endpoint, Object body) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(BASE_URL + endpoint);
            request.setHeader("Content-Type", "application/json");
            
            String jsonBody = gson.toJson(body);
            request.setEntity(new StringEntity(jsonBody));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
    
    /**
     * Realiza una petición DELETE
     * @param endpoint Endpoint (ej: "/usuarios/1")
     * @return Respuesta como String JSON
     */
    public static String delete(String endpoint) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BASE_URL + endpoint);
            request.setHeader("Content-Type", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }
    
    /**
     * Convierte un JSON a un objeto usando Gson
     * @param json JSON como String
     * @param clazz Clase del objeto destino
     * @return Objeto deserializado
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
    
    /**
     * Convierte un objeto a JSON usando Gson
     * @param object Objeto a convertir
     * @return JSON como String
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }
    
    /**
     * Parsea una respuesta estándar del backend {success, message, data}
     * @param jsonResponse Respuesta JSON del backend
     * @return Map con success, message y data
     */
    @SuppressWarnings("unchecked")
public static Map<String, Object> parseResponse(String jsonResponse) {
    if (jsonResponse == null) {
        return Map.of("success", false, "message", "Respuesta nula del servidor");
    }
    String s = jsonResponse.trim();
    if (s.isEmpty()) {
        return Map.of("success", false, "message", "Respuesta vacía del servidor");
    }

    try {
        // Caso 1: empieza como JSON objeto/arreglo
        if (s.startsWith("{")) {
            return gson.fromJson(s, Map.class); // objeto esperado
        } else if (s.startsWith("[")) {
            // si vino una lista, la envolvemos en un objeto estándar
            Object list = gson.fromJson(s, Object.class);
            return Map.of("success", true, "message", "OK", "data", list);
        } else if (s.startsWith("\"") && s.endsWith("\"")) {
            // Caso 2: cadena JSON entrecomillada => des-escapar y tratar como mensaje
            String text = gson.fromJson(s, String.class);
            return Map.of("success", false, "message", text);
        } else {
            // Caso 3: texto plano (HTML, “Usuario creado”, etc.)
            return Map.of("success", false, "message", s);
        }
    } catch (Exception ex) {
        // Si igual falló, no tumbar la app: devolver mensaje como texto
        return Map.of("success", false, "message", s, "error", ex.getClass().getSimpleName());
    }
}

}