package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.Parametros;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador para la vista de Parámetros Generales
 * Solo accesible para usuarios ADMINISTRATIVOS
 * 
 * @author Dylan
 */
public class ParametrosController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private TabPane tabPane;
    
    // TAB 1: Configuración General
    @FXML private Tab tabGeneral;
    @FXML private Label lblIdioma;
    @FXML private ComboBox<String> cmbIdioma;
    @FXML private Label lblRestaurante;
    @FXML private TextField txtRestaurante;
    @FXML private Label lblDireccion;
    @FXML private TextArea txtDireccion;
    @FXML private Label lblTelefono1;
    @FXML private TextField txtTelefono1;
    @FXML private Label lblTelefono2;
    @FXML private TextField txtTelefono2;
    
    // TAB 2: Impuestos y Descuentos
    @FXML private Tab tabImpuestos;
    @FXML private Label lblImpuestoVenta;
    @FXML private TextField txtImpuestoVenta;
    @FXML private Label lblImpuestoServicio;
    @FXML private TextField txtImpuestoServicio;
    @FXML private Label lblDescuentoMax;
    @FXML private TextField txtDescuentoMax;
    
    // TAB 3: Configuración de Correo
    @FXML private Tab tabCorreo;
    @FXML private Label lblCorreoSistema;
    @FXML private TextField txtCorreoSistema;
    @FXML private Label lblClaveCorreo;
    @FXML private PasswordField txtClaveCorreo;
    @FXML private Label lblInfoCorreo;
    
    // Botones
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    
    private Parametros parametrosActuales;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Verificar que sea administrador
        if (!AppContext.getInstance().isAdministrador()) {
            Mensaje.showError("Acceso Denegado", "Solo los administradores pueden acceder a esta sección.");
            onCancelar(null);
            return;
        }
        
        configurarCombos();
        cargarParametros();
        actualizarTextos();
    }

    /**
     * Configura los ComboBox
     */
    private void configurarCombos() {
        cmbIdioma.getItems().addAll("Español", "English");
    }

    /**
     * Carga los parámetros existentes del servidor
     */
    private void cargarParametros() {
        try {
            String jsonResponse = RestClient.get("/parametros");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                // Parsear los parámetros
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                parametrosActuales = gson.fromJson(dataJson, Parametros.class);
                
                llenarCampos();
            } else {
                // No existen parámetros, mostrar formulario vacío
                Mensaje.showInfo("Configuración Inicial", 
                    "No se encontraron parámetros. Configure el sistema por primera vez.");
                parametrosActuales = new Parametros();
                parametrosActuales.setIdioma("es");
                parametrosActuales.setPorcImpuestoVenta(new BigDecimal("13.00"));
                parametrosActuales.setPorcImpuestoServicio(new BigDecimal("10.00"));
                parametrosActuales.setPorcDescuentoMaximo(new BigDecimal("10.00"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudieron cargar los parámetros:\n" + e.getMessage());
        }
    }

    /**
     * Llena los campos del formulario con los datos cargados
     */
    private void llenarCampos() {
        if (parametrosActuales != null) {
            // General
            cmbIdioma.setValue("es".equals(parametrosActuales.getIdioma()) ? "Español" : "English");
            txtRestaurante.setText(parametrosActuales.getNombreRestaurante());
            txtDireccion.setText(parametrosActuales.getDireccion());
            txtTelefono1.setText(parametrosActuales.getTelefono1());
            txtTelefono2.setText(parametrosActuales.getTelefono2());
            
            // Impuestos
            if (parametrosActuales.getPorcImpuestoVenta() != null) {
                txtImpuestoVenta.setText(parametrosActuales.getPorcImpuestoVenta().toString());
            }
            if (parametrosActuales.getPorcImpuestoServicio() != null) {
                txtImpuestoServicio.setText(parametrosActuales.getPorcImpuestoServicio().toString());
            }
            if (parametrosActuales.getPorcDescuentoMaximo() != null) {
                txtDescuentoMax.setText(parametrosActuales.getPorcDescuentoMaximo().toString());
            }
            
            // Correo
            txtCorreoSistema.setText(parametrosActuales.getCorreoSistema());
            txtClaveCorreo.setText(parametrosActuales.getClaveCorreoSistema());
        }
    }

    /**
     * Guarda los parámetros
     */
    @FXML
    private void onGuardar(ActionEvent event) {
        if (!validarCampos()) {
            return;
        }
        
        try {
            // Actualizar objeto con valores del formulario
            actualizarObjetoDesdeFormulario();
            
            String jsonResponse;
            if (parametrosActuales.getId() == null) {
                // Crear nuevos parámetros
                jsonResponse = RestClient.post("/parametros", parametrosActuales);
            } else {
                // Actualizar parámetros existentes
                jsonResponse = RestClient.put("/parametros", parametrosActuales);
            }
            
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", "Parámetros guardados correctamente.");
                
                // Aplicar cambio de idioma si cambió
                aplicarCambioIdioma();
                
                // Recargar parámetros
                cargarParametros();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudieron guardar los parámetros:\n" + e.getMessage());
        }
    }

    /**
     * Valida los campos del formulario
     */
    private boolean validarCampos() {
        // Validar nombre del restaurante
        if (txtRestaurante.getText() == null || txtRestaurante.getText().trim().isEmpty()) {
            Mensaje.showError("Error", "El nombre del restaurante es obligatorio.");
            tabPane.getSelectionModel().select(tabGeneral);
            txtRestaurante.requestFocus();
            return false;
        }
        
        // Validar impuesto de venta
        if (!validarPorcentaje(txtImpuestoVenta.getText(), "Impuesto de Venta")) {
            tabPane.getSelectionModel().select(tabImpuestos);
            txtImpuestoVenta.requestFocus();
            return false;
        }
        
        // Validar impuesto de servicio
        if (!validarPorcentaje(txtImpuestoServicio.getText(), "Impuesto de Servicio")) {
            tabPane.getSelectionModel().select(tabImpuestos);
            txtImpuestoServicio.requestFocus();
            return false;
        }
        
        // Validar descuento máximo
        if (!validarPorcentaje(txtDescuentoMax.getText(), "Descuento Máximo")) {
            tabPane.getSelectionModel().select(tabImpuestos);
            txtDescuentoMax.requestFocus();
            return false;
        }
        
        return true;
    }

    /**
     * Valida que un texto sea un porcentaje válido
     */
    private boolean validarPorcentaje(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            Mensaje.showError("Error", campo + " es obligatorio.");
            return false;
        }
        
        try {
            BigDecimal valor = new BigDecimal(texto);
            if (valor.compareTo(BigDecimal.ZERO) < 0 || valor.compareTo(new BigDecimal("100")) > 0) {
                Mensaje.showError("Error", campo + " debe estar entre 0 y 100.");
                return false;
            }
        } catch (NumberFormatException e) {
            Mensaje.showError("Error", campo + " debe ser un número válido.");
            return false;
        }
        
        return true;
    }

    /**
     * Actualiza el objeto Parametros con los valores del formulario
     */
    private void actualizarObjetoDesdeFormulario() {
        parametrosActuales.setIdioma("Español".equals(cmbIdioma.getValue()) ? "es" : "en");
        parametrosActuales.setNombreRestaurante(txtRestaurante.getText().trim());
        parametrosActuales.setDireccion(txtDireccion.getText() != null ? txtDireccion.getText().trim() : null);
        parametrosActuales.setTelefono1(txtTelefono1.getText() != null ? txtTelefono1.getText().trim() : null);
        parametrosActuales.setTelefono2(txtTelefono2.getText() != null ? txtTelefono2.getText().trim() : null);
        
        parametrosActuales.setPorcImpuestoVenta(new BigDecimal(txtImpuestoVenta.getText()));
        parametrosActuales.setPorcImpuestoServicio(new BigDecimal(txtImpuestoServicio.getText()));
        parametrosActuales.setPorcDescuentoMaximo(new BigDecimal(txtDescuentoMax.getText()));
        
        parametrosActuales.setCorreoSistema(txtCorreoSistema.getText() != null ? txtCorreoSistema.getText().trim() : null);
        parametrosActuales.setClaveCorreoSistema(txtClaveCorreo.getText() != null ? txtClaveCorreo.getText().trim() : null);
    }

    /**
     * Aplica el cambio de idioma en la interfaz
     */
    private void aplicarCambioIdioma() {
        String idiomaSeleccionado = "Español".equals(cmbIdioma.getValue()) ? "es" : "en";
        if (!idiomaSeleccionado.equals(I18n.getCurrentLanguage())) {
            I18n.setLocale(new java.util.Locale(idiomaSeleccionado));
            actualizarTextos();
        }
    }

    /**
     * Cancela y cierra la ventana
     */
    @FXML
    private void onCancelar(ActionEvent event) {
        FlowController.getInstance().goToView("MenuPrincipal", "RestUNA - Menú Principal", 1200, 800);
    }
    /**
     * Actualiza los textos según el idioma
     */
    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();
        
        lblTitle.setText(esEspanol ? "Parámetros Generales del Sistema" : "System General Parameters");
        
        // Tab General
        tabGeneral.setText(esEspanol ? "General" : "General");
        lblIdioma.setText(esEspanol ? "Idioma:" : "Language:");
        lblRestaurante.setText(esEspanol ? "Nombre del Restaurante:" : "Restaurant Name:");
        lblDireccion.setText(esEspanol ? "Dirección:" : "Address:");
        lblTelefono1.setText(esEspanol ? "Teléfono 1:" : "Phone 1:");
        lblTelefono2.setText(esEspanol ? "Teléfono 2:" : "Phone 2:");
        
        // Tab Impuestos
        tabImpuestos.setText(esEspanol ? "Impuestos y Descuentos" : "Taxes and Discounts");
        lblImpuestoVenta.setText(esEspanol ? "Impuesto de Venta (%):" : "Sales Tax (%):");
        lblImpuestoServicio.setText(esEspanol ? "Impuesto de Servicio (%):" : "Service Tax (%):");
        lblDescuentoMax.setText(esEspanol ? "Descuento Máximo (%):" : "Maximum Discount (%):");
        
        // Tab Correo
        tabCorreo.setText(esEspanol ? "Configuración de Correo" : "Email Configuration");
        lblCorreoSistema.setText(esEspanol ? "Correo del Sistema:" : "System Email:");
        lblClaveCorreo.setText(esEspanol ? "Contraseña del Correo:" : "Email Password:");
        lblInfoCorreo.setText(esEspanol ? 
            "Configure el correo desde el cual se enviarán las facturas a los clientes." :
            "Configure the email from which invoices will be sent to customers.");
        
        // Botones
        btnGuardar.setText(esEspanol ? "Guardar" : "Save");
        btnCancelar.setText(esEspanol ? "Cancelar" : "Cancel");
    }
}