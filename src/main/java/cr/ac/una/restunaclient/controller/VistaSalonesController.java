package cr.ac.una.restunaclient.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cr.ac.una.restunaclient.model.Mesa;
import cr.ac.una.restunaclient.model.Salon;
import cr.ac.una.restunaclient.service.RestClient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.*;

/**
 * Controlador para la vista operativa de salones
 * - Visualizar mesas y su estado (libre/ocupada)
 * - Modo edición para diseñar distribución de mesas (solo Admin)
 * - Crear órdenes al hacer clic en una mesa
 * - Drag & Drop para reposicionar mesas
 * - Menú contextual para editar/eliminar mesas
 */
public class VistaSalonesController implements Initializable {

    @FXML private Label lblTitle;
    @FXML private Label lblUsuario;
    @FXML private Button btnVolver;
    @FXML private ComboBox<Salon> cmbSalones;
    @FXML private Button btnModoEdicion;
    @FXML private Button btnGuardarDiseno;
    @FXML private Label lblInfo;
    @FXML private Pane panelMesas;
    @FXML private Label lblLibre;
    @FXML private Label lblOcupada;
    @FXML private Label lblEdicion;
    
    private List<Salon> listaSalones;
    private Salon salonActual;
    private boolean modoEdicion = false;
    private Map<Long, StackPane> mapaMesasVista = new HashMap<>();
    private Image imagenMesaBase; // Imagen de mesa cargada del salón

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarUsuario();
        configurarComboSalones();
        cargarSalones();
        actualizarTextos();

        // Mostrar botón de edición solo para administradores
        if (AppContext.getInstance().isAdministrador()) {
            btnModoEdicion.setManaged(true);
            btnModoEdicion.setVisible(true);
        }

        // Limpiar del contexto
        AppContext.getInstance().set("salonParaDisenar", null);
    }

    
    // ==================== CONFIGURACIÓN INICIAL ====================
    
    private void configurarUsuario() {
        var usuario = AppContext.getInstance().getUsuarioLogueado();
        if (usuario != null) {
            lblUsuario.setText((I18n.isSpanish() ? "Usuario: " : "User: ") + usuario.getNombre());
        }
    }

    private void configurarComboSalones() {
        cmbSalones.setConverter(new javafx.util.StringConverter<Salon>() {
            @Override
            public String toString(Salon salon) {
                return salon != null ? salon.getNombre() : "";
            }

            @Override
            public Salon fromString(String string) {
                return null;
            }
        });
        
        cmbSalones.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    // Salir de modo edición al cambiar de salón
                    if (modoEdicion) {
                        modoEdicion = false;
                        actualizarUIModEdicion();
                    }
                    cargarMesasSalon(newVal);
                }
            }
        );
    }

    // ==================== CARGA DE DATOS ====================
    
    private void cargarSalones() {
        try {
            String jsonResponse = RestClient.get("/salones/tipo/salon");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                listaSalones = gson.fromJson(dataJson, new TypeToken<List<Salon>>(){}.getType());
                
                cmbSalones.getItems().clear();
                cmbSalones.getItems().addAll(listaSalones);
                
                if (!listaSalones.isEmpty()) {
                    cmbSalones.getSelectionModel().selectFirst();
                }
                
                System.out.println("✅ Salones cargados: " + listaSalones.size());
            } else {
                Mensaje.showWarning("Aviso", "No hay salones disponibles.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar salones:\n" + e.getMessage());
        }
    }

    private void cargarMesasSalon(Salon salon) {
        try {
            salonActual = salon;
            
            // Cargar imagen de mesa del salón
            if (salon.getImagenMesa() != null && salon.getImagenMesa().length > 0) {
                try {
                    imagenMesaBase = new Image(new ByteArrayInputStream(salon.getImagenMesa()));
                    System.out.println("✅ Imagen de mesa cargada para salón: " + salon.getNombre());
                } catch (Exception e) {
                    System.out.println("⚠️ No se pudo cargar imagen de mesa: " + e.getMessage());
                    imagenMesaBase = null;
                }
            } else {
                imagenMesaBase = null;
            }
            
            String jsonResponse = RestClient.get("/salones/" + salon.getId() + "/mesas");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                List<Mesa> mesas = gson.fromJson(dataJson, new TypeToken<List<Mesa>>(){}.getType());
                
                salon.setMesas(mesas);
                mostrarMesasEnPanel(salon);
                
                System.out.println("✅ Mesas cargadas: " + mesas.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al cargar mesas:\n" + e.getMessage());
        }
    }

    // ==================== VISUALIZACIÓN DE MESAS ====================
    
    private void mostrarMesasEnPanel(Salon salon) {
        panelMesas.getChildren().clear();
        mapaMesasVista.clear();
        
        if (salon.getMesas() == null || salon.getMesas().isEmpty()) {
            // Mostrar mensaje de que no hay mesas
            VBox vboxMensaje = new VBox(15);
            vboxMensaje.setAlignment(Pos.CENTER);
            
            Label lblSinMesas = new Label(
                I18n.isSpanish() ? 
                "No hay mesas en este salón" :
                "No tables in this room"
            );
            lblSinMesas.setStyle("-fx-text-fill: #999; -fx-font-size: 18px; -fx-font-weight: bold;");
            
            Label lblInstruccion = new Label(
                I18n.isSpanish() ? 
                "Active el modo edición y haga clic derecho para agregar mesas" :
                "Enable edit mode and right-click to add tables"
            );
            lblInstruccion.setStyle("-fx-text-fill: #666; -fx-font-size: 14px;");
            
            vboxMensaje.getChildren().addAll(lblSinMesas, lblInstruccion);
            vboxMensaje.setLayoutX(panelMesas.getPrefWidth() / 2 - 200);
            vboxMensaje.setLayoutY(panelMesas.getPrefHeight() / 2 - 40);
            
            panelMesas.getChildren().add(vboxMensaje);
            return;
        }
        
        // Crear visualización de cada mesa
        for (Mesa mesa : salon.getMesas()) {
            StackPane mesaPane = crearVistaMesa(mesa);
            panelMesas.getChildren().add(mesaPane);
            mapaMesasVista.put(mesa.getId(), mesaPane);
        }
    }

    private StackPane crearVistaMesa(Mesa mesa) {
        StackPane container = new StackPane();
        container.setLayoutX(mesa.getPosicionX());
        container.setLayoutY(mesa.getPosicionY());
        container.setPrefSize(90, 90);
        container.setUserData(mesa); // Guardar referencia a la mesa
        
        // Fondo de la mesa según estado
        Rectangle fondo = new Rectangle(90, 90);
        fondo.setArcWidth(15);
        fondo.setArcHeight(15);
        
        if (modoEdicion) {
            fondo.setFill(Color.web("#fff3cd"));
            fondo.setStroke(Color.web("#ffc107"));
        } else if (mesa.isOcupada()) {
            fondo.setFill(Color.web("#f8d7da"));
            fondo.setStroke(Color.web("#dc3545"));
        } else {
            fondo.setFill(Color.web("#d4edda"));
            fondo.setStroke(Color.web("#28a745"));
        }
        fondo.setStrokeWidth(3);
        
        container.getChildren().add(fondo);
        
        // Imagen de la mesa si existe
        if (imagenMesaBase != null) {
            ImageView imagen = new ImageView(imagenMesaBase);
            imagen.setFitWidth(55);
            imagen.setFitHeight(55);
            imagen.setPreserveRatio(true);
            container.getChildren().add(imagen);
        }
        
        // Etiqueta con identificador de la mesa
        Label lblIdentificador = new Label(mesa.getIdentificador());
        lblIdentificador.setFont(Font.font("System", FontWeight.BOLD, 13));
        lblIdentificador.setStyle(
            "-fx-background-color: white; " +
            "-fx-padding: 4 10; " +
            "-fx-background-radius: 10; " +
            "-fx-border-color: #333; " +
            "-fx-border-width: 1.5; " +
            "-fx-border-radius: 10;"
        );
        lblIdentificador.setTranslateY(30);
        container.getChildren().add(lblIdentificador);
        
        // Efectos hover
        container.setOnMouseEntered(e -> {
            container.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 18, 0, 0, 4);");
            container.setScaleX(1.08);
            container.setScaleY(1.08);
        });
        
        container.setOnMouseExited(e -> {
            container.setStyle("");
            container.setScaleX(1.0);
            container.setScaleY(1.0);
        });
        
        // Eventos según modo
        if (modoEdicion) {
            configurarDragAndDrop(container, mesa);
            configurarMenuContextual(container, mesa);
        } else {
            container.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                    onMesaClick(mesa);
                }
            });
        }
        
        return container;
    }

    // ==================== MODO EDICIÓN (Drag & Drop) ====================
    
    private void configurarDragAndDrop(StackPane mesaPane, Mesa mesa) {
        final double[] dragDelta = new double[2];
        final boolean[] isDragging = {false};
        
        mesaPane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragDelta[0] = mesaPane.getLayoutX() - e.getSceneX();
                dragDelta[1] = mesaPane.getLayoutY() - e.getSceneY();
                mesaPane.setCursor(Cursor.MOVE);
                isDragging[0] = false;
            }
        });
        
        mesaPane.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                isDragging[0] = true;
                
                double newX = e.getSceneX() + dragDelta[0];
                double newY = e.getSceneY() + dragDelta[1];
                
                // Límites del panel
                newX = Math.max(0, Math.min(newX, panelMesas.getWidth() - 90));
                newY = Math.max(0, Math.min(newY, panelMesas.getHeight() - 90));
                
                mesaPane.setLayoutX(newX);
                mesaPane.setLayoutY(newY);
            }
        });
        
        mesaPane.setOnMouseReleased(e -> {
            mesaPane.setCursor(Cursor.HAND);
            if (isDragging[0]) {
                // Actualizar posición en el modelo
                mesa.actualizarPosicion(mesaPane.getLayoutX(), mesaPane.getLayoutY());
                System.out.println("📍 Mesa " + mesa.getIdentificador() + 
                                 " reposicionada: (" + mesa.getPosicionX() + ", " + mesa.getPosicionY() + ")");
            }
        });
    }

    private void configurarMenuContextual(StackPane mesaPane, Mesa mesa) {
        ContextMenu menu = new ContextMenu();
        
        MenuItem itemEditar = new MenuItem(I18n.isSpanish() ? "✏️ Editar Identificador" : "✏️ Edit Identifier");
        itemEditar.setOnAction(e -> editarIdentificadorMesa(mesa));
        
        MenuItem itemEliminar = new MenuItem(I18n.isSpanish() ? "🗑️ Eliminar Mesa" : "🗑️ Delete Table");
        itemEliminar.setOnAction(e -> eliminarMesa(mesa));
        
        menu.getItems().addAll(itemEditar, itemEliminar);
        
        mesaPane.setOnContextMenuRequested(e -> {
            menu.show(mesaPane, e.getScreenX(), e.getScreenY());
        });
    }

    private void editarIdentificadorMesa(Mesa mesa) {
        TextInputDialog dialog = new TextInputDialog(mesa.getIdentificador());
        dialog.setTitle(I18n.isSpanish() ? "Editar Mesa" : "Edit Table");
        dialog.setHeaderText(I18n.isSpanish() ? 
            "Editar identificador de la mesa:" : 
            "Edit table identifier:");
        dialog.setContentText(I18n.isSpanish() ? "Identificador:" : "Identifier:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(identificador -> {
            if (identificador.trim().isEmpty()) {
                Mensaje.showWarning("Error", I18n.isSpanish() ? 
                    "El identificador no puede estar vacío." : 
                    "Identifier cannot be empty.");
                return;
            }
            
            try {
                Mesa mesaUpdate = new Mesa();
                mesaUpdate.setId(mesa.getId());
                mesaUpdate.setIdentificador(identificador.trim());
                mesaUpdate.setPosicionX(mesa.getPosicionX());
                mesaUpdate.setPosicionY(mesa.getPosicionY());
                
                String jsonResponse = RestClient.put("/salones/mesas/" + mesa.getId(), mesaUpdate);
                Map<String, Object> response = RestClient.parseResponse(jsonResponse);
                
                if (Boolean.TRUE.equals(response.get("success"))) {
                    mesa.setIdentificador(identificador.trim());
                    mostrarMesasEnPanel(salonActual);
                    Mensaje.showSuccess("Éxito", I18n.isSpanish() ? 
                        "Mesa actualizada correctamente." : 
                        "Table updated successfully.");
                } else {
                    Mensaje.showError("Error", response.get("message").toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Mensaje.showError("Error", "Error al actualizar mesa:\n" + e.getMessage());
            }
        });
    }

    private void eliminarMesa(Mesa mesa) {
        boolean confirmar = Mensaje.showConfirmation(
            I18n.isSpanish() ? "Confirmar Eliminación" : "Confirm Deletion",
            I18n.isSpanish() ? 
            "¿Está seguro de eliminar la mesa '" + mesa.getIdentificador() + "'?" :
            "Are you sure you want to delete table '" + mesa.getIdentificador() + "'?"
        );
        
        if (!confirmar) return;
        
        try {
            String jsonResponse = RestClient.delete("/salones/mesas/" + mesa.getId());
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                salonActual.getMesas().remove(mesa);
                mostrarMesasEnPanel(salonActual);
                Mensaje.showSuccess("Éxito", I18n.isSpanish() ? 
                    "Mesa eliminada correctamente." : 
                    "Table deleted successfully.");
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al eliminar mesa:\n" + e.getMessage());
        }
    }

    // ==================== EVENTOS DE BOTONES ====================
    
    @FXML
    private void onVolver(ActionEvent event) {
        if (modoEdicion) {
            boolean confirmar = Mensaje.showConfirmation(
                "Confirmar",
                I18n.isSpanish() ? 
                "Está en modo edición. ¿Desea guardar los cambios antes de salir?" :
                "You are in edit mode. Do you want to save changes before leaving?"
            );
            if (confirmar) {
                guardarDisenoSilencioso();
            }
        }
        FlowController.getInstance().goToView("MenuPrincipal", "RestUNA - Menú Principal", 1200, 800);
    }
    
    @FXML
    private void onModoEdicion(ActionEvent event) {
        if (salonActual == null) {
            Mensaje.showWarning("Aviso", "Seleccione un salón primero.");
            return;
        }
        
        modoEdicion = !modoEdicion;
        actualizarUIModEdicion();
        mostrarMesasEnPanel(salonActual); // Recargar mesas con nuevo modo
    }

    private void actualizarUIModEdicion() {
        if (modoEdicion) {
            btnModoEdicion.setText(I18n.isSpanish() ? "❌ Salir de Edición" : "❌ Exit Edit Mode");
            btnModoEdicion.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8;");
            btnGuardarDiseno.setManaged(true);
            btnGuardarDiseno.setVisible(true);
            lblInfo.setText(I18n.isSpanish() ? 
                "Arrastre mesas para moverlas | Clic derecho: editar/eliminar | Clic derecho en panel: agregar mesa" :
                "Drag tables to move | Right-click: edit/delete | Right-click on panel: add table");
            
            // Configurar clic derecho en panel vacío para agregar mesas
            panelMesas.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    // Verificar que no se hizo clic sobre una mesa
                    if (e.getTarget() == panelMesas) {
                        agregarNuevaMesa(e.getX(), e.getY());
                    }
                }
            });
        } else {
            btnModoEdicion.setText(I18n.isSpanish() ? "🛠️ Modo Edición" : "🛠️ Edit Mode");
            btnModoEdicion.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8;");
            btnGuardarDiseno.setManaged(false);
            btnGuardarDiseno.setVisible(false);
            lblInfo.setText(I18n.isSpanish() ? 
                "Haga clic en una mesa para crear una orden" :
                "Click on a table to create an order");
            panelMesas.setOnMouseClicked(null);
        }
    }
    
    @FXML
    private void onGuardarDiseno(ActionEvent event) {
        guardarDiseno();
    }

    private void guardarDiseno() {
        if (salonActual == null || salonActual.getMesas().isEmpty()) {
            Mensaje.showWarning("Aviso", "No hay mesas para guardar.");
            return;
        }
        
        try {
            // Preparar lista de mesas con posiciones actualizadas
            List<Mesa> mesasActualizadas = new ArrayList<>();
            for (Mesa mesa : salonActual.getMesas()) {
                mesasActualizadas.add(mesa.toUpdateDTO());
            }
            
            String jsonResponse = RestClient.put("/salones/mesas/posiciones", mesasActualizadas);
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);
            
            if (Boolean.TRUE.equals(response.get("success"))) {
                Mensaje.showSuccess("Éxito", I18n.isSpanish() ? 
                    "Diseño guardado correctamente." :
                    "Design saved successfully.");
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar diseño:\n" + e.getMessage());
        }
    }

    private void guardarDisenoSilencioso() {
        if (salonActual == null || salonActual.getMesas().isEmpty()) return;
        
        try {
            List<Mesa> mesasActualizadas = new ArrayList<>();
            for (Mesa mesa : salonActual.getMesas()) {
                mesasActualizadas.add(mesa.toUpdateDTO());
            }
            
            RestClient.put("/salones/mesas/posiciones", mesasActualizadas);
        } catch (Exception e) {
            System.err.println("Error al guardar diseño: " + e.getMessage());
        }
    }

    private void agregarNuevaMesa(double x, double y) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(I18n.isSpanish() ? "Agregar Mesa" : "Add Table");
        dialog.setHeaderText(I18n.isSpanish() ? "Ingrese el identificador de la mesa:" : "Enter table identifier:");
        dialog.setContentText(I18n.isSpanish() ? "Identificador:" : "Identifier:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(identificador -> {
            if (identificador.trim().isEmpty()) {
                Mensaje.showWarning("Error", I18n.isSpanish() ? 
                    "El identificador no puede estar vacío." : 
                    "Identifier cannot be empty.");
                return;
            }
            
            try {
                Mesa nuevaMesa = new Mesa(identificador.trim(), x, y);
                nuevaMesa.setSalonId(salonActual.getId());
                nuevaMesa.setEstado("LIBRE");
                
                String jsonResponse = RestClient.post("/salones/" + salonActual.getId() + "/mesas", nuevaMesa);
                Map<String, Object> response = RestClient.parseResponse(jsonResponse);
                
                if (Boolean.TRUE.equals(response.get("success"))) {
                    Mensaje.showSuccess("Éxito", I18n.isSpanish() ? 
                        "Mesa agregada correctamente." : 
                        "Table added successfully.");
                    cargarMesasSalon(salonActual);
                } else {
                    Mensaje.showError("Error", response.get("message").toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Mensaje.showError("Error", "Error al agregar mesa:\n" + e.getMessage());
            }
        });
    }

    // ==================== EVENTO CLIC EN MESA (Modo Normal) ====================
    
    private void onMesaClick(Mesa mesa) {
        // Mostrar diálogo de opciones
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(I18n.isSpanish() ? "Mesa " + mesa.getIdentificador() : "Table " + mesa.getIdentificador());
        alert.setHeaderText(I18n.isSpanish() ? 
            "Estado: " + (mesa.isLibre() ? "Libre ✓" : "Ocupada ⚠") :
            "Status: " + (mesa.isLibre() ? "Available ✓" : "Occupied ⚠"));
        
        String contenido = I18n.isSpanish() ?
            "Salón: " + salonActual.getNombre() + "\n" +
            "Mesa: " + mesa.getIdentificador() + "\n\n" +
            "¿Qué desea hacer?" :
            "Room: " + salonActual.getNombre() + "\n" +
            "Table: " + mesa.getIdentificador() + "\n\n" +
            "What would you like to do?";
        
        alert.setContentText(contenido);
        
        ButtonType btnCrearOrden = new ButtonType(
            I18n.isSpanish() ? "📝 Crear/Ver Orden" : "📝 Create/View Order"
        );
        ButtonType btnCancelar = new ButtonType(
            I18n.isSpanish() ? "Cancelar" : "Cancel", 
            ButtonBar.ButtonData.CANCEL_CLOSE
        );
        
        alert.getButtonTypes().setAll(btnCrearOrden, btnCancelar);
        
        Optional<ButtonType> result = alert.showAndWait();
        
        if (result.isPresent() && result.get() == btnCrearOrden) {
            // Guardar contexto para la pantalla de órdenes
            AppContext.getInstance().set("mesaSeleccionada", mesa);
            AppContext.getInstance().set("salonSeleccionado", salonActual);
            
            // TODO: Navegar a pantalla de crear orden
            Mensaje.showInfo("Próximamente", 
                I18n.isSpanish() ? 
                "La funcionalidad de crear órdenes está en desarrollo.\n\n" +
                "Contexto guardado:\n" +
                "- Salón: " + salonActual.getNombre() + "\n" +
                "- Mesa: " + mesa.getIdentificador() + "\n" +
                "- Estado: " + mesa.getEstado() :
                "Order creation feature is under development.\n\n" +
                "Context saved:\n" +
                "- Room: " + salonActual.getNombre() + "\n" +
                "- Table: " + mesa.getIdentificador() + "\n" +
                "- Status: " + mesa.getEstado());
            
            // FUTURO: Descomentar cuando exista la vista de órdenes
            // FlowController.getInstance().goToView("CrearOrden", 
            //     "Orden - Mesa " + mesa.getIdentificador(), 1200, 800);
        }
    }

    // ==================== ACTUALIZACIÓN DE TEXTOS (i18n) ====================
    
    private void actualizarTextos() {
        boolean esEspanol = I18n.isSpanish();
        
        lblTitle.setText(esEspanol ? "🍽️ Salones del Restaurante" : "🍽️ Restaurant Rooms");
        btnVolver.setText(esEspanol ? "← Volver" : "← Back");
        cmbSalones.setPromptText(esEspanol ? "Seleccione un salón" : "Select a room");
        btnModoEdicion.setText(esEspanol ? "🛠️ Modo Edición" : "🛠️ Edit Mode");
        btnGuardarDiseno.setText(esEspanol ? "💾 Guardar Diseño" : "💾 Save Design");
        lblInfo.setText(esEspanol ? 
            "Haga clic en una mesa para crear una orden" :
            "Click on a table to create an order");
        lblLibre.setText(esEspanol ? "Libre" : "Available");
        lblOcupada.setText(esEspanol ? "Ocupada" : "Occupied");
        lblEdicion.setText(esEspanol ? "Modo Edición" : "Edit Mode");
    }
}