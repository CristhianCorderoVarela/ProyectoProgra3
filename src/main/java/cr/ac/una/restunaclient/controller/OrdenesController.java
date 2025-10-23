package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.*;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador para gestión de órdenes
 * Permite crear/editar órdenes, agregar productos y gestionar detalles
 */
public class OrdenesController implements Initializable {

    // ==================== HEADER ====================
    @FXML private Label lblTitle;
    @FXML private Label lblUsuario;
    @FXML private Button btnVolver;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelarOrden;
    
    // ==================== INFO ORDEN ====================
    @FXML private Label lblMesaInfo;
    @FXML private Label lblSalonInfo;
    @FXML private Label lblFechaHora;
    @FXML private TextArea txtObservaciones;
    
    // ==================== SELECTOR DE PRODUCTOS ====================
    @FXML private TextField txtBuscarProducto;
    @FXML private ComboBox<GrupoProducto> cmbGrupos;
    @FXML private FlowPane flowProductos;
    
    // ==================== TABLA DE DETALLES ====================
    @FXML private TableView<DetalleOrden> tblDetalles;
    @FXML private TableColumn<DetalleOrden, String> colProducto;
    @FXML private TableColumn<DetalleOrden, Integer> colCantidad;
    @FXML private TableColumn<DetalleOrden, String> colPrecio;
    @FXML private TableColumn<DetalleOrden, String> colSubtotal;
    
    // ==================== TOTALES ====================
    @FXML private Label lblTotal;
    
    // ==================== VARIABLES ====================
    private Orden ordenActual;
    private Mesa mesaSeleccionada;
    private Salon salonSeleccionado;
    private ObservableList<DetalleOrden> detallesOrden;
    private List<GrupoProducto> listaGrupos;
    private List<Producto> listaProductos;
    private boolean modoEdicion = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Obtener contexto
        mesaSeleccionada = (Mesa) AppContext.getInstance().get("mesaSeleccionada");
        salonSeleccionado = (Salon) AppContext.getInstance().get("salonSeleccionado");
        
        configurarTabla();
        configurarComboGrupos();
        cargarGrupos();
        cargarProductos();
        cargarOrdenExistente();
        actualizarInfoOrden();
        actualizarTextos();
        
        // Configurar búsqueda
        txtBuscarProducto.textProperty().addListener((obs, old, val) -> filtrarProductos(val));
    }

    // ==================== CONFIGURACIÓN INICIAL ====================
    
    private void configurarTabla() {
        colProducto.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getProducto() != null ? 
                data.getValue().getProducto().getNombre() : "")
        );
        
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));
        
        colPrecio.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("₡%.2f", data.getValue().getPrecioUnitario()))
        );
        
        colSubtotal.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("₡%.2f", data.getValue().getSubtotal()))
        );
        
        detallesOrden = FXCollections.observableArrayList();
        tblDetalles.setItems(detallesOrden);
        
        // Doble clic para editar cantidad
        tblDetalles.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                DetalleOrden detalle = tblDetalles.getSelectionModel().getSelectedItem();
                if (detalle != null) {
                    editarCantidad(detalle);
                }
            }
        });
        
        // Menú contextual
        ContextMenu contextMenu = new ContextMenu();
        MenuItem itemEditar = new MenuItem(I18n.isSpanish() ? "✏️ Editar cantidad" : "✏️ Edit quantity");
        MenuItem itemEliminar = new MenuItem(I18n.isSpanish() ? "🗑️ Eliminar" : "🗑️ Delete");
        
        itemEditar.setOnAction(e -> {
            DetalleOrden detalle = tblDetalles.getSelectionModel().getSelectedItem();
            if (detalle != null) editarCantidad(detalle);
        });
        
        itemEliminar.setOnAction(e -> {
            DetalleOrden detalle = tblDetalles.getSelectionModel().getSelectedItem();
            if (detalle != null) eliminarDetalle(detalle);
        });
        
        contextMenu.getItems().addAll(itemEditar, itemEliminar);
        tblDetalles.setContextMenu(contextMenu);
    }

    private void configurarComboGrupos() {
        cmbGrupos.setConverter(new javafx.util.StringConverter<GrupoProducto>() {
            @Override
            public String toString(GrupoProducto grupo) {
                return grupo != null ? grupo.getNombre() : "";
            }
            @Override
            public GrupoProducto fromString(String string) {
                return null;
            }
        });
        
        cmbGrupos.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, grupo) -> {
                if (grupo != null) {
                    filtrarProductosPorGrupo(grupo);
                }
            }
        );
    }

    // ==================== CARGA DE DATOS ====================
    
    private void cargarGrupos() {
        try {
            String jsonResponse = RestClient.get("/grupos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                listaGrupos = gson.fromJson(dataJson, new TypeToken<List<GrupoProducto>>(){}.getType());
                
                cmbGrupos.getItems().clear();
                cmbGrupos.getItems().add(null); // Opción "Todos"
                cmbGrupos.getItems().addAll(listaGrupos);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar grupos:\n" + e.getMessage());
        }
    }

    private void cargarProductos() {
        try {
            String jsonResponse = RestClient.get("/productos/activos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                listaProductos = gson.fromJson(dataJson, new TypeToken<List<Producto>>(){}.getType());
                
                mostrarProductos(listaProductos);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar productos:\n" + e.getMessage());
        }
    }

    private void cargarOrdenExistente() {
        if (mesaSeleccionada == null || !mesaSeleccionada.isOcupada()) {
            // Nueva orden
            ordenActual = new Orden();
            ordenActual.setMesaId(mesaSeleccionada != null ? mesaSeleccionada.getId() : null);
            ordenActual.setUsuarioId(AppContext.getInstance().getUsuarioLogueado().getId());
            modoEdicion = false;
            return;
        }
        
        // Cargar orden existente
        try {
            String jsonResponse = RestClient.get("/ordenes/mesa/" + mesaSeleccionada.getId());
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                ordenActual = gson.fromJson(dataJson, Orden.class);
                
                // Cargar detalles
                cargarDetalles();
                modoEdicion = true;
            }
        } catch (Exception e) {
            System.out.println("No hay orden existente para esta mesa");
            ordenActual = new Orden();
            ordenActual.setMesaId(mesaSeleccionada.getId());
            ordenActual.setUsuarioId(AppContext.getInstance().getUsuarioLogueado().getId());
            modoEdicion = false;
        }
    }

    private void cargarDetalles() {
        if (ordenActual == null || ordenActual.getId() == null) return;
        
        try {
            String jsonResponse = RestClient.get("/ordenes/" + ordenActual.getId() + "/detalles");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                List<DetalleOrden> detalles = gson.fromJson(dataJson, new TypeToken<List<DetalleOrden>>(){}.getType());
                
                detallesOrden.clear();
                detallesOrden.addAll(detalles);
                calcularTotal();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar detalles:\n" + e.getMessage());
        }
    }

    // ==================== VISUALIZACIÓN DE PRODUCTOS ====================
    
    private void mostrarProductos(List<Producto> productos) {
        flowProductos.getChildren().clear();
        
        if (productos == null || productos.isEmpty()) {
            Label lblVacio = new Label(I18n.isSpanish() ? "No hay productos disponibles" : "No products available");
            lblVacio.setStyle("-fx-text-fill: #999; -fx-font-size: 14px;");
            flowProductos.getChildren().add(lblVacio);
            return;
        }
        
        for (Producto producto : productos) {
            flowProductos.getChildren().add(crearBotonProducto(producto));
        }
    }

    private VBox crearBotonProducto(Producto producto) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(120, 100);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                     "-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 10; " +
                     "-fx-padding: 10; -fx-cursor: hand;");
        
        Label lblNombre = new Label(producto.getNombreDisplay());
        lblNombre.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblNombre.setWrapText(true);
        lblNombre.setMaxWidth(100);
        lblNombre.setAlignment(Pos.CENTER);
        
        Label lblPrecio = new Label(producto.getPrecioFormateado());
        lblPrecio.setStyle("-fx-text-fill: #FF7A00; -fx-font-size: 13px; -fx-font-weight: bold;");
        
        card.getChildren().addAll(lblNombre, lblPrecio);
        
        // Efectos hover
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #FFF8F0; -fx-background-radius: 10; " +
                         "-fx-border-color: #FF7A00; -fx-border-width: 2; -fx-border-radius: 10; " +
                         "-fx-padding: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                         "-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 10; " +
                         "-fx-padding: 10; -fx-cursor: hand;");
        });
        
        card.setOnMouseClicked(e -> agregarProducto(producto));
        
        return card;
    }

    private void filtrarProductos(String busqueda) {
        if (busqueda == null || busqueda.trim().isEmpty()) {
            if (cmbGrupos.getValue() != null) {
                filtrarProductosPorGrupo(cmbGrupos.getValue());
            } else {
                mostrarProductos(listaProductos);
            }
            return;
        }
        
        String busquedaLower = busqueda.toLowerCase();
        List<Producto> filtrados = new ArrayList<>();
        
        for (Producto p : listaProductos) {
            if (p.getNombre().toLowerCase().contains(busquedaLower) ||
                p.getNombreCorto().toLowerCase().contains(busquedaLower)) {
                filtrados.add(p);
            }
        }
        
        mostrarProductos(filtrados);
    }

    private void filtrarProductosPorGrupo(GrupoProducto grupo) {
        List<Producto> filtrados = new ArrayList<>();
        
        for (Producto p : listaProductos) {
            if (p.getGrupoId() != null && p.getGrupoId().equals(grupo.getId())) {
                filtrados.add(p);
            }
        }
        
        mostrarProductos(filtrados);
    }

    // ==================== GESTIÓN DE DETALLES ====================
    
    private void agregarProducto(Producto producto) {
        // Buscar si ya existe
        for (DetalleOrden detalle : detallesOrden) {
            if (detalle.getProducto() != null && 
                detalle.getProducto().getId().equals(producto.getId())) {
                // Incrementar cantidad
                detalle.setCantidad(detalle.getCantidad() + 1);
                detalle.calcularSubtotal();
                tblDetalles.refresh();
                calcularTotal();
                return;
            }
        }
        
        // Crear nuevo detalle
        DetalleOrden nuevoDetalle = new DetalleOrden();
        nuevoDetalle.setProducto(producto);
        nuevoDetalle.setProductoId(producto.getId());
        nuevoDetalle.setCantidad(1);
        nuevoDetalle.setPrecioUnitario(producto.getPrecio());
        nuevoDetalle.calcularSubtotal();
        
        detallesOrden.add(nuevoDetalle);
        calcularTotal();
    }

    private void editarCantidad(DetalleOrden detalle) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(detalle.getCantidad()));
        dialog.setTitle(I18n.isSpanish() ? "Editar Cantidad" : "Edit Quantity");
        dialog.setHeaderText(detalle.getProducto().getNombre());
        dialog.setContentText(I18n.isSpanish() ? "Cantidad:" : "Quantity:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(cantidad -> {
            try {
                int nuevaCantidad = Integer.parseInt(cantidad);
                if (nuevaCantidad > 0) {
                    detalle.setCantidad(nuevaCantidad);
                    detalle.calcularSubtotal();
                    tblDetalles.refresh();
                    calcularTotal();
                } else {
                    Mensaje.showWarning("Error", I18n.isSpanish() ? 
                        "La cantidad debe ser mayor a 0" : 
                        "Quantity must be greater than 0");
                }
            } catch (NumberFormatException e) {
                Mensaje.showWarning("Error", I18n.isSpanish() ? 
                    "Cantidad inválida" : 
                    "Invalid quantity");
            }
        });
    }

    private void eliminarDetalle(DetalleOrden detalle) {
        boolean confirmar = Mensaje.showConfirmation(
            I18n.isSpanish() ? "Confirmar" : "Confirm",
            I18n.isSpanish() ? 
                "¿Eliminar " + detalle.getProducto().getNombre() + "?" :
                "Delete " + detalle.getProducto().getNombre() + "?"
        );
        
        if (confirmar) {
            detallesOrden.remove(detalle);
            calcularTotal();
        }
    }

    private void calcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleOrden detalle : detallesOrden) {
            total = total.add(detalle.getSubtotal());
        }
        lblTotal.setText(String.format("₡%.2f", total));
    }

    // ==================== EVENTOS ====================
    
    @FXML
    private void onVolver(ActionEvent event) {
        if (!detallesOrden.isEmpty()) {
            boolean confirmar = Mensaje.showConfirmation(
                I18n.isSpanish() ? "Confirmar" : "Confirm",
                I18n.isSpanish() ? 
                    "¿Salir sin guardar? Se perderán los cambios." :
                    "Exit without saving? Changes will be lost."
            );
            if (!confirmar) return;
        }
        
        FlowController.getInstance().goToView("VistaSalones", "RestUNA - Salones", 1400, 800);
    }

    @FXML
    private void onGuardar(ActionEvent event) {
        if (detallesOrden.isEmpty()) {
            Mensaje.showWarning("Aviso", I18n.isSpanish() ? 
                "Debe agregar al menos un producto" : 
                "Must add at least one product");
            return;
        }
        
        try {
            ordenActual.setObservaciones(txtObservaciones.getText());
            ordenActual.setDetalles(new ArrayList<>(detallesOrden));
            
            String jsonResponse;
            if (modoEdicion && ordenActual.getId() != null) {
                // Actualizar orden existente
                jsonResponse = RestClient.put("/ordenes/" + ordenActual.getId(), ordenActual);
            } else {
                // Crear nueva orden
                jsonResponse = RestClient.post("/ordenes", ordenActual);
            }
            
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", I18n.isSpanish() ? 
                    "Orden guardada correctamente" : 
                    "Order saved successfully");
                
                // Ocupar mesa si es nueva orden
                if (!modoEdicion && mesaSeleccionada != null) {
                    RestClient.post("/salones/mesas/" + mesaSeleccionada.getId() + "/ocupar", null);
                }
                
                FlowController.getInstance().goToView("VistaSalones", "RestUNA - Salones", 1400, 800);
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar orden:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancelarOrden(ActionEvent event) {
        if (ordenActual == null || ordenActual.getId() == null) {
            onVolver(null);
            return;
        }
        
        boolean confirmar = Mensaje.showConfirmation(
            I18n.isSpanish() ? "Confirmar Cancelación" : "Confirm Cancellation",
            I18n.isSpanish() ? 
                "¿Está seguro de cancelar esta orden?\nEsta acción no se puede deshacer." :
                "Are you sure you want to cancel this order?\nThis action cannot be undone."
        );
        
        if (!confirmar) return;
        
        try {
            String jsonResponse = RestClient.post("/ordenes/" + ordenActual.getId() + "/cancelar", null);
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", I18n.isSpanish() ? 
                    "Orden cancelada correctamente" : 
                    "Order cancelled successfully");
                
                // Liberar mesa
                if (mesaSeleccionada != null) {
                    RestClient.post("/salones/mesas/" + mesaSeleccionada.getId() + "/liberar", null);
                }
                
                FlowController.getInstance().goToView("VistaSalones", "RestUNA - Salones", 1400, 800);
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cancelar orden:\n" + e.getMessage());
        }
    }

    // ==================== ACTUALIZACIÓN DE TEXTOS ====================
    
    private void actualizarInfoOrden() {
        Usuario usuario = AppContext.getInstance().getUsuarioLogueado();
        lblUsuario.setText((I18n.isSpanish() ? "Salonero: " : "Waiter: ") + usuario.getNombre());
        
        if (mesaSeleccionada != null) {
            lblMesaInfo.setText((I18n.isSpanish() ? "Mesa: " : "Table: ") + mesaSeleccionada.getIdentificador());
        } else {
            lblMesaInfo.setText(I18n.isSpanish() ? "Orden Directa (Sin mesa)" : "Direct Order (No table)");
        }
        
        if (salonSeleccionado != null) {
            lblSalonInfo.setText((I18n.isSpanish() ? "Salón: " : "Room: ") + salonSeleccionado.getNombre());
        } else {
            lblSalonInfo.setText(I18n.isSpanish() ? "Barra" : "Bar");
        }
        
        if (ordenActual != null && ordenActual.getFechaHora() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            lblFechaHora.setText(ordenActual.getFechaHora().format(formatter));
        }
    }

    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();
        
        lblTitle.setText(esEspanol ? "📝 Gestión de Orden" : "📝 Order Management");
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        btnGuardar.setText(esEspanol ? "💾 Guardar Orden" : "💾 Save Order");
        btnCancelarOrden.setText(esEspanol ? "❌ Cancelar Orden" : "❌ Cancel Order");
        
        txtBuscarProducto.setPromptText(esEspanol ? "Buscar producto..." : "Search product...");
        cmbGrupos.setPromptText(esEspanol ? "Todos los grupos" : "All groups");
        
        colProducto.setText(esEspanol ? "Producto" : "Product");
        colCantidad.setText(esEspanol ? "Cant." : "Qty");
        colPrecio.setText(esEspanol ? "Precio" : "Price");
        colSubtotal.setText(esEspanol ? "Subtotal" : "Subtotal");
        
        txtObservaciones.setPromptText(esEspanol ? "Observaciones adicionales..." : "Additional notes...");
    }
}