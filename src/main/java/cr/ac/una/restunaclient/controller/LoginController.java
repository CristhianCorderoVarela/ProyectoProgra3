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
 * Controlador para la vista de inicio de sesión.
 * Maneja la autenticación de usuarios y la navegación.
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

        // Para pruebas rápidas (comentá en producción si no te gusta dejar esto)
        // txtUsername.setText("admin");
        // txtPassword.setText("admin123");

        // Enter en los campos dispara login
        txtUsername.setOnAction(this::onLogin);
        txtPassword.setOnAction(this::onLogin);
    }

    /**
     * Maneja el evento de inicio de sesión.
     * Valida credenciales y autentica al usuario contra el backend.
     */
    @FXML
    private void onLogin(ActionEvent event) {
        // Validar usuario
        if (txtUsername.getText() == null || txtUsername.getText().trim().isEmpty()) {
            Mensaje.showError(
                    I18n.get("app.error"),
                    I18n.get("login.error.camposVacios")
            );
            txtUsername.requestFocus();
            return;
        }

        // Validar contraseña
        if (txtPassword.getText() == null || txtPassword.getText().trim().isEmpty()) {
            Mensaje.showError(
                    I18n.get("app.error"),
                    I18n.get("login.error.camposVacios")
            );
            txtPassword.requestFocus();
            return;
        }

        try {
            // Deshabilitar botón mientras se procesa
            btnLogin.setDisable(true);
            btnLogin.setText(I18n.isSpanish() ? "Ingresando..." : "Logging in...");

            // Armar credenciales para enviar al backend
            Map<String, String> credentials = new HashMap<>();
            credentials.put("usuario", txtUsername.getText().trim());
            credentials.put("clave", txtPassword.getText());

            // Llamar al servicio REST de autenticación
            String rawResponse = RestClient.post("/usuarios/login", credentials);

            // DEBUG: ver respuesta cruda del backend
            System.out.println("DEBUG Login raw response: " + rawResponse);

            Map<String, Object> response = RestClient.parseResponse(rawResponse);

            // ¿Login exitoso?
            if (Boolean.TRUE.equals(response.get("success"))) {

                // Intentar mapear data -> Usuario
                Object dataObj = response.get("data");
                Usuario usuario = null;
                if (dataObj != null) {
                    String dataJson = RestClient.toJson(dataObj);
                    usuario = RestClient.fromJson(dataJson, Usuario.class);
                }

                // Guardar usuario en el contexto si vino
                if (usuario != null) {
                    AppContext.getInstance().setUsuarioLogueado(usuario);
                }

                // Mensaje de bienvenida
                String nombreMostrar = (usuario != null && usuario.getNombre() != null)
                        ? usuario.getNombre()
                        : txtUsername.getText().trim();

                Mensaje.showSuccess(
                        I18n.get("app.exito"),
                        I18n.get("login.exito", nombreMostrar)
                );

                // Ir al menú principal
                FlowController.getInstance().goToView(
                        "MenuPrincipal",
                        "RestUNA - Menú Principal",
                        1200,
                        800
                );

            } else {
                // Falló el login (credenciales inválidas o backend respondió error)
                String backendMsg = String.valueOf(
                        response.getOrDefault("message", I18n.get("login.error.credenciales"))
                );

                Mensaje.showError(
                        I18n.get("app.error"),
                        backendMsg
                );

                txtPassword.clear();
                txtPassword.requestFocus();
            }

        } catch (Exception e) {
            // Excepción de red / server caído / etc.
            e.printStackTrace();
            Mensaje.showError(
                    I18n.get("app.error"),
                    I18n.get("login.error.conexion") + "\n" + e.getMessage()
            );
        } finally {
            // Rehabilitar botón siempre
            btnLogin.setDisable(false);
            btnLogin.setText(I18n.get("login.btnIngresar"));
        }
    }

    /**
     * Abre la pantalla de registro
     */
    @FXML
    private void onRegister(ActionEvent event) {
        FlowController.getInstance().goToView(
                "Register",
                "RestUNA - Registro",
                1024,
                826
        );
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
     * Actualiza todos los textos visibles según el idioma actual
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