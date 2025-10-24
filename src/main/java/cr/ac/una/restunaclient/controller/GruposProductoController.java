package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.GrupoProducto;
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

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador para CRUD de Grupos de Productos
 * Permite crear, editar, eliminar y listar grupos
 */
public class GruposProductoController implements Initializable {

    // ==================== COMPONENTES DEL HEADER ====================
    @FXML private Label lblTitle;
    @FXML private Button btnVolver;
    
    // ==================== COMPONENTES DE LA TABLA ====================
    @FXML private TextField txtBuscar;
    @FXML private Button btnNuevo;
    @FXML private TableView<GrupoProducto> tblGrupos;
    @FXML private TableColumn<GrupoProducto, String> colNombre;
    @FXML private TableColumn<GrupoProducto, String> colMenuRapido;
    @FXML private TableColumn<GrupoProducto, String> colVentas;
    @FXML private TableColumn<GrupoProducto, String> colEstado;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnRefrescar;
    
    // ==================== COMPONENTES DEL FORMULARIO ====================
    @FXML private Label lblFormTitle;
    @FXML private Label lblNombre;
    @FXML private TextField txtNombre;
    @FXML private Label lblMenuRapido;
    @FXML private CheckBox chkMenuRapido;
    @FXML private Label lblEstado;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    
    // ==================== VARIABLES DE CLASE ====================
    private ObservableList<GrupoProducto> listaGrupos;
    private FilteredList<GrupoProducto> listaFiltrada;
    private GrupoProducto grupoSeleccionado;
    private boolean modoEdicion = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Verificar permisos (solo Admin puede gestionar grupos)
        if (!AppContext.getInstance().isAdministrador()) {
            Mensaje.showError("Acceso Denegado", "Solo los administradores pueden gestionar grupos.");
            onVolver(null);
            return;
        }
        
        configurarTabla();
        configurarBusqueda();
        cargarGrupos();
        limpiarFormulario();
        actualizarTextos();
    }

    // ==================== CONFIGURACIÓN INICIAL ====================
    
    /**
     * Configura las columnas de la tabla
     */
    private void configurarTabla() {
        // Columna Nombre
        colNombre.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNombre())
        );
        
        // Columna Menú Rápido
        colMenuRapido.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isMenuRapido() ? "✓ Sí" : "✗ No")
        );
        
        // Columna Ventas
        colVentas.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getTotalVentas() != null ? 
                data.getValue().getTotalVentas().toString() : "0")
        );
        
        // Columna Estado
        colEstado.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isActivo() ? "Activo" : "Inactivo")
        );
        
        // Listener para selección en la tabla
        tblGrupos.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    grupoSeleccionado = newSelection;
                    habilitarBotonesAccion(true);
                }
            }
        );
    }

    /**
     * Configura el filtro de búsqueda en tiempo real
     */
    private void configurarBusqueda() {
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            if (listaFiltrada != null) {
                listaFiltrada.setPredicate(grupo -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    
                    String busqueda = newValue.toLowerCase();
                    return grupo.getNombre().toLowerCase().contains(busqueda);
                });
            }
        });
    }

    // ==================== CARGA DE DATOS ====================
    
    /**
     * Carga todos los grupos desde el backend
     */
    private void cargarGrupos() {
        try {
            String jsonResponse = RestClient.get("/grupos");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                // Parsear lista de grupos
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                List<GrupoProducto> grupos = gson.fromJson(dataJson, 
                    new TypeToken<List<GrupoProducto>>(){}.getType());
                
                listaGrupos = FXCollections.observableArrayList(grupos);
                listaFiltrada = new FilteredList<>(listaGrupos, p -> true);
                tblGrupos.setItems(listaFiltrada);
                
                System.out.println("✅ Grupos cargados: " + grupos.size());
            } else {
                Mensaje.showWarning("Aviso", "No se pudieron cargar los grupos.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar grupos:\n" + e.getMessage());
        }
    }

    // ==================== EVENTOS DE BOTONES ====================
    
    /**
     * Volver al menú principal
     */
    @FXML
private void onVolver(ActionEvent event) {
    FlowController.getInstance().goToViewKeepSizeScaled(
        "MenuPrincipal",
        "RestUNA - Menú Principal",
        1200, 800
    );
}

    /**
     * Preparar formulario para nuevo grupo
     */
    @FXML
    private void onNuevo(ActionEvent event) {
        modoEdicion = false;
        grupoSeleccionado = null;
        limpiarFormulario();
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Grupo" : "New Group");
        txtNombre.requestFocus();
    }

    /**
     * Editar grupo seleccionado
     */
    @FXML
    private void onEditar(ActionEvent event) {
        if (grupoSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un grupo de la tabla para editar.");
            return;
        }
        
        modoEdicion = true;
        cargarGrupoEnFormulario(grupoSeleccionado);
        lblFormTitle.setText(I18n.isSpanish() ? "Editar Grupo" : "Edit Group");
        txtNombre.requestFocus();
    }

    /**
     * Eliminar grupo seleccionado
     */
    @FXML
    private void onEliminar(ActionEvent event) {
        if (grupoSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un grupo de la tabla para eliminar.");
            return;
        }
        
        boolean confirmar = Mensaje.showConfirmation(
            "Confirmar Eliminación",
            "¿Está seguro de eliminar el grupo '" + grupoSeleccionado.getNombre() + "'?\n\n" +
            "Los productos de este grupo NO se eliminarán, pero quedarán sin categoría."
        );
        
        if (!confirmar) {
            return;
        }
        
        try {
            String jsonResponse = RestClient.delete("/grupos/" + grupoSeleccionado.getId());
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", "Grupo eliminado correctamente.");
                cargarGrupos();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al eliminar grupo:\n" + e.getMessage());
        }
    }

    /**
     * Refrescar lista de grupos
     */
    @FXML
    private void onRefrescar(ActionEvent event) {
        cargarGrupos();
        limpiarFormulario();
        Mensaje.showSuccess("Actualizado", "Lista de grupos refrescada.");
    }

    /**
     * Guardar grupo (crear o actualizar)
     */
    @FXML
    private void onGuardar(ActionEvent event) {
        if (!validarFormulario()) {
            return;
        }
        
        try {
            // Crear objeto GrupoProducto desde el formulario
            GrupoProducto grupo = new GrupoProducto();
            
            if (modoEdicion && grupoSeleccionado != null) {
                grupo.setId(grupoSeleccionado.getId());
                grupo.setVersion(grupoSeleccionado.getVersion());
            }
            
            grupo.setNombre(txtNombre.getText().trim());
            grupo.setMenuRapido(chkMenuRapido.isSelected() ? "S" : "N");
            grupo.setEstado(chkActivo.isSelected() ? "A" : "I");
            
            // Llamar al backend
            String jsonResponse;
            if (modoEdicion) {
                jsonResponse = RestClient.put("/grupos/" + grupo.getId(), grupo);
            } else {
                jsonResponse = RestClient.post("/grupos", grupo);
            }
            
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                String mensaje = modoEdicion ? "Grupo actualizado correctamente." : "Grupo creado correctamente.";
                Mensaje.showSuccess("Éxito", mensaje);
                cargarGrupos();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar grupo:\n" + e.getMessage());
        }
    }

    /**
     * Cancelar edición/creación
     */
    @FXML
    private void onCancelar(ActionEvent event) {
        limpiarFormulario();
        tblGrupos.getSelectionModel().clearSelection();
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Valida los campos del formulario
     */
    private boolean validarFormulario() {
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            Mensaje.showWarning("Campo Requerido", "El nombre del grupo es obligatorio.");
            txtNombre.requestFocus();
            return false;
        }
        
        if (txtNombre.getText().trim().length() < 3) {
            Mensaje.showWarning("Validación", "El nombre debe tener al menos 3 caracteres.");
            txtNombre.requestFocus();
            return false;
        }
        
        return true;
    }

    /**
     * Limpia el formulario
     */
    private void limpiarFormulario() {
        txtNombre.clear();
        chkMenuRapido.setSelected(false);
        chkActivo.setSelected(true);
        modoEdicion = false;
        grupoSeleccionado = null;
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Grupo" : "New Group");
        habilitarBotonesAccion(false);
    }

    /**
     * Carga un grupo en el formulario para edición
     */
    private void cargarGrupoEnFormulario(GrupoProducto grupo) {
        txtNombre.setText(grupo.getNombre());
        chkMenuRapido.setSelected(grupo.isMenuRapido());
        chkActivo.setSelected(grupo.isActivo());
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
        
        lblTitle.setText(esEspanol ? "Gestión de Grupos de Productos" : "Product Groups Management");
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        
        txtBuscar.setPromptText(esEspanol ? "Buscar grupo..." : "Search group...");
        btnNuevo.setText(esEspanol ? "+ Nuevo Grupo" : "+ New Group");
        
        colNombre.setText(esEspanol ? "Nombre" : "Name");
        colMenuRapido.setText(esEspanol ? "Menú Rápido" : "Quick Menu");
        colVentas.setText(esEspanol ? "Ventas" : "Sales");
        colEstado.setText(esEspanol ? "Estado" : "Status");
        
        btnEditar.setText(esEspanol ? "✏️ Editar" : "✏️ Edit");
        btnEliminar.setText(esEspanol ? "🗑️ Eliminar" : "🗑️ Delete");
        btnRefrescar.setText(esEspanol ? "🔄 Refrescar" : "🔄 Refresh");
        
        lblNombre.setText(esEspanol ? "Nombre del Grupo:" : "Group Name:");
        lblMenuRapido.setText(esEspanol ? "Opciones:" : "Options:");
        chkMenuRapido.setText(esEspanol ? "Incluir en Menú Rápido" : "Include in Quick Menu");
        lblEstado.setText(esEspanol ? "Estado:" : "Status:");
        chkActivo.setText(esEspanol ? "Activo" : "Active");
        
        btnGuardar.setText(esEspanol ? "💾 Guardar" : "💾 Save");
        btnCancelar.setText(esEspanol ? "✖️ Cancelar" : "✖️ Cancel");
    }
}