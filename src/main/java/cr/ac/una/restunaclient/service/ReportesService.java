package cr.ac.una.restunaclient.service;

import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReportesService {

    private static final String BASE = "http://localhost:8080/ProyectoProgra3WS";
    private static final String API  = "/api";

    // ✅ Modo directo (sin OpenAPI)
    private static final boolean DIRECT_MODE = true;
    private static final Map<String, String> FIXED = new HashMap<>();

    static {
        // JSON
        FIXED.put("reportes.facturas.get",        "/reportes/facturas");
        FIXED.put("reportes.cierres.get",         "/reportes/cierres");
        FIXED.put("reportes.productos.top",       "/reportes/productos/top");
        FIXED.put("reportes.ventas.periodo.get",  "/reportes/ventas/periodo");
        FIXED.put("reportes.ventas.salonero.get", "/reportes/ventas/salonero");
        FIXED.put("reportes.clientes.top.get",    "/reportes/clientes/top");
        FIXED.put("reportes.descuentos.get",      "/reportes/descuentos");

        // PDF (déjalos si ya creaste endpoints /pdf en el WS; si no, verás UnsupportedOperationException en el controller)
        FIXED.put("reportes.facturas.pdf",        "/reportes/facturas/pdf");
        FIXED.put("reportes.cierres.pdf",         "/reportes/cierres/pdf");
        FIXED.put("reportes.productos.top.pdf",   "/reportes/productos/top/pdf");
        FIXED.put("reportes.ventas.periodo.pdf",  "/reportes/ventas/periodo/pdf");
        FIXED.put("reportes.ventas.salonero.pdf", "/reportes/ventas/salonero/pdf");
        FIXED.put("reportes.clientes.top.pdf",    "/reportes/clientes/top/pdf");
        FIXED.put("reportes.descuentos.pdf",      "/reportes/descuentos/pdf");
    }

    private static final Gson G = new GsonBuilder().create();
    private final Map<String, String> pathCache = new ConcurrentHashMap<>();

    // ---------------------- Público ----------------------

    public List<Map<String,Object>> facturas(LocalDate desde, LocalDate hasta, String cajeroNombre, String estado) throws Exception {
        String path = resolvePath("reportes.facturas.get");
        String qs   = query(
                "fechaInicio", iso(desde),
                "fechaFin",    iso(hasta),
                "estado",      nz(estado),
                "cajero",      nz(cajeroNombre),
                "usuario",     nz(cajeroNombre)
        );
        String url  = buildUrl(path, qs);
        log("GET facturas -> " + url);
        return extractList(httpGetText(url));
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
        String url  = buildUrl(path, qs);
        log("PDF facturas -> " + url);
        return downloadPdf(url, "facturas");
    }

    public List<Map<String,Object>> cierres(LocalDate fecha, String cajeroNombre) throws Exception {
        String path = resolvePath("reportes.cierres.get");
        String qs   = query("fecha", iso(fecha), "cajero", nz(cajeroNombre), "usuario", nz(cajeroNombre));
        String url  = buildUrl(path, qs);
        log("GET cierres -> " + url);
        return extractList(httpGetText(url));
    }

    public File cierrePdf(LocalDate fecha, String cajeroNombre) throws Exception {
        String path = resolvePath("reportes.cierres.pdf");
        String qs   = query("fecha", iso(fecha), "cajero", nz(cajeroNombre), "usuario", nz(cajeroNombre));
        String url  = buildUrl(path, qs);
        log("PDF cierre -> " + url);
        return downloadPdf(url, "cierre-caja");
    }

    public List<Map<String,Object>> productosTop(LocalDate desde, LocalDate hasta, String grupo, Integer top) throws Exception {
        String path = resolvePath("reportes.productos.top");
        String qs   = query(
                "fechaInicio", iso(desde),
                "fechaFin",    iso(hasta),
                "grupo",       nz(grupo),
                "top",         top==null? null : String.valueOf(top)
        );
        String url  = buildUrl(path, qs);
        log("GET productosTop -> " + url);
        return extractList(httpGetText(url));
    }

    public File productosTopPdf(LocalDate desde, LocalDate hasta, String grupo, Integer top) throws Exception {
        String path = resolvePath("reportes.productos.top.pdf");
        String qs   = query(
                "fechaInicio", iso(desde),
                "fechaFin",    iso(hasta),
                "grupo",       nz(grupo),
                "top",         top==null? null : String.valueOf(top)
        );
        String url  = buildUrl(path, qs);
        log("PDF productosTop -> " + url);
        return downloadPdf(url, "productos-top");
    }

    public List<Map<String,Object>> ventasPeriodo(LocalDate desde, LocalDate hasta) throws Exception {
        String path = resolvePath("reportes.ventas.periodo.get");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta));
        String url  = buildUrl(path, qs);
        log("GET ventasPeriodo -> " + url);
        return extractList(httpGetText(url));
    }

    public File ventasPeriodoPdf(LocalDate desde, LocalDate hasta) throws Exception {
        String path = resolvePath("reportes.ventas.periodo.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta));
        String url  = buildUrl(path, qs);
        log("PDF ventasPeriodo -> " + url);
        return downloadPdf(url, "ventas-periodo");
    }

    public List<Map<String,Object>> ventasSalonero(LocalDate desde, LocalDate hasta, String saloneroNombre) throws Exception {
        String path = resolvePath("reportes.ventas.salonero.get");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta),
                "salonero", nz(saloneroNombre), "usuario", nz(saloneroNombre));
        String url  = buildUrl(path, qs);
        log("GET ventasSalonero -> " + url);
        return extractList(httpGetText(url));
    }

    public File ventasSaloneroPdf(LocalDate desde, LocalDate hasta, String saloneroNombre) throws Exception {
        String path = resolvePath("reportes.ventas.salonero.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta),
                "salonero", nz(saloneroNombre), "usuario", nz(saloneroNombre));
        String url  = buildUrl(path, qs);
        log("PDF ventasSalonero -> " + url);
        return downloadPdf(url, "ventas-salonero");
    }

    public List<Map<String,Object>> clientesTop(LocalDate desde, LocalDate hasta, Integer top) throws Exception {
        String path = resolvePath("reportes.clientes.top.get");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta),
                "top", top == null? null : String.valueOf(top));
        String url  = buildUrl(path, qs);
        log("GET clientesTop -> " + url);
        return extractList(httpGetText(url));
    }

    public File clientesTopPdf(LocalDate desde, LocalDate hasta, Integer top) throws Exception {
        String path = resolvePath("reportes.clientes.top.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta),
                "top", top == null? null : String.valueOf(top));
        String url  = buildUrl(path, qs);
        log("PDF clientesTop -> " + url);
        return downloadPdf(url, "clientes-top");
    }

    public List<Map<String,Object>> descuentos(LocalDate desde, LocalDate hasta, String cajeroNombre) throws Exception {
        String path = resolvePath("reportes.descuentos.get");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta),
                "cajero", nz(cajeroNombre), "usuario", nz(cajeroNombre));
        String url  = buildUrl(path, qs);
        log("GET descuentos -> " + url);
        return extractList(httpGetText(url));
    }

    public File descuentosPdf(LocalDate desde, LocalDate hasta, String cajeroNombre) throws Exception {
        String path = resolvePath("reportes.descuentos.pdf");
        String qs   = query("fechaInicio", iso(desde), "fechaFin", iso(hasta),
                "cajero", nz(cajeroNombre), "usuario", nz(cajeroNombre));
        String url  = buildUrl(path, qs);
        log("PDF descuentos -> " + url);
        return downloadPdf(url, "descuentos");
    }

    // ---------------------- Resolución de paths ----------------------

    private String resolvePath(String key) {
        if (DIRECT_MODE) {
            String fixed = FIXED.get(key);
            if (fixed == null) throw new RuntimeException("Ruta fija no configurada para key: " + key);
            return fixed;
        }
        String cached = pathCache.get(key);
        if (cached != null) return cached;
        throw new RuntimeException("OpenAPI deshabilitado. Usa DIRECT_MODE.");
    }

    private String buildUrl(String path, String rawQs) {
        return BASE + API + path + (rawQs == null ? "" : rawQs);
    }

    // ---------------------- HTTP helpers ----------------------

    private static String httpGetText(String url) throws Exception {
        try (CloseableHttpClient http = HttpClients.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Accept", "application/json, text/plain, */*");
            try (CloseableHttpResponse resp = http.execute(get)) {
                int code = resp.getCode();
                String txt = EntityUtils.toString(resp.getEntity(), StandardCharsets.UTF_8);
                if (code < 200 || code >= 300) {
                    throw new RuntimeException("HTTP " + code + " en " + url + " -> " + preview(txt));
                }
                if (!isJsonLike(txt)) {
                    throw new RuntimeException("La respuesta no es JSON en " + url + " -> " + preview(txt));
                }
                return txt;
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
                String sniff = new String(bytes, 0, Math.min(bytes.length, 160), StandardCharsets.UTF_8);
                if (code >= 200 && code < 300 && !looksLikeHtmlError(sniff)) {
                    File out = File.createTempFile("reporte-" + prefix + "-", ".pdf");
                    out.deleteOnExit();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        fos.write(bytes);
                    }
                    return out;
                }
                throw new RuntimeException("El endpoint no devolvió PDF. HTTP " + code + " en " + url + " -> " + preview(sniff));
            }
        }
    }

    private static boolean looksLikeHtmlError(String s) {
        if (s == null) return false;
        String x = s.trim().toLowerCase(Locale.ROOT);
        return x.startsWith("<!doctype") || x.contains("<html") || x.contains("http status");
    }

    private static boolean isJsonLike(String t) {
        if (t == null) return false;
        String s = t.trim();
        return s.startsWith("{") || s.startsWith("[");
    }

    // ---------------------- Util helpers ----------------------

    private static String nz(String s){ return (s==null || s.isBlank())? null : s; }
    private static String iso(LocalDate d){ return d==null? null : d.toString(); }

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

    private static String encode(String s){
        try { return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8); }
        catch (Exception e){ return s; }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> extractList(String json) {
        Map<String,Object> parsed = RestClient.parseResponse(json);
        Object data = parsed.get("data");
        if (data instanceof List) return (List<Map<String, Object>>) data;
        if (json.trim().startsWith("[")) return G.fromJson(json, List.class);
        return Collections.emptyList();
    }

    private static String preview(String s){ return (s==null)? "null" : (s.length()<=200? s : s.substring(0,200)+"..."); }
    private static void log(String m){ System.out.println("[ReportesService] " + m); }
}