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
import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

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
 * - Detección de colisiones entre mesas
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
    private Image imagenMesaBase;
    
    // Variables para animaciones y efectos
    private StackPane mesaSeleccionada = null;
    private Timeline pulseTimeline;
    
    // Constantes para colisiones
    private static final double MESA_SIZE = 90.0;
    private static final double MARGEN_BORDE = 10.0;
    private static final double MARGEN_ENTRE_MESAS = 15.0;

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
        
        // Configurar grid de fondo en modo edición
        configurarPanelMesas();
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
    
    private void configurarPanelMesas() {
        // Aplicar efecto de sombra interna sutil al panel
        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setColor(Color.rgb(0, 0, 0, 0.05));
        innerShadow.setOffsetX(0);
        innerShadow.setOffsetY(2);
        innerShadow.setRadius(5);
        panelMesas.setEffect(innerShadow);
    }

    // ==================== CARGA DE DATOS ====================
    
    private void cargarSalones() {
        try {
            System.out.println("📤 Solicitando salones tipo SALON...");
            String jsonResponse = RestClient.get("/salones/tipo/salon");

            System.out.println("📥 Respuesta recibida (" + jsonResponse.length() + " caracteres):");
            System.out.println(jsonResponse.substring(0, Math.min(300, jsonResponse.length())));

            if (jsonResponse.trim().startsWith("<")) {
                System.err.println("❌ El backend devolvió HTML (error 500)");
                Mensaje.showError("Error del Servidor",
                        "El servidor tuvo un error al cargar los salones.\n\n"
                        + "Verifica los logs de Payara para más detalles.");
                return;
            }

            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                listaSalones = gson.fromJson(dataJson, new TypeToken<List<Salon>>() {
                }.getType());

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

            System.out.println("📋 Cargando salón: " + salon.getNombre());
            System.out.println("   - Tipo: " + salon.getTipo());
            System.out.println("   - Tiene imagen: " + (salon.getImagenMesa() != null));
            if (salon.getImagenMesa() != null) {
                System.out.println("   - Tamaño imagen: " + salon.getImagenMesa().length + " bytes");
            }

            // Cargar imagen de mesa del salón
            if (salon.getImagenMesa() != null && salon.getImagenMesa().length > 0) {
                try {
                    imagenMesaBase = new Image(new ByteArrayInputStream(salon.getImagenMesa()));
                    System.out.println("✅ Imagen de mesa cargada correctamente");
                    System.out.println("   - Ancho: " + imagenMesaBase.getWidth());
                    System.out.println("   - Alto: " + imagenMesaBase.getHeight());
                } catch (Exception e) {
                    System.err.println("❌ Error al crear imagen desde bytes:");
                    e.printStackTrace();
                    imagenMesaBase = null;
                }
            } else {
                System.out.println("⚠ Salón sin imagen de mesa");
                imagenMesaBase = null;
            }

            // Cargar mesas del salón desde el backend
            String jsonResponse = RestClient.get("/salones/" + salon.getId() + "/mesas");
            Map<String, Object> response = RestClient.parseResponse(jsonResponse);

            if (Boolean.TRUE.equals(response.get("success"))) {
                Gson gson = new Gson();
                String dataJson = gson.toJson(response.get("data"));
                List<Mesa> mesas = gson.fromJson(dataJson, new TypeToken<List<Mesa>>() {
                }.getType());

                salon.setMesas(mesas);
                System.out.println("✅ Mesas cargadas: " + mesas.size());

                // Mostrar mesas en el panel con animación
                mostrarMesasEnPanel(salon);
            } else {
                System.err.println("❌ Error al cargar mesas: " + response.get("message"));
                Mensaje.showError("Error", "No se pudieron cargar las mesas del salón");
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
            
            // Animación de aparición
            vboxMensaje.setOpacity(0);
            FadeTransition fade = new FadeTransition(Duration.millis(500), vboxMensaje);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
            
            panelMesas.getChildren().add(vboxMensaje);
            return;
        }
        
        // Crear visualización de cada mesa con animación escalonada
        int delay = 0;
        for (Mesa mesa : salon.getMesas()) {
            StackPane mesaPane = crearVistaMesa(mesa);
            panelMesas.getChildren().add(mesaPane);
            mapaMesasVista.put(mesa.getId(), mesaPane);
            
            // Animación de entrada escalonada
            animarEntradaMesa(mesaPane, delay);
            delay += 50;
        }
    }
    
    private void animarEntradaMesa(StackPane mesaPane, int delay) {
        mesaPane.setScaleX(0);
        mesaPane.setScaleY(0);
        mesaPane.setOpacity(0);
        
        PauseTransition pause = new PauseTransition(Duration.millis(delay));
        pause.setOnFinished(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(400), mesaPane);
            scale.setFromX(0);
            scale.setFromY(0);
            scale.setToX(1);
            scale.setToY(1);
            scale.setInterpolator(Interpolator.EASE_OUT);
            
            FadeTransition fade = new FadeTransition(Duration.millis(400), mesaPane);
            fade.setFromValue(0);
            fade.setToValue(1);
            
            ParallelTransition parallel = new ParallelTransition(scale, fade);
            parallel.play();
        });
        pause.play();
    }

    private StackPane crearVistaMesa(Mesa mesa) {
        StackPane container = new StackPane();
        container.setLayoutX(mesa.getPosicionX());
        container.setLayoutY(mesa.getPosicionY());
        container.setPrefSize(90, 90);
        container.setUserData(mesa);
        
        // Fondo de la mesa según estado con gradiente
        Rectangle fondo = new Rectangle(90, 90);
        fondo.setArcWidth(15);
        fondo.setArcHeight(15);
        
        // Sombra dinámica
        DropShadow sombra = new DropShadow();
        sombra.setRadius(8);
        sombra.setOffsetY(3);
        sombra.setColor(Color.rgb(0, 0, 0, 0.2));
        fondo.setEffect(sombra);
        
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
        
        // Indicador de estado (círculo pulsante para mesas ocupadas)
        if (mesa.isOcupada() && !modoEdicion) {
            Circle indicador = new Circle(6);
            indicador.setFill(Color.web("#dc3545"));
            indicador.setTranslateX(30);
            indicador.setTranslateY(-30);
            
            DropShadow glow = new DropShadow();
            glow.setColor(Color.web("#dc3545"));
            glow.setRadius(10);
            indicador.setEffect(glow);
            
            // Animación pulsante
            ScaleTransition pulse = new ScaleTransition(Duration.millis(1000), indicador);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.3);
            pulse.setToY(1.3);
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
            
            container.getChildren().add(indicador);
        }
        
        // Imagen de la mesa si existe
        if (imagenMesaBase != null) {
            try {
                ImageView imagen = new ImageView(imagenMesaBase);
                imagen.setFitWidth(55);
                imagen.setFitHeight(55);
                imagen.setPreserveRatio(true);
                
                // Efecto de brillo sutil
                ColorAdjust ajuste = new ColorAdjust();
                ajuste.setBrightness(0.1);
                imagen.setEffect(ajuste);
                
                container.getChildren().add(imagen);
                System.out.println("✅ Imagen agregada a mesa: " + mesa.getIdentificador());
            } catch (Exception e) {
                System.err.println("❌ Error al crear ImageView: " + e.getMessage());
            }
        } else {
            System.out.println("⚠ Sin imagen base para mesa: " + mesa.getIdentificador());
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
            "-fx-border-radius: 10; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 3, 0, 0, 1);"
        );
        lblIdentificador.setTranslateY(30);
        container.getChildren().add(lblIdentificador);
        
        // Efectos hover mejorados
        container.setOnMouseEntered(e -> {
            if (!isDragging(container)) {
                animarHoverEnter(container, fondo, sombra);
            }
        });
        
        container.setOnMouseExited(e -> {
            if (!isDragging(container)) {
                animarHoverExit(container, fondo, sombra);
            }
        });
        
        // Eventos según modo
        if (modoEdicion) {
            configurarDragAndDrop(container, mesa, fondo, sombra);
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
    
    private void animarHoverEnter(StackPane container, Rectangle fondo, DropShadow sombra) {
        container.setCursor(Cursor.HAND);
        
        // Animación de escala
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), container);
        scale.setToX(1.08);
        scale.setToY(1.08);
        scale.setInterpolator(Interpolator.EASE_OUT);
        
        // Animación de sombra
        Timeline shadowAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(sombra.radiusProperty(), 8),
                new KeyValue(sombra.offsetYProperty(), 3)
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(sombra.radiusProperty(), 15, Interpolator.EASE_OUT),
                new KeyValue(sombra.offsetYProperty(), 6, Interpolator.EASE_OUT)
            )
        );
        
        scale.play();
        shadowAnim.play();
    }
    
    private void animarHoverExit(StackPane container, Rectangle fondo, DropShadow sombra) {
        container.setCursor(Cursor.DEFAULT);
        
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), container);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);
        
        Timeline shadowAnim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(sombra.radiusProperty(), 15),
                new KeyValue(sombra.offsetYProperty(), 6)
            ),
            new KeyFrame(Duration.millis(200),
                new KeyValue(sombra.radiusProperty(), 8, Interpolator.EASE_OUT),
                new KeyValue(sombra.offsetYProperty(), 3, Interpolator.EASE_OUT)
            )
        );
        
        scale.play();
        shadowAnim.play();
    }
    
    private boolean isDragging(StackPane container) {
        Object isDrag = container.getProperties().get("isDragging");
        return isDrag != null && (boolean) isDrag;
    }

    // ==================== MODO EDICIÓN (Drag & Drop) ====================
    
    private void configurarDragAndDrop(StackPane mesaPane, Mesa mesa, Rectangle fondo, DropShadow sombra) {
        final double[] dragDelta = new double[2];
        final double[] originalPos = new double[2];
        
        mesaPane.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragDelta[0] = mesaPane.getLayoutX() - e.getSceneX();
                dragDelta[1] = mesaPane.getLayoutY() - e.getSceneY();
                originalPos[0] = mesaPane.getLayoutX();
                originalPos[1] = mesaPane.getLayoutY();
                
                mesaPane.setCursor(Cursor.CLOSED_HAND);
                mesaPane.getProperties().put("isDragging", false);
                
                // Efecto visual al iniciar drag
                mesaPane.setScaleX(1.12);
                mesaPane.setScaleY(1.12);
                sombra.setRadius(20);
                sombra.setOffsetY(8);
                mesaPane.toFront();
                
                // Cambiar opacidad ligeramente
                FadeTransition fade = new FadeTransition(Duration.millis(100), mesaPane);
                fade.setToValue(0.85);
                fade.play();
            }
        });
        
        mesaPane.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                mesaPane.getProperties().put("isDragging", true);
                
                double newX = e.getSceneX() + dragDelta[0];
                double newY = e.getSceneY() + dragDelta[1];
                
                // Límites del panel con margen
                newX = Math.max(MARGEN_BORDE, Math.min(newX, panelMesas.getWidth() - MESA_SIZE - MARGEN_BORDE));
                newY = Math.max(MARGEN_BORDE, Math.min(newY, panelMesas.getHeight() - MESA_SIZE - MARGEN_BORDE));
                
                // Verificar colisiones con otras mesas
                if (!hayColision(newX, newY, mesa.getId())) {
                    mesaPane.setLayoutX(newX);
                    mesaPane.setLayoutY(newY);
                    
                    // Efecto de rastro visual
                    crearEfectoRastro(newX, newY);
                } else {
                    // Feedback visual de colisión
                    mesaPane.setEffect(new Glow(0.8));
                    PauseTransition pause = new PauseTransition(Duration.millis(100));
                    pause.setOnFinished(ev -> mesaPane.setEffect(null));
                    pause.play();
                }
            }
        });
        
        mesaPane.setOnMouseReleased(e -> {
            boolean wasDragging = isDragging(mesaPane);
            mesaPane.getProperties().put("isDragging", false);
            mesaPane.setCursor(Cursor.HAND);
            
            if (wasDragging) {
                // Animación de "snap" al soltar
                double currentX = mesaPane.getLayoutX();
                double currentY = mesaPane.getLayoutY();
                
                // Snap a grid (opcional, cada 10 píxeles)
                final double finalX = Math.round(currentX / 10) * 10;
                final double finalY = Math.round(currentY / 10) * 10;
                
                TranslateTransition snap = new TranslateTransition(Duration.millis(150), mesaPane);
                snap.setToX(finalX - currentX);
                snap.setToY(finalY - currentY);
                snap.setInterpolator(Interpolator.EASE_OUT);
                
                snap.setOnFinished(ev -> {
                    mesaPane.setLayoutX(finalX);
                    mesaPane.setLayoutY(finalY);
                    mesaPane.setTranslateX(0);
                    mesaPane.setTranslateY(0);
                    mesa.actualizarPosicion(finalX, finalY);
                    
                    System.out.println("📍 Mesa " + mesa.getIdentificador() + 
                                     " reposicionada: (" + finalX + ", " + finalY + ")");
                });
                
                // Restaurar efectos visuales
                ScaleTransition scale = new ScaleTransition(Duration.millis(200), mesaPane);
                scale.setToX(1.0);
                scale.setToY(1.0);
                scale.setInterpolator(Interpolator.EASE_OUT);
                
                FadeTransition fade = new FadeTransition(Duration.millis(200), mesaPane);
                fade.setToValue(1.0);
                
                Timeline shadowAnim = new Timeline(
                    new KeyFrame(Duration.millis(200),
                        new KeyValue(sombra.radiusProperty(), 8, Interpolator.EASE_OUT),
                        new KeyValue(sombra.offsetYProperty(), 3, Interpolator.EASE_OUT)
                    )
                );
                
                ParallelTransition parallel = new ParallelTransition(snap, scale, fade);
                parallel.play();
                shadowAnim.play();
            } else {
                // Si no se arrastró, solo restaurar efectos
                ScaleTransition scale = new ScaleTransition(Duration.millis(150), mesaPane);
                scale.setToX(1.0);
                scale.setToY(1.0);
                
                FadeTransition fade = new FadeTransition(Duration.millis(150), mesaPane);
                fade.setToValue(1.0);
                
                Timeline shadowAnim = new Timeline(
                    new KeyFrame(Duration.millis(150),
                        new KeyValue(sombra.radiusProperty(), 8, Interpolator.EASE_OUT),
                        new KeyValue(sombra.offsetYProperty(), 3, Interpolator.EASE_OUT)
                    )
                );
                
                ParallelTransition parallel = new ParallelTransition(scale, fade);
                parallel.play();
                shadowAnim.play();
            }
        });
    }
    
    private void crearEfectoRastro(double x, double y) {
        // Crear círculo de rastro que se desvanece
        Circle rastro = new Circle(5);
        rastro.setFill(Color.rgb(255, 193, 7, 0.4));
        rastro.setCenterX(x + 45);
        rastro.setCenterY(y + 45);
        
        panelMesas.getChildren().add(0, rastro); // Agregar al fondo
        
        FadeTransition fade = new FadeTransition(Duration.millis(500), rastro);
        fade.setFromValue(0.4);
        fade.setToValue(0);
        fade.setOnFinished(e -> panelMesas.getChildren().remove(rastro));
        fade.play();
    }
    
    /**
     * Verifica si una mesa en la posición (x, y) colisiona con otras mesas
     * @param x Posición X propuesta
     * @param y Posición Y propuesta
     * @param mesaIdActual ID de la mesa que se está moviendo (para excluirla)
     * @return true si hay colisión, false si está libre
     */
    private boolean hayColision(double x, double y, Long mesaIdActual) {
        // Crear rectángulo de la mesa que se está moviendo (con margen)
        double x1 = x - MARGEN_ENTRE_MESAS;
        double y1 = y - MARGEN_ENTRE_MESAS;
        double x2 = x + MESA_SIZE + MARGEN_ENTRE_MESAS;
        double y2 = y + MESA_SIZE + MARGEN_ENTRE_MESAS;
        
        // Verificar colisión con cada mesa existente
        for (Mesa otraMesa : salonActual.getMesas()) {
            // Excluir la mesa actual
            if (otraMesa.getId().equals(mesaIdActual)) {
                continue;
            }
            
            // Obtener posición de la otra mesa
            StackPane otraMesaPane = mapaMesasVista.get(otraMesa.getId());
            if (otraMesaPane == null) {
                continue;
            }
            
            double otroX = otraMesaPane.getLayoutX();
            double otroY = otraMesaPane.getLayoutY();
            
            // Rectángulo de la otra mesa
            double otroX1 = otroX;
            double otroY1 = otroY;
            double otroX2 = otroX + MESA_SIZE;
            double otroY2 = otroY + MESA_SIZE;
            
            // Detectar colisión (AABB - Axis-Aligned Bounding Box)
            boolean colisionX = x1 < otroX2 && x2 > otroX1;
            boolean colisionY = y1 < otroY2 && y2 > otroY1;
            
            if (colisionX && colisionY) {
                return true; // Hay colisión
            }
        }
        
        return false; // No hay colisión
    }

    private void configurarMenuContextual(StackPane mesaPane, Mesa mesa) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: white; -fx-padding: 5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 3);");
        
        MenuItem itemEditar = new MenuItem(I18n.isSpanish() ? "✏ Editar Identificador" : "✏ Edit Identifier");
        itemEditar.setStyle("-fx-padding: 8 15;");
        itemEditar.setOnAction(e -> editarIdentificadorMesa(mesa));
        
        MenuItem itemEliminar = new MenuItem(I18n.isSpanish() ? "🗑 Eliminar Mesa" : "🗑 Delete Table");
        itemEliminar.setStyle("-fx-padding: 8 15;");
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
                // Animación de salida antes de eliminar
                StackPane mesaPane = mapaMesasVista.get(mesa.getId());
                if (mesaPane != null) {
                    animarSalidaMesa(mesaPane, () -> {
                        salonActual.getMesas().remove(mesa);
                        mostrarMesasEnPanel(salonActual);
                    });
                }
                
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
    
    private void animarSalidaMesa(StackPane mesaPane, Runnable callback) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), mesaPane);
        scale.setToX(0);
        scale.setToY(0);
        scale.setInterpolator(Interpolator.EASE_IN);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), mesaPane);
        fade.setToValue(0);
        
        RotateTransition rotate = new RotateTransition(Duration.millis(300), mesaPane);
        rotate.setByAngle(180);
        
        ParallelTransition parallel = new ParallelTransition(scale, fade, rotate);
        parallel.setOnFinished(e -> {
            if (callback != null) callback.run();
        });
        parallel.play();
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
                "✨ Arrastre mesas para moverlas | Clic derecho: editar/eliminar | Clic derecho en panel: agregar mesa" :
                "✨ Drag tables to move | Right-click: edit/delete | Right-click on panel: add table");
            
            // Efecto de fondo en modo edición
            panelMesas.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2); " +
                "-fx-border-color: #ffc107; " +
                "-fx-border-width: 3; " +
                "-fx-border-style: dashed; " +
                "-fx-border-radius: 12;"
            );
            
            // Configurar clic derecho en panel vacío para agregar mesas
            panelMesas.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    // Verificar que no se hizo clic sobre una mesa
                    if (e.getTarget() == panelMesas) {
                        agregarNuevaMesa(e.getX(), e.getY());
                    }
                }
            });
            
            // Animación de transición a modo edición
            FadeTransition fade = new FadeTransition(Duration.millis(300), panelMesas);
            fade.setFromValue(0.7);
            fade.setToValue(1.0);
            fade.play();
        } else {
            btnModoEdicion.setText(I18n.isSpanish() ? "🛠️ Modo Edición" : "🛠️ Edit Mode");
            btnModoEdicion.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 8;");
            btnGuardarDiseno.setManaged(false);
            btnGuardarDiseno.setVisible(false);
            lblInfo.setText(I18n.isSpanish() ? 
                "💡 Haga clic en una mesa para crear una orden" :
                "💡 Click on a table to create an order");
            
            // Restaurar estilo normal
            panelMesas.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);"
            );
            
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
                // Animación de éxito
                mostrarAnimacionGuardado();
                
                Mensaje.showSuccess("Éxito", I18n.isSpanish() ? 
                    "✅ Diseño guardado correctamente." :
                    "✅ Design saved successfully.");
            } else {
                Mensaje.showError("Error", response.get("message").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "Error al guardar diseño:\n" + e.getMessage());
        }
    }
    
    private void mostrarAnimacionGuardado() {
        // Efecto flash verde en el panel
        String estiloOriginal = panelMesas.getStyle();
        
        Timeline flash = new Timeline(
            new KeyFrame(Duration.ZERO, e -> {
                panelMesas.setStyle(estiloOriginal + "-fx-border-color: #28a745; -fx-border-width: 4;");
            }),
            new KeyFrame(Duration.millis(200), e -> {
                panelMesas.setStyle(estiloOriginal);
            }),
            new KeyFrame(Duration.millis(400), e -> {
                panelMesas.setStyle(estiloOriginal + "-fx-border-color: #28a745; -fx-border-width: 4;");
            }),
            new KeyFrame(Duration.millis(600), e -> {
                panelMesas.setStyle(estiloOriginal);
            })
        );
        flash.play();
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
            
            // Ajustar posición al centro del clic y validar límites
            double posX = Math.max(MARGEN_BORDE, Math.min(x - MESA_SIZE / 2, 
                          panelMesas.getWidth() - MESA_SIZE - MARGEN_BORDE));
            double posY = Math.max(MARGEN_BORDE, Math.min(y - MESA_SIZE / 2, 
                          panelMesas.getHeight() - MESA_SIZE - MARGEN_BORDE));
            
            // Verificar que no haya colisión en la posición inicial
            if (hayColision(posX, posY, -1L)) {
                Mensaje.showWarning("Error", I18n.isSpanish() ? 
                    "No hay espacio suficiente en esa ubicación. Intente en otro lugar." : 
                    "Not enough space at that location. Try another place.");
                return;
            }
            
            try {
                Mesa nuevaMesa = new Mesa(identificador.trim(), posX, posY);
                nuevaMesa.setSalonId(salonActual.getId());
                nuevaMesa.setEstado("LIBRE");
                
                String jsonResponse = RestClient.post("/salones/" + salonActual.getId() + "/mesas", nuevaMesa);
                Map<String, Object> response = RestClient.parseResponse(jsonResponse);
                
                if (Boolean.TRUE.equals(response.get("success"))) {
                    Mensaje.showSuccess("Éxito", I18n.isSpanish() ? 
                        "✨ Mesa agregada correctamente." : 
                        "✨ Table added successfully.");
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
        // Animación de selección
        StackPane mesaPane = mapaMesasVista.get(mesa.getId());
        if (mesaPane != null) {
            animarSeleccionMesa(mesaPane);
        }
        
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
    
    private void animarSeleccionMesa(StackPane mesaPane) {
        // Animación de "pop" al seleccionar
        ScaleTransition scale1 = new ScaleTransition(Duration.millis(100), mesaPane);
        scale1.setToX(1.15);
        scale1.setToY(1.15);
        
        ScaleTransition scale2 = new ScaleTransition(Duration.millis(100), mesaPane);
        scale2.setToX(1.0);
        scale2.setToY(1.0);
        
        SequentialTransition seq = new SequentialTransition(scale1, scale2);
        seq.play();
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
            "💡 Haga clic en una mesa para crear una orden" :
            "💡 Click on a table to create an order");
        lblLibre.setText(esEspanol ? "Libre" : "Available");
        lblOcupada.setText(esEspanol ? "Ocupada" : "Occupied");
        lblEdicion.setText(esEspanol ? "Modo Edición" : "Edit Mode");
    }
}