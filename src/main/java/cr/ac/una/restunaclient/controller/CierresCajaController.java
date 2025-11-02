package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.util.FlowController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import java.net.URL;
import java.util.ResourceBundle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import javafx.event.ActionEvent;

/**
 * Controlador para la gestión de Cierres de Caja
 * Cumple con el requisito 17 del proyecto
 * 
 * @author Tu Nombre
 */
public class CierresCajaController implements Initializable {
    
    // ==================== COMPONENTES FXML ====================
    
    // Header
    @FXML private Label lblTitle;
    @FXML private Button btnVolver;
    
    // Panel Izquierdo - Estado Actual
    @FXML private Label lblEstadoCaja;
    @FXML private Label lblCajero;
    @FXML private Label lblFechaApertura;
    @FXML private Label lblEfectivoSistema;
    @FXML private Label lblTarjetaSistema;
    @FXML private Label lblTotalSistema;
    
    // Panel Izquierdo - Filtros
    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private ComboBox<String> cmbFiltroCajero;
    @FXML private Button btnBuscar;
    
    // Panel Izquierdo - Tabla
    @FXML private TableView<CierreCaja> tblCierres;
    @FXML private TableColumn<CierreCaja, String> colFechaApertura;
    @FXML private TableColumn<CierreCaja, String> colFechaCierre;
    @FXML private TableColumn<CierreCaja, String> colCajero;
    @FXML private TableColumn<CierreCaja, String> colEfectivoSistema;
    @FXML private TableColumn<CierreCaja, String> colTarjetaSistema;
    @FXML private TableColumn<CierreCaja, String> colEfectivoDeclarado;
    @FXML private TableColumn<CierreCaja, String> colTarjetaDeclarado;
    @FXML private TableColumn<CierreCaja, String> colDiferenciaTotal;
    @FXML private TableColumn<CierreCaja, String> colEstado;
    
    // Panel Izquierdo - Botones
    @FXML private Button btnVerDetalle;
    @FXML private Button btnGenerarReporte;
    @FXML private Button btnRefrescar;
    
    // Panel Derecho - Formulario
    @FXML private Label lblFormTitle;
    @FXML private Label lblInfoCajero;
    @FXML private Label lblInfoApertura;
    @FXML private Label lblInfoFacturas;
    @FXML private Label lblSistemaEfectivo;
    @FXML private Label lblSistemaTarjeta;
    @FXML private Label lblSistemaTotal;
    @FXML private Label lblEfectivoDeclarado;
    @FXML private Label lblTarjetaDeclarado;
    @FXML private TextField txtEfectivoDeclarado;
    @FXML private TextField txtTarjetaDeclarado;
    @FXML private VBox vboxDiferencias;
    @FXML private Label lblDiferenciaEfectivo;
    @FXML private Label lblDiferenciaTarjeta;
    @FXML private Label lblDiferenciaTotal;
    @FXML private Button btnCalcular;
    @FXML private Button btnCerrarCaja;
    
    // ==================== VARIABLES DE INSTANCIA ====================
    
    private ObservableList<CierreCaja> listaCierres;
    private CierreCaja cierreActual;
    private DecimalFormat formatoMoneda = new DecimalFormat("#,##0.00");
    private DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");
    
    // ==================== INICIALIZACIÓN ====================
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabla();
        configurarValidaciones();
        cargarDatosIniciales();
        cargarCierreActual();
    }
    
    /**
     * Configura las columnas de la tabla de cierres
     */
    private void configurarTabla() {
        colFechaApertura.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getFechaApertura().format(formatoFecha)));
        
        colFechaCierre.setCellValueFactory(cellData -> {
            LocalDateTime fechaCierre = cellData.getValue().getFechaCierre();
            return new SimpleStringProperty(fechaCierre != null ? fechaCierre.format(formatoFecha) : "-");
        });
        
        colCajero.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getNombreCajero()));
        
        colEfectivoSistema.setCellValueFactory(cellData -> 
            new SimpleStringProperty("₡" + formatoMoneda.format(cellData.getValue().getEfectivoSistema())));
        
        colTarjetaSistema.setCellValueFactory(cellData -> 
            new SimpleStringProperty("₡" + formatoMoneda.format(cellData.getValue().getTarjetaSistema())));
        
        colEfectivoDeclarado.setCellValueFactory(cellData -> 
            new SimpleStringProperty("₡" + formatoMoneda.format(cellData.getValue().getEfectivoDeclarado())));
        
        colTarjetaDeclarado.setCellValueFactory(cellData -> 
            new SimpleStringProperty("₡" + formatoMoneda.format(cellData.getValue().getTarjetaDeclarado())));
        
        colDiferenciaTotal.setCellValueFactory(cellData -> {
            double diferencia = cellData.getValue().getDiferenciaEfectivo() + 
                              cellData.getValue().getDiferenciaTarjeta();
            String texto = "₡" + formatoMoneda.format(Math.abs(diferencia));
            return new SimpleStringProperty(diferencia >= 0 ? "+" + texto : "-" + texto);
        });
        
        // Colorear la columna de diferencia según el valor
        colDiferenciaTotal.setCellFactory(column -> new TableCell<CierreCaja, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.startsWith("+")) {
                        setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;"); // Sobrante (azul)
                    } else if (item.startsWith("-")) {
                        setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // Faltante (rojo)
                    } else {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;"); // Cuadrado (verde)
                    }
                }
            }
        });
        
        colEstado.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getEstado()));
        
        // Colorear la columna de estado
        colEstado.setCellFactory(column -> new TableCell<CierreCaja, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("ABIERTO")) {
                        setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6c757d; -fx-font-weight: bold;");
                    }
                }
            }
        });
        
        listaCierres = FXCollections.observableArrayList();
        tblCierres.setItems(listaCierres);
    }
    
    /**
     * Configura las validaciones de los campos numéricos
     */
    private void configurarValidaciones() {
        // Validar que solo se ingresen números y punto decimal
        txtEfectivoDeclarado.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                txtEfectivoDeclarado.setText(oldValue);
            }
        });
        
        txtTarjetaDeclarado.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                txtTarjetaDeclarado.setText(oldValue);
            }
        });
    }
    
    /**
     * Carga los datos iniciales del sistema
     */
    private void cargarDatosIniciales() {
        // TODO: Cargar cajeros desde el servicio web
        cmbFiltroCajero.setItems(FXCollections.observableArrayList(
            "Todos los cajeros", "CRIS", "MARIA", "JUAN"
        ));
        cmbFiltroCajero.getSelectionModel().selectFirst();
        
        // Cargar historial de cierres
        cargarHistorialCierres();
    }
    
    /**
     * Carga el cierre actual del cajero autenticado
     */
    private void cargarCierreActual() {
        // TODO: Obtener del servicio web el cierre actual del usuario autenticado
        // Por ahora, datos de ejemplo
        cierreActual = new CierreCaja();
        cierreActual.setId(1L);
        cierreActual.setNombreCajero("CRIS");
        cierreActual.setFechaApertura(LocalDateTime.now().minusHours(4));
        cierreActual.setEfectivoSistema(125500.00);
        cierreActual.setTarjetaSistema(89300.00);
        cierreActual.setEstado("ABIERTO");
        cierreActual.setNumeroFacturas(45);
        
        actualizarEstadoActual();
        actualizarFormularioCierre();
    }
    
    /**
     * Actualiza la sección de estado actual de caja
     */
    private void actualizarEstadoActual() {
        if (cierreActual != null && cierreActual.getEstado().equals("ABIERTO")) {
            lblEstadoCaja.setText("Estado: ABIERTO");
            lblEstadoCaja.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #28a745;");
            lblCajero.setText("Cajero: " + cierreActual.getNombreCajero());
            lblFechaApertura.setText("Apertura: " + cierreActual.getFechaApertura().format(formatoFecha));
            lblEfectivoSistema.setText("Efectivo: ₡" + formatoMoneda.format(cierreActual.getEfectivoSistema()));
            lblTarjetaSistema.setText("Tarjeta: ₡" + formatoMoneda.format(cierreActual.getTarjetaSistema()));
            double total = cierreActual.getEfectivoSistema() + cierreActual.getTarjetaSistema();
            lblTotalSistema.setText("Total: ₡" + formatoMoneda.format(total));
        } else {
            lblEstadoCaja.setText("Estado: CERRADO");
            lblEstadoCaja.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #6c757d;");
            lblCajero.setText("Cajero: -");
            lblFechaApertura.setText("Apertura: -");
            lblEfectivoSistema.setText("Efectivo: ₡0.00");
            lblTarjetaSistema.setText("Tarjeta: ₡0.00");
            lblTotalSistema.setText("Total: ₡0.00");
        }
    }
    
    /**
     * Actualiza el formulario de cierre con los datos actuales
     */
    private void actualizarFormularioCierre() {
        if (cierreActual != null && cierreActual.getEstado().equals("ABIERTO")) {
            lblInfoCajero.setText("Cajero: " + cierreActual.getNombreCajero());
            lblInfoApertura.setText("Apertura: " + cierreActual.getFechaApertura().format(formatoFecha));
            lblInfoFacturas.setText("Facturas realizadas: " + cierreActual.getNumeroFacturas());
            lblSistemaEfectivo.setText("₡" + formatoMoneda.format(cierreActual.getEfectivoSistema()));
            lblSistemaTarjeta.setText("₡" + formatoMoneda.format(cierreActual.getTarjetaSistema()));
            double total = cierreActual.getEfectivoSistema() + cierreActual.getTarjetaSistema();
            lblSistemaTotal.setText("₡" + formatoMoneda.format(total));
            
            // Habilitar formulario
            txtEfectivoDeclarado.setDisable(false);
            txtTarjetaDeclarado.setDisable(false);
            btnCalcular.setDisable(false);
        } else {
            lblInfoCajero.setText("Cajero: -");
            lblInfoApertura.setText("Apertura: -");
            lblInfoFacturas.setText("Facturas realizadas: 0");
            lblSistemaEfectivo.setText("₡0.00");
            lblSistemaTarjeta.setText("₡0.00");
            lblSistemaTotal.setText("₡0.00");
            
            // Deshabilitar formulario
            txtEfectivoDeclarado.setDisable(true);
            txtTarjetaDeclarado.setDisable(true);
            btnCalcular.setDisable(true);
            btnCerrarCaja.setDisable(true);
        }
    }
    
    /**
     * Carga el historial de cierres desde el servicio web
     */
    private void cargarHistorialCierres() {
        // TODO: Obtener del servicio web
        // Por ahora, datos de ejemplo
        listaCierres.clear();
        
        CierreCaja cierre1 = new CierreCaja();
        cierre1.setId(1L);
        cierre1.setNombreCajero("CRIS");
        cierre1.setFechaApertura(LocalDateTime.now().minusDays(1).withHour(8).withMinute(0));
        cierre1.setFechaCierre(LocalDateTime.now().minusDays(1).withHour(18).withMinute(30));
        cierre1.setEfectivoSistema(145000.00);
        cierre1.setTarjetaSistema(98500.00);
        cierre1.setEfectivoDeclarado(145200.00);
        cierre1.setTarjetaDeclarado(98500.00);
        cierre1.setDiferenciaEfectivo(200.00);
        cierre1.setDiferenciaTarjeta(0.00);
        cierre1.setEstado("CERRADO");
        
        CierreCaja cierre2 = new CierreCaja();
        cierre2.setId(2L);
        cierre2.setNombreCajero("MARIA");
        cierre2.setFechaApertura(LocalDateTime.now().minusDays(1).withHour(18).withMinute(30));
        cierre2.setFechaCierre(LocalDateTime.now().minusDays(1).withHour(23).withMinute(0));
        cierre2.setEfectivoSistema(89000.00);
        cierre2.setTarjetaSistema(67800.00);
        cierre2.setEfectivoDeclarado(88500.00);
        cierre2.setTarjetaDeclarado(67800.00);
        cierre2.setDiferenciaEfectivo(-500.00);
        cierre2.setDiferenciaTarjeta(0.00);
        cierre2.setEstado("CERRADO");
        
        listaCierres.addAll(cierre1, cierre2);
    }
    
    // ==================== EVENTOS ====================
    
    @FXML
private void onVolver(ActionEvent event) {
    FlowController.getInstance().goHomeWithFade();
}
    
    @FXML
    private void onBuscar() {
        // TODO: Filtrar cierres según los criterios seleccionados
        System.out.println("Buscar cierres con filtros");
        cargarHistorialCierres();
    }
    
    @FXML
    private void onVerDetalle() {
        CierreCaja seleccionado = tblCierres.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Por favor seleccione un cierre para ver el detalle.", Alert.AlertType.WARNING);
            return;
        }
        
        // TODO: Abrir ventana con detalle completo del cierre
        System.out.println("Ver detalle del cierre ID: " + seleccionado.getId());
    }
    
    @FXML
    private void onGenerarReporte() {
        CierreCaja seleccionado = tblCierres.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarAlerta("Selección requerida", "Por favor seleccione un cierre para generar el reporte.", Alert.AlertType.WARNING);
            return;
        }
        
        // TODO: Generar reporte en Jasper Reports
        System.out.println("Generar reporte del cierre ID: " + seleccionado.getId());
        mostrarAlerta("Reporte Generado", "El reporte del cierre ha sido generado exitosamente.", Alert.AlertType.INFORMATION);
    }
    
    @FXML
    private void onRefrescar() {
        cargarCierreActual();
        cargarHistorialCierres();
        mostrarAlerta("Datos Actualizados", "Los datos han sido actualizados correctamente.", Alert.AlertType.INFORMATION);
    }
    
    @FXML
    private void onCalcular() {
        if (!validarCampos()) {
            return;
        }
        
        double efectivoDeclarado = Double.parseDouble(txtEfectivoDeclarado.getText());
        double tarjetaDeclarado = Double.parseDouble(txtTarjetaDeclarado.getText());
        
        double diferenciaEfectivo = efectivoDeclarado - cierreActual.getEfectivoSistema();
        double diferenciaTarjeta = tarjetaDeclarado - cierreActual.getTarjetaSistema();
        double diferenciaTotal = diferenciaEfectivo + diferenciaTarjeta;
        
        // Actualizar labels de diferencias
        lblDiferenciaEfectivo.setText(formatearDiferencia(diferenciaEfectivo));
        lblDiferenciaTarjeta.setText(formatearDiferencia(diferenciaTarjeta));
        lblDiferenciaTotal.setText(formatearDiferencia(diferenciaTotal));
        
        // Colorear según el resultado
        aplicarColorDiferencia(lblDiferenciaEfectivo, diferenciaEfectivo);
        aplicarColorDiferencia(lblDiferenciaTarjeta, diferenciaTarjeta);
        aplicarColorDiferencia(lblDiferenciaTotal, diferenciaTotal);
        
        // Mostrar sección de diferencias
        vboxDiferencias.setVisible(true);
        vboxDiferencias.setManaged(true);
        
        // Habilitar botón de cerrar caja
        btnCerrarCaja.setDisable(false);
    }
    
    @FXML
    private void onCerrarCaja() {
        if (!validarCampos()) {
            return;
        }
        
        // Confirmar cierre
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar Cierre de Caja");
        confirmacion.setHeaderText("¿Está seguro de cerrar la caja?");
        confirmacion.setContentText("Esta acción no se puede deshacer. Se generará un reporte con todos los movimientos.");
        
        if (confirmacion.showAndWait().get() == ButtonType.OK) {
            // TODO: Enviar al servicio web para cerrar la caja
            cierreActual.setFechaCierre(LocalDateTime.now());
            cierreActual.setEfectivoDeclarado(Double.parseDouble(txtEfectivoDeclarado.getText()));
            cierreActual.setTarjetaDeclarado(Double.parseDouble(txtTarjetaDeclarado.getText()));
            cierreActual.setDiferenciaEfectivo(cierreActual.getEfectivoDeclarado() - cierreActual.getEfectivoSistema());
            cierreActual.setDiferenciaTarjeta(cierreActual.getTarjetaDeclarado() - cierreActual.getTarjetaSistema());
            cierreActual.setEstado("CERRADO");
            
            System.out.println("Cierre de caja realizado exitosamente");
            
            // TODO: Generar e imprimir reporte de cierre
            
            // Limpiar formulario
            limpiarFormulario();
            
            // Actualizar vistas
            cargarCierreActual();
            cargarHistorialCierres();
            
            mostrarAlerta("Cierre Exitoso", "La caja ha sido cerrada exitosamente. El reporte ha sido generado.", Alert.AlertType.INFORMATION);
        }
    }
    
    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Valida que los campos del formulario estén completos
     */
    private boolean validarCampos() {
        if (txtEfectivoDeclarado.getText().isEmpty()) {
            mostrarAlerta("Campo requerido", "Por favor ingrese el monto de efectivo declarado.", Alert.AlertType.WARNING);
            txtEfectivoDeclarado.requestFocus();
            return false;
        }
        
        if (txtTarjetaDeclarado.getText().isEmpty()) {
            mostrarAlerta("Campo requerido", "Por favor ingrese el monto de tarjeta declarado.", Alert.AlertType.WARNING);
            txtTarjetaDeclarado.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Formatea una diferencia monetaria con signo
     */
    private String formatearDiferencia(double diferencia) {
        String signo = diferencia > 0 ? "+" : diferencia < 0 ? "-" : "";
        return signo + "₡" + formatoMoneda.format(Math.abs(diferencia));
    }
    
    /**
     * Aplica color a un label según la diferencia
     */
    private void aplicarColorDiferencia(Label label, double diferencia) {
        if (diferencia > 0) {
            label.setStyle("-fx-text-fill: #007bff; -fx-font-weight: bold;"); // Sobrante (azul)
        } else if (diferencia < 0) {
            label.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;"); // Faltante (rojo)
        } else {
            label.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;"); // Cuadrado (verde)
        }
    }
    
    /**
     * Limpia el formulario de cierre
     */
    private void limpiarFormulario() {
        txtEfectivoDeclarado.clear();
        txtTarjetaDeclarado.clear();
        vboxDiferencias.setVisible(false);
        vboxDiferencias.setManaged(false);
        btnCerrarCaja.setDisable(true);
    }
    
    /**
     * Muestra una alerta al usuario
     */
    private void mostrarAlerta(String titulo, String mensaje, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
    
    // ==================== CLASE INTERNA ====================
    
    /**
     * Clase que representa un cierre de caja
     * Mapea la tabla cierre_caja de la base de datos
     */
    public static class CierreCaja {
        private Long id;
        private String nombreCajero;
        private LocalDateTime fechaApertura;
        private LocalDateTime fechaCierre;
        private double efectivoDeclarado;
        private double tarjetaDeclarado;
        private double efectivoSistema;
        private double tarjetaSistema;
        private double diferenciaEfectivo;
        private double diferenciaTarjeta;
        private String estado;
        private int numeroFacturas;
        
        // Getters y Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getNombreCajero() { return nombreCajero; }
        public void setNombreCajero(String nombreCajero) { this.nombreCajero = nombreCajero; }
        
        public LocalDateTime getFechaApertura() { return fechaApertura; }
        public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }
        
        public LocalDateTime getFechaCierre() { return fechaCierre; }
        public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
        
        public double getEfectivoDeclarado() { return efectivoDeclarado; }
        public void setEfectivoDeclarado(double efectivoDeclarado) { this.efectivoDeclarado = efectivoDeclarado; }
        
        public double getTarjetaDeclarado() { return tarjetaDeclarado; }
        public void setTarjetaDeclarado(double tarjetaDeclarado) { this.tarjetaDeclarado = tarjetaDeclarado; }
        
        public double getEfectivoSistema() { return efectivoSistema; }
        public void setEfectivoSistema(double efectivoSistema) { this.efectivoSistema = efectivoSistema; }
        
        public double getTarjetaSistema() { return tarjetaSistema; }
        public void setTarjetaSistema(double tarjetaSistema) { this.tarjetaSistema = tarjetaSistema; }
        
        public double getDiferenciaEfectivo() { return diferenciaEfectivo; }
        public void setDiferenciaEfectivo(double diferenciaEfectivo) { this.diferenciaEfectivo = diferenciaEfectivo; }
        
        public double getDiferenciaTarjeta() { return diferenciaTarjeta; }
        public void setDiferenciaTarjeta(double diferenciaTarjeta) { this.diferenciaTarjeta = diferenciaTarjeta; }
        
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        
        public int getNumeroFacturas() { return numeroFacturas; }
        public void setNumeroFacturas(int numeroFacturas) { this.numeroFacturas = numeroFacturas; }
    }
}
