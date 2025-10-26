package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.Cliente;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.LocalDateAdapter;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador para CRUD de Clientes
 * Accesible para Administradores y Cajeros
 */
public class ClientesController implements Initializable {

    // ==================== COMPONENTES DEL HEADER ====================
    @FXML private Label lblTitle;
    @FXML private Button btnVolver;
    
    // ==================== COMPONENTES DE LA TABLA ====================
    @FXML private TextField txtBuscar;
    @FXML private Button btnNuevo;
    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, String> colFecha;
    @FXML private TableColumn<Cliente, String> colEstado;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnRefrescar;
    
    // ==================== COMPONENTES DEL FORMULARIO ====================
    @FXML private Label lblFormTitle;
    @FXML private Label lblNombre;
    @FXML private TextField txtNombre;
    @FXML private Label lblCorreo;
    @FXML private TextField txtCorreo;
    @FXML private Label lblTelefono;
    @FXML private TextField txtTelefono;
    @FXML private Label lblEstado;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    
    // ==================== VARIABLES DE CLASE ====================
    private ObservableList<Cliente> listaClientes;
    private FilteredList<Cliente> listaFiltrada;
    private Cliente clienteSeleccionado;
    private boolean modoEdicion = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Verificar permisos (Admin o Cajero)
        if (!AppContext.getInstance().isAdministrador() && !AppContext.getInstance().isCajero()) {
            Mensaje.showError("Acceso Denegado", "No tiene permisos para gestionar clientes.");
            onVolver(null);
            return;
        }
        
        configurarTabla();
        configurarBusqueda();
        configurarValidaciones();
        cargarClientes();
        limpiarFormulario();
        actualizarTextos();
    }

    // ==================== CONFIGURACIÓN INICIAL ====================
    
    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNombre())
        );
        
        colCorreo.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getCorreo())
        );
        
        colTelefono.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getTelefono())
        );
        
        colFecha.setCellValueFactory(data -> 
            new SimpleStringProperty(
                data.getValue().getFechaCreacion() != null ?
                data.getValue().getFechaCreacion().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) :
                ""
            )
        );
        
        colEstado.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isActivo() ? "Activo" : "Inactivo")
        );
        
        // Listener para selección
        tblClientes.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    clienteSeleccionado = newSelection;
                    habilitarBotonesAccion(true);
                }
            }
        );
    }

    private void configurarBusqueda() {
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            if (listaFiltrada != null) {
                listaFiltrada.setPredicate(cliente -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    
                    String busqueda = newValue.toLowerCase();
                    return cliente.getNombre().toLowerCase().contains(busqueda) ||
                           (cliente.getCorreo() != null && cliente.getCorreo().toLowerCase().contains(busqueda)) ||
                           (cliente.getTelefono() != null && cliente.getTelefono().contains(busqueda));
                });
            }
        });
    }

    private void configurarValidaciones() {
        // Validación básica de email en tiempo real
        txtCorreo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && txtCorreo.getText() != null && !txtCorreo.getText().isEmpty()) {
                if (!txtCorreo.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                    txtCorreo.setStyle("-fx-border-color: red;");
                } else {
                    txtCorreo.setStyle("");
                }
            }
        });
    }

    // ==================== CARGA DE DATOS ====================
       
    private void cargarClientes() {
    try {
        String jsonResponse = RestClient.get("/clientes");
        Map<String, Object> response = RestClient.parseResponse(jsonResponse);
        
        if (Boolean.TRUE.equals(response.get("success"))) {
            // ✅ Configurar Gson con el adaptador para LocalDate
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                    .create();
            
            String dataJson = gson.toJson(response.get("data"));
            List<Cliente> clientes = gson.fromJson(dataJson, 
                new TypeToken<List<Cliente>>(){}.getType());
            
            listaClientes = FXCollections.observableArrayList(clientes);
            listaFiltrada = new FilteredList<>(listaClientes, p -> true);
            tblClientes.setItems(listaFiltrada);
            
            System.out.println("✅ Clientes cargados: " + clientes.size());
        } else {
            Mensaje.showWarning("Aviso", "No se pudieron cargar los clientes.");
        }
    } catch (Exception e) {
        e.printStackTrace();
        Mensaje.showError("Error", "Error al cargar clientes:\n" + e.getMessage());
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
        clienteSeleccionado = null;
        limpiarFormulario();
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Cliente" : "New Customer");
        txtNombre.requestFocus();
    }

    @FXML
    private void onEditar(ActionEvent event) {
        if (clienteSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un cliente de la tabla para editar.");
            return;
        }
        
        modoEdicion = true;
        cargarClienteEnFormulario(clienteSeleccionado);
        lblFormTitle.setText(I18n.isSpanish() ? "Editar Cliente" : "Edit Customer");
        txtNombre.requestFocus();
    }

    @FXML
    private void onEliminar(ActionEvent event) {
        if (clienteSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un cliente de la tabla para eliminar.");
            return;
        }
        
        boolean confirmar = Mensaje.showConfirmation(
            "Confirmar Eliminación",
            "¿Está seguro de eliminar al cliente '" + clienteSeleccionado.getNombre() + "'?"
        );
        
        if (!confirmar) return;
        
        try {
            String jsonResponse = RestClient.delete("/clientes/" + clienteSeleccionado.getId());
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", "Cliente eliminado correctamente.");
                cargarClientes();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al eliminar cliente:\n" + e.getMessage());
        }
    }

    @FXML
    private void onRefrescar(ActionEvent event) {
        cargarClientes();
        limpiarFormulario();
        Mensaje.showSuccess("Actualizado", "Lista de clientes refrescada.");
    }

    @FXML
    private void onGuardar(ActionEvent event) {
        if (!validarFormulario()) return;
        
        try {
            Cliente cliente = new Cliente();
            
            if (modoEdicion && clienteSeleccionado != null) {
                cliente.setId(clienteSeleccionado.getId());
                cliente.setVersion(clienteSeleccionado.getVersion());
            }
            
            cliente.setNombre(txtNombre.getText().trim());
            cliente.setCorreo(txtCorreo.getText() != null ? txtCorreo.getText().trim() : null);
            cliente.setTelefono(txtTelefono.getText() != null ? txtTelefono.getText().trim() : null);
            cliente.setEstado(chkActivo.isSelected() ? "A" : "I");
            
            // Llamar al backend
            String jsonResponse;
            if (modoEdicion) {
                jsonResponse = RestClient.put("/clientes/" + cliente.getId(), cliente);
            } else {
                jsonResponse = RestClient.post("/clientes", cliente);
            }
            
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                String mensaje = modoEdicion ? 
                    "Cliente actualizado correctamente." : 
                    "Cliente creado correctamente.";
                Mensaje.showSuccess("Éxito", mensaje);
                cargarClientes();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar cliente:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        limpiarFormulario();
        tblClientes.getSelectionModel().clearSelection();
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    private boolean validarFormulario() {
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            Mensaje.showWarning("Campo Requerido", "El nombre del cliente es obligatorio.");
            txtNombre.requestFocus();
            return false;
        }
        
        // Validar email si se ingresó
        if (txtCorreo.getText() != null && !txtCorreo.getText().trim().isEmpty()) {
            if (!txtCorreo.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                Mensaje.showWarning("Email Inválido", "El formato del correo electrónico no es válido.");
                txtCorreo.requestFocus();
                return false;
            }
        }
        
        return true;
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtCorreo.setStyle("");
        chkActivo.setSelected(true);
        modoEdicion = false;
        clienteSeleccionado = null;
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Cliente" : "New Customer");
        habilitarBotonesAccion(false);
    }

    private void cargarClienteEnFormulario(Cliente cliente) {
        txtNombre.setText(cliente.getNombre());
        txtCorreo.setText(cliente.getCorreo());
        txtTelefono.setText(cliente.getTelefono());
        chkActivo.setSelected(cliente.isActivo());
    }

    private void habilitarBotonesAccion(boolean habilitar) {
        btnEditar.setDisable(!habilitar);
        btnEliminar.setDisable(!habilitar);
    }

    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();
        
        lblTitle.setText(esEspanol ? "Gestión de Clientes" : "Customers Management");
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        
        txtBuscar.setPromptText(esEspanol ? "Buscar cliente..." : "Search customer...");
        btnNuevo.setText(esEspanol ? "+ Nuevo Cliente" : "+ New Customer");
        
        colNombre.setText(esEspanol ? "Nombre" : "Name");
        colCorreo.setText(esEspanol ? "Correo" : "Email");
        colTelefono.setText(esEspanol ? "Teléfono" : "Phone");
        colFecha.setText(esEspanol ? "Fecha Creación" : "Creation Date");
        colEstado.setText(esEspanol ? "Estado" : "Status");
        
        btnEditar.setText(esEspanol ? "✏️ Editar" : "✏️ Edit");
        btnEliminar.setText(esEspanol ? "🗑️ Eliminar" : "🗑️ Delete");
        btnRefrescar.setText(esEspanol ? "🔄 Refrescar" : "🔄 Refresh");
        
        lblNombre.setText(esEspanol ? "Nombre Completo:" : "Full Name:");
        lblCorreo.setText(esEspanol ? "Correo Electrónico:" : "Email:");
        lblTelefono.setText(esEspanol ? "Teléfono:" : "Phone:");
        lblEstado.setText(esEspanol ? "Estado:" : "Status:");
        chkActivo.setText(esEspanol ? "Activo" : "Active");
        
        btnGuardar.setText(esEspanol ? "💾 Guardar" : "💾 Save");
        btnCancelar.setText(esEspanol ? "✖️ Cancelar" : "✖️ Cancel");
    }
}