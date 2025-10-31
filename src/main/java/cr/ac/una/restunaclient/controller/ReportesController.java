package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.util.FlowController;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import java.time.LocalDate;

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

    @FXML
    public void initialize() {
        inicializarComboBoxes();
        configurarFechasPorDefecto();
        aplicarEstilosIniciales();
    }

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

        if (productosComboGrupo != null)
            productosComboGrupo.setItems(FXCollections.observableArrayList(
                    "Todos los grupos", "Bebidas Calientes", "Bebidas Frías", "Platos Fuertes", "Entradas"
            ));

        var topOptions = FXCollections.observableArrayList("Top 10", "Top 20", "Top 50", "Todos");
        if (productosComboTop != null) {
            productosComboTop.setItems(topOptions);
            productosComboTop.setValue("Top 10");
        }
        if (clientesComboTop != null) {
            clientesComboTop.setItems(topOptions);
            clientesComboTop.setValue("Top 10");
        }

        if (ventasSaloneroCombo != null)
            ventasSaloneroCombo.setItems(FXCollections.observableArrayList(
                    "Todos los saloneros", "Pedro Martínez", "Ana López", "Luis Fernández"
            ));
    }

    private void configurarFechasPorDefecto() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        if (facturasDateInicio != null) facturasDateInicio.setValue(inicioMes);
        if (facturasDateFin != null) facturasDateFin.setValue(hoy);
        if (productosDateInicio != null) productosDateInicio.setValue(inicioMes);
        if (productosDateFin != null) productosDateFin.setValue(hoy);
        if (ventasPeriodoDateInicio != null) ventasPeriodoDateInicio.setValue(inicioMes);
        if (ventasPeriodoDateFin != null) ventasPeriodoDateFin.setValue(hoy);
        if (ventasSaloneroDateInicio != null) ventasSaloneroDateInicio.setValue(inicioMes);
        if (ventasSaloneroDateFin != null) ventasSaloneroDateFin.setValue(hoy);
        if (clientesDateInicio != null) clientesDateInicio.setValue(inicioMes);
        if (clientesDateFin != null) clientesDateFin.setValue(hoy);
        if (descuentosDateInicio != null) descuentosDateInicio.setValue(inicioMes);
        if (descuentosDateFin != null) descuentosDateFin.setValue(hoy);
        if (cierresDateFecha != null) cierresDateFecha.setValue(hoy);
    }

    private void aplicarEstilosIniciales() { }

    // --- SELECCIÓN DE REPORTES ---
    @FXML private void selectFacturas()       { mostrar(formFacturas,      btnFacturas,      "#3B82F6"); }
    @FXML private void selectProductos()      { mostrar(formProductos,     btnProductos,     "#FF7A00"); }
    @FXML private void selectCierres()        { mostrar(formCierres,       btnCierres,       "#22C55E"); }
    @FXML private void selectVentasPeriodo()  { mostrar(formVentasPeriodo, btnVentasPeriodo, "#A855F7"); }
    @FXML private void selectVentasSalonero() { mostrar(formVentasSalonero,btnVentasSalonero,"#14B8A6"); }
    @FXML private void selectClientes()       { mostrar(formClientes,      btnClientes,      "#EC4899"); }
    @FXML private void selectDescuentos()     { mostrar(formDescuentos,    btnDescuentos,    "#6366F1"); }

    private void mostrar(VBox form, Button btn, String color) {
        ocultarTodosFormularios();
        if (placeholder != null) {
            placeholder.setVisible(false);
            placeholder.setManaged(false);
        }
        if (form != null) {
            form.setVisible(true);
            form.setManaged(true);
        }
        resetearEstilosBotones();
        if (btn != null) {
            String base = btn.getStyle() == null ? "" : btn.getStyle();
            btn.setStyle(base +
                    "-fx-border-color: " + color + "; -fx-border-width: 2px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        }
    }

    private void ocultarTodosFormularios() {
        VBox[] forms = {formFacturas, formProductos, formCierres, formVentasPeriodo, formVentasSalonero, formClientes, formDescuentos};
        for (VBox f : forms) {
            if (f != null) {
                f.setVisible(false);
                f.setManaged(false);
            }
        }
    }

    private void resetearEstilosBotones() {
        Button[] btns = {btnFacturas, btnProductos, btnCierres, btnVentasPeriodo, btnVentasSalonero, btnClientes, btnDescuentos};
        for (Button b : btns) {
            if (b != null) {
                String s = b.getStyle() == null ? "" : b.getStyle();
                s = s.replaceAll("-fx-border-color: #[0-9A-Fa-f]+;", "-fx-border-color: #E0E0E0;")
                     .replaceAll("-fx-effect: [^;]+;", "");
                b.setStyle(s);
            }
        }
    }

    // --- BOTÓN VOLVER ---
    @FXML
    private void handleVolver() {
        FlowController.getInstance().goHomeWithFade();
    }

    // --- HANDLERS REFERENCIADOS POR EL FXML (FALTABAN) ---
    @FXML private void handleGenerarFacturas()       { if (validarFechas(facturasDateInicio, facturasDateFin))  mostrarMensaje("Reporte de Facturas", "Generando reporte...", Alert.AlertType.INFORMATION); }
    @FXML private void handlePDFFacturas()           { if (validarFechas(facturasDateInicio, facturasDateFin))  mostrarMensaje("PDF Facturas", "Generando archivo PDF...", Alert.AlertType.INFORMATION); }
    @FXML private void handleImprimirFacturas()      { if (validarFechas(facturasDateInicio, facturasDateFin))  mostrarMensaje("Imprimir Facturas", "Enviando a impresora...", Alert.AlertType.INFORMATION); }

    @FXML private void handleGenerarProductos()      { if (validarFechas(productosDateInicio, productosDateFin)) mostrarMensaje("Reporte de Productos", "Generando reporte...", Alert.AlertType.INFORMATION); }
    @FXML private void handlePDFProductos()          { if (validarFechas(productosDateInicio, productosDateFin)) mostrarMensaje("PDF Productos", "Generando archivo PDF...", Alert.AlertType.INFORMATION); }
    @FXML private void handleImprimirProductos()     { if (validarFechas(productosDateInicio, productosDateFin)) mostrarMensaje("Imprimir Productos", "Enviando a impresora...", Alert.AlertType.INFORMATION); }

    @FXML private void handleGenerarCierres() {
        if (cierresComboCajero == null || cierresDateFecha == null ||
            cierresComboCajero.getValue() == null || cierresDateFecha.getValue() == null) {
            mostrarMensaje("Validación", "Por favor complete todos los campos requeridos", Alert.AlertType.WARNING);
            return;
        }
        mostrarMensaje("Reporte de Cierres", "Generando reporte...", Alert.AlertType.INFORMATION);
    }
    @FXML private void handlePDFCierres() {
        if (cierresComboCajero == null || cierresDateFecha == null ||
            cierresComboCajero.getValue() == null || cierresDateFecha.getValue() == null) {
            mostrarMensaje("Validación", "Por favor complete todos los campos requeridos", Alert.AlertType.WARNING);
            return;
        }
        mostrarMensaje("PDF Cierres", "Generando archivo PDF...", Alert.AlertType.INFORMATION);
    }
    @FXML private void handleImprimirCierres() {
        if (cierresComboCajero == null || cierresDateFecha == null ||
            cierresComboCajero.getValue() == null || cierresDateFecha.getValue() == null) {
            mostrarMensaje("Validación", "Por favor complete todos los campos requeridos", Alert.AlertType.WARNING);
            return;
        }
        mostrarMensaje("Imprimir Cierres", "Enviando a impresora...", Alert.AlertType.INFORMATION);
    }

    @FXML private void handleGenerarVentasPeriodo()  { if (validarFechas(ventasPeriodoDateInicio, ventasPeriodoDateFin)) mostrarMensaje("Reporte de Ventas", "Generando reporte...", Alert.AlertType.INFORMATION); }
    @FXML private void handlePDFVentasPeriodo()      { if (validarFechas(ventasPeriodoDateInicio, ventasPeriodoDateFin)) mostrarMensaje("PDF Ventas", "Generando archivo PDF...", Alert.AlertType.INFORMATION); }
    @FXML private void handleImprimirVentasPeriodo() { if (validarFechas(ventasPeriodoDateInicio, ventasPeriodoDateFin)) mostrarMensaje("Imprimir Ventas", "Enviando a impresora...", Alert.AlertType.INFORMATION); }

    @FXML private void handleGenerarVentasSalonero()  { if (validarFechas(ventasSaloneroDateInicio, ventasSaloneroDateFin)) mostrarMensaje("Reporte de Ventas por Salonero", "Generando reporte...", Alert.AlertType.INFORMATION); }
    @FXML private void handlePDFVentasSalonero()      { if (validarFechas(ventasSaloneroDateInicio, ventasSaloneroDateFin)) mostrarMensaje("PDF Ventas por Salonero", "Generando archivo PDF...", Alert.AlertType.INFORMATION); }
    @FXML private void handleImprimirVentasSalonero() { if (validarFechas(ventasSaloneroDateInicio, ventasSaloneroDateFin)) mostrarMensaje("Imprimir Ventas por Salonero", "Enviando a impresora...", Alert.AlertType.INFORMATION); }

    @FXML private void handleGenerarClientes()   { if (validarFechas(clientesDateInicio, clientesDateFin)) mostrarMensaje("Reporte de Clientes", "Generando reporte...", Alert.AlertType.INFORMATION); }
    @FXML private void handlePDFClientes()       { if (validarFechas(clientesDateInicio, clientesDateFin)) mostrarMensaje("PDF Clientes", "Generando archivo PDF...", Alert.AlertType.INFORMATION); }
    @FXML private void handleImprimirClientes()  { if (validarFechas(clientesDateInicio, clientesDateFin)) mostrarMensaje("Imprimir Clientes", "Enviando a impresora...", Alert.AlertType.INFORMATION); }

    @FXML private void handleGenerarDescuentos() { if (validarFechas(descuentosDateInicio, descuentosDateFin)) mostrarMensaje("Reporte de Descuentos", "Generando reporte...", Alert.AlertType.INFORMATION); }
    @FXML private void handlePDFDescuentos()     { if (validarFechas(descuentosDateInicio, descuentosDateFin)) mostrarMensaje("PDF Descuentos", "Generando archivo PDF...", Alert.AlertType.INFORMATION); }
    @FXML private void handleImprimirDescuentos(){ if (validarFechas(descuentosDateInicio, descuentosDateFin)) mostrarMensaje("Imprimir Descuentos", "Enviando a impresora...", Alert.AlertType.INFORMATION); }

    // --- VALIDACIONES Y MENSAJES ---
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