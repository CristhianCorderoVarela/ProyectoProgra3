package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.model.Usuario;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador para la vista de inicio de sesión
 * Maneja la autenticación de usuarios y navegación
 * 
 * @author RestUNA Team
 */
public class LoginController implements Initializable {

    @FXML private Label lblAppSubtitle;
    @FXML private Button btnLanguage;
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;
    @FXML private Label lblUsername;
    @FXML private TextField txtUsername;
    @FXML private Label lblPassword;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Label lblNoAccount;
    @FXML private Hyperlink linkRegister;
    @FXML private Label lblFooter;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Cargar textos iniciales según idioma
        actualizarTextos();
        
        // Establecer valores por defecto para pruebas (comentar en producción)
        // txtUsername.setText("admin");
        // txtPassword.setText("admin123");
        
        // Agregar listener para Enter en los campos
        txtUsername.setOnAction(this::onLogin);
        txtPassword.setOnAction(this::onLogin);
    }

    /**
     * Maneja el evento de inicio de sesión
     * Valida credenciales y autentica al usuario contra el backend
     */
    @FXML
    private void onLogin(ActionEvent event) {
        // Validar que el campo de usuario no esté vacío
        if (txtUsername.getText() == null || txtUsername.getText().trim().isEmpty()) {
            Mensaje.showError(
                I18n.get("app.error"), 
                I18n.get("login.error.camposVacios")
            );
            txtUsername.requestFocus();
            return;
        }

        // Validar que el campo de contraseña no esté vacío
        if (txtPassword.getText() == null || txtPassword.getText().trim().isEmpty()) {
            Mensaje.showError(
                I18n.get("app.error"), 
                I18n.get("login.error.camposVacios")
            );
            txtPassword.requestFocus();
            return;
        }

        try {
            // Deshabilitar botón mientras se procesa la petición
            btnLogin.setDisable(true);
            btnLogin.setText(I18n.isSpanish() ? "Ingresando..." : "Logging in...");

            // Preparar credenciales para enviar al backend
            Map<String, String> credentials = new HashMap<>();
            credentials.put("usuario", txtUsername.getText().trim());
            credentials.put("clave", txtPassword.getText());

            // Llamar al servicio REST de autenticación
            String jsonResponse = RestClient.post("/usuarios/login", credentials);
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            // Verificar si la autenticación fue exitosa
            if (Boolean.TRUE.equals(response.get("success"))) {
                // Deserializar el objeto Usuario desde la respuesta
                String dataJson = RestClient.toJson(response.get("data"));
                Usuario usuario = RestClient.fromJson(dataJson, Usuario.class);

                // Guardar usuario en el contexto de la aplicación
                AppContext.getInstance().setUsuarioLogueado(usuario);

                // Mostrar mensaje de bienvenida
                Mensaje.showSuccess(
                    I18n.get("app.exito"),
                    I18n.get("login.exito", usuario.getNombre())
                );

                // Navegar al menú principal
                FlowController.getInstance().goToView("MenuPrincipal", "RestUNA - Menú Principal", 1200, 800);

            } else {
                // Credenciales inválidas
                Mensaje.showError(
                    I18n.get("app.error"),
                    I18n.get("login.error.credenciales")
                );
                txtPassword.clear();
                txtPassword.requestFocus();
            }

        } catch (Exception e) {
            // Error de conexión o del servidor
            e.printStackTrace();
            Mensaje.showError(
                I18n.get("app.error"),
                I18n.get("login.error.conexion") + "\n" + e.getMessage()
            );
        } finally {
            // Rehabilitar botón
            btnLogin.setDisable(false);
            btnLogin.setText(I18n.get("login.btnIngresar"));
        }
    }

    /**
     * Navega a la vista de registro de usuarios
     */
    @FXML
    private void onRegister(ActionEvent event) {
        FlowController.getInstance().goToView("Register", "RestUNA - Registro", 1024, 826);
    }

    /**
     * Cambia el idioma de la aplicación entre español e inglés
     */
    @FXML
    private void onLanguageChange(ActionEvent event) {
        if (I18n.isSpanish()) {
            I18n.setLocale(new Locale("en"));
        } else {
            I18n.setLocale(new Locale("es", "CR"));
        }
        actualizarTextos();
    }

    /**
     * Actualiza todos los textos de la interfaz según el idioma actual
     */
    private void actualizarTextos() {
        lblAppSubtitle.setText(I18n.get("app.nombre"));
        lblTitle.setText(I18n.get("login.titulo"));
        lblSubtitle.setText(I18n.get("app.nombre"));
        lblUsername.setText(I18n.get("login.usuario"));
        txtUsername.setPromptText(I18n.get("login.usuario"));
        lblPassword.setText(I18n.get("login.clave"));
        txtPassword.setPromptText(I18n.get("login.clave"));
        btnLogin.setText(I18n.get("login.btnIngresar"));
        lblNoAccount.setText(I18n.get("login.noAccount"));
        linkRegister.setText(I18n.get("login.linkRegister"));
        lblFooter.setText("Universidad Nacional - Sede Regional Brunca");
    }
}