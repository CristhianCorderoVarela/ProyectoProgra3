package cr.ac.una.restunaclient.controller;

import cr.ac.una.restunaclient.model.Usuario;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.stage.Stage;

/**
 * Controlador del menú principal
 * Muestra opciones según el rol del usuario
 * 
 * CAMBIOS:
 * - Agregado módulo de Clientes (visible para Admin y Cajero)
 * - Habilitados módulos ya implementados (Grupos, Productos, Parametros, Salones)
 * - Los módulos en desarrollo quedan deshabilitados con mensaje
 */
public class MenuPrincipalController implements Initializable {

    @FXML private Label lblSubtitle;
    @FXML private Label lblUsuario;
    @FXML private Label lblRol;
    @FXML private Button btnLanguage;
    @FXML private Button btnLogout;
    @FXML private Label lblWelcome;
    
    // Labels de secciones
    @FXML private Label lblMantenimientos;
    @FXML private Label lblOperaciones;
    
    // Botones de mantenimientos
    @FXML private VBox btnUsuarios;
    @FXML private Label lblUsuarios;
    @FXML private VBox btnSalones;
    @FXML private Label lblSalones;
    @FXML private VBox btnGrupos;
    @FXML private Label lblGrupos;
    @FXML private VBox btnProductos;
    @FXML private Label lblProductos;
    @FXML private VBox btnClientes;        // ⭐ NUEVO
    @FXML private Label lblClientes;       // ⭐ NUEVO
    @FXML private VBox btnParametros;
    @FXML private Label lblParametros;
    
    // Botones de operaciones
    @FXML private VBox btnVerSalones;
    @FXML private Label lblVerSalones;
    @FXML private VBox btnOrdenes;
    @FXML private Label lblOrdenes;
    @FXML private VBox btnFacturacion;
    @FXML private Label lblFacturacion;
    @FXML private VBox btnCierres;
    @FXML private Label lblCierres;
    @FXML private VBox btnReportes;
    @FXML private Label lblReportes;

    private Usuario usuarioLogueado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        usuarioLogueado = AppContext.getInstance().getUsuarioLogueado();
        
        if (usuarioLogueado == null) {
            Mensaje.showError("Error", "No hay usuario logueado");
            FlowController.getInstance().goToView("Login", "RestUNA - Login", 1024, 768);
            return;
        }
        
        lblUsuario.setText(I18n.isSpanish() ? "Usuario: " : "User: " + usuarioLogueado.getNombre());
        lblRol.setText(I18n.isSpanish() ? "Rol: " : "Role: " + obtenerRolTraducido());
        
        configurarAccesosPorRol();
        actualizarTextos();
    }

    /**
     * Configura qué módulos ve cada rol
     */
    private void configurarAccesosPorRol() {
        // Por defecto, ocultar TODO
        ocultarTodoMantenimientos();
        ocultarTodasOperaciones();
        
        if (AppContext.getInstance().isAdministrador()) {
            // ✅ ADMIN: acceso total
            mostrarTodoMantenimientos();
            mostrarTodasOperaciones();
            
        } else if (AppContext.getInstance().isCajero()) {
            // ✅ CAJERO: puede gestionar clientes, facturar, cerrar caja
            btnClientes.setVisible(true);
            btnClientes.setManaged(true);
            btnFacturacion.setVisible(true);
            btnFacturacion.setManaged(true);
            btnCierres.setVisible(true);
            btnCierres.setManaged(true);
            btnReportes.setVisible(true);
            btnReportes.setManaged(true);
        }
        // SALONERO: solo Ver Salones y Órdenes (visible por defecto)
    }

    private void ocultarTodoMantenimientos() {
        btnUsuarios.setVisible(false);
        btnUsuarios.setManaged(false);
        btnSalones.setVisible(false);
        btnSalones.setManaged(false);
        btnGrupos.setVisible(false);
        btnGrupos.setManaged(false);
        btnProductos.setVisible(false);
        btnProductos.setManaged(false);
        btnClientes.setVisible(false);     // ⭐ NUEVO
        btnClientes.setManaged(false);     // ⭐ NUEVO
        btnParametros.setVisible(false);
        btnParametros.setManaged(false);
    }

    private void mostrarTodoMantenimientos() {
        btnUsuarios.setVisible(true);
        btnUsuarios.setManaged(true);
        btnSalones.setVisible(true);
        btnSalones.setManaged(true);
        btnGrupos.setVisible(true);
        btnGrupos.setManaged(true);
        btnProductos.setVisible(true);
        btnProductos.setManaged(true);
        btnClientes.setVisible(true);      // ⭐ NUEVO
        btnClientes.setManaged(true);      // ⭐ NUEVO
        btnParametros.setVisible(true);
        btnParametros.setManaged(true);
    }

    private void ocultarTodasOperaciones() {
        btnFacturacion.setVisible(false);
        btnFacturacion.setManaged(false);
        btnCierres.setVisible(false);
        btnCierres.setManaged(false);
        btnReportes.setVisible(false);
        btnReportes.setManaged(false);
    }

    private void mostrarTodasOperaciones() {
        btnFacturacion.setVisible(true);
        btnFacturacion.setManaged(true);
        btnCierres.setVisible(true);
        btnCierres.setManaged(true);
        btnReportes.setVisible(true);
        btnReportes.setManaged(true);
    }

    private String obtenerRolTraducido() {
        String rol = usuarioLogueado.getRol();
        if ("ADMINISTRATIVO".equals(rol)) {
            return I18n.get("usuarios.rol.admin");
        } else if ("CAJERO".equals(rol)) {
            return I18n.get("usuarios.rol.cajero");
        } else if ("SALONERO".equals(rol)) {
            return I18n.get("usuarios.rol.salonero");
        }
        return rol;
    }

    // ==================== MANTENIMIENTOS ====================
    
    @FXML
    private void onUsuarios(MouseEvent event) {
        FlowController.getInstance().goToView("Usuarios", "RestUNA - Gestión de Usuarios", 1400, 800);
    }

    @FXML
    private void onSalones(MouseEvent event) {
        FlowController.getInstance().goToView("Salones", "RestUNA - Gestión de Salones", 1300, 850);
    }

    @FXML
    private void onGrupos(MouseEvent event) {
        FlowController.getInstance().goToView("GruposProducto", "RestUNA - Grupos de Productos", 1300, 800);
    }

    @FXML
    private void onProductos(MouseEvent event) {
        FlowController.getInstance().goToView("Productos", "RestUNA - Gestión de Productos", 1400, 850);
    }

    // ⭐ NUEVO MÉTODO
    @FXML
    private void onClientes(MouseEvent event) {
        FlowController.getInstance().goToView("Clientes", "RestUNA - Gestión de Clientes", 1300, 800);
    }

    @FXML
    private void onParametros(MouseEvent event) {
        FlowController.getInstance().goToView("Parametros", "RestUNA - Parámetros Generales", 900, 700);
    }

    // ==================== OPERACIONES ====================
    
    @FXML
    private void onVerSalones(MouseEvent event) {
        FlowController.getInstance().goToView("VistaSalones", "RestUNA - Salones", 1400, 800);
    }

    @FXML
    private void onOrdenes(MouseEvent event) {
        // TODO: Gestión de órdenes
        Mensaje.showInfo("Próximamente", "Módulo de Órdenes en desarrollo");
    }

    @FXML
    private void onFacturacion(MouseEvent event) {
        // TODO: Sistema de facturación
        Mensaje.showInfo("Próximamente", "Módulo de Facturación en desarrollo");
    }

    @FXML
    private void onCierres(MouseEvent event) {
        // TODO: Cierre de caja
        Mensaje.showInfo("Próximamente", "Módulo de Cierres de Caja en desarrollo");
    }

    @FXML
    private void onReportes(MouseEvent event) {
        // TODO: Reportes con JasperReports
        Mensaje.showInfo("Próximamente", "Módulo de Reportes en desarrollo");
    }

    // ==================== HEADER ====================
    
    @FXML
    private void onLanguageChange(ActionEvent event) {
        if (I18n.isSpanish()) {
            I18n.setLocale(new Locale("en"));
        } else {
            I18n.setLocale(new Locale("es", "CR"));
        }
        actualizarTextos();
    }

    @FXML
private void onLogout(ActionEvent event) {
    if (Mensaje.showConfirmation(
            I18n.get("app.confirmacion"),
            I18n.isSpanish() ? "¿Está seguro de cerrar sesión?" : "Are you sure you want to logout?")) {

        // 1) limpiar sesión
        AppContext.getInstance().logout();

        // 2) ocultar la ventana principal para que SOLO se vea el login
        FlowController fc = FlowController.getInstance();
        Stage main = fc.getMainStage();
        main.hide();  // <-- CLAVE: oculta la principal

        // 3) abrir login como MODAL (bloquea hasta que cierre)
        fc.goToViewInModal("Login", "RestUNA - Iniciar sesión", main);

        // 4) decidir qué hacer después del login modal
        if (AppContext.getInstance().getUsuarioLogueado() != null) {
            // Login correcto: (re)cargar menú y volver a mostrar principal
            fc.goToView("MenuPrincipal", "RestUNA - Menú Principal", 1200, 800);
            main.show();
        } else {
            // Canceló o falló: cerrar la app o quedarte oculto (elige)
            main.close();
        }
    }
}



    private void actualizarTextos() {
        lblSubtitle.setText(I18n.get("app.nombre"));
        lblWelcome.setText(I18n.isSpanish() ? 
            "Bienvenido al Sistema" : "Welcome to the System");
        lblUsuario.setText((I18n.isSpanish() ? "Usuario: " : "User: ") + usuarioLogueado.getNombre());
        lblRol.setText((I18n.isSpanish() ? "Rol: " : "Role: ") + obtenerRolTraducido());
        
        lblMantenimientos.setText(I18n.get("menu.mantenimientos"));
        lblOperaciones.setText(I18n.get("menu.operaciones"));
        
        lblUsuarios.setText(I18n.get("menu.usuarios"));
        lblSalones.setText(I18n.get("menu.salones"));
        lblGrupos.setText(I18n.get("menu.grupos"));
        lblProductos.setText(I18n.get("menu.productos"));
        lblClientes.setText(I18n.get("menu.clientes"));  // ⭐ NUEVO
        lblParametros.setText(I18n.get("menu.parametros"));
        
        lblVerSalones.setText(I18n.isSpanish() ? "Ver Salones" : "View Rooms");
        lblOrdenes.setText(I18n.get("menu.ordenes"));
        lblFacturacion.setText(I18n.get("menu.facturacion"));
        lblCierres.setText(I18n.get("menu.cierres"));
        lblReportes.setText(I18n.get("menu.reportes"));
    }
}