package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.service.ReportesService;
import cr.ac.una.restunaclient.util.FlowController;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;

import java.io.File;
import java.time.LocalDate;

/**
 * Controlador del módulo de Reportes.
 * - Maneja visibilidad/managed de formularios con helpers seguros (evitan escribir en propiedades enlazadas).
 * - Usa ReportesService (basado en RestClient estático) para consultar JSON / descargar PDF.
 * - Abre/Imprime PDF con java.awt.Desktop (requiere `requires java.desktop;` en module-info).
 */
public class ReportesController {

    // Panel inicial "Seleccione un reporte"
    @FXML private VBox placeholder;

    // Botones de selección
    @FXML private Button btnFacturas;
    @FXML private Button btnProductos;
    @FXML private Button btnCierres;
    @FXML private Button btnVentasPeriodo;
    @FXML private Button btnVentasSalonero;
    @FXML private Button btnClientes;
    @FXML private Button btnDescuentos;

    // Formularios
    @FXML private VBox formFacturas;
    @FXML private VBox formProductos;
    @FXML private VBox formCierres;
    @FXML private VBox formVentasPeriodo;
    @FXML private VBox formVentasSalonero;
    @FXML private VBox formClientes;
    @FXML private VBox formDescuentos;

    // Campos Facturas
    @FXML private DatePicker facturasDateInicio, facturasDateFin;
    @FXML private ComboBox<String> facturasComboCajero, facturasComboEstado;

    // Campos Productos
    @FXML private DatePicker productosDateInicio, productosDateFin;
    @FXML private ComboBox<String> productosComboGrupo, productosComboTop;

    // Campos Cierres
    @FXML private ComboBox<String> cierresComboCajero;
    @FXML private DatePicker cierresDateFecha;

    // Campos Ventas por Período
    @FXML private DatePicker ventasPeriodoDateInicio, ventasPeriodoDateFin;

    // Campos Ventas por Salonero
    @FXML private DatePicker ventasSaloneroDateInicio, ventasSaloneroDateFin;
    @FXML private ComboBox<String> ventasSaloneroCombo;

    // Campos Clientes
    @FXML private DatePicker clientesDateInicio, clientesDateFin;
    @FXML private ComboBox<String> clientesComboTop;

    // Campos Descuentos
    @FXML private DatePicker descuentosDateInicio, descuentosDateFin;
    @FXML private ComboBox<String> descuentosComboCajero;

    // Servicio de reportes
    private ReportesService reportesService;

    // ===================== Ciclo de vida =====================
    @FXML
    public void initialize() {
        inicializarComboBoxes();
        configurarFechasPorDefecto();
        aplicarEstilosIniciales();

        // Asegura estado inicial: placeholder visible, formularios ocultos
        setVisibleSafe(placeholder, true);
        setManagedSafe(placeholder, true);

        VBox[] forms = { formFacturas, formProductos, formCierres, formVentasPeriodo, formVentasSalonero, formClientes, formDescuentos };
        for (VBox v : forms) {
            setVisibleSafe(v, false);
            setManagedSafe(v, false);
        }

        this.reportesService = new ReportesService();
    }

    // ===================== Init UI =====================
    private void inicializarComboBoxes() {
        var cajeros = FXCollections.observableArrayList(
                "Todos los cajeros", "Juan Pérez", "María González", "Carlos Rodríguez"
        );
        if (facturasComboCajero != null) facturasComboCajero.setItems(cajeros);
        if (cierresComboCajero != null) cierresComboCajero.setItems(
                FXCollections.observableArrayList("Juan Pérez", "María González", "Carlos Rodríguez")
        );
        if (descuentosComboCajero != null) descuentosComboCajero.setItems(cajeros);

        if (facturasComboEstado != null) {
            facturasComboEstado.setItems(FXCollections.observableArrayList("Todas", "Activas", "Canceladas"));
            facturasComboEstado.setValue("Todas");
        }

        if (productosComboGrupo != null) {
            productosComboGrupo.setItems(FXCollections.observableArrayList(
                    "Todos los grupos", "Bebidas Calientes", "Bebidas Frías", "Platos Fuertes", "Entradas"
            ));
        }

        var topOptions = FXCollections.observableArrayList("Top 10", "Top 20", "Top 50", "Todos");
        if (productosComboTop != null) {
            productosComboTop.setItems(topOptions);
            productosComboTop.setValue("Top 10");
        }
        if (clientesComboTop != null) {
            clientesComboTop.setItems(topOptions);
            clientesComboTop.setValue("Top 10");
        }

        if (ventasSaloneroCombo != null) {
            ventasSaloneroCombo.setItems(FXCollections.observableArrayList(
                    "Todos los saloneros", "Pedro Martínez", "Ana López", "Luis Fernández"
            ));
        }
    }

    private void configurarFechasPorDefecto() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        if (facturasDateInicio != null)      facturasDateInicio.setValue(inicioMes);
        if (facturasDateFin != null)         facturasDateFin.setValue(hoy);
        if (productosDateInicio != null)     productosDateInicio.setValue(inicioMes);
        if (productosDateFin != null)        productosDateFin.setValue(hoy);
        if (ventasPeriodoDateInicio != null) ventasPeriodoDateInicio.setValue(inicioMes);
        if (ventasPeriodoDateFin != null)    ventasPeriodoDateFin.setValue(hoy);
        if (ventasSaloneroDateInicio != null)ventasSaloneroDateInicio.setValue(inicioMes);
        if (ventasSaloneroDateFin != null)   ventasSaloneroDateFin.setValue(hoy);
        if (clientesDateInicio != null)      clientesDateInicio.setValue(inicioMes);
        if (clientesDateFin != null)         clientesDateFin.setValue(hoy);
        if (descuentosDateInicio != null)    descuentosDateInicio.setValue(inicioMes);
        if (descuentosDateFin != null)       descuentosDateFin.setValue(hoy);
        if (cierresDateFecha != null)        cierresDateFecha.setValue(hoy);
    }

    private void aplicarEstilosInitialBtn(Button b, String color) {
        if (b == null) return;
        String base = b.getStyle() == null ? "" : b.getStyle();
        b.setStyle(base +
                "-fx-border-color: " + color + "; -fx-border-width: 2px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
    }

    private void aplicarEstilosIniciales() {
        // Si necesitas estilos adicionales globales, colócalos aquí.
    }

    // ===================== Helpers de visibilidad seguros =====================
    private void setVisibleSafe(Node n, boolean value) {
        if (n == null) return;
        if (n.visibleProperty().isBound()) n.visibleProperty().unbind();
        n.setVisible(value);
    }
    private void setManagedSafe(Node n, boolean value) {
        if (n == null) return;
        if (n.managedProperty().isBound()) n.managedProperty().unbind();
        n.setManaged(value);
    }

    // ===================== Navegación de formularios =====================
    @FXML private void selectFacturas()       { mostrar(formFacturas,       btnFacturas,       "#3B82F6"); }
    @FXML private void selectProductos()      { mostrar(formProductos,      btnProductos,      "#FF7A00"); }
    @FXML private void selectCierres()        { mostrar(formCierres,        btnCierres,        "#22C55E"); }
    @FXML private void selectVentasPeriodo()  { mostrar(formVentasPeriodo,  btnVentasPeriodo,  "#A855F7"); }
    @FXML private void selectVentasSalonero() { mostrar(formVentasSalonero, btnVentasSalonero, "#14B8A6"); }
    @FXML private void selectClientes()       { mostrar(formClientes,       btnClientes,       "#EC4899"); }
    @FXML private void selectDescuentos()     { mostrar(formDescuentos,     btnDescuentos,     "#6366F1"); }

    private void mostrar(VBox form, Button btn, String color) {
        ocultarTodosFormularios();
        if (placeholder != null) {
            setVisibleSafe(placeholder, false);
            setManagedSafe(placeholder, false);
        }
        if (form != null) {
            setVisibleSafe(form, true);
            setManagedSafe(form, true);
        }
        resetearEstilosBotones();
        aplicarEstilosInitialBtn(btn, color);
    }

    private void ocultarTodosFormularios() {
        VBox[] forms = { formFacturas, formProductos, formCierres, formVentasPeriodo, formVentasSalonero, formClientes, formDescuentos };
        for (VBox f : forms) {
            setVisibleSafe(f, false);
            setManagedSafe(f, false);
        }
        // Si no hay ninguno visible, vuelve a mostrar el placeholder
        if (placeholder != null) {
            setVisibleSafe(placeholder, true);
            setManagedSafe(placeholder, true);
        }
    }

    private void resetearEstilosBotones() {
        Button[] btns = { btnFacturas, btnProductos, btnCierres, btnVentasPeriodo, btnVentasSalonero, btnClientes, btnDescuentos };
        for (Button b : btns) {
            if (b != null) {
                String s = b.getStyle() == null ? "" : b.getStyle();
                s = s.replaceAll("-fx-border-color: #[0-9A-Fa-f]+;", "-fx-border-color: #E0E0E0;")
                     .replaceAll("-fx-effect: [^;]+;", "");
                b.setStyle(s);
            }
        }
    }

    // ===================== Botón Volver =====================
    @FXML
    private void handleVolver() {
        FlowController.getInstance().goHomeWithFade();
    }

    // ===================== Handlers: FACTURAS =====================
    @FXML private void handleGenerarFacturas() {
        if (!validarFechas(facturasDateInicio, facturasDateFin)) return;
        var desde  = facturasDateInicio.getValue();
        var hasta  = facturasDateFin.getValue();
        var cajero = (facturasComboCajero == null) ? null : facturasComboCajero.getValue();
        var estado = (facturasComboEstado == null) ? null : facturasComboEstado.getValue();

        runAsync(
            () -> reportesService.facturas(desde, hasta, cajero, estado),
            data -> mostrarMensaje("Facturas", "Registros: " + (data == null ? 0 : data.size()), Alert.AlertType.INFORMATION),
            "Consultando facturas…"
        );
    }

    @FXML private void handlePDFFacturas() {
        if (!validarFechas(facturasDateInicio, facturasDateFin)) return;
        var desde  = facturasDateInicio.getValue();
        var hasta  = facturasDateFin.getValue();
        var cajero = (facturasComboCajero == null) ? null : facturasComboCajero.getValue();
        var estado = (facturasComboEstado == null) ? null : facturasComboEstado.getValue();

        runAsync(
            () -> reportesService.facturasPdf(desde, hasta, cajero, estado),
            this::abrirPdf,
            "Generando PDF (Facturas)…"
        );
    }

    @FXML private void handleImprimirFacturas() {
        if (!validarFechas(facturasDateInicio, facturasDateFin)) return;
        var desde  = facturasDateInicio.getValue();
        var hasta  = facturasDateFin.getValue();
        var cajero = (facturasComboCajero == null) ? null : facturasComboCajero.getValue();
        var estado = (facturasComboEstado == null) ? null : facturasComboEstado.getValue();

        runAsync(
            () -> reportesService.facturasPdf(desde, hasta, cajero, estado),
            this::imprimirPdf,
            "Preparando impresión (Facturas)…"
        );
    }

    // ===================== Handlers: PRODUCTOS TOP =====================
    @FXML private void handleGenerarProductos() {
        if (!validarFechas(productosDateInicio, productosDateFin)) return;
        var desde = productosDateInicio.getValue();
        var hasta = productosDateFin.getValue();
        var grupo = (productosComboGrupo == null) ? null : productosComboGrupo.getValue();
        var top   = parseTop( (productosComboTop == null) ? null : productosComboTop.getValue() );

        runAsync(
            () -> reportesService.productosTop(desde, hasta, grupo, top),
            data -> mostrarMensaje("Productos Top", "Registros: " + (data == null ? 0 : data.size()), Alert.AlertType.INFORMATION),
            "Consultando productos…"
        );
    }

    @FXML private void handlePDFProductos() {
        if (!validarFechas(productosDateInicio, productosDateFin)) return;
        var desde = productosDateInicio.getValue();
        var hasta = productosDateFin.getValue();
        var grupo = (productosComboGrupo == null) ? null : productosComboGrupo.getValue();
        var top   = parseTop( (productosComboTop == null) ? null : productosComboTop.getValue() );

        runAsync(
            () -> reportesService.productosTopPdf(desde, hasta, grupo, top),
            this::abrirPdf,
            "Generando PDF (Productos)…"
        );
    }

    @FXML private void handleImprimirProductos() {
        if (!validarFechas(productosDateInicio, productosDateFin)) return;
        var desde = productosDateInicio.getValue();
        var hasta = productosDateFin.getValue();
        var grupo = (productosComboGrupo == null) ? null : productosComboGrupo.getValue();
        var top   = parseTop( (productosComboTop == null) ? null : productosComboTop.getValue() );

        runAsync(
            () -> reportesService.productosTopPdf(desde, hasta, grupo, top),
            this::imprimirPdf,
            "Preparando impresión (Productos)…"
        );
    }

    // ===================== Handlers: CIERRES =====================
    @FXML private void handleGenerarCierres() {
        if (cierresComboCajero == null || cierresDateFecha == null ||
            cierresComboCajero.getValue() == null || cierresDateFecha.getValue() == null) {
            mostrarMensaje("Validación", "Por favor complete todos los campos requeridos", Alert.AlertType.WARNING);
            return;
        }
        var fecha  = cierresDateFecha.getValue();
        var cajero = cierresComboCajero.getValue();

        runAsync(
            () -> reportesService.cierres(fecha, cajero),
            data -> mostrarMensaje("Cierres", "Registros: " + (data == null ? 0 : data.size()), Alert.AlertType.INFORMATION),
            "Consultando cierres…"
        );
    }

    @FXML private void handlePDFCierres() {
        if (cierresComboCajero == null || cierresDateFecha == null ||
            cierresComboCajero.getValue() == null || cierresDateFecha.getValue() == null) {
            mostrarMensaje("Validación", "Por favor complete todos los campos requeridos", Alert.AlertType.WARNING);
            return;
        }
        var fecha  = cierresDateFecha.getValue();
        var cajero = cierresComboCajero.getValue();

        runAsync(
            () -> reportesService.cierrePdf(fecha, cajero),
            this::abrirPdf,
            "Generando PDF (Cierre)…"
        );
    }

    @FXML private void handleImprimirCierres() {
        if (cierresComboCajero == null || cierresDateFecha == null ||
            cierresComboCajero.getValue() == null || cierresDateFecha.getValue() == null) {
            mostrarMensaje("Validación", "Por favor complete todos los campos requeridos", Alert.AlertType.WARNING);
            return;
        }
        var fecha  = cierresDateFecha.getValue();
        var cajero = cierresComboCajero.getValue();

        runAsync(
            () -> reportesService.cierrePdf(fecha, cajero),
            this::imprimirPdf,
            "Preparando impresión (Cierre)…"
        );
    }

    // ===================== Handlers: VENTAS POR PERÍODO =====================
    @FXML private void handleGenerarVentasPeriodo() {
        if (!validarFechas(ventasPeriodoDateInicio, ventasPeriodoDateFin)) return;
        var desde = ventasPeriodoDateInicio.getValue();
        var hasta = ventasPeriodoDateFin.getValue();

        runAsync(
            () -> reportesService.ventasPeriodo(desde, hasta),
            data -> mostrarMensaje("Ventas por período", "Registros: " + (data == null ? 0 : data.size()), Alert.AlertType.INFORMATION),
            "Consultando ventas (período)…"
        );
    }

    @FXML private void handlePDFVentasPeriodo() {
        if (!validarFechas(ventasPeriodoDateInicio, ventasPeriodoDateFin)) return;
        var desde = ventasPeriodoDateInicio.getValue();
        var hasta = ventasPeriodoDateFin.getValue();

        runAsync(
            () -> reportesService.ventasPeriodoPdf(desde, hasta),
            this::abrirPdf,
            "Generando PDF (Ventas período)…"
        );
    }

    @FXML private void handleImprimirVentasPeriodo() {
        if (!validarFechas(ventasPeriodoDateInicio, ventasPeriodoDateFin)) return;
        var desde = ventasPeriodoDateInicio.getValue();
        var hasta = ventasPeriodoDateFin.getValue();

        runAsync(
            () -> reportesService.ventasPeriodoPdf(desde, hasta),
            this::imprimirPdf,
            "Preparando impresión (Ventas período)…"
        );
    }

    // ===================== Handlers: VENTAS POR SALONERO =====================
    @FXML private void handleGenerarVentasSalonero() {
        if (!validarFechas(ventasSaloneroDateInicio, ventasSaloneroDateFin)) return;
        var desde    = ventasSaloneroDateInicio.getValue();
        var hasta    = ventasSaloneroDateFin.getValue();
        var salonero = (ventasSaloneroCombo == null) ? null : ventasSaloneroCombo.getValue();

        runAsync(
            () -> reportesService.ventasSalonero(desde, hasta, salonero),
            data -> mostrarMensaje("Ventas por salonero", "Registros: " + (data == null ? 0 : data.size()), Alert.AlertType.INFORMATION),
            "Consultando ventas (salonero)…"
        );
    }

    @FXML private void handlePDFVentasSalonero() {
        if (!validarFechas(ventasSaloneroDateInicio, ventasSaloneroDateFin)) return;
        var desde    = ventasSaloneroDateInicio.getValue();
        var hasta    = ventasSaloneroDateFin.getValue();
        var salonero = (ventasSaloneroCombo == null) ? null : ventasSaloneroCombo.getValue();

        runAsync(
            () -> reportesService.ventasSaloneroPdf(desde, hasta, salonero),
            this::abrirPdf,
            "Generando PDF (Ventas salonero)…"
        );
    }

    @FXML private void handleImprimirVentasSalonero() {
        if (!validarFechas(ventasSaloneroDateInicio, ventasSaloneroDateFin)) return;
        var desde    = ventasSaloneroDateInicio.getValue();
        var hasta    = ventasSaloneroDateFin.getValue();
        var salonero = (ventasSaloneroCombo == null) ? null : ventasSaloneroCombo.getValue();

        runAsync(
            () -> reportesService.ventasSaloneroPdf(desde, hasta, salonero),
            this::imprimirPdf,
            "Preparando impresión (Ventas salonero)…"
        );
    }

    // ===================== Handlers: CLIENTES TOP =====================
    @FXML private void handleGenerarClientes() {
        if (!validarFechas(clientesDateInicio, clientesDateFin)) return;
        var desde = clientesDateInicio.getValue();
        var hasta = clientesDateFin.getValue();
        var top   = parseTop( (clientesComboTop == null) ? null : clientesComboTop.getValue() );

        runAsync(
            () -> reportesService.clientesTop(desde, hasta, top),
            data -> mostrarMensaje("Clientes frecuentes", "Registros: " + (data == null ? 0 : data.size()), Alert.AlertType.INFORMATION),
            "Consultando clientes…"
        );
    }

    @FXML private void handlePDFClientes() {
        if (!validarFechas(clientesDateInicio, clientesDateFin)) return;
        var desde = clientesDateInicio.getValue();
        var hasta = clientesDateFin.getValue();
        var top   = parseTop( (clientesComboTop == null) ? null : clientesComboTop.getValue() );

        runAsync(
            () -> reportesService.clientesTopPdf(desde, hasta, top),
            this::abrirPdf,
            "Generando PDF (Clientes)…"
        );
    }

    @FXML private void handleImprimirClientes() {
        if (!validarFechas(clientesDateInicio, clientesDateFin)) return;
        var desde = clientesDateInicio.getValue();
        var hasta = clientesDateFin.getValue();
        var top   = parseTop( (clientesComboTop == null) ? null : clientesComboTop.getValue() );

        runAsync(
            () -> reportesService.clientesTopPdf(desde, hasta, top),
            this::imprimirPdf,
            "Preparando impresión (Clientes)…"
        );
    }

    // ===================== Handlers: DESCUENTOS =====================
    @FXML private void handleGenerarDescuentos() {
        if (!validarFechas(descuentosDateInicio, descuentosDateFin)) return;
        var desde  = descuentosDateInicio.getValue();
        var hasta  = descuentosDateFin.getValue();
        var cajero = (descuentosComboCajero == null) ? null : descuentosComboCajero.getValue();

        runAsync(
            () -> reportesService.descuentos(desde, hasta, cajero),
            data -> mostrarMensaje("Descuentos", "Registros: " + (data == null ? 0 : data.size()), Alert.AlertType.INFORMATION),
            "Consultando descuentos…"
        );
    }

    @FXML private void handlePDFDescuentos() {
        if (!validarFechas(descuentosDateInicio, descuentosDateFin)) return;
        var desde  = descuentosDateInicio.getValue();
        var hasta  = descuentosDateFin.getValue();
        var cajero = (descuentosComboCajero == null) ? null : descuentosComboCajero.getValue();

        runAsync(
            () -> reportesService.descuentosPdf(desde, hasta, cajero),
            this::abrirPdf,
            "Generando PDF (Descuentos)…"
        );
    }

    @FXML private void handleImprimirDescuentos() {
        if (!validarFechas(descuentosDateInicio, descuentosDateFin)) return;
        var desde  = descuentosDateInicio.getValue();
        var hasta  = descuentosDateFin.getValue();
        var cajero = (descuentosComboCajero == null) ? null : descuentosComboCajero.getValue();

        runAsync(
            () -> reportesService.descuentosPdf(desde, hasta, cajero),
            this::imprimirPdf,
            "Preparando impresión (Descuentos)…"
        );
    }

    // ===================== Abrir / Imprimir PDF =====================
    private void abrirPdf(File f) {
        try {
            if (f == null || !f.exists()) {
                mostrarMensaje("PDF", "No se encontró el archivo a abrir.", Alert.AlertType.WARNING);
                return;
            }
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(f);
            } else {
                mostrarMensaje("PDF generado", "Archivo: " + f.getAbsolutePath(), Alert.AlertType.INFORMATION);
            }
        } catch (Exception ex) {
            mostrarMensaje("PDF generado", "Guardado en: " + (f == null ? "(desconocido)" : f.getAbsolutePath())
                    + "\nNo se pudo abrir automáticamente.\n" + ex.getMessage(), Alert.AlertType.INFORMATION);
        }
    }

    private void imprimirPdf(File f) {
        try {
            if (f == null || !f.exists()) {
                mostrarMensaje("Imprimir", "No se encontró el archivo a imprimir.", Alert.AlertType.WARNING);
                return;
            }
            if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.PRINT)) {
                java.awt.Desktop.getDesktop().print(f);
            } else {
                // Fallback: abrir para que el usuario imprima manualmente
                java.awt.Desktop.getDesktop().open(f);
            }
        } catch (Exception ex) {
            mostrarMensaje("Imprimir", "No fue posible enviar a impresión.\n" + ex.getMessage()
                    + "\nArchivo: " + f.getAbsolutePath(), Alert.AlertType.ERROR);
        }
    }

    // ===================== Infra de tareas con diálogo de progreso =====================
    private <T> void runAsync(java.util.concurrent.Callable<T> work,
                              java.util.function.Consumer<T> onOk,
                              String tituloCarga) {

        ProgressIndicator pi = new ProgressIndicator();
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(tituloCarga);
        dlg.getDialogPane().setContent(pi);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };

        task.setOnSucceeded(ev -> {
            dlg.close();
            onOk.accept(task.getValue());
        });

        task.setOnFailed(ev -> {
            dlg.close();
            Throwable ex = task.getException();
            mostrarMensaje("Error", (ex == null ? "Error desconocido" : ex.getMessage()), Alert.AlertType.ERROR);
        });

        dlg.setResultConverter(btn -> { if (btn == ButtonType.CANCEL) task.cancel(true); return null; });

        Thread t = new Thread(task, "Reporte-Task");
        t.setDaemon(true);
        t.start();

        dlg.showAndWait();
    }

    // ===================== Utilidades =====================
    // Convierte "Top 10" → 10; "Todos" → null
    private Integer parseTop(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        if (t.equalsIgnoreCase("Todos")) return null;
        if (t.toLowerCase().startsWith("top")) {
            t = t.replaceAll("[^0-9]", "");
        }
        try { return t.isEmpty() ? null : Integer.parseInt(t); }
        catch (Exception e) { return null; }
    }

    private boolean validarFechas(DatePicker inicio, DatePicker fin) {
        if (inicio == null || fin == null || inicio.getValue() == null || fin.getValue() == null) {
            mostrarMensaje("Validación", "Por favor seleccione ambas fechas", Alert.AlertType.WARNING);
            return false;
        }
        if (inicio.getValue().isAfter(fin.getValue())) {
            mostrarMensaje("Validación", "La fecha inicial no puede ser posterior a la fecha final", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    private void mostrarMensaje(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}