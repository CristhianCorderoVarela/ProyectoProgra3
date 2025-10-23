package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.model.Usuario;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador para la vista de registro de usuarios
 */
public class RegisterController implements Initializable {

    @FXML private Label lblAppSubtitle;
    @FXML private Button btnLanguage;
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblFullName;
    @FXML private TextField txtFullName;
    @FXML private Label lblUsername;
    @FXML private TextField txtUsername;
    @FXML private Label lblPassword;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblConfirmPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblRole;
    @FXML private ComboBox<String> cmbRole;
    @FXML private Button btnRegister;
    @FXML private Label lblHaveAccount;
    @FXML private Hyperlink linkLogin;
    @FXML private Label lblFooter;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Cargar roles en el ComboBox
        cargarRoles();
        
        // Cargar textos iniciales
        actualizarTextos();
    }

    /**
     * Carga los roles disponibles en el ComboBox
     */
    private void cargarRoles() {
        cmbRole.setItems(FXCollections.observableArrayList(
            I18n.get("usuarios.rol.salonero"),
            I18n.get("usuarios.rol.cajero"),
            I18n.get("usuarios.rol.admin")
        ));
    }

    /**
     * Maneja el evento de registro
     */
    @FXML
    private void onRegister(ActionEvent event) {
        // Validar campos
        if (!validarCampos()) {
            return;
        }

        try {
            // Deshabilitar botón mientras se procesa
            btnRegister.setDisable(true);
            btnRegister.setText(I18n.isSpanish() ? "Registrando..." : "Registering...");

            // Crear objeto Usuario
            Usuario usuario = new Usuario();
            usuario.setNombre(txtFullName.getText().trim());
            usuario.setUsuario(txtUsername.getText().trim());
            usuario.setClave(txtPassword.getText());
            usuario.setRol(obtenerRolSeleccionado());
            usuario.setEstado("A");

            // Llamar al servicio REST
            String jsonResponse = RestClient.post("/usuarios", usuario);
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            // Verificar respuesta
            if (Boolean.TRUE.equals(response.get("success"))) {
                // Registro exitoso
                Mensaje.showSuccess(
                    I18n.get("app.exito"),
                    I18n.get("mensaje.guardadoExito")
                );

                // Volver al login
                onLogin(event);

            } else {
                // Error en el registro
                String message = response.get("message") != null ? 
                    response.get("message").toString() : 
                    I18n.get("mensaje.errorGuardar");
                
                Mensaje.showError(I18n.get("app.error"), message);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError(
                I18n.get("app.error"),
                I18n.get("mensaje.errorGuardar") + "\n" + e.getMessage()
            );
        } finally {
            // Rehabilitar botón
            btnRegister.setDisable(false);
            btnRegister.setText(I18n.isSpanish() ? "Registrarse" : "Register");
        }
    }

    /**
     * Valida todos los campos del formulario
     */
    private boolean validarCampos() {
        // Validar nombre completo
        if (txtFullName.getText() == null || txtFullName.getText().trim().isEmpty()) {
            Mensaje.showError(
                I18n.get("app.error"), 
                I18n.get("mensaje.camposObligatorios")
            );
            txtFullName.requestFocus();
            return false;
        }

        // Validar usuario
        if (txtUsername.getText() == null || txtUsername.getText().trim().isEmpty()) {
            Mensaje.showError(
                I18n.get("app.error"), 
                I18n.get("mensaje.camposObligatorios")
            );
            txtUsername.requestFocus();
            return false;
        }

        // Validar contraseña
        if (txtPassword.getText() == null || txtPassword.getText().isEmpty()) {
            Mensaje.showError(
                I18n.get("app.error"), 
                I18n.get("mensaje.camposObligatorios")
            );
            txtPassword.requestFocus();
            return false;
        }

        // Validar longitud de contraseña
        if (txtPassword.getText().length() < 4) {
            Mensaje.showError(
                I18n.get("app.error"), 
                "La contraseña debe tener al menos 4 caracteres"
            );
            txtPassword.requestFocus();
            return false;
        }

        // Validar confirmación de contraseña
        if (txtConfirmPassword.getText() == null || txtConfirmPassword.getText().isEmpty()) {
            Mensaje.showError(
                I18n.get("app.error"), 
                I18n.get("mensaje.camposObligatorios")
            );
            txtConfirmPassword.requestFocus();
            return false;
        }

        // Validar que las contraseñas coincidan
        if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            Mensaje.showError(
                I18n.get("app.error"), 
                "Las contraseñas no coinciden"
            );
            txtConfirmPassword.clear();
            txtPassword.clear();
            txtPassword.requestFocus();
            return false;
        }

        // Validar rol
        if (cmbRole.getValue() == null) {
            Mensaje.showError(
                I18n.get("app.error"), 
                I18n.get("mensaje.camposObligatorios")
            );
            cmbRole.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Obtiene el rol seleccionado en formato de base de datos
     */
    private String obtenerRolSeleccionado() {
        String rolSeleccionado = cmbRole.getValue();
        
        if (rolSeleccionado.equals(I18n.get("usuarios.rol.salonero"))) {
            return "SALONERO";
        } else if (rolSeleccionado.equals(I18n.get("usuarios.rol.cajero"))) {
            return "CAJERO";
        } else if (rolSeleccionado.equals(I18n.get("usuarios.rol.admin"))) {
            return "ADMINISTRATIVO";
        }
        
        return "SALONERO"; // Por defecto
    }

    /**
     * Navega a la vista de login
     */
    @FXML
    private void onLogin(ActionEvent event) {
        FlowController.getInstance().goToView("Login", "RestUNA - Login", 1024, 768);
    }

    /**
     * Cambia el idioma de la aplicación
     */
    @FXML
    private void onLanguageChange(ActionEvent event) {
        if (I18n.isSpanish()) {
            I18n.setLocale(new Locale("en"));
        } else {
            I18n.setLocale(new Locale("es", "CR"));
        }
        actualizarTextos();
        cargarRoles(); // Recargar roles con el nuevo idioma
    }

    /**
     * Actualiza todos los textos de la interfaz según el idioma actual
     */
    private void actualizarTextos() {
        lblAppSubtitle.setText(I18n.isSpanish() ? "Crear nueva cuenta" : "Create new account");
        lblTitle.setText(I18n.isSpanish() ? "Registro de Usuario" : "User Registration");
        lblSubtitle.setText(I18n.isSpanish() ? "Crear nueva cuenta" : "Create new account");
        lblFullName.setText(I18n.isSpanish() ? "Nombre Completo" : "Full Name");
        txtFullName.setPromptText(I18n.isSpanish() ? "Ingrese su nombre completo" : "Enter your full name");
        lblUsername.setText(I18n.get("login.usuario"));
        txtUsername.setPromptText(I18n.isSpanish() ? "Ingrese un usuario único" : "Enter a unique username");
        lblPassword.setText(I18n.get("login.clave"));
        txtPassword.setPromptText(I18n.isSpanish() ? "Ingrese su contraseña" : "Enter your password");
        lblConfirmPassword.setText(I18n.isSpanish() ? "Confirmar Contraseña" : "Confirm Password");
        txtConfirmPassword.setPromptText(I18n.isSpanish() ? "Confirme su contraseña" : "Confirm your password");
        lblRole.setText(I18n.isSpanish() ? "Rol" : "Role");
        cmbRole.setPromptText(I18n.isSpanish() ? "Seleccione un rol" : "Select a role");
        btnRegister.setText(I18n.isSpanish() ? "Registrarse" : "Register");
        lblHaveAccount.setText(I18n.isSpanish() ? "¿Ya tiene cuenta?" : "Already have an account?");
        linkLogin.setText(I18n.isSpanish() ? "Iniciar sesión aquí" : "Login here");
        lblFooter.setText("Universidad Nacional - Sede Regional Brunca");
    }
}