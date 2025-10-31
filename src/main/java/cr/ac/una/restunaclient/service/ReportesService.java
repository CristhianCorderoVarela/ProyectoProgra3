package cr.ac.una.restunaclient.service;

import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ReportesService sin rutas "adivinadas".
 * - Descubre los endpoints leyendo /openapi (MicroProfile OpenAPI de Payara).
 * - Cachea la resolución de cada clave (p.ej. "reportes.facturas.get", "reportes.facturas.pdf").
 * - Llama a la ruta EXACTA que el backend expone.
 *
 * Requisitos backend:
 *   1) Payara con MicroProfile OpenAPI habilitado.
 *   2) Tu app desplegada como /ProyectoProgra3WS con @ApplicationPath("/api") (ya lo tienes).
 *   3) OpenAPI disponible en:   http://localhost:8080/ProyectoProgra3WS/openapi
 *
 * Cómo mapea claves:
 *   - "reportes.facturas.get"  -> busca un path GET que contenga "factura" (o "facturas") y acepte params fechaInicio & fechaFin
 *   - "reportes.facturas.pdf"  -> busca un path GET que contenga "factura" y "pdf" y produzca application/pdf
 *   - "reportes.cierres.get"   -> path GET que contenga "cierre" y acepte fecha|usuario
 *   - "reportes.productos.top" -> path GET que contenga "producto" y "top" (o "mas-ven")
 *   - "reportes.ventas.periodo.get" -> path GET con "ventas" y "periodo"
 *   - "reportes.ventas.salonero.get"-> path GET con "ventas" y "salonero"
 *   - "reportes.clientes.top.get"   -> path GET con "cliente" y "top|frecu"
 *   - "reportes.descuentos.get"     -> path GET con "descuento"
 *
 * Si no encuentra un path en OpenAPI, lanza RuntimeException con mensaje claro (no inventa nada).
 */
public class ReportesService {

    private static final String BASE = "http://localhost:8080/ProyectoProgra3WS";
    private static final String API  = "/api";
    private static final String OPENAPI_PATH = "/openapi";

    private static final Gson G = new GsonBuilder().create();

    // Cache de rutas descubiertas por clave lógica
    private final Map<String, String> pathCache = new ConcurrentHashMap<>();
    // Documento OpenAPI cacheado
    private volatile JsonObject openApiDoc;

    // ---------------------- Público: métodos que usa tu ReportesController ----------------------

    public List<Map<String,Object>> facturas(LocalDate desde, LocalDate hasta, String cajeroNombre, String estado) throws Exception {
        String path = resolvePath("reportes.facturas.get");
        String qs   = query(
                "fechaInicio", iso(desde),
                "fechaFin",    iso(hasta),
                // solo agregamos filtros si el OpenAPI indica que existen como parámetros:
                // (la lógica de filtro de params vive en buildQueryRespectingOpenAPI)
                "estado",      nz(estado),
                "cajero",      nz(cajeroNombre),
                "usuario",     nz(cajeroNombre),
                "usuarioId",   null,   // no inventamos ID; si OpenAPI pide usuarioId tendrás que mapear nombre->id en el cliente
                "clienteId",   null
        );
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        String body = httpGetText(url);
        ensureJsonNot404(body, url);
        return extractList(body);
    }

    public File facturasPdf(LocalDate desde, LocalDate hasta, String cajeroNombre, String estado) throws Exception {
        String path = resolvePath("reportes.facturas.pdf");
        String qs   = query(
                "fechaInicio", iso(desde),
                "fechaFin",    iso(hasta),
                "estado",      nz(estado),
                "cajero",      nz(cajeroNombre),
                "usuario",     nz(cajeroNombre)
        );
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        return downloadPdf(url, "facturas");
    }

    public List<Map<String,Object>> cierres(LocalDate fecha, String cajeroNombre) throws Exception {
        String path = resolvePath("reportes.cierres.get");
        String qs   = query("fecha", iso(fecha), "cajero", nz(cajeroNombre), "usuario", nz(cajeroNombre));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        String body = httpGetText(url);
        ensureJsonNot404(body, url);
        return extractList(body);
    }

    public File cierrePdf(LocalDate fecha, String cajeroNombre) throws Exception {
        String path = resolvePath("reportes.cierres.pdf");
        String qs   = query("fecha", iso(fecha), "cajero", nz(cajeroNombre), "usuario", nz(cajeroNombre));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        return downloadPdf(url, "cierre-caja");
    }

    public List<Map<String,Object>> productosTop(LocalDate desde, LocalDate hasta, String grupo, Integer top) throws Exception {
        String path = resolvePath("reportes.productos.top");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta), "grupo", nz(grupo),
                            "top", top == null ? null : String.valueOf(top));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        String body = httpGetText(url);
        ensureJsonNot404(body, url);
        return extractList(body);
    }

    public File productosTopPdf(LocalDate desde, LocalDate hasta, String grupo, Integer top) throws Exception {
        String path = resolvePath("reportes.productos.top.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta), "grupo", nz(grupo),
                            "top", top == null ? null : String.valueOf(top));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        return downloadPdf(url, "productos-top");
    }

    public List<Map<String,Object>> ventasPeriodo(LocalDate desde, LocalDate hasta) throws Exception {
        String path = resolvePath("reportes.ventas.periodo.get");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        String body = httpGetText(url);
        ensureJsonNot404(body, url);
        return extractList(body);
    }

    public File ventasPeriodoPdf(LocalDate desde, LocalDate hasta) throws Exception {
        String path = resolvePath("reportes.ventas.periodo.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        return downloadPdf(url, "ventas-periodo");
    }

    public List<Map<String,Object>> ventasSalonero(LocalDate desde, LocalDate hasta, String saloneroNombre) throws Exception {
        String path = resolvePath("reportes.ventas.salonero.get");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta), "salonero", nz(saloneroNombre), "usuario", nz(saloneroNombre));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        String body = httpGetText(url);
        ensureJsonNot404(body, url);
        return extractList(body);
    }

    public File ventasSaloneroPdf(LocalDate desde, LocalDate hasta, String saloneroNombre) throws Exception {
        String path = resolvePath("reportes.ventas.salonero.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta), "salonero", nz(saloneroNombre), "usuario", nz(saloneroNombre));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        return downloadPdf(url, "ventas-salonero");
    }

    public List<Map<String,Object>> clientesTop(LocalDate desde, LocalDate hasta, Integer top) throws Exception {
        String path = resolvePath("reportes.clientes.top.get");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta), "top", top == null? null : String.valueOf(top));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        String body = httpGetText(url);
        ensureJsonNot404(body, url);
        return extractList(body);
    }

    public File clientesTopPdf(LocalDate desde, LocalDate hasta, Integer top) throws Exception {
        String path = resolvePath("reportes.clientes.top.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta), "top", top == null? null : String.valueOf(top));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        return downloadPdf(url, "clientes-top");
    }

    public List<Map<String,Object>> descuentos(LocalDate desde, LocalDate hasta, String cajeroNombre) throws Exception {
        String path = resolvePath("reportes.descuentos.get");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta), "cajero", nz(cajeroNombre), "usuario", nz(cajeroNombre));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        String body = httpGetText(url);
        ensureJsonNot404(body, url);
        return extractList(body);
    }

    public File descuentosPdf(LocalDate desde, LocalDate hasta, String cajeroNombre) throws Exception {
        String path = resolvePath("reportes.descuentos.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta), "cajero", nz(cajeroNombre), "usuario", nz(cajeroNombre));
        String url  = BASE + API + path + buildQueryRespectingOpenAPI(path, qs);
        return downloadPdf(url, "descuentos");
    }

    // ---------------------- Resolución de rutas por OpenAPI ----------------------

    private String resolvePath(String key) throws Exception {
        String cached = pathCache.get(key);
        if (cached != null) return cached;

        JsonObject openapi = loadOpenApi();
        JsonObject paths = openapi.getAsJsonObject("paths");
        if (paths == null || paths.entrySet().isEmpty()) {
            throw new RuntimeException("No se encontró 'paths' en OpenAPI (" + BASE + OPENAPI_PATH + "). Activa OpenAPI en el backend.");
        }

        String chosen = switch (key) {
            case "reportes.facturas.get"       -> findPath(paths, "get",    p -> containsAny(p,"/factura","/facturas") && hasParams(paths, p,"fechaInicio","fechaFin"));
            case "reportes.facturas.pdf"       -> findPath(paths, "get",    p -> containsAny(p,"factura") && containsAny(p,"/pdf","/reporte","/reporte-pdf") && producesPdf(paths,p));
            case "reportes.cierres.get"        -> findPath(paths, "get",    p -> containsAny(p,"cierre") && (hasAnyParam(paths,p,"fecha","fechaInicio","fechaFin") ));
            case "reportes.cierres.pdf"        -> findPath(paths, "get",    p -> containsAny(p,"cierre") && containsAny(p,"/pdf","/reporte") && producesPdf(paths,p));
            case "reportes.productos.top"      -> findPath(paths, "get",    p -> containsAny(p,"producto") && containsAny(p,"top","mas-ven","masVend"));
            case "reportes.productos.top.pdf"  -> findPath(paths, "get",    p -> containsAny(p,"producto") && containsAny(p,"top","mas-ven","masVend") && containsAny(p,"pdf") && producesPdf(paths,p));
            case "reportes.ventas.periodo.get" -> findPath(paths, "get",    p -> containsAll(p,"ventas","periodo"));
            case "reportes.ventas.periodo.pdf" -> findPath(paths, "get",    p -> containsAll(p,"ventas","periodo") && containsAny(p,"pdf") && producesPdf(paths,p));
            case "reportes.ventas.salonero.get"-> findPath(paths, "get",    p -> containsAll(p,"ventas","salonero"));
            case "reportes.ventas.salonero.pdf"-> findPath(paths, "get",    p -> containsAll(p,"ventas","salonero") && containsAny(p,"pdf") && producesPdf(paths,p));
            case "reportes.clientes.top.get"   -> findPath(paths, "get",    p -> containsAny(p,"cliente") && containsAny(p,"top","frecu"));
            case "reportes.clientes.top.pdf"   -> findPath(paths, "get",    p -> containsAny(p,"cliente") && containsAny(p,"top","frecu") && containsAny(p,"pdf") && producesPdf(paths,p));
            case "reportes.descuentos.get"     -> findPath(paths, "get",    p -> containsAny(p,"descuento"));
            case "reportes.descuentos.pdf"     -> findPath(paths, "get",    p -> containsAny(p,"descuento") && containsAny(p,"pdf") && producesPdf(paths,p));
            default -> null;
        };

        if (chosen == null) {
            throw new RuntimeException(
                "No encuentro en OpenAPI un path para la clave '" + key +
                "'. Revisa que el recurso exista y que OpenAPI lo exponga. URL: " + (BASE + OPENAPI_PATH)
            );
        }
        pathCache.put(key, chosen);
        return chosen;
    }

    private JsonObject loadOpenApi() throws Exception {
        JsonObject local = openApiDoc;
        if (local != null) return local;

        String url = BASE + OPENAPI_PATH;
        String body = httpGetText(url);
        if (body == null || !body.trim().startsWith("{")) {
            throw new RuntimeException("OpenAPI no disponible en " + url + ". Activa @OpenAPIDefinition y MP OpenAPI en Payara.");
        }
        JsonObject parsed = G.fromJson(body, JsonObject.class);
        this.openApiDoc = parsed;
        return parsed;
    }

    private static boolean containsAny(String path, String... needles) {
        String p = path.toLowerCase(Locale.ROOT);
        for (String n : needles) if (p.contains(n.toLowerCase(Locale.ROOT))) return true;
        return false;
    }
    private static boolean containsAll(String path, String... needles) {
        String p = path.toLowerCase(Locale.ROOT);
        for (String n : needles) if (!p.contains(n.toLowerCase(Locale.ROOT))) return false;
        return true;
    }

    private static boolean hasParams(JsonObject paths, String path, String... names) {
        JsonObject op = getOp(paths, path, "get");
        if (op == null) return false;
        if (!op.has("parameters")) return false;
        Set<String> ps = new HashSet<>();
        for (JsonElement e : op.getAsJsonArray("parameters")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("name")) ps.add(o.get("name").getAsString());
        }
        for (String n : names) if (!ps.contains(n)) return false;
        return true;
    }
    private static boolean hasAnyParam(JsonObject paths, String path, String... names) {
        JsonObject op = getOp(paths, path, "get");
        if (op == null) return false;
        if (!op.has("parameters")) return false;
        Set<String> ps = new HashSet<>();
        for (JsonElement e : op.getAsJsonArray("parameters")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("name")) ps.add(o.get("name").getAsString());
        }
        for (String n : names) if (ps.contains(n)) return true;
        return false;
    }
    private static boolean producesPdf(JsonObject paths, String path) {
        JsonObject op = getOp(paths, path, "get");
        if (op == null) return false;
        JsonObject responses = op.getAsJsonObject("responses");
        if (responses == null) return false;
        for (Map.Entry<String, JsonElement> e : responses.entrySet()) {
            JsonObject r = e.getValue().getAsJsonObject();
            JsonObject content = r.getAsJsonObject("content");
            if (content == null) continue;
            for (String mt : content.keySet()) {
                String mtLow = mt.toLowerCase(Locale.ROOT);
                if (mtLow.contains("application/pdf")) return true;
            }
        }
        return false;
    }
    private static JsonObject getOp(JsonObject paths, String path, String method) {
        JsonObject p = paths.getAsJsonObject(path);
        if (p == null) return null;
        JsonElement e = p.get(method);
        return e == null ? null : e.getAsJsonObject();
    }

    private static String findPath(JsonObject paths, String method, java.util.function.Predicate<String> predicate) {
        // prioriza rutas bajo /api si OpenAPI las declaró así
        List<String> all = new ArrayList<>();
        for (String k : paths.keySet()) all.add(k);
        all.sort(Comparator.comparingInt(String::length)); // primero las más cortas

        for (String p : all) {
            JsonObject op = getOp(paths, p, method);
            if (op == null) continue;
            if (predicate.test(p)) return p;
        }
        return null;
    }

    // ---------------------- HTTP helpers ----------------------

    private static String httpGetText(String url) throws Exception {
        try (CloseableHttpClient http = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Accept", "application/json, text/plain, */*");
            try (CloseableHttpResponse resp = http.execute(get)) {
                return EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }

    private File downloadPdf(String url, String prefix) throws Exception {
        try (CloseableHttpClient http = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Accept", "application/pdf, application/octet-stream");
            try (CloseableHttpResponse resp = http.execute(get)) {
                int code = resp.getCode();
                byte[] bytes = EntityUtils.toByteArray(resp.getEntity());
                String sniff = new String(bytes, 0, Math.min(bytes.length, 120), StandardCharsets.UTF_8);
                if (code >= 200 && code < 300 && !looksLikeHtmlError(sniff)) {
                    File out = File.createTempFile("reporte-" + prefix + "-", ".pdf");
                    out.deleteOnExit();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        fos.write(bytes);
                    }
                    return out;
                }
                throw new RuntimeException("El endpoint no devolvió PDF. HTTP " + code + " en " + url);
            }
        }
    }

    private static boolean looksLikeHtmlError(String s) {
        if (s == null) return false;
        String x = s.trim().toLowerCase(Locale.ROOT);
        return x.startsWith("<!doctype") || x.contains("http status") || x.contains("<html");
    }

    private static void ensureJsonNot404(String body, String url) {
        if (body == null) throw new RuntimeException("Respuesta vacía de " + url);
        String t = body.trim();
        if (t.startsWith("<")) {
            throw new RuntimeException("El endpoint devolvió HTML (posible 404) en " + url + ". Revisa OpenAPI/ruta.");
        }
        if (!(t.startsWith("{") || t.startsWith("["))) {
            // parseResponse lo va a marcar como error de JSON
            // pero preferimos mensaje explícito:
            throw new RuntimeException("La respuesta no es JSON válido en " + url + ". Cuerpo: " + preview(t));
        }
    }

    private static String preview(String s){
        if (s == null) return "null";
        return s.length() <= 200 ? s : s.substring(0,200)+"...";
    }

    // ---------------------- JSON & Query helpers ----------------------

    private static String nz(String s){ return (s==null || s.isBlank())? null : s; }
    private static String iso(LocalDate d){ return d==null? null : d.toString(); }

    /** arma query-string con pares k/v, ignorando nulos o vacíos */
    private static String query(String... kv){
        if (kv == null || kv.length==0) return "";
        List<String> parts = new ArrayList<>();
        for(int i=0;i+1<kv.length;i+=2){
            String k = kv[i], v = kv[i+1];
            if(k==null || k.isBlank() || v==null || v.isBlank()) continue;
            parts.add(encode(k)+"="+encode(v));
        }
        return parts.isEmpty()? "" : "?" + parts.stream().collect(Collectors.joining("&"));
    }

    /** recorta el query a SOLO los parámetros que el OpenAPI declara para ese path (evita enviar params ajenos) */
    private String buildQueryRespectingOpenAPI(String path, String rawQuery) throws Exception {
        if (rawQuery == null || rawQuery.isBlank()) return "";
        Map<String,String> all = Arrays.stream(rawQuery.substring(1).split("&"))
                .map(p -> p.split("=",2))
                .filter(arr -> arr.length==2)
                .collect(Collectors.toMap(a->decode(a[0]), a->decode(a[1]), (a,b)->a, LinkedHashMap::new));

        JsonObject paths = loadOpenApi().getAsJsonObject("paths");
        JsonObject op = getOp(paths, path, "get");
        if (op == null || !op.has("parameters")) {
            // no hay params declarados → no mandamos ninguno
            return "";
        }
        Set<String> allowed = new HashSet<>();
        for (JsonElement e : op.getAsJsonArray("parameters")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("in") && "query".equalsIgnoreCase(o.get("in").getAsString()) && o.has("name")) {
                allowed.add(o.get("name").getAsString());
            }
        }
        Map<String,String> filtered = new LinkedHashMap<>();
        for (var entry : all.entrySet()) {
            if (allowed.contains(entry.getKey())) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        if (filtered.isEmpty()) return "";
        String qs = filtered.entrySet().stream()
                .map(e -> encode(e.getKey())+"="+encode(e.getValue()))
                .collect(Collectors.joining("&"));
        return "?" + qs;
    }

    private static String encode(String s){
        try { return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8); }
        catch (Exception e){ return s; }
    }
    private static String decode(String s){
        try { return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8); }
        catch (Exception e){ return s; }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> extractList(String json) {
        Map<String,Object> parsed = RestClient.parseResponse(json);
        Object data = parsed.get("data");
        if (data instanceof List) return (List<Map<String, Object>>) data;
        if (data instanceof Map) {
            Object items = ((Map<?, ?>) data).get("items");
            if (items instanceof List) return (List<Map<String, Object>>) items;
        }
        // si el backend devuelve el array "desnudo", parseamos directo
        if (json.trim().startsWith("[")) {
            return G.fromJson(json, List.class);
        }
        return Collections.emptyList();
    }
}