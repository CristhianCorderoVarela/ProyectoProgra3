package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.GrupoProducto;
import cr.ac.una.restunaclient.model.Producto;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador para CRUD de Productos del Menú
 * Permite crear, editar, eliminar y listar productos
 */
public class ProductosController implements Initializable {

    // ==================== COMPONENTES DEL HEADER ====================
    @FXML private Label lblTitle;
    @FXML private Button btnVolver;
    
    // ==================== COMPONENTES DE LA TABLA ====================
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<GrupoProducto> cmbFiltroGrupo;
    @FXML private Button btnNuevo;
    @FXML private TableView<Producto> tblProductos;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colNombreCorto;
    @FXML private TableColumn<Producto, String> colGrupo;
    @FXML private TableColumn<Producto, String> colPrecio;
    @FXML private TableColumn<Producto, String> colMenuRapido;
    @FXML private TableColumn<Producto, String> colVentas;
    @FXML private TableColumn<Producto, String> colEstado;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnRefrescar;
    
    // ==================== COMPONENTES DEL FORMULARIO ====================
    @FXML private Label lblFormTitle;
    @FXML private Label lblNombre;
    @FXML private TextField txtNombre;
    @FXML private Label lblNombreCorto;
    @FXML private TextField txtNombreCorto;
    @FXML private Label lblGrupo;
    @FXML private ComboBox<GrupoProducto> cmbGrupo;
    @FXML private Label lblPrecio;
    @FXML private TextField txtPrecio;
    @FXML private Label lblOpciones;
    @FXML private CheckBox chkMenuRapido;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    
    // ==================== VARIABLES DE CLASE ====================
    private ObservableList<Producto> listaProductos;
    private FilteredList<Producto> listaFiltrada;
    private ObservableList<GrupoProducto> listaGrupos;
    private java.util.Map<Long, GrupoProducto> gruposById = new java.util.HashMap<>();
    private Producto productoSeleccionado;
    private boolean modoEdicion = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Verificar permisos
        if (!AppContext.getInstance().isAdministrador()) {
            Mensaje.showError("Acceso Denegado", "Solo los administradores pueden gestionar productos.");
            onVolver(null);
            return;
        }
        
        configurarTabla();
        cargarGrupos();
        configurarBusqueda();
        cargarProductos();
        limpiarFormulario();
        actualizarTextos();
    }

    
    
    
    
    // ==================== CONFIGURACIÓN INICIAL ====================
    
    /**
     * Configura las columnas de la tabla
     */
    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNombre())
        );
        
        colNombreCorto.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNombreCorto())
        );
        
        colGrupo.setCellValueFactory(data -> {
    Producto p = data.getValue();
    String nombreGrupo;
    if (p.getGrupo() != null) {
        nombreGrupo = p.getGrupo().getNombre();
    } else if (p.getGrupoId() != null && gruposById.get(p.getGrupoId()) != null) {
        nombreGrupo = gruposById.get(p.getGrupoId()).getNombre();
    } else {
        nombreGrupo = "Sin grupo";
    }
    return new SimpleStringProperty(nombreGrupo);
});
        
        colPrecio.setCellValueFactory(data -> {
    BigDecimal pr = data.getValue().getPrecio();
    String txt = "₡" + String.format("%.2f", pr == null ? BigDecimal.ZERO : pr);
    return new SimpleStringProperty(txt);
});
        
        colMenuRapido.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isMenuRapido() ? "✓ Sí" : "✗ No")
        );
        
        colVentas.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getTotalVentas() != null ? 
                data.getValue().getTotalVentas().toString() : "0")
        );
        
        colEstado.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isActivo() ? "Activo" : "Inactivo")
        );
        
        // Listener para selección
        tblProductos.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    productoSeleccionado = newSelection;
                    habilitarBotonesAccion(true);
                }
            }
        );
    }

    /**
     * Carga los grupos de productos desde el backend
     */
    private void cargarGrupos() {
        try {
            String jsonResponse = RestClient.get("/grupos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                List<GrupoProducto> grupos = gson.fromJson(dataJson, 
                    new TypeToken<List<GrupoProducto>>(){}.getType());
                
                
                
                
                listaGrupos = FXCollections.observableArrayList(grupos);
                
                // indexar por id (para resolver grupoId -> GrupoProducto)
gruposById.clear();
for (GrupoProducto g : listaGrupos) {
    if (g.getId() != null) gruposById.put(g.getId(), g);
}
                
                // Configurar ComboBox del formulario
                cmbGrupo.setItems(listaGrupos);
                cmbGrupo.setCellFactory(param -> new ListCell<GrupoProducto>() {
                    @Override
                    protected void updateItem(GrupoProducto item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNombre());
                    }
                });
                cmbGrupo.setButtonCell(new ListCell<GrupoProducto>() {
                    @Override
                    protected void updateItem(GrupoProducto item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNombre());
                    }
                });
                
                // Configurar ComboBox de filtro
                ObservableList<GrupoProducto> gruposConTodos = FXCollections.observableArrayList();
                GrupoProducto todos = new GrupoProducto();
                todos.setId(-1L);
                todos.setNombre("-- Todos los grupos --");
                gruposConTodos.add(todos);
                gruposConTodos.addAll(grupos);
                
                cmbFiltroGrupo.setItems(gruposConTodos);
                cmbFiltroGrupo.setCellFactory(param -> new ListCell<GrupoProducto>() {
                    @Override
                    protected void updateItem(GrupoProducto item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNombre());
                    }
                });
                cmbFiltroGrupo.setButtonCell(new ListCell<GrupoProducto>() {
                    @Override
                    protected void updateItem(GrupoProducto item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNombre());
                    }
                });
                cmbFiltroGrupo.getSelectionModel().selectFirst();
                
                System.out.println("✅ Grupos cargados: " + grupos.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar grupos:\n" + e.getMessage());
        }
    }

    /**
     * Configura el filtro de búsqueda
     */
    private void configurarBusqueda() {
        // Filtro por texto
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            aplicarFiltros();
        });
        
        // Filtro por grupo
        cmbFiltroGrupo.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                aplicarFiltros();
            }
        );
    }

    /**
     * Aplica los filtros combinados (texto + grupo)
     */
    private void aplicarFiltros() {
        if (listaFiltrada == null) return;
        
        listaFiltrada.setPredicate(producto -> {
            // Filtro por texto de búsqueda
            String busqueda = txtBuscar.getText();
            boolean cumpleBusqueda = true;
            
            if (busqueda != null && !busqueda.isEmpty()) {
                String busquedaLower = busqueda.toLowerCase();
                cumpleBusqueda = producto.getNombre().toLowerCase().contains(busquedaLower) ||
                                producto.getNombreCorto().toLowerCase().contains(busquedaLower);
            }
            
            // Filtro por grupo
GrupoProducto grupoFiltro = cmbFiltroGrupo.getSelectionModel().getSelectedItem();
boolean cumpleGrupo = true;
if (grupoFiltro != null && grupoFiltro.getId() != -1L) {
    Long idFiltro = grupoFiltro.getId();
    Long idDelProducto = (producto.getGrupo() != null) 
            ? producto.getGrupo().getId() 
            : producto.getGrupoId();
    cumpleGrupo = idDelProducto != null && idDelProducto.equals(idFiltro);
}
            
            return cumpleBusqueda && cumpleGrupo;
        });
    }

    // ==================== CARGA DE DATOS ====================
    
    /**
     * Carga todos los productos desde el backend
     */
    private void cargarProductos() {
        try {
            String jsonResponse = RestClient.get("/productos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                List<Producto> productos = gson.fromJson(dataJson, 
                    new TypeToken<List<Producto>>(){}.getType());
                
                // Resolver grupo a partir de grupoId para que la tabla/filtros funcionen
for (Producto p : productos) {
if (p.getGrupo() == null && p.getGrupoId() != null) {
GrupoProducto g = gruposById.get(p.getGrupoId());
if (g != null) p.setGrupo(g);
}
}
                
                listaProductos = FXCollections.observableArrayList(productos);
                listaFiltrada = new FilteredList<>(listaProductos, p -> true);
                tblProductos.setItems(listaFiltrada);
                
                System.out.println("✅ Productos cargados: " + productos.size());
            } else {
                Mensaje.showWarning("Aviso", "No se pudieron cargar los productos.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar productos:\n" + e.getMessage());
        }
    }

    // ==================== EVENTOS DE BOTONES ====================
    
   @FXML
private void onVolver(ActionEvent event) {
    FlowController.getInstance().goHomeWithFade();
}

    @FXML
    private void onNuevo(ActionEvent event) {
        modoEdicion = false;
        productoSeleccionado = null;
        limpiarFormulario();
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Producto" : "New Product");
        txtNombre.requestFocus();
    }

    @FXML
    private void onEditar(ActionEvent event) {
        if (productoSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un producto de la tabla para editar.");
            return;
        }
        
        modoEdicion = true;
        cargarProductoEnFormulario(productoSeleccionado);
        lblFormTitle.setText(I18n.isSpanish() ? "Editar Producto" : "Edit Product");
        txtNombre.requestFocus();
    }

    @FXML
    private void onEliminar(ActionEvent event) {
        if (productoSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un producto de la tabla para eliminar.");
            return;
        }
        
        boolean confirmar = Mensaje.showConfirmation(
            "Confirmar Eliminación",
            "¿Está seguro de eliminar el producto '" + productoSeleccionado.getNombre() + "'?"
        );
        
        if (!confirmar) return;
        
        try {
            String jsonResponse = RestClient.delete("/productos/" + productoSeleccionado.getId());
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", "Producto eliminado correctamente.");
                cargarProductos();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al eliminar producto:\n" + e.getMessage());
        }
    }

    @FXML
    private void onRefrescar(ActionEvent event) {
        cargarProductos();
        limpiarFormulario();
        Mensaje.showSuccess("Actualizado", "Lista de productos refrescada.");
    }

    @FXML
    private void onGuardar(ActionEvent event) {
        if (!validarFormulario()) return;
        
        try {
            Producto producto = new Producto();
            
            if (modoEdicion && productoSeleccionado != null) {
                producto.setId(productoSeleccionado.getId());
                producto.setVersion(productoSeleccionado.getVersion());
            }
            
            producto.setNombre(txtNombre.getText().trim());
            producto.setNombreCorto(txtNombreCorto.getText().trim());
            producto.setPrecio(new BigDecimal(txtPrecio.getText().trim()));
            producto.setMenuRapido(chkMenuRapido.isSelected() ? "S" : "N");
            producto.setEstado(chkActivo.isSelected() ? "A" : "I");
            
            // Asignar grupo
            GrupoProducto grupoSeleccionado = cmbGrupo.getSelectionModel().getSelectedItem();
if (grupoSeleccionado != null) {
    producto.setGrupoId(grupoSeleccionado.getId());
    // Evitar enviar el objeto grupo porque el backend lo ignora por @JsonbTransient
    producto.setGrupo(null);
}
            
            // Llamar al backend
            String jsonResponse;
            if (modoEdicion) {
                jsonResponse = RestClient.put("/productos/" + producto.getId(), producto);
            } else {
                jsonResponse = RestClient.post("/productos", producto);
            }
            
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                String mensaje = modoEdicion ? 
                    "Producto actualizado correctamente." : 
                    "Producto creado correctamente.";
                Mensaje.showSuccess("Éxito", mensaje);
                cargarProductos();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar producto:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        limpiarFormulario();
        tblProductos.getSelectionModel().clearSelection();
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Valida el formulario
     */
    private boolean validarFormulario() {
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            Mensaje.showWarning("Campo Requerido", "El nombre del producto es obligatorio.");
            txtNombre.requestFocus();
            return false;
        }
        
        if (txtNombreCorto.getText() == null || txtNombreCorto.getText().trim().isEmpty()) {
            Mensaje.showWarning("Campo Requerido", "El nombre corto es obligatorio.");
            txtNombreCorto.requestFocus();
            return false;
        }
        
        if (cmbGrupo.getSelectionModel().getSelectedItem() == null) {
            Mensaje.showWarning("Campo Requerido", "Debe seleccionar un grupo.");
            cmbGrupo.requestFocus();
            return false;
        }
        
        if (txtPrecio.getText() == null || txtPrecio.getText().trim().isEmpty()) {
            Mensaje.showWarning("Campo Requerido", "El precio es obligatorio.");
            txtPrecio.requestFocus();
            return false;
        }
        
        try {
            BigDecimal precio = new BigDecimal(txtPrecio.getText().trim());
            if (precio.compareTo(BigDecimal.ZERO) < 0) {
                Mensaje.showWarning("Validación", "El precio no puede ser negativo.");
                txtPrecio.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            Mensaje.showWarning("Validación", "El precio debe ser un número válido.");
            txtPrecio.requestFocus();
            return false;
        }
        
        return true;
    }

    /**
     * Limpia el formulario
     */
    private void limpiarFormulario() {
        txtNombre.clear();
        txtNombreCorto.clear();
        txtPrecio.clear();
        cmbGrupo.getSelectionModel().clearSelection();
        chkMenuRapido.setSelected(false);
        chkActivo.setSelected(true);
        modoEdicion = false;
        productoSeleccionado = null;
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Producto" : "New Product");
        habilitarBotonesAccion(false);
    }

    /**
     * Carga un producto en el formulario
     */
    private void cargarProductoEnFormulario(Producto producto) {
        txtNombre.setText(producto.getNombre());
        txtNombreCorto.setText(producto.getNombreCorto());
        txtPrecio.setText(producto.getPrecio().toString());
        chkMenuRapido.setSelected(producto.isMenuRapido());
        chkActivo.setSelected(producto.isActivo());
        
        // Seleccionar el grupo en el ComboBox
Long idGrupo = null;
if (producto.getGrupo() != null) {
    idGrupo = producto.getGrupo().getId();
} else if (producto.getGrupoId() != null) {
    idGrupo = producto.getGrupoId();
}
if (idGrupo != null) {
    for (GrupoProducto g : cmbGrupo.getItems()) {
        if (idGrupo.equals(g.getId())) {
            cmbGrupo.getSelectionModel().select(g);
            break;
        }
    }
}
    }

    /**
     * Habilita/deshabilita botones de acción
     */
    private void habilitarBotonesAccion(boolean habilitar) {
        btnEditar.setDisable(!habilitar);
        btnEliminar.setDisable(!habilitar);
    }

    /**
     * Actualiza textos según idioma
     */
    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();
        
        lblTitle.setText(esEspanol ? "Gestión de Productos del Menú" : "Menu Products Management");
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        
        txtBuscar.setPromptText(esEspanol ? "Buscar producto..." : "Search product...");
        cmbFiltroGrupo.setPromptText(esEspanol ? "Filtrar por grupo" : "Filter by group");
        btnNuevo.setText(esEspanol ? "+ Nuevo Producto" : "+ New Product");
        
        colNombre.setText(esEspanol ? "Nombre" : "Name");
        colNombreCorto.setText(esEspanol ? "Nombre Corto" : "Short Name");
        colGrupo.setText(esEspanol ? "Grupo" : "Group");
        colPrecio.setText(esEspanol ? "Precio" : "Price");
        colMenuRapido.setText(esEspanol ? "Menú Rápido" : "Quick Menu");
        colVentas.setText(esEspanol ? "Ventas" : "Sales");
        colEstado.setText(esEspanol ? "Estado" : "Status");
        
        btnEditar.setText(esEspanol ? "✏️ Editar" : "✏️ Edit");
        btnEliminar.setText(esEspanol ? "🗑️ Eliminar" : "🗑️ Delete");
        btnRefrescar.setText(esEspanol ? "🔄 Refrescar" : "🔄 Refresh");
        
        lblNombre.setText(esEspanol ? "Nombre del Producto:" : "Product Name:");
        lblNombreCorto.setText(esEspanol ? "Nombre Corto:" : "Short Name:");
        lblGrupo.setText(esEspanol ? "Grupo:" : "Group:");
        lblPrecio.setText(esEspanol ? "Precio (₡):" : "Price (₡):");
        lblOpciones.setText(esEspanol ? "Opciones:" : "Options:");
        chkMenuRapido.setText(esEspanol ? "Incluir en Menú Rápido" : "Include in Quick Menu");
        chkActivo.setText(esEspanol ? "Activo" : "Active");
        
        btnGuardar.setText(esEspanol ? "💾 Guardar" : "💾 Save");
        btnCancelar.setText(esEspanol ? "✖️ Cancelar" : "✖️ Cancel");
    }
}