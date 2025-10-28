package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.util.FlowController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador para la ventana de ventas/facturación del sistema RestUNA
 * Maneja la lógica de facturación, cálculo de impuestos, descuentos y procesamiento de pagos
 * 
 * @author Tu Nombre
 * @version 1.0
 */
public class VentanaVentasController implements Initializable {

    // ==================== FXML Components - Header ====================
    @FXML private Label lblTitle;
    @FXML private Label lblUsuario;
    @FXML private Label lblFechaHora;
    @FXML private Button btnVolver;

    // ==================== FXML Components - Orden Info ====================
    @FXML private Label lblOrdenInfo;
    @FXML private Label lblMesaInfo;
    @FXML private Button btnSeleccionarOrden;

    // ==================== FXML Components - Cliente ====================
    @FXML private TextField txtCliente;
    @FXML private Button btnBuscarCliente;

    // ==================== FXML Components - Tabla Productos ====================
    @FXML private TableView<ProductoVenta> tblProductos;
    @FXML private TableColumn<ProductoVenta, String> colProducto;
    @FXML private TableColumn<ProductoVenta, Integer> colCantidad;
    @FXML private TableColumn<ProductoVenta, String> colPrecio;
    @FXML private TableColumn<ProductoVenta, String> colSubtotal;
    @FXML private TableColumn<ProductoVenta, Void> colAcciones;
    
    @FXML private Button btnAgregarProducto;
    @FXML private Button btnModificarCantidad;
    @FXML private Button btnEliminarProducto;

    // ==================== FXML Components - Resumen ====================
    @FXML private Label lblSubtotal;
    @FXML private CheckBox chkImpuestoVentas;
    @FXML private Label lblPorcentajeIV;
    @FXML private Label lblImpuestoVentas;
    @FXML private CheckBox chkImpuestoServicio;
    @FXML private Label lblPorcentajeIS;
    @FXML private Label lblImpuestoServicio;
    @FXML private TextField txtDescuento;
    @FXML private Label lblDescuento;
    @FXML private Label lblDescuentoMax;
    @FXML private Label lblTotal;

    // ==================== FXML Components - Pago ====================
    @FXML private TextField txtEfectivo;
    @FXML private TextField txtTarjeta;
    @FXML private Label lblVuelto;
    @FXML private Button btnProcesarPago;
    @FXML private Button btnImprimir;
    @FXML private Button btnEnviarEmail;
    @FXML private Button btnCancelar;

    // ==================== Variables de instancia ====================
    private ObservableList<ProductoVenta> productosVenta;
    private DecimalFormat formatoMoneda;
    private NumberFormat formatoPorcentaje;
    
    // Parámetros del sistema (normalmente se cargarían de la BD)
    private double porcentajeImpuestoVentas = 13.0;
    private double porcentajeImpuestoServicio = 10.0;
    private double porcentajeDescuentoMaximo = 15.0;
    
    // Valores calculados
    private double subtotal = 0.0;
    private double impuestoVentas = 0.0;
    private double impuestoServicio = 0.0;
    private double descuento = 0.0;
    private double total = 0.0;
    
    // Información de la orden actual
    private Long ordenId;
    private String mesaNombre;
    private String salonNombre;
    private boolean cobraServicio = true;

    /**
     * Inicializa el controlador después de que su elemento raíz ha sido procesado
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Inicializar formatos
        formatoMoneda = new DecimalFormat("₡#,##0.00");
        formatoPorcentaje = NumberFormat.getPercentInstance(new Locale("es", "CR"));
        formatoPorcentaje.setMaximumFractionDigits(2);
        
        // Inicializar lista de productos
        productosVenta = FXCollections.observableArrayList();
        
        // Configurar tabla de productos
        configurarTablaProductos();
        
        // Configurar reloj en tiempo real
        iniciarReloj();
        
        // Cargar parámetros del sistema
        cargarParametros();
        
        // Configurar listeners
        configurarListeners();
        
        // Actualizar interfaz inicial
        actualizarInterfaz();
    }

    /**
     * Configura las columnas de la tabla de productos
     */
    private void configurarTablaProductos() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        
        // Formatear precio con símbolo de moneda
        colPrecio.setCellValueFactory(cellData -> 
            new SimpleStringProperty(formatoMoneda.format(cellData.getValue().getPrecio()))
        );
        
        // Formatear subtotal con símbolo de moneda
        colSubtotal.setCellValueFactory(cellData -> 
            new SimpleStringProperty(formatoMoneda.format(cellData.getValue().getSubtotal()))
        );
        
        // Configurar columna de acciones con botones
        configurarColumnaAcciones();
        
        tblProductos.setItems(productosVenta);
    }

    /**
     * Configura la columna de acciones con botones para cada fila
     */
    private void configurarColumnaAcciones() {
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEliminar = new Button("🗑️");
            
            {
                btnEliminar.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnEliminar.setOnAction(event -> {
                    ProductoVenta producto = getTableView().getItems().get(getIndex());
                    eliminarProducto(producto);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnEliminar);
                }
            }
        });
    }

    /**
     * Inicia el reloj que actualiza la fecha y hora en tiempo real
     */
    private void iniciarReloj() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a");
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            lblFechaHora.setText(now.format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    /**
     * Carga los parámetros del sistema desde la base de datos
     * En una implementación real, esto haría una llamada al servicio web
     */
    private void cargarParametros() {
        // TODO: Implementar llamada al servicio web para obtener parámetros
        // Por ahora usamos valores por defecto
        
        lblPorcentajeIV.setText("(" + porcentajeImpuestoVentas + "%)");
        lblPorcentajeIS.setText("(" + porcentajeImpuestoServicio + "%)");
        lblDescuentoMax.setText("Descuento máximo permitido: " + porcentajeDescuentoMaximo + "%");
    }

    /**
     * Configura los listeners para los campos de entrada
     */
    private void configurarListeners() {
        // Validar que solo se ingresen números en campos numéricos
        txtDescuento.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                txtDescuento.setText(oldValue);
            }
        });
        
        txtEfectivo.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                txtEfectivo.setText(oldValue);
            }
        });
        
        txtTarjeta.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                txtTarjeta.setText(oldValue);
            }
        });
    }

    /**
     * Actualiza la interfaz con la información actual
     */
    private void actualizarInterfaz() {
        // Actualizar información de la orden
        if (ordenId != null) {
            lblOrdenInfo.setText("Orden #" + ordenId);
            if (mesaNombre != null && salonNombre != null) {
                lblMesaInfo.setText("Mesa: " + mesaNombre + " - " + salonNombre);
            }
        }
        
        // Actualizar estado de impuesto de servicio según la mesa/salón
        chkImpuestoServicio.setSelected(cobraServicio);
        
        // Calcular totales
        onCalcularTotales(null);
    }

    // ==================== Event Handlers - Header ====================
    
    @FXML
private void onVolver(ActionEvent event) {
    FlowController.getInstance().goHomeWithFade();
}

    // ==================== Event Handlers - Orden ====================
    
    @FXML
    private void onSeleccionarOrden(ActionEvent event) {
        // TODO: Abrir diálogo para seleccionar una orden existente
        mostrarMensaje("Información", "Funcionalidad en desarrollo", 
            "Esta función permitirá seleccionar órdenes pendientes de facturación", Alert.AlertType.INFORMATION);
    }

    // ==================== Event Handlers - Cliente ====================
    
    @FXML
    private void onBuscarCliente(ActionEvent event) {
        // TODO: Abrir diálogo de búsqueda de clientes
        mostrarMensaje("Información", "Funcionalidad en desarrollo", 
            "Esta función permitirá buscar clientes registrados", Alert.AlertType.INFORMATION);
    }

    // ==================== Event Handlers - Productos ====================
    
    @FXML
    private void onAgregarProducto(ActionEvent event) {
        // TODO: Abrir diálogo de selección de productos
        // Por ahora agregamos un producto de ejemplo
        ProductoVenta ejemplo = new ProductoVenta(
            1L, "Producto Ejemplo", 1, 5000.0
        );
        productosVenta.add(ejemplo);
        onCalcularTotales(null);
    }

    @FXML
    private void onModificarCantidad(ActionEvent event) {
        ProductoVenta seleccionado = tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Advertencia", "Seleccione un producto", 
                "Debe seleccionar un producto de la tabla para modificar su cantidad", Alert.AlertType.WARNING);
            return;
        }
        
        // Solicitar nueva cantidad
        TextInputDialog dialog = new TextInputDialog(String.valueOf(seleccionado.getCantidad()));
        dialog.setTitle("Modificar Cantidad");
        dialog.setHeaderText("Producto: " + seleccionado.getNombre());
        dialog.setContentText("Nueva cantidad:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cantidad -> {
            try {
                int nuevaCantidad = Integer.parseInt(cantidad);
                if (nuevaCantidad > 0) {
                    seleccionado.setCantidad(nuevaCantidad);
                    tblProductos.refresh();
                    onCalcularTotales(null);
                } else {
                    mostrarMensaje("Error", "Cantidad inválida", 
                        "La cantidad debe ser mayor a cero", Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException e) {
                mostrarMensaje("Error", "Cantidad inválida", 
                    "Debe ingresar un número entero válido", Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void onEliminarProducto(ActionEvent event) {
        ProductoVenta seleccionado = tblProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Advertencia", "Seleccione un producto", 
                "Debe seleccionar un producto de la tabla para eliminarlo", Alert.AlertType.WARNING);
            return;
        }
        
        eliminarProducto(seleccionado);
    }

    /**
     * Elimina un producto de la lista
     */
    private void eliminarProducto(ProductoVenta producto) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar eliminación");
        alert.setHeaderText("¿Eliminar producto?");
        alert.setContentText("¿Está seguro que desea eliminar: " + producto.getNombre() + "?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            productosVenta.remove(producto);
            onCalcularTotales(null);
        }
    }

    // ==================== Event Handlers - Cálculos ====================
    
    @FXML
    private void onCalcularTotales(ActionEvent event) {
        // Calcular subtotal
        subtotal = productosVenta.stream()
            .mapToDouble(ProductoVenta::getSubtotal)
            .sum();
        
        // Calcular impuesto de ventas
        impuestoVentas = chkImpuestoVentas.isSelected() 
            ? subtotal * (porcentajeImpuestoVentas / 100.0) 
            : 0.0;
        
        // Calcular impuesto de servicio
        impuestoServicio = chkImpuestoServicio.isSelected() 
            ? subtotal * (porcentajeImpuestoServicio / 100.0) 
            : 0.0;
        
        // Calcular descuento
        double porcentajeDescuento = 0.0;
        try {
            porcentajeDescuento = Double.parseDouble(txtDescuento.getText());
            if (porcentajeDescuento > porcentajeDescuentoMaximo) {
                porcentajeDescuento = porcentajeDescuentoMaximo;
                txtDescuento.setText(String.valueOf(porcentajeDescuentoMaximo));
                mostrarMensaje("Advertencia", "Descuento excedido", 
                    "El descuento máximo permitido es " + porcentajeDescuentoMaximo + "%", 
                    Alert.AlertType.WARNING);
            }
        } catch (NumberFormatException e) {
            porcentajeDescuento = 0.0;
        }
        
        descuento = (subtotal + impuestoVentas + impuestoServicio) * (porcentajeDescuento / 100.0);
        
        // Calcular total
        total = subtotal + impuestoVentas + impuestoServicio - descuento;
        
        // Actualizar labels
        lblSubtotal.setText(formatoMoneda.format(subtotal));
        lblImpuestoVentas.setText(formatoMoneda.format(impuestoVentas));
        lblImpuestoServicio.setText(formatoMoneda.format(impuestoServicio));
        lblDescuento.setText("-" + formatoMoneda.format(descuento));
        lblTotal.setText(formatoMoneda.format(total));
        
        // Recalcular vuelto
        onCalcularVuelto(null);
    }

    @FXML
    private void onCalcularVuelto(ActionEvent event) {
        try {
            double efectivo = Double.parseDouble(txtEfectivo.getText());
            double tarjeta = Double.parseDouble(txtTarjeta.getText());
            double recibido = efectivo + tarjeta;
            double vuelto = recibido - total;
            
            if (vuelto < 0) {
                lblVuelto.setText(formatoMoneda.format(0.0));
                lblVuelto.setStyle("-fx-font-weight: bold; -fx-text-fill: #dc3545;");
            } else {
                lblVuelto.setText(formatoMoneda.format(vuelto));
                lblVuelto.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            }
        } catch (NumberFormatException e) {
            lblVuelto.setText(formatoMoneda.format(0.0));
        }
    }

    // ==================== Event Handlers - Pago ====================
    
    @FXML
    private void onProcesarPago(ActionEvent event) {
        // Validaciones
        if (productosVenta.isEmpty()) {
            mostrarMensaje("Advertencia", "No hay productos", 
                "Debe agregar al menos un producto para procesar el pago", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            double efectivo = Double.parseDouble(txtEfectivo.getText());
            double tarjeta = Double.parseDouble(txtTarjeta.getText());
            double recibido = efectivo + tarjeta;
            
            if (recibido < total) {
                mostrarMensaje("Advertencia", "Monto insuficiente", 
                    "El monto recibido es menor al total a pagar", Alert.AlertType.WARNING);
                return;
            }
            
            // Confirmar procesamiento
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmar Pago");
            alert.setHeaderText("¿Procesar el pago?");
            alert.setContentText("Total: " + formatoMoneda.format(total) + 
                "\nRecibido: " + formatoMoneda.format(recibido) +
                "\nVuelto: " + formatoMoneda.format(recibido - total));
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // TODO: Implementar guardado de factura en BD mediante servicio web
                procesarFactura();
            }
            
        } catch (NumberFormatException e) {
            mostrarMensaje("Error", "Montos inválidos", 
                "Verifique que los montos de efectivo y tarjeta sean válidos", Alert.AlertType.ERROR);
        }
    }

    /**
     * Procesa la factura y la guarda en la base de datos
     */
    private void procesarFactura() {
        // TODO: Implementar llamada al servicio web para guardar la factura
        
        mostrarMensaje("Éxito", "Pago procesado", 
            "La factura ha sido procesada exitosamente", Alert.AlertType.INFORMATION);
        
        // Limpiar formulario
        limpiarFormulario();
    }

    @FXML
    private void onImprimir(ActionEvent event) {
        if (productosVenta.isEmpty()) {
            mostrarMensaje("Advertencia", "No hay productos", 
                "Debe agregar productos antes de imprimir", Alert.AlertType.WARNING);
            return;
        }
        
        // TODO: Implementar generación e impresión de factura con JasperReports
        mostrarMensaje("Información", "Funcionalidad en desarrollo", 
            "Esta función generará e imprimirá la factura", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void onEnviarEmail(ActionEvent event) {
        if (productosVenta.isEmpty()) {
            mostrarMensaje("Advertencia", "No hay productos", 
                "Debe agregar productos antes de enviar por email", Alert.AlertType.WARNING);
            return;
        }
        
        // Solicitar email del cliente
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Enviar Factura por Email");
        dialog.setHeaderText("Ingrese el correo electrónico del cliente");
        dialog.setContentText("Email:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(email -> {
            if (validarEmail(email)) {
                // TODO: Implementar envío de email con factura
                mostrarMensaje("Información", "Email enviado", 
                    "La factura ha sido enviada a: " + email, Alert.AlertType.INFORMATION);
            } else {
                mostrarMensaje("Error", "Email inválido", 
                    "El formato del email no es válido", Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        if (productosVenta.isEmpty()) {
            cerrarVentana();
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Cancelación");
        alert.setHeaderText("¿Cancelar la venta?");
        alert.setContentText("Se perderán todos los productos agregados");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            limpiarFormulario();
        }
    }

    // ==================== Métodos auxiliares ====================
    
    /**
     * Limpia el formulario y reinicia los valores
     */
    private void limpiarFormulario() {
        productosVenta.clear();
        txtCliente.clear();
        txtDescuento.setText("0");
        txtEfectivo.setText("0.00");
        txtTarjeta.setText("0.00");
        chkImpuestoVentas.setSelected(true);
        chkImpuestoServicio.setSelected(cobraServicio);
        ordenId = null;
        mesaNombre = null;
        salonNombre = null;
        
        lblOrdenInfo.setText("Orden #0001");
        lblMesaInfo.setText("Mesa: No asignada");
        
        onCalcularTotales(null);
    }

    /**
     * Cierra la ventana actual
     */
    private void cerrarVentana() {
        Stage stage = (Stage) btnVolver.getScene().getWindow();
        stage.close();
    }

    /**
     * Muestra un mensaje al usuario
     */
    private void mostrarMensaje(String titulo, String encabezado, String contenido, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(encabezado);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    /**
     * Valida el formato de un email
     */
    private boolean validarEmail(String email) {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(regex);
    }

    // ==================== Métodos públicos para configuración ====================
    
    /**
     * Configura la orden a facturar
     */
    public void configurarOrden(Long ordenId, String salonNombre, String mesaNombre, boolean cobraServicio) {
        this.ordenId = ordenId;
        this.salonNombre = salonNombre;
        this.mesaNombre = mesaNombre;
        this.cobraServicio = cobraServicio;
        actualizarInterfaz();
    }

    /**
     * Carga los productos de una orden existente
     */
    public void cargarProductosOrden(ObservableList<ProductoVenta> productos) {
        this.productosVenta.clear();
        this.productosVenta.addAll(productos);
        onCalcularTotales(null);
    }

    // ==================== Clase interna ProductoVenta ====================
    
    /**
     * Clase que representa un producto en la venta
     */
    public static class ProductoVenta {
        private Long id;
        private String nombre;
        private int cantidad;
        private double precio;

        public ProductoVenta(Long id, String nombre, int cantidad, double precio) {
            this.id = id;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precio = precio;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNombre() {
            return nombre;
        }

        public void setNombre(String nombre) {
            this.nombre = nombre;
        }

        public int getCantidad() {
            return cantidad;
        }

        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }

        public double getPrecio() {
            return precio;
        }

        public void setPrecio(double precio) {
            this.precio = precio;
        }

        public double getSubtotal() {
            return cantidad * precio;
        }
    }
}
