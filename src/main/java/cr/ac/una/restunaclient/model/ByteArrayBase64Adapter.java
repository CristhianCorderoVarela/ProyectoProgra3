package cr.ac.una.restunaclient.model;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Base64;

/**
 * Adaptador Gson para serializar/deserializar byte[] como Base64
 */
public class ByteArrayBase64Adapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {
    
    @Override
    public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null || src.length == 0) {
            return JsonNull.INSTANCE;
        }
        return new JsonPrimitive(Base64.getEncoder().encodeToString(src));
    }
    
    @Override
    public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        if (json.isJsonNull() || json.getAsString().isEmpty()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(json.getAsString());
        } catch (Exception e) {
            System.err.println("⚠️ Error al decodificar Base64: " + e.getMessage());
            return null;
        }
    }
}