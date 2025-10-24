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

public class ProductosController implements Initializable {

    // ======= CONSTANTE de base de diseño (COHERENTE con MenuPrincipal y el FlowController) =======
    private static final double BASE_W = 1200;
    private static final double BASE_H = 800;

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
    private Producto productoSeleccionado;
    private boolean modoEdicion = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Verificar permisos
        if (!AppContext.getInstance().isAdministrador()) {
            Mensaje.showError("Acceso Denegado", "Solo los administradores pueden gestionar productos.");
            onVolver(null); // vuelve manteniendo escala
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

    private void configurarTabla() {
        colNombre.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getNombre())
        );

        colNombreCorto.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getNombreCorto())
        );

        colGrupo.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getGrupo() != null ?
                data.getValue().getGrupo().getNombre() : "Sin grupo")
        );

        colPrecio.setCellValueFactory(data ->
            new SimpleStringProperty("₡" + String.format("%.2f", data.getValue().getPrecio()))
        );

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

        tblProductos.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                productoSeleccionado = newSelection;
                habilitarBotonesAccion(newSelection != null);
            }
        );
    }

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

            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar grupos:\n" + e.getMessage());
        }
    }

    private void configurarBusqueda() {
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> aplicarFiltros());
        cmbFiltroGrupo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> aplicarFiltros());
    }

    private void aplicarFiltros() {
        if (listaFiltrada == null) return;

        listaFiltrada.setPredicate(producto -> {
            String busqueda = txtBuscar.getText();
            boolean cumpleBusqueda = true;

            if (busqueda != null && !busqueda.isEmpty()) {
                String q = busqueda.toLowerCase();
                cumpleBusqueda =
                        (producto.getNombre() != null && producto.getNombre().toLowerCase().contains(q)) ||
                        (producto.getNombreCorto() != null && producto.getNombreCorto().toLowerCase().contains(q));
            }

            GrupoProducto grupoFiltro = cmbFiltroGrupo.getSelectionModel().getSelectedItem();
            boolean cumpleGrupo = true;

            if (grupoFiltro != null && grupoFiltro.getId() != -1L) {
                cumpleGrupo = producto.getGrupo() != null &&
                              producto.getGrupo().getId() != null &&
                              producto.getGrupo().getId().equals(grupoFiltro.getId());
            }

            return cumpleBusqueda && cumpleGrupo;
        });
    }

    // ==================== CARGA DE DATOS ====================

    private void cargarProductos() {
        try {
            String jsonResponse = RestClient.get("/productos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                List<Producto> productos = gson.fromJson(dataJson,
                    new TypeToken<List<Producto>>(){}.getType());

                listaProductos = FXCollections.observableArrayList(productos);
                listaFiltrada = new FilteredList<>(listaProductos, p -> true);
                tblProductos.setItems(listaFiltrada);
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
private void onVolver(ActionEvent e) {
    FlowController.getInstance().goToViewKeepSizeScaled(
        "MenuPrincipal", "RestUNA - Menú Principal", 1200, 800
    );
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

            GrupoProducto grupoSeleccionado = cmbGrupo.getSelectionModel().getSelectedItem();
            if (grupoSeleccionado != null) {
                producto.setGrupo(grupoSeleccionado);
            }

            String jsonResponse = modoEdicion
                    ? RestClient.put("/productos/" + producto.getId(), producto)
                    : RestClient.post("/productos", producto);

            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", modoEdicion ? "Producto actualizado correctamente." : "Producto creado correctamente.");
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

    private void cargarProductoEnFormulario(Producto p) {
        txtNombre.setText(p.getNombre());
        txtNombreCorto.setText(p.getNombreCorto());
        txtPrecio.setText(p.getPrecio().toString());
        chkMenuRapido.setSelected(p.isMenuRapido());
        chkActivo.setSelected(p.isActivo());

        if (p.getGrupo() != null) {
            for (GrupoProducto g : cmbGrupo.getItems()) {
                if (g.getId().equals(p.getGrupo().getId())) {
                    cmbGrupo.getSelectionModel().select(g);
                    break;
                }
            }
        }
    }

    private void habilitarBotonesAccion(boolean habilitar) {
        btnEditar.setDisable(!habilitar);
        btnEliminar.setDisable(!habilitar);
    }

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