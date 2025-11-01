package cr.ac.una.restunaclient.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Cliente REST para comunicarse con el backend WsRestUNA.
 * Maneja las peticiones HTTP y la (de)serialización JSON.
 */
public class RestClient {

    // OJO: el contexto real según tu log es /ProyectoProgra3WS y los servicios REST están en /api/*
    // Ejemplo completo: http://localhost:8080/ProyectoProgra3WS/api/usuarios/login
    private static final String BASE_URL = "http://localhost:8080/ProyectoProgra3WS/api";

    private static final Gson gson;

    static {
        // Configurar Gson con adaptadores para LocalDate, LocalDateTime y byte[]
        gson = new GsonBuilder()
                // SERIALIZER para LocalDate (Object -> JSON)
                .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, context)
                        -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                // DESERIALIZER para LocalDate (JSON -> Object)
                .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, context)
                        -> LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE))
                // SERIALIZER para LocalDateTime (Object -> JSON)
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context)
                        -> new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                // DESERIALIZER para LocalDateTime (JSON -> Object)
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context)
                        -> LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                // SERIALIZER para byte[] (convertir a Base64)
                .registerTypeAdapter(byte[].class, (JsonSerializer<byte[]>) (src, typeOfSrc, context)
                        -> new JsonPrimitive(java.util.Base64.getEncoder().encodeToString(src)))
                // DESERIALIZER para byte[] (convertir desde Base64)
                .registerTypeAdapter(byte[].class, (JsonDeserializer<byte[]>) (json, type, context) -> {
                    try {
                        return java.util.Base64.getDecoder().decode(json.getAsString());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .create();
    }

    /**
     * Realiza una petición GET
     * @param endpoint Endpoint (ej: "/usuarios")
     * @return Respuesta como String
     */
    public static String get(String endpoint) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + endpoint);
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.out.println("DEBUG GET " + endpoint + " -> " + result);
                return result;
            }
        }
    }

    /**
     * Realiza una petición POST
     * @param endpoint Endpoint (ej: "/usuarios/login")
     * @param body Cuerpo de la petición (puede ser Map u otro objeto)
     * @return Respuesta como String
     */
    public static String post(String endpoint, Object body) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL + endpoint);
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            request.setHeader("Accept", "application/json");

            String jsonBody = gson.toJson(body);
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.out.println("DEBUG POST " + endpoint + " -> " + result);
                return result;
            }
        }
    }

    /**
     * Realiza una petición PUT
     * @param endpoint Endpoint (ej: "/usuarios/1")
     * @param body Cuerpo de la petición
     * @return Respuesta como String
     */
    public static String put(String endpoint, Object body) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPut request = new HttpPut(BASE_URL + endpoint);
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            request.setHeader("Accept", "application/json");

            String jsonBody = gson.toJson(body);
            request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.out.println("DEBUG PUT " + endpoint + " -> " + result);
                return result;
            }
        }
    }

    /**
     * Realiza una petición DELETE
     * @param endpoint Endpoint (ej: "/usuarios/1")
     * @return Respuesta como String
     */
    public static String delete(String endpoint) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(BASE_URL + endpoint);
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                System.out.println("DEBUG DELETE " + endpoint + " -> " + result);
                return result;
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
     * Parsea la respuesta del backend de forma robusta.
     * Soporta:
     *  - Objeto JSON tipo { "success": true, "message": "...", "data": {...} }
     *  - "true" / "false"
     *  - Texto plano (por ejemplo "Credenciales inválidas" o HTML)
     *
     * @param jsonResponse Respuesta cruda del backend
     * @return Map normalizado con al menos "success" y "message"
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseResponse(String jsonResponse) {
        String body = (jsonResponse == null) ? "" : jsonResponse.trim();
        Map<String, Object> out = new HashMap<>();

        if (body.isEmpty()) {
            out.put("success", false);
            out.put("message", "Respuesta vacía del servidor");
            return out;
        }

        // Caso 1: parece un objeto JSON
        if (body.startsWith("{")) {
            try {
                Map<String, Object> parsed = new HashMap<>(gson.fromJson(body, Map.class));

                // Aseguramos que siempre haya campo success
                if (!parsed.containsKey("success")) {
                    parsed.put("success", true);
                }

                return parsed;
            } catch (Exception ex) {
                out.put("success", false);
                out.put("message", "JSON inválido: " + ex.getMessage());
                out.put("raw", body);
                return out;
            }
        }

        // Caso 2: boolean plano
        if ("true".equalsIgnoreCase(body)) {
            out.put("success", true);
            out.put("message", "OK");
            return out;
        }
        if ("false".equalsIgnoreCase(body)) {
            out.put("success", false);
            out.put("message", "Operación no exitosa");
            return out;
        }

        // Caso 3: string entre comillas -> las quitamos
        if (body.length() >= 2 && body.startsWith("\"") && body.endsWith("\"")) {
            body = body.substring(1, body.length() - 1);
        }

        // Caso 4: texto plano / HTML / mensaje de error del backend
        out.put("success", false);
        out.put("message", body);
        out.put("raw", body);
        return out;
    }
    // --- NUEVO: GET binario (PDF, etc.) ---
public static byte[] getBytes(String endpoint, Map<String, String> headers) throws Exception {
    try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
        HttpGet request = constructGet(BASE_URL + endpoint);
        // headers custom (ej. Accept: application/pdf)
        if (headers != null) {
            headers.forEach(request::setHeader);
        } else {
            request.setHeader("Accept", "*/*");
        }
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            byte[] bytes = EntityUtils.toByteArray(response.getEntity());
            System.out.println("DEBUG GET[bytes] " + endpoint + " -> " + bytes.length + " bytes");
            return bytes;
        }
    }
}

// --- (Opcional) helper para fijar headers comunes GET ---
private static HttpGet constructGet(String url) {
    HttpGet request = new HttpGet(url);
    request.setHeader("Content-Type", "application/json; charset=UTF-8");
    request.setHeader("Accept", "application/json");
    return request;
}
}