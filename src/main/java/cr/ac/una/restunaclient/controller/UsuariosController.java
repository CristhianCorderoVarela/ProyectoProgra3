package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.Usuario;
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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador para CRUD de Usuarios
 * Permite crear, editar, eliminar y cambiar contraseñas de usuarios
 */
public class UsuariosController implements Initializable {

    // ==================== COMPONENTES DEL HEADER ====================
    @FXML private Label lblTitle;
    @FXML private Button btnVolver;
    
    // ==================== COMPONENTES DE LA TABLA ====================
    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbFiltroRol;
    @FXML private Button btnNuevo;
    @FXML private TableView<Usuario> tblUsuarios;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colUsuario;
    @FXML private TableColumn<Usuario, String> colRol;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, String> colFecha;
    @FXML private Button btnEditar;
    @FXML private Button btnCambiarClave;
    @FXML private Button btnEliminar;
    @FXML private Button btnRefrescar;
    
    // ==================== COMPONENTES DEL FORMULARIO ====================
    @FXML private Label lblFormTitle;
    @FXML private Label lblNombre;
    @FXML private TextField txtNombre;
    @FXML private Label lblUsuario;
    @FXML private TextField txtUsuario;
    @FXML private Label lblUsuarioInfo;
    @FXML private VBox vboxClave;
    @FXML private Label lblClave;
    @FXML private PasswordField txtClave;
    @FXML private Label lblRol;
    @FXML private ComboBox<String> cmbRol;
    @FXML private Label lblRolInfo;
    @FXML private Label lblEstado;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    
    // ==================== VARIABLES DE CLASE ====================
    private ObservableList<Usuario> listaUsuarios;
    private FilteredList<Usuario> listaFiltrada;
    private Usuario usuarioSeleccionado;
    private boolean modoEdicion = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Verificar permisos
        if (!AppContext.getInstance().isAdministrador()) {
            Mensaje.showError("Acceso Denegado", "Solo los administradores pueden gestionar usuarios.");
            onVolver(null);
            return;
        }
        
        configurarTabla();
        configurarCombos();
        configurarBusqueda();
        cargarUsuarios();
        limpiarFormulario();
        actualizarTextos();
    }

    // ==================== CONFIGURACIÓN INICIAL ====================
    
    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNombre())
        );
        
        colUsuario.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getUsuario())
        );
        
        colRol.setCellValueFactory(data -> {
            String rol = data.getValue().getRol();
            String rolTraducido = traducirRol(rol);
            return new SimpleStringProperty(rolTraducido);
        });
        
        colEstado.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isActivo() ? 
                (I18n.isSpanish() ? "Activo" : "Active") : 
                (I18n.isSpanish() ? "Inactivo" : "Inactive"))
        );
        
        colFecha.setCellValueFactory(data -> {
            if (data.getValue().getFechaCreacion() != null) {
                String fecha = data.getValue().getFechaCreacion()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                return new SimpleStringProperty(fecha);
            }
            return new SimpleStringProperty("");
        });
        
        // Listener para selección
        tblUsuarios.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    usuarioSeleccionado = newSelection;
                    habilitarBotonesAccion(true);
                }
            }
        );
    }

    private void configurarCombos() {
        // ComboBox de roles
        cmbRol.getItems().addAll("ADMINISTRATIVO", "CAJERO", "SALONERO");
        
        // ComboBox de filtro de rol
        cmbFiltroRol.getItems().add(I18n.isSpanish() ? "Todos" : "All");
        cmbFiltroRol.getItems().addAll("ADMINISTRATIVO", "CAJERO", "SALONERO");
        cmbFiltroRol.getSelectionModel().selectFirst();
        
        // Listener para filtro de rol
        cmbFiltroRol.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> aplicarFiltros()
        );
    }

    private void configurarBusqueda() {
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            aplicarFiltros();
        });
    }

    // ==================== CARGA DE DATOS ====================
    
    private void cargarUsuarios() {
        try {
            String jsonResponse = RestClient.get("/usuarios");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                // ✅ Configurar Gson con el adaptador para LocalDate
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                        .create();

                String dataJson = gson.toJson(response.get("data"));
                List<Usuario> usuarios = gson.fromJson(dataJson,
                        new TypeToken<List<Usuario>>() {
                        }.getType());

                listaUsuarios = FXCollections.observableArrayList(usuarios);
                listaFiltrada = new FilteredList<>(listaUsuarios, p -> true);
                tblUsuarios.setItems(listaFiltrada);

                System.out.println("✅ Usuarios cargados: " + usuarios.size());
            } else {
                Mensaje.showWarning("Aviso", "No se pudieron cargar los usuarios.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar usuarios:\n" + e.getMessage());
        }
    }

    // ==================== FILTROS ====================
    
    private void aplicarFiltros() {
        if (listaFiltrada == null) return;
        
        listaFiltrada.setPredicate(usuario -> {
            // Filtro de búsqueda
            String busqueda = txtBuscar.getText();
            if (busqueda != null && !busqueda.isEmpty()) {
                String busquedaLower = busqueda.toLowerCase();
                boolean coincideBusqueda = usuario.getNombre().toLowerCase().contains(busquedaLower) ||
                                          usuario.getUsuario().toLowerCase().contains(busquedaLower) ||
                                          usuario.getRol().toLowerCase().contains(busquedaLower);
                if (!coincideBusqueda) return false;
            }
            
            // Filtro de rol
            String rolFiltro = cmbFiltroRol.getSelectionModel().getSelectedItem();
            if (rolFiltro != null && !rolFiltro.equals("Todos") && !rolFiltro.equals("All")) {
                if (!usuario.getRol().equals(rolFiltro)) return false;
            }
            
            return true;
        });
    }

    // ==================== EVENTOS DE BOTONES ====================
    
    @FXML
private void onVolver(ActionEvent event) {
    FlowController.getInstance().goHomeWithFade();
}

    @FXML
    private void onNuevo(ActionEvent event) {
        modoEdicion = false;
        usuarioSeleccionado = null;
        limpiarFormulario();
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Usuario" : "New User");
        vboxClave.setManaged(true);
        vboxClave.setVisible(true);
        txtNombre.requestFocus();
    }

    @FXML
    private void onEditar(ActionEvent event) {
        if (usuarioSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un usuario de la tabla para editar.");
            return;
        }
        
        // No permitir editar al usuario logueado si no es admin
        Usuario usuarioLogueado = AppContext.getInstance().getUsuarioLogueado();
        if (usuarioSeleccionado.getId().equals(usuarioLogueado.getId()) && 
            !usuarioLogueado.isAdministrativo()) {
            Mensaje.showWarning("Aviso", "No puede editar su propio usuario.");
            return;
        }
        
        modoEdicion = true;
        cargarUsuarioEnFormulario(usuarioSeleccionado);
        lblFormTitle.setText(I18n.isSpanish() ? "Editar Usuario" : "Edit User");
        vboxClave.setManaged(false);
        vboxClave.setVisible(false);
        txtNombre.requestFocus();
    }

    @FXML
    private void onCambiarClave(ActionEvent event) {
        if (usuarioSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un usuario de la tabla.");
            return;
        }
        
        // Crear diálogo para cambiar clave
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle(I18n.isSpanish() ? "Cambiar Contraseña" : "Change Password");
        dialog.setHeaderText(I18n.isSpanish() ? 
            "Usuario: " + usuarioSeleccionado.getUsuario() : 
            "User: " + usuarioSeleccionado.getUsuario());
        
        ButtonType btnAceptar = new ButtonType(
            I18n.isSpanish() ? "Cambiar" : "Change", 
            ButtonBar.ButtonData.OK_DONE
        );
        dialog.getDialogPane().getButtonTypes().addAll(btnAceptar, ButtonType.CANCEL);
        
        // Crear campos
        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(20));
        
        Label lblAntigua = new Label(I18n.isSpanish() ? "Contraseña Antigua:" : "Old Password:");
        PasswordField txtClaveAntigua = new PasswordField();
        txtClaveAntigua.setPromptText(I18n.isSpanish() ? "Contraseña actual" : "Current password");
        
        Label lblNueva = new Label(I18n.isSpanish() ? "Contraseña Nueva:" : "New Password:");
        PasswordField txtClaveNueva = new PasswordField();
        txtClaveNueva.setPromptText(I18n.isSpanish() ? "Mínimo 6 caracteres" : "Minimum 6 characters");
        
        Label lblConfirmar = new Label(I18n.isSpanish() ? "Confirmar Contraseña:" : "Confirm Password:");
        PasswordField txtClaveConfirmar = new PasswordField();
        
        vbox.getChildren().addAll(
            lblAntigua, txtClaveAntigua,
            lblNueva, txtClaveNueva,
            lblConfirmar, txtClaveConfirmar
        );
        
        dialog.getDialogPane().setContent(vbox);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnAceptar) {
                return new String[]{
                    txtClaveAntigua.getText(),
                    txtClaveNueva.getText(),
                    txtClaveConfirmar.getText()
                };
            }
            return null;
        });
        
        Optional<String[]> result = dialog.showAndWait();
        
        result.ifPresent(claves -> {
            String claveAntigua = claves[0];
            String claveNueva = claves[1];
            String claveConfirmar = claves[2];
            
            // Validaciones
            if (claveAntigua.isEmpty() || claveNueva.isEmpty() || claveConfirmar.isEmpty()) {
                Mensaje.showWarning("Campos Requeridos", "Complete todos los campos.");
                return;
            }
            
            if (claveNueva.length() < 6) {
                Mensaje.showWarning("Error", "La contraseña debe tener al menos 6 caracteres.");
                return;
            }
            
            if (!claveNueva.equals(claveConfirmar)) {
                Mensaje.showWarning("Error", "Las contraseñas no coinciden.");
                return;
            }
            
            // Llamar al backend
            try {
                Map<String, String> datos = Map.of(
                    "claveAntigua", claveAntigua,
                    "claveNueva", claveNueva
                );
                
                String jsonResponse = RestClient.post(
                    "/usuarios/" + usuarioSeleccionado.getId() + "/cambiar-clave", 
                    datos
                );
                Map<String, Object> response = RestClient.parseResponse(jsonResponse);
                
                if (Boolean.TRUE.equals(response.get("success"))) {
                    Mensaje.showSuccess("Éxito", "Contraseña cambiada correctamente.");
                } else {
                    Mensaje.showError("Error", response.get("message").toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Mensaje.showError("Error", "Error al cambiar contraseña:\n" + e.getMessage());
            }
        });
    }

    @FXML
    private void onEliminar(ActionEvent event) {
        if (usuarioSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un usuario de la tabla para eliminar.");
            return;
        }
        
        // No permitir eliminar al usuario logueado
        Usuario usuarioLogueado = AppContext.getInstance().getUsuarioLogueado();
        if (usuarioSeleccionado.getId().equals(usuarioLogueado.getId())) {
            Mensaje.showWarning("Aviso", "No puede eliminar su propio usuario.");
            return;
        }
        
        boolean confirmar = Mensaje.showConfirmation(
            "Confirmar Eliminación",
            "¿Está seguro de desactivar al usuario '" + usuarioSeleccionado.getUsuario() + "'?\n\n" +
            "El usuario no podrá iniciar sesión hasta que sea reactivado."
        );
        
        if (!confirmar) return;
        
        try {
            String jsonResponse = RestClient.delete("/usuarios/" + usuarioSeleccionado.getId());
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", "Usuario desactivado correctamente.");
                cargarUsuarios();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al eliminar usuario:\n" + e.getMessage());
        }
    }

    @FXML
    private void onRefrescar(ActionEvent event) {
        cargarUsuarios();
        limpiarFormulario();
        Mensaje.showSuccess("Actualizado", "Lista de usuarios refrescada.");
    }

    @FXML
    private void onGuardar(ActionEvent event) {
        if (!validarFormulario()) return;
        
        try {
            Usuario usuario = new Usuario();
            
            if (modoEdicion && usuarioSeleccionado != null) {
                usuario.setId(usuarioSeleccionado.getId());
                usuario.setVersion(usuarioSeleccionado.getVersion());
                usuario.setClave(usuarioSeleccionado.getClave()); // Mantener clave antigua
            } else {
                // Modo creación: incluir contraseña
                usuario.setClave(txtClave.getText().trim());
            }
            
            usuario.setNombre(txtNombre.getText().trim());
            usuario.setUsuario(txtUsuario.getText().trim());
            usuario.setRol(cmbRol.getSelectionModel().getSelectedItem());
            usuario.setEstado(chkActivo.isSelected() ? "A" : "I");
            
            // Llamar al backend
            String jsonResponse;
            if (modoEdicion) {
                jsonResponse = RestClient.put("/usuarios/" + usuario.getId(), usuario);
            } else {
                jsonResponse = RestClient.post("/usuarios", usuario);
            }
            
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                String mensaje = modoEdicion ? 
                    "Usuario actualizado correctamente." : 
                    "Usuario creado correctamente.";
                Mensaje.showSuccess("Éxito", mensaje);
                cargarUsuarios();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar usuario:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        limpiarFormulario();
        tblUsuarios.getSelectionModel().clearSelection();
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    private boolean validarFormulario() {
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            Mensaje.showWarning("Campo Requerido", "El nombre es obligatorio.");
            txtNombre.requestFocus();
            return false;
        }
        
        if (txtUsuario.getText() == null || txtUsuario.getText().trim().isEmpty()) {
            Mensaje.showWarning("Campo Requerido", "El nombre de usuario es obligatorio.");
            txtUsuario.requestFocus();
            return false;
        }
        
        if (!modoEdicion) {
            if (txtClave.getText() == null || txtClave.getText().trim().isEmpty()) {
                Mensaje.showWarning("Campo Requerido", "La contraseña es obligatoria.");
                txtClave.requestFocus();
                return false;
            }
            
            if (txtClave.getText().length() < 6) {
                Mensaje.showWarning("Error", "La contraseña debe tener al menos 6 caracteres.");
                txtClave.requestFocus();
                return false;
            }
        }
        
        if (cmbRol.getSelectionModel().getSelectedItem() == null) {
            Mensaje.showWarning("Campo Requerido", "Debe seleccionar un rol.");
            cmbRol.requestFocus();
            return false;
        }
        
        return true;
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtUsuario.clear();
        txtClave.clear();
        cmbRol.getSelectionModel().clearSelection();
        chkActivo.setSelected(true);
        modoEdicion = false;
        usuarioSeleccionado = null;
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Usuario" : "New User");
        vboxClave.setManaged(true);
        vboxClave.setVisible(true);
        habilitarBotonesAccion(false);
    }

    private void cargarUsuarioEnFormulario(Usuario usuario) {
        txtNombre.setText(usuario.getNombre());
        txtUsuario.setText(usuario.getUsuario());
        cmbRol.getSelectionModel().select(usuario.getRol());
        chkActivo.setSelected(usuario.isActivo());
    }

    private void habilitarBotonesAccion(boolean habilitar) {
        btnEditar.setDisable(!habilitar);
        btnEliminar.setDisable(!habilitar);
        btnCambiarClave.setDisable(!habilitar);
    }

    private String traducirRol(String rol) {
        if (I18n.isSpanish()) {
            switch (rol) {
                case "ADMINISTRATIVO": return "Administrativo";
                case "CAJERO": return "Cajero";
                case "SALONERO": return "Salonero";
                default: return rol;
            }
        } else {
            switch (rol) {
                case "ADMINISTRATIVO": return "Administrative";
                case "CAJERO": return "Cashier";
                case "SALONERO": return "Waiter";
                default: return rol;
            }
        }
    }

    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();
        
        lblTitle.setText(esEspanol ? "Gestión de Usuarios" : "User Management");
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        
        txtBuscar.setPromptText(esEspanol ? "Buscar usuario..." : "Search user...");
        cmbFiltroRol.setPromptText(esEspanol ? "Filtrar por rol" : "Filter by role");
        btnNuevo.setText(esEspanol ? "+ Nuevo Usuario" : "+ New User");
        
        colNombre.setText(esEspanol ? "Nombre" : "Name");
        colUsuario.setText(esEspanol ? "Usuario" : "Username");
        colRol.setText(esEspanol ? "Rol" : "Role");
        colEstado.setText(esEspanol ? "Estado" : "Status");
        colFecha.setText(esEspanol ? "Fecha Creación" : "Creation Date");
        
        btnEditar.setText(esEspanol ? "✏️ Editar" : "✏️ Edit");
        btnCambiarClave.setText(esEspanol ? "🔑 Cambiar Clave" : "🔑 Change Password");
        btnEliminar.setText(esEspanol ? "🗑️ Eliminar" : "🗑️ Delete");
        btnRefrescar.setText(esEspanol ? "🔄 Refrescar" : "🔄 Refresh");
        
        lblNombre.setText(esEspanol ? "Nombre Completo:" : "Full Name:");
        lblUsuario.setText(esEspanol ? "Nombre de Usuario:" : "Username:");
        lblUsuarioInfo.setText(esEspanol ? "Debe ser único en el sistema" : "Must be unique in the system");
        lblClave.setText(esEspanol ? "Contraseña:" : "Password:");
        lblRol.setText(esEspanol ? "Rol del Usuario:" : "User Role:");
        lblRolInfo.setText(esEspanol ? 
            "ADMINISTRATIVO: acceso total | CAJERO: facturación | SALONERO: solo órdenes" :
            "ADMINISTRATIVE: full access | CASHIER: billing | WAITER: orders only");
        lblEstado.setText(esEspanol ? "Estado:" : "Status:");
        chkActivo.setText(esEspanol ? "Activo" : "Active");
        
        btnGuardar.setText(esEspanol ? "💾 Guardar" : "💾 Save");
        btnCancelar.setText(esEspanol ? "✖️ Cancelar" : "✖️ Cancel");
    }
}