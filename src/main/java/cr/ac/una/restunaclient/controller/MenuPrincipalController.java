package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.Salon;
import cr.ac.una.restunaclient.model.Usuario;
import cr.ac.una.restunaclient.service.RestClient;
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
import javafx.stage.Stage;

import java.net.URL;
import java.util.*;

/**
 * Controlador del menú principal
 * Muestra opciones según el rol del usuario
 *
 * CAMBIOS:
 * - Agregado módulo de Clientes (visible para Admin y Cajero)
 * - Habilitados módulos ya implementados (Grupos, Productos, Parametros, Salones)
 * - Los módulos en desarrollo quedan deshabilitados con mensaje
 * - ⭐ NUEVO: Validación de barras antes de entrar a "Órdenes de Barra"
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

    // Botones / tarjetas de mantenimientos
    @FXML private VBox btnUsuarios;
    @FXML private Label lblUsuarios;
    @FXML private VBox btnSalones;
    @FXML private Label lblSalones;
    @FXML private VBox btnGrupos;
    @FXML private Label lblGrupos;
    @FXML private VBox btnProductos;
    @FXML private Label lblProductos;
    @FXML private VBox btnClientes;
    @FXML private Label lblClientes;
    @FXML private VBox btnParametros;
    @FXML private Label lblParametros;

    // Botones / tarjetas de operaciones
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

        lblUsuario.setText(
                (I18n.isSpanish() ? "Usuario: " : "User: ")
                        + usuarioLogueado.getNombre()
        );
        lblRol.setText(
                (I18n.isSpanish() ? "Rol: " : "Role: ")
                        + obtenerRolTraducido()
        );

        configurarAccesosPorRol();
        actualizarTextos();
    }

    /**
     * Configura qué módulos ve cada rol.
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
        // SALONERO: solo Ver Salones y Órdenes (dejamos visibles esos dos por defecto)
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

        btnClientes.setVisible(false);
        btnClientes.setManaged(false);

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

        btnClientes.setVisible(true);
        btnClientes.setManaged(true);

        btnParametros.setVisible(true);
        btnParametros.setManaged(true);
    }

    private void ocultarTodasOperaciones() {
        // Nota: aquí NO ocultamos VerSalones ni Órdenes,
        // porque pueden ser visibles para salonero.
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
        FlowController.getInstance().goToView("Usuarios", "RestUNA - Gestión de Usuarios", 1000, 560);
    }

    @FXML
    private void onSalones(MouseEvent event) {
        FlowController.getInstance().goToView("Salones", "RestUNA - Gestión de Salones", 1000, 560);
    }

    @FXML
    private void onGrupos(MouseEvent event) {
        FlowController.getInstance().goToView("GruposProducto", "RestUNA - Grupos de Productos", 1000, 560);
    }

    @FXML
    private void onProductos(MouseEvent event) {
        FlowController.getInstance().goToView("Productos", "RestUNA - Gestión de Productos", 1000, 560);
    }

    @FXML
    private void onClientes(MouseEvent event) {
        FlowController.getInstance().goToView("Clientes", "RestUNA - Gestión de Clientes", 1000, 560);
    }

    @FXML
    private void onParametros(MouseEvent event) {
        FlowController.getInstance().goToView("Parametros", "RestUNA - Parámetros Generales", 1000, 560);
    }

    // ==================== OPERACIONES ====================

    @FXML
    private void onVerSalones(MouseEvent event) {
        FlowController.getInstance().goToView("VistaSalones", "RestUNA - Salones", 1000, 560);
    }

    /**
     * ⭐ MODIFICADO: Ahora es "Órdenes de Barra"
     * Valida que existan barras antes de permitir entrar
     */
    @FXML
    private void onOrdenes(MouseEvent event) {
        // Verificar si existen barras disponibles
        if (!existenBarras()) {
            Mensaje.showWarning(
                I18n.isSpanish() ? "Sin Barras Disponibles" : "No Bars Available",
                I18n.isSpanish() 
                    ? "No se pueden guardar pedidos de barra ya que no hay barras creadas."
                    : "Cannot save bar orders because no bars have been created."
            );
            return;
        }

        // Limpiar contexto previo
        AppContext.getInstance().remove("mesaSeleccionada");
        AppContext.getInstance().remove("salonSeleccionado");
        
        // Marcar que se entra desde "Órdenes de Barra"
        AppContext.getInstance().set("modoOrden", "BARRA");
        
        FlowController.getInstance().goToView("Ordenes", "RestUNA - Órdenes de Barra", 1400, 800);
    }

    /**
     * ⭐ NUEVO: Verifica si existen barras en el sistema
     */
    private boolean existenBarras() {
        try {
            String jsonResponse = RestClient.get("/salones");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (!Boolean.TRUE.equals(response.get("success"))) {
                return false;
            }

            Gson gson = new Gson();
            String dataJson = gson.toJson(response.get("data"));
            List<Salon> salones = gson.fromJson(dataJson, new TypeToken<List<Salon>>(){}.getType());

            if (salones == null || salones.isEmpty()) {
                return false;
            }

            // Verificar si hay al menos una barra activa
            for (Salon salon : salones) {
                if ("BARRA".equals(salon.getTipo()) && "A".equals(salon.getEstado())) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void onFacturacion(MouseEvent event) {
        FlowController.getInstance().goToView(
        "VentanaVentas",              // el nombre del .fxml registrado en FlowController
        "RestUNA - Facturación",      // título de la ventana
        1200, 700                     // tamaño; tu layout es ancho, algo tipo POS, dale más horizontal
    );
    }

    @FXML
    private void onCierres(MouseEvent event) {
        // TODO: Cierre de caja
        Mensaje.showInfo("Próximamente", "Módulo de Cierres de Caja en desarrollo");
    }

    @FXML
    private void onReportes(MouseEvent event) {
        FlowController.getInstance().goToView(
        "Reportes",              // el nombre del .fxml registrado en FlowController
        "RestUNA - Reportes",      // título de la ventana
        1200, 700                     // tamaño; tu layout es ancho, algo tipo POS, dale más horizontal
    );
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

            AppContext.getInstance().logout();

            FlowController fc = FlowController.getInstance();
            Stage main = fc.getMainStage();

            main.hide();

            fc.goToViewInModal("Login", "RestUNA - Iniciar sesión", main);

            if (AppContext.getInstance().getUsuarioLogueado() != null) {
                fc.showMenuPrincipal();
                main.show();
            } else {
                main.close();
            }
        }
    }

    private void actualizarTextos() {
        lblSubtitle.setText(I18n.get("app.nombre"));
        lblWelcome.setText(I18n.isSpanish()
                ? "Bienvenido al Sistema"
                : "Welcome to the System");

        lblUsuario.setText(
                (I18n.isSpanish() ? "Usuario: " : "User: ")
                        + usuarioLogueado.getNombre()
        );
        lblRol.setText(
                (I18n.isSpanish() ? "Rol: " : "Role: ")
                        + obtenerRolTraducido()
        );

        lblMantenimientos.setText(I18n.get("menu.mantenimientos"));
        lblOperaciones.setText(I18n.get("menu.operaciones"));

        lblUsuarios.setText(I18n.get("menu.usuarios"));
        lblSalones.setText(I18n.get("menu.salones"));
        lblGrupos.setText(I18n.get("menu.grupos"));
        lblProductos.setText(I18n.get("menu.productos"));
        lblClientes.setText(I18n.get("menu.clientes"));
        lblParametros.setText(I18n.get("menu.parametros"));

        lblVerSalones.setText(I18n.isSpanish() ? "Ver Salones" : "View Rooms");
        
        // ⭐ MODIFICADO: Ahora dice "Órdenes de Barra"
        lblOrdenes.setText(I18n.isSpanish() ? "Órdenes de Barra" : "Bar Orders");
        
        lblFacturacion.setText(I18n.get("menu.facturacion"));
        lblCierres.setText(I18n.get("menu.cierres"));
        lblReportes.setText(I18n.get("menu.reportes"));
    }
}