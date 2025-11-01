package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.service.ReportesService;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

public class ReportesController {

    // Header
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;

    // Placeholder
    @FXML private VBox placeholder;

    // Botones menú
    @FXML private Button btnFacturas;
    @FXML private Button btnProductos;
    @FXML private Button btnCierres;

    // Formularios
    @FXML private VBox formFacturas;
    @FXML private VBox formProductos;
    @FXML private VBox formCierres;

    // ----- Facturas -----
    @FXML private DatePicker facturasDateInicio, facturasDateFin;
    @FXML private ComboBox<String> facturasComboEstado;
    // opcional tabla (si existe en FXML, se llena; si no existe, se ignora sin romper)
    @FXML private TableView<Map<String,Object>> tableFacturas;

    // ----- Productos Top -----
    @FXML private DatePicker productosDateInicio, productosDateFin;
    @FXML private ComboBox<String> productosComboTop;
    @FXML private TableView<Map<String,Object>> tableProductos;

    // ----- Cierre de Caja -----
    @FXML private ComboBox<String> cierresComboCajero;
    @FXML private DatePicker cierresDateFecha;
    @FXML private TableView<Map<String,Object>> tableCierres;

    // Servicio
    private ReportesService reportesService;

    @FXML
    public void initialize() {
        cargarUsuarioHeader();
        configurarFechasPorDefecto();
        inicializarCombos();

        // Estado inicial
        setVisibleManaged(placeholder, true);
        setVisibleManaged(formFacturas, false);
        setVisibleManaged(formProductos, false);
        setVisibleManaged(formCierres, false);

        reportesService = new ReportesService();

        // Si tienes un endpoint para cajeros, aquí podrías llenarlo; de momento vacío.
        // cargarCajerosAsync();
    }

    // ---------------------- UX helpers ----------------------

    private void cargarUsuarioHeader() {
        try {
            Object u = AppContext.getInstance().get("Usuario");
            String nombre = safeExtract(u, "getNombre", "nombre", "usuario");
            String rol    = safeExtract(u, "getRol", "rol");
            lblUsuario.setText(nombre == null ? "" : nombre);
            lblRol.setText(rol == null ? "" : rol);
        } catch (Exception ignore) {
            lblUsuario.setText(""); lblRol.setText("");
        }
    }

    private String safeExtract(Object bean, String... tries) {
        if (bean == null) return null;
        for (String it : tries) {
            try {
                if (it.startsWith("get")) {
                    var m = bean.getClass().getMethod(it);
                    Object v = m.invoke(bean);
                    return v == null ? null : String.valueOf(v);
                } else {
                    var f = bean.getClass().getDeclaredField(it);
                    f.setAccessible(true);
                    Object v = f.get(bean);
                    return v == null ? null : String.valueOf(v);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private void configurarFechasPorDefecto() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        if (facturasDateInicio != null)  facturasDateInicio.setValue(inicioMes);
        if (facturasDateFin != null)     facturasDateFin.setValue(hoy);
        if (productosDateInicio != null) productosDateInicio.setValue(inicioMes);
        if (productosDateFin != null)    productosDateFin.setValue(hoy);
        if (cierresDateFecha != null)    cierresDateFecha.setValue(hoy);
    }

    private void inicializarCombos() {
        if (facturasComboEstado != null) {
            facturasComboEstado.setItems(FXCollections.observableArrayList("Todas", "Activas", "Canceladas"));
            facturasComboEstado.setValue("Todas");
        }
        if (productosComboTop != null) {
            productosComboTop.setItems(FXCollections.observableArrayList("Top 10", "Top 20", "Top 50", "Todos"));
            productosComboTop.setValue("Top 10");
        }
        if (cierresComboCajero != null) {
            cierresComboCajero.setItems(FXCollections.observableArrayList());
        }
    }

    private void setVisibleManaged(Node n, boolean on) {
        if (n == null) return;
        if (n.visibleProperty().isBound()) n.visibleProperty().unbind();
        if (n.managedProperty().isBound()) n.managedProperty().unbind();
        n.setVisible(on); n.setManaged(on);
    }

    // ---------------------- Navegación ----------------------

    @FXML private void selectFacturas()  { mostrar(formFacturas); }
    @FXML private void selectProductos() { mostrar(formProductos); }
    @FXML private void selectCierres()   { mostrar(formCierres); }

    private void mostrar(VBox form) {
        setVisibleManaged(placeholder, false);
        setVisibleManaged(formFacturas, form == formFacturas);
        setVisibleManaged(formProductos, form == formProductos);
        setVisibleManaged(formCierres,  form == formCierres);
    }

    @FXML private void handleVolver() {
        FlowController.getInstance().goHomeWithFade();
    }

    // ---------------------- FACTURAS ----------------------

    @FXML private void handleGenerarFacturas() {
        if (!validarRango(facturasDateInicio, facturasDateFin)) return;
        String estadoBD = mapEstadoFactura(getValue(facturasComboEstado)); // "A","C",null

        runAsync(
            () -> reportesService.facturas(facturasDateInicio.getValue(), facturasDateFin.getValue(), null, estadoBD),
            data -> {
                renderTable(tableFacturas, data, List.of("id","fecha","estado","subtotal","impuestoVenta","impuestoServicio","descuento","total","usuario","cliente","ordenId"));
                info("Facturas", "Registros: " + size(data));
            },
            "Consultando facturas…"
        );
    }

    @FXML private void handlePDFFacturas() {
        // OJO: solo funcionará si expusiste /api/reportes/facturas/pdf en el WS
        if (!validarRango(facturasDateInicio, facturasDateFin)) return;
        String estadoBD = mapEstadoFactura(getValue(facturasComboEstado));
        runAsync(
            () -> reportesService.facturasPdf(facturasDateInicio.getValue(), facturasDateFin.getValue(), null, estadoBD),
            this::abrirPdf,
            "Generando PDF (Facturas)…"
        );
    }

    @FXML private void handleImprimirFacturas() {
        if (!validarRango(facturasDateInicio, facturasDateFin)) return;
        String estadoBD = mapEstadoFactura(getValue(facturasComboEstado));
        runAsync(
            () -> reportesService.facturasPdf(facturasDateInicio.getValue(), facturasDateFin.getValue(), null, estadoBD),
            this::imprimirPdf,
            "Preparando impresión (Facturas)…"
        );
    }

    private String mapEstadoFactura(String ui) {
        if (ui == null || ui.isBlank() || ui.equalsIgnoreCase("Todas")) return null;
        if (ui.equalsIgnoreCase("Activas")) return "A";
        if (ui.equalsIgnoreCase("Canceladas")) return "C";
        return null;
    }

    // ---------------------- PRODUCTOS TOP ----------------------

    @FXML private void handleGenerarProductos() {
        if (!validarRango(productosDateInicio, productosDateFin)) return;
        runAsync(
            () -> reportesService.productosTop(
                    productosDateInicio.getValue(), productosDateFin.getValue(),
                    null, parseTop(getValue(productosComboTop))),
            data -> {
                renderTable(tableProductos, data, List.of("id","nombre","nombreCorto","grupo","precio","totalVentas","estado"));
                info("Productos Top", "Registros: " + size(data));
            },
            "Consultando productos…"
        );
    }

    @FXML private void handlePDFProductos() {
        runAsync(
            () -> reportesService.productosTopPdf(
                    productosDateInicio.getValue(), productosDateFin.getValue(),
                    null, parseTop(getValue(productosComboTop))),
            this::abrirPdf,
            "Generando PDF (Productos)…"
        );
    }

    @FXML private void handleImprimirProductos() {
        runAsync(
            () -> reportesService.productosTopPdf(
                    productosDateInicio.getValue(), productosDateFin.getValue(),
                    null, parseTop(getValue(productosComboTop))),
            this::imprimirPdf,
            "Preparando impresión (Productos)…"
        );
    }

    // ---------------------- CIERRES ----------------------

    @FXML private void handleGenerarCierres() {
        if (isBlank(getValue(cierresComboCajero)) || cierresDateFecha.getValue() == null) {
            warn("Validación", "Por favor seleccione cajero y fecha.");
            return;
        }
        runAsync(
            () -> reportesService.cierres(cierresDateFecha.getValue(), getValue(cierresComboCajero)),
            data -> {
                renderTable(tableCierres, data, List.of("id","estado","fechaApertura","fechaCierre","usuario","usuarioLogin","efectivoSistema","tarjetaSistema","difEfectivo","difTarjeta"));
                info("Cierre de Caja", "Registros: " + size(data));
            },
            "Consultando cierres…"
        );
    }

    @FXML private void handlePDFCierres() {
        if (isBlank(getValue(cierresComboCajero)) || cierresDateFecha.getValue() == null) {
            warn("Validación", "Por favor seleccione cajero y fecha.");
            return;
        }
        runAsync(
            () -> reportesService.cierrePdf(cierresDateFecha.getValue(), getValue(cierresComboCajero)),
            this::abrirPdf,
            "Generando PDF (Cierre)…"
        );
    }

    @FXML private void handleImprimirCierres() {
        if (isBlank(getValue(cierresComboCajero)) || cierresDateFecha.getValue() == null) {
            warn("Validación", "Por favor seleccione cajero y fecha.");
            return;
        }
        runAsync(
            () -> reportesService.cierrePdf(cierresDateFecha.getValue(), getValue(cierresComboCajero)),
            this::imprimirPdf,
            "Preparando impresión (Cierre)…"
        );
    }

    // ---------------------- Utilidades comunes ----------------------

    private boolean validarRango(DatePicker ini, DatePicker fin) {
        if (ini.getValue() == null || fin.getValue() == null) {
            warn("Validación", "Por favor seleccione ambas fechas");
            return false;
        }
        if (ini.getValue().isAfter(fin.getValue())) {
            warn("Validación", "La fecha inicial no puede ser posterior a la fecha final");
            return false;
        }
        return true;
    }

    private String getValue(ComboBox<String> cb) {
        return (cb == null || cb.getValue() == null) ? null : cb.getValue().trim();
    }

    private Integer parseTop(String s) {
        if (s == null || s.isBlank() || s.equalsIgnoreCase("Todos")) return null;
        String t = s.toLowerCase().startsWith("top") ? s.replaceAll("[^0-9]", "") : s;
        try { return t.isBlank() ? null : Integer.parseInt(t); } catch (Exception e) { return null; }
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private void info(String t, String m) { alert(t, m, Alert.AlertType.INFORMATION); }
    private void warn(String t, String m) { alert(t, m, Alert.AlertType.WARNING); }
    private void err (String t, String m) { alert(t, m, Alert.AlertType.ERROR); }
    private void alert(String t, String m, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type);
            a.setTitle(t); a.setHeaderText(null); a.setContentText(m);
            a.showAndWait();
        });
    }

    private int size(List<?> l){ return l==null? 0 : l.size(); }

    private <T> void runAsync(java.util.concurrent.Callable<T> work,
                              java.util.function.Consumer<T> onOk,
                              String tituloCarga) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(tituloCarga);
        ProgressIndicator pi = new ProgressIndicator();
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Task<T> task = new Task<>() { @Override protected T call() throws Exception { return work.call(); } };
        task.setOnSucceeded(ev -> { dlg.close(); try { onOk.accept(task.getValue()); } catch (Exception ignored) {} });
        task.setOnFailed(ev -> { dlg.close(); Throwable ex = task.getException();
            err("Error", (ex == null ? "Error desconocido" : ex.getMessage())); });

        dlg.setResultConverter(btn -> { if (btn == ButtonType.CANCEL) task.cancel(true); return null; });

        Thread t = new Thread(task, "Reporte-Task");
        t.setDaemon(true);
        t.start();

        dlg.showAndWait();
    }

    private void abrirPdf(File f) {
        try {
            if (f == null || !f.exists()) { warn("PDF", "No se encontró el archivo a abrir."); return; }
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(f);
            else info("PDF generado", "Archivo: " + f.getAbsolutePath());
        } catch (Exception ex) {
            info("PDF generado", "Guardado en: " + (f == null ? "(desconocido)" : f.getAbsolutePath())
                    + "\nNo se pudo abrir automáticamente.\n" + ex.getMessage());
        }
    }

    private void imprimirPdf(File f) {
        try {
            if (f == null || !f.exists()) { warn("Imprimir", "No se encontró el archivo a imprimir."); return; }
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.PRINT)) {
                java.awt.Desktop.getDesktop().print(f);
            } else {
                java.awt.Desktop.getDesktop().open(f);
            }
        } catch (Exception ex) {
            err("Imprimir", "No fue posible enviar a impresión.\n" + ex.getMessage()
                    + "\nArchivo: " + (f==null? "(desconocido)" : f.getAbsolutePath()));
        }
    }

    // ---------------------- Render dinámico de tablas (opcional) ----------------------

    private void renderTable(TableView<Map<String,Object>> table,
                             List<Map<String,Object>> data,
                             List<String> preferredOrder) {
        if (table == null) return; // No hay tabla en el FXML → no hacemos nada
        table.getItems().clear();
        table.getColumns().clear();

        if (data == null || data.isEmpty()) {
            return;
        }
        // Determinar columnas (mantener orden sugerido si existe)
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (preferredOrder != null) keys.addAll(preferredOrder);
        data.get(0).keySet().forEach(keys::add); // añade las que no estén

        for (String k : keys) {
            TableColumn<Map<String,Object>, Object> col = new TableColumn<>(normalizeHeader(k));
            col.setCellValueFactory(param ->
                    new SimpleObjectProperty<>(param.getValue().getOrDefault(k, "")));
            col.setPrefWidth(120);
            table.getColumns().add(col);
        }

        table.setItems(FXCollections.observableArrayList(data));
    }

    private String normalizeHeader(String key) {
        // "usuarioLogin" → "Usuario Login"
        StringBuilder sb = new StringBuilder();
        char[] arr = key.toCharArray();
        for (int i=0;i<arr.length;i++){
            char c = arr[i];
            if (i==0) { sb.append(Character.toUpperCase(c)); continue; }
            if (Character.isUpperCase(c) && Character.isLetter(arr[i-1])) sb.append(' ');
            sb.append(c);
        }
        return sb.toString().replace('_',' ');
    }
}