package cr.ac.una.restunaclient.util;

import cr.ac.una.restunaclient.util.I18n;
import cr.ac.una.restunaclient.util.Mensaje;
import cr.ac.una.restunaclient.util.AppContext;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Controlador de flujo para manejar la navegación entre pantallas
 */
public class FlowController {

    private static FlowController instance;
    private static Stage mainStage;
    private static HashMap<String, FXMLLoader> loaders = new HashMap<>();

    // Tamaño base inicial pensado para tu MenuPrincipal
    private final double BASE_WIDTH  = 1000;
    private final double BASE_HEIGHT = 560;

    // Límite mínimo permitido para que no se deforme
    private final double MIN_WIDTH   = 750;
    private final double MIN_HEIGHT  = 480;

    // Último tamaño "normal" (no maximizado) recordado
    // Se usa para restaurar después de maximizar y para navegar entre pantallas
    private double lastNormalWidth  = BASE_WIDTH;
    private double lastNormalHeight = BASE_HEIGHT;

    // Para no registrar listeners más de una vez
    private boolean restoreListenerAttached = false;
    private boolean sizeTrackersAttached = false;

    // ===== Navegación interna (fade) =====
    private StackPane contentPane;
    private Node homeSnapshot;

    private FlowController() {
    }

    public static FlowController getInstance() {
        if (instance == null) {
            instance = new FlowController();
        }
        return instance;
    }

    /**
     * Stage principal de la app (la ventana grande donde vive todo)
     */
    public void setMainStage(Stage stage) {
        mainStage = stage;
        mainStage.setResizable(true);
        mainStage.setMinWidth(MIN_WIDTH);
        mainStage.setMinHeight(MIN_HEIGHT);

        // Adjuntar listeners que mantienen actualizado el tamaño "normal"
        attachSizeTrackers();
    }

    public Stage getMainStage() {
        return mainStage;
    }

    /**
     * Muestra el menú principal con el tamaño base (900x560),
     * aplica mínimos y configura restauración post-maximizar.
     *
     * También inicializa lastNormalWidth/Height por primera vez.
     */
    public void showMenuPrincipal() {
        // aseguramos que el primer "tamaño normal" sea el base
        lastNormalWidth  = BASE_WIDTH;
        lastNormalHeight = BASE_HEIGHT;

        goToViewInternal(
                "MenuPrincipal",
                "RestUNA - Menú Principal",
                BASE_WIDTH,
                BASE_HEIGHT,
                true
        );
    }

    /**
     * Va a una vista usando el tamaño preferido del FXML sin forzar width/height,
     * pero ahora es "inteligente":
     *
     * - Si la ventana está maximizada, se queda maximizada.
     * - Si NO está maximizada, se mantiene el último tamaño normal recordado.
     */
    public void goToView(String fxmlName, String title) {
        try {
            Parent root = loadView(fxmlName);

            boolean wasMaximized = mainStage.isMaximized();

            Scene scene;
            if (wasMaximized) {
                // seguimos en grande
                scene = new Scene(root, mainStage.getWidth(), mainStage.getHeight());
            } else {
                // usamos el tamaño normal recordado
                scene = new Scene(root, lastNormalWidth, lastNormalHeight);
            }

            mainStage.setScene(scene);
            mainStage.setTitle(title);

            mainStage.setMinWidth(MIN_WIDTH);
            mainStage.setMinHeight(MIN_HEIGHT);

            // conserva estado de maximizado
            mainStage.setMaximized(wasMaximized);

            attachRestoreSizeListenerIfNeeded();

            if (!mainStage.isMaximized()) {
                // solo centramos si no está maximizada
                mainStage.centerOnScreen();
            }

            mainStage.show();
            mainStage.toFront();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    /**
     * Va a una vista forzando un tamaño concreto.
     *
     * - Si el stage está MAXIMIZADO: mantén maximizado.
     * - Si NO: aplica ese width/height y actualiza el tamaño normal recordado.
     */
    public void goToView(String fxmlName, String title, double width, double height) {
        goToViewInternal(fxmlName, title, width, height, false);
    }

    /**
     * Lógica compartida entre showMenuPrincipal() y goToView(...,width,height)
     *
     * forceBaseSize:
     *   true  = este tamaño pasa a ser el tamaño "normal" inicial (p.ej. menú principal)
     *   false = úsalo solo si no estás maximizado
     */
    private void goToViewInternal(String fxmlName,
                                  String title,
                                  double width,
                                  double height,
                                  boolean forceBaseSize) {
        try {
            Parent root = loadView(fxmlName);

            boolean wasMaximized = mainStage.isMaximized();

            Scene scene;
            if (wasMaximized) {
                // mantener tamaños grandes actuales si estaba maximizada
                scene = new Scene(root, mainStage.getWidth(), mainStage.getHeight());
            } else {
                // usar el size solicitado explícitamente
                scene = new Scene(root, width, height);

                // si estamos forzando tamaño base (primera carga) o navegando sin maximizar,
                // este pasa a ser el último tamaño normal conocido
                if (forceBaseSize) {
                    lastNormalWidth  = width;
                    lastNormalHeight = height;
                } else {
                    lastNormalWidth  = width;
                    lastNormalHeight = height;
                }
            }

            mainStage.setScene(scene);
            mainStage.setTitle(title);

            mainStage.setMinWidth(MIN_WIDTH);
            mainStage.setMinHeight(MIN_HEIGHT);

            if (wasMaximized) {
                mainStage.setMaximized(true);
            } else {
                mainStage.setMaximized(false);
                mainStage.setWidth(lastNormalWidth);
                mainStage.setHeight(lastNormalHeight);
            }

            mainStage.setResizable(true);

            attachRestoreSizeListenerIfNeeded();

            if (!mainStage.isMaximized()) {
                mainStage.centerOnScreen();
            }

            mainStage.show();
            mainStage.toFront();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    /**
     * Modal NORMAL (redimensionable según FXML).
     */
    public Stage goToViewInModal(String fxmlName, String title, Stage parent) {
        try {
            Parent root = loadView(fxmlName);
            Scene scene = new Scene(root);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(parent);
            stage.initStyle(StageStyle.DECORATED);
            stage.centerOnScreen();
            stage.showAndWait();
            return stage;
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista modal: " + fxmlName + "\n" + e.getMessage());
            return null;
        }
    }

    /**
     * Modal con tamaño fijo y sin permitir resize.
     * Usado para Login, por ejemplo.
     */
    public Stage goToViewInModalFixed(String fxmlName,
                                      String title,
                                      Stage parent,
                                      double width,
                                      double height) {
        try {
            Parent root = loadView(fxmlName);

            Scene scene = new Scene(root, width, height);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle(title);

            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initStyle(StageStyle.DECORATED);

            stage.setResizable(false);
            stage.setWidth(width);
            stage.setHeight(height);

            stage.centerOnScreen();

            // bloquea hasta que cierre
            stage.showAndWait();

            return stage;
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError(
                    "Error",
                    "No se pudo cargar la vista modal: " + fxmlName + "\n" + e.getMessage()
            );
            return null;
        }
    }

    /**
     * Carga el FXML y aplica ResourceBundle para escenas completas.
     * Limpia cache para tener siempre versiones frescas.
     */
    private Parent loadView(String fxmlName) throws IOException {
        // limpiar cache para tener versión fresca
        loaders.remove(fxmlName);

        FXMLLoader loader = new FXMLLoader();

        String fxmlPath = "/cr/ac/una/restunaclient/view/" + fxmlName + "View.fxml";
        System.out.println("Intentando cargar: " + fxmlPath);

        loader.setLocation(FlowController.class.getResource(fxmlPath));
        if (loader.getLocation() == null) {
            throw new IOException("No se pudo encontrar el archivo FXML: " + fxmlPath);
        }

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "cr.ac.una.restunaclient.i18n.messages",
                    I18n.getCurrentLocale()
            );
            loader.setResources(bundle);
        } catch (Exception e) {
            System.out.println("Advertencia: No se pudo cargar el ResourceBundle: " + e.getMessage());
        }

        Parent root = loader.load();
        loaders.put(fxmlName, loader);
        return root;
    }

    /**
     * Carga FXML como Node SIN tocar la Scene del Stage.
     * Para pantallas internas con fade.
     */
    private Parent loadViewNode(String fxmlName) throws IOException {
        FXMLLoader loader = new FXMLLoader();

        String fxmlPath = "/cr/ac/una/restunaclient/view/" + fxmlName + "View.fxml";
        System.out.println("Intentando cargar vista interna: " + fxmlPath);

        loader.setLocation(FlowController.class.getResource(fxmlPath));

        try {
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "cr.ac.una.restunaclient.i18n.messages",
                    I18n.getCurrentLocale()
            );
            loader.setResources(bundle);
        } catch (Exception e) {
            System.out.println("Advertencia: No se pudo cargar ResourceBundle para " + fxmlName);
        }

        return loader.load();
    }

    /**
     * Devuelve el controlador ya cargado vía loadView(...)
     */
    public Object getController(String fxmlName) {
        if (loaders.containsKey(fxmlName)) {
            return loaders.get(fxmlName).getController();
        }
        return null;
    }

    /**
     * Limpia caché de loaders
     */
    public void clearCache() {
        loaders.clear();
    }

    /**
     * Cierra la aplicación ordenadamente
     */
    public void exitApp() {
        AppContext.getInstance().logout();
        System.exit(0);
    }

    /**
     * Listener global:
     * cuando el usuario deja de estar maximizado,
     * restauramos el último tamaño normal "bueno".
     */
    private void attachRestoreSizeListenerIfNeeded() {
        if (restoreListenerAttached) {
            return;
        }
        restoreListenerAttached = true;

        mainStage.maximizedProperty().addListener((obs, wasMax, isNowMax) -> {
            if (!isNowMax) {
                // restaurar el tamaño normal más reciente
                mainStage.setWidth(lastNormalWidth);
                mainStage.setHeight(lastNormalHeight);
                mainStage.centerOnScreen();
            }
        });
    }

    /**
     * (Compatibilidad con tu versión anterior.)
     * Antes devolvía siempre width/height fijos.
     * Ahora lo que hacemos es actualizar nuestro "tamaño normal"
     * inicial y luego delegar al listener normal.
     */
    private void attachRestoreSizeListener(double width, double height) {
        // actualizar tamaño normal base
        lastNormalWidth  = width;
        lastNormalHeight = height;
        attachRestoreSizeListenerIfNeeded();
    }

    /**
     * Trackers: guardan lastNormalWidth/Height cuando el usuario
     * está en modo normal (no maximizado) y redimensiona manualmente.
     * Así ese tamaño se mantiene entre pantallas.
     */
    private void attachSizeTrackers() {
        if (sizeTrackersAttached) {
            return;
        }
        sizeTrackersAttached = true;

        mainStage.widthProperty().addListener((obs, oldW, newW) -> {
            if (!mainStage.isMaximized()) {
                lastNormalWidth = newW.doubleValue();
            }
        });

        mainStage.heightProperty().addListener((obs, oldH, newH) -> {
            if (!mainStage.isMaximized()) {
                lastNormalHeight = newH.doubleValue();
            }
        });
    }

    // =========================
    // Navegación interna con fade
    // =========================

    /**
     * El MenuPrincipalController nos pasa el StackPane central donde
     * debemos meter las subpantallas.
     */
    public void setContentPane(StackPane pane) {
        this.contentPane = pane;
    }

    /**
     * Guardamos el dashboard inicial (homeView) para poder volver luego
     * con animación sin recargar toda la escena.
     */
    public void captureHomeSnapshot(Node homeRoot) {
        this.homeSnapshot = homeRoot;
    }

    /**
     * Carga una vista interna (por nombre FXML sin "View.fxml"),
     * la pone dentro de contentPane y hace fade-in.
     *
     * Ejemplo:
     * FlowController.getInstance().swapCenterContent("Productos");
     */
    public void swapCenterContent(String fxmlName) {
        if (contentPane == null) {
            System.out.println("swapCenterContent llamado pero contentPane es null.");
            return;
        }

        try {
            Parent newContent = loadViewNode(fxmlName);

            contentPane.getChildren().clear();
            contentPane.getChildren().add(newContent);

            FadeTransition ft = new FadeTransition(Duration.millis(200), newContent);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

        } catch (IOException ex) {
            ex.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista interna: " + fxmlName + "\n" + ex.getMessage());
        }
    }

    /**
     * Regresa al dashboard inicial (homeSnapshot) con fade-in.
     * Si no hay snapshot por alguna razón, reabre el menú principal completo.
     */
    public void goHomeWithFade() {
        if (contentPane == null || homeSnapshot == null) {
            // fallback seguro
            showMenuPrincipal();
            return;
        }

        contentPane.getChildren().clear();
        contentPane.getChildren().add(homeSnapshot);

        FadeTransition ft = new FadeTransition(Duration.millis(200), homeSnapshot);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }
}