package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.Salon;
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
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador para CRUD de Salones y Secciones del Restaurante
 * Permite crear, editar, eliminar salones y gestionar el diseño de mesas
 */
public class SalonesController implements Initializable {

    // ==================== COMPONENTES DEL HEADER ====================
    @FXML private Label lblTitle;
    @FXML private Button btnVolver;
    
    // ==================== COMPONENTES DE LA TABLA ====================
    @FXML private TextField txtBuscar;
    @FXML private Button btnNuevo;
    @FXML private TableView<Salon> tblSalones;
    @FXML private TableColumn<Salon, String> colNombre;
    @FXML private TableColumn<Salon, String> colTipo;
    @FXML private TableColumn<Salon, String> colMesas;
    @FXML private TableColumn<Salon, String> colServicio;
    @FXML private TableColumn<Salon, String> colEstado;
    @FXML private Button btnEditar;
    @FXML private Button btnDisenaMesas;
    @FXML private Button btnEliminar;
    @FXML private Button btnRefrescar;
    
    // ==================== COMPONENTES DEL FORMULARIO ====================
    @FXML private Label lblFormTitle;
    @FXML private Label lblNombre;
    @FXML private TextField txtNombre;
    @FXML private Label lblTipo;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private Label lblTipoInfo;
    @FXML private Label lblOpciones;
    @FXML private CheckBox chkCobraServicio;
    @FXML private VBox vboxImagenMesa;
    @FXML private Label lblImagenMesa;
    @FXML private Button btnSeleccionarImagen;
    @FXML private Label lblImagenSeleccionada;
    @FXML private Label lblEstado;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    
    // ==================== VARIABLES DE CLASE ====================
    private ObservableList<Salon> listaSalones;
    private FilteredList<Salon> listaFiltrada;
    private Salon salonSeleccionado;
    private boolean modoEdicion = false;
    private byte[] imagenMesaBytes = null;
    private String tipoImagen = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Verificar permisos
        if (!AppContext.getInstance().isAdministrador()) {
            Mensaje.showError("Acceso Denegado", "Solo los administradores pueden gestionar salones.");
            onVolver(null);
            return;
        }
        
        configurarTabla();
        configurarCombos();
        configurarBusqueda();
        cargarSalones();
        limpiarFormulario();
        actualizarTextos();
    }

    // ==================== CONFIGURACIÓN INICIAL ====================
    
    /**
     * Configura las columnas de la tabla
     */
    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNombre())
        );
        
        colTipo.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getTipo())
        );
        
        colMesas.setCellValueFactory(data -> 
            new SimpleStringProperty(
                data.getValue().getMesas() != null ? 
                String.valueOf(data.getValue().getMesas().size()) : "0"
            )
        );
        
        colServicio.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().cobraServicio() ? "✓ Sí" : "✗ No")
        );
        
        colEstado.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().isActivo() ? "Activo" : "Inactivo")
        );
        
        // Listener para selección
        tblSalones.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    salonSeleccionado = newSelection;
                    habilitarBotonesAccion(true);
                }
            }
        );
    }

    /**
     * Configura los ComboBox
     */
    private void configurarCombos() {
        cmbTipo.getItems().addAll("SALON", "BARRA");
        
        // Listener para mostrar/ocultar sección de imagen según el tipo
        cmbTipo.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                boolean esSalon = "SALON".equals(newVal);
                vboxImagenMesa.setManaged(esSalon);
                vboxImagenMesa.setVisible(esSalon);
            }
        );
    }

    /**
     * Configura el filtro de búsqueda
     */
    private void configurarBusqueda() {
        txtBuscar.textProperty().addListener((observable, oldValue, newValue) -> {
            if (listaFiltrada != null) {
                listaFiltrada.setPredicate(salon -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    
                    String busqueda = newValue.toLowerCase();
                    return salon.getNombre().toLowerCase().contains(busqueda) ||
                           salon.getTipo().toLowerCase().contains(busqueda);
                });
            }
        });
    }

    // ==================== CARGA DE DATOS ====================
    
    /**
     * Carga todos los salones desde el backend
     */
    private void cargarSalones() {
        try {
            String jsonResponse = RestClient.get("/salones");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                List<Salon> salones = gson.fromJson(dataJson, 
                    new TypeToken<List<Salon>>(){}.getType());
                
                listaSalones = FXCollections.observableArrayList(salones);
                listaFiltrada = new FilteredList<>(listaSalones, p -> true);
                tblSalones.setItems(listaFiltrada);
                
                System.out.println("✅ Salones cargados: " + salones.size());
            } else {
                Mensaje.showWarning("Aviso", "No se pudieron cargar los salones.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar salones:\n" + e.getMessage());
        }
    }

    // ==================== EVENTOS DE BOTONES ====================
    
    @FXML
private void onVolver(ActionEvent event) {
    FlowController.getInstance().goToViewKeepSizeScaled(
        "MenuPrincipal",
        "RestUNA - Menú Principal",
        1200, 800
    );
}

    @FXML
    private void onNuevo(ActionEvent event) {
        modoEdicion = false;
        salonSeleccionado = null;
        limpiarFormulario();
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Salón" : "New Room");
        txtNombre.requestFocus();
    }

    @FXML
    private void onEditar(ActionEvent event) {
        if (salonSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un salón de la tabla para editar.");
            return;
        }
        
        modoEdicion = true;
        cargarSalonEnFormulario(salonSeleccionado);
        lblFormTitle.setText(I18n.isSpanish() ? "Editar Salón" : "Edit Room");
        txtNombre.requestFocus();
    }

    @FXML
    private void onDisenaMesas(ActionEvent event) {
        if (salonSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un salón de la tabla para diseñar sus mesas.");
            return;
        }
        
        if (!"SALON".equals(salonSeleccionado.getTipo())) {
            Mensaje.showWarning("Aviso", "Solo los salones tipo SALON pueden tener diseño de mesas.");
            return;
        }
        
        // TODO: Navegar a la vista de diseño de mesas
        Mensaje.showInfo("Próximamente", 
            "La funcionalidad de diseño de mesas está en desarrollo.\n\n" +
            "Aquí podrás arrastrar y posicionar las mesas del salón.");
    }

    @FXML
    private void onEliminar(ActionEvent event) {
        if (salonSeleccionado == null) {
            Mensaje.showWarning("Aviso", "Seleccione un salón de la tabla para eliminar.");
            return;
        }
        
        boolean confirmar = Mensaje.showConfirmation(
            "Confirmar Eliminación",
            "¿Está seguro de eliminar el salón '" + salonSeleccionado.getNombre() + "'?\n\n" +
            "ADVERTENCIA: Esto también eliminará todas las mesas asociadas."
        );
        
        if (!confirmar) return;
        
        try {
            String jsonResponse = RestClient.delete("/salones/" + salonSeleccionado.getId());
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", "Salón eliminado correctamente.");
                cargarSalones();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al eliminar salón:\n" + e.getMessage());
        }
    }

    @FXML
    private void onRefrescar(ActionEvent event) {
        cargarSalones();
        limpiarFormulario();
        Mensaje.showSuccess("Actualizado", "Lista de salones refrescada.");
    }

    @FXML
    private void onSeleccionarImagen(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen de Mesa");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes", ".png", ".jpg", ".jpeg", ".gif")
        );
        
        File archivo = fileChooser.showOpenDialog(btnSeleccionarImagen.getScene().getWindow());
        
        if (archivo != null) {
            try {
                imagenMesaBytes = Files.readAllBytes(archivo.toPath());
                tipoImagen = Files.probeContentType(archivo.toPath());
                lblImagenSeleccionada.setText(archivo.getName());
                lblImagenSeleccionada.setStyle("-fx-text-fill: green;");
                System.out.println("✅ Imagen seleccionada: " + archivo.getName());
            } catch (IOException e) {
                e.printStackTrace();
                Mensaje.showError("Error", "No se pudo cargar la imagen:\n" + e.getMessage());
            }
        }
    }

    @FXML
    private void onGuardar(ActionEvent event) {
        if (!validarFormulario()) return;
        
        try {
            Salon salon = new Salon();
            
            if (modoEdicion && salonSeleccionado != null) {
                salon.setId(salonSeleccionado.getId());
                salon.setVersion(salonSeleccionado.getVersion());
            }
            
            salon.setNombre(txtNombre.getText().trim());
            salon.setTipo(cmbTipo.getSelectionModel().getSelectedItem());
            salon.setCobraServicio(chkCobraServicio.isSelected() ? "S" : "N");
            salon.setEstado(chkActivo.isSelected() ? "A" : "I");
            
            // Agregar imagen si se seleccionó una nueva
            if (imagenMesaBytes != null) {
                salon.setImagenMesa(imagenMesaBytes);
                salon.setTipoImagen(tipoImagen);
            } else if (modoEdicion && salonSeleccionado != null) {
                // Mantener la imagen anterior si existe
                salon.setImagenMesa(salonSeleccionado.getImagenMesa());
                salon.setTipoImagen(salonSeleccionado.getTipoImagen());
            }
            
            // Llamar al backend
            String jsonResponse;
            if (modoEdicion) {
                jsonResponse = RestClient.put("/salones/" + salon.getId(), salon);
            } else {
                jsonResponse = RestClient.post("/salones", salon);
            }
            
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                String mensaje = modoEdicion ? 
                    "Salón actualizado correctamente." : 
                    "Salón creado correctamente.";
                Mensaje.showSuccess("Éxito", mensaje);
                cargarSalones();
                limpiarFormulario();
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar salón:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancelar(ActionEvent event) {
        limpiarFormulario();
        tblSalones.getSelectionModel().clearSelection();
    }

    // ==================== MÉTODOS AUXILIARES ====================
    
    /**
     * Valida el formulario
     */
    private boolean validarFormulario() {
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            Mensaje.showWarning("Campo Requerido", "El nombre del salón es obligatorio.");
            txtNombre.requestFocus();
            return false;
        }
        
        if (cmbTipo.getSelectionModel().getSelectedItem() == null) {
            Mensaje.showWarning("Campo Requerido", "Debe seleccionar un tipo de sección.");
            cmbTipo.requestFocus();
            return false;
        }
        
        return true;
    }

    /**
     * Limpia el formulario
     */
    private void limpiarFormulario() {
        txtNombre.clear();
        cmbTipo.getSelectionModel().clearSelection();
        chkCobraServicio.setSelected(true);
        chkActivo.setSelected(true);
        imagenMesaBytes = null;
        tipoImagen = null;
        lblImagenSeleccionada.setText("Sin imagen");
        lblImagenSeleccionada.setStyle("-fx-text-fill: gray;");
        vboxImagenMesa.setManaged(false);
        vboxImagenMesa.setVisible(false);
        modoEdicion = false;
        salonSeleccionado = null;
        lblFormTitle.setText(I18n.isSpanish() ? "Nuevo Salón" : "New Room");
        habilitarBotonesAccion(false);
    }

    /**
     * Carga un salón en el formulario
     */
    private void cargarSalonEnFormulario(Salon salon) {
        txtNombre.setText(salon.getNombre());
        cmbTipo.getSelectionModel().select(salon.getTipo());
        chkCobraServicio.setSelected(salon.cobraServicio());
        chkActivo.setSelected(salon.isActivo());
        
        // Cargar imagen si existe
        if (salon.getImagenMesa() != null) {
            imagenMesaBytes = salon.getImagenMesa();
            tipoImagen = salon.getTipoImagen();
            lblImagenSeleccionada.setText("Imagen cargada");
            lblImagenSeleccionada.setStyle("-fx-text-fill: blue;");
        }
    }

    /**
     * Habilita/deshabilita botones de acción
     */
    private void habilitarBotonesAccion(boolean habilitar) {
        btnEditar.setDisable(!habilitar);
        btnEliminar.setDisable(!habilitar);
        btnDisenaMesas.setDisable(!habilitar);
    }

    /**
     * Actualiza textos según idioma
     */
    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();
        
        lblTitle.setText(esEspanol ? "Gestión de Salones y Secciones" : "Rooms and Sections Management");
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        
        txtBuscar.setPromptText(esEspanol ? "Buscar salón..." : "Search room...");
        btnNuevo.setText(esEspanol ? "+ Nuevo Salón" : "+ New Room");
        
        colNombre.setText(esEspanol ? "Nombre" : "Name");
        colTipo.setText(esEspanol ? "Tipo" : "Type");
        colMesas.setText(esEspanol ? "Mesas" : "Tables");
        colServicio.setText(esEspanol ? "Cobra Servicio" : "Charges Service");
        colEstado.setText(esEspanol ? "Estado" : "Status");
        
        btnEditar.setText(esEspanol ? "✏ Editar" : "✏ Edit");
        btnDisenaMesas.setText(esEspanol ? "🪑 Diseñar Mesas" : "🪑 Design Tables");
        btnEliminar.setText(esEspanol ? "🗑 Eliminar" : "🗑 Delete");
        btnRefrescar.setText(esEspanol ? "🔄 Refrescar" : "🔄 Refresh");
        
        lblNombre.setText(esEspanol ? "Nombre del Salón:" : "Room Name:");
        lblTipo.setText(esEspanol ? "Tipo de Sección:" : "Section Type:");
        lblTipoInfo.setText(esEspanol ? 
            "SALON: Con diseño de mesas | BARRA: Venta directa" :
            "SALON: With table design | BARRA: Direct sale");
        lblOpciones.setText(esEspanol ? "Opciones:" : "Options:");
        chkCobraServicio.setText(esEspanol ? "Cobra Impuesto de Servicio" : "Charges Service Tax");
        lblImagenMesa.setText(esEspanol ? "Imagen de Mesa:" : "Table Image:");
        btnSeleccionarImagen.setText(esEspanol ? "📁 Seleccionar Imagen" : "📁 Select Image");
        lblEstado.setText(esEspanol ? "Estado:" : "Status:");
        chkActivo.setText(esEspanol ? "Activo" : "Active");
        
        btnGuardar.setText(esEspanol ? "💾 Guardar" : "💾 Save");
        btnCancelar.setText(esEspanol ? "✖ Cancelar" : "✖ Cancel");
    }
}