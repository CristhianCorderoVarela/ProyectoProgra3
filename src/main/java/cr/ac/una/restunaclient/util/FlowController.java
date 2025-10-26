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
    private double lastNormalWidth  = BASE_WIDTH;
    private double lastNormalHeight = BASE_HEIGHT;

    // Para no registrar listeners más de una vez
    private boolean restoreListenerAttached = false;
    private boolean sizeTrackersAttached = false;

    // ===== Navegación interna (fade) =====
    private StackPane contentPane;
    private Node homeSnapshot;

    private FlowController() {}

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
        attachSizeTrackers(); // recuerda tamaño normal cuando no está maximizada
    }

    public Stage getMainStage() {
        return mainStage;
    }

    /**
     * Muestra el menú principal. En la primera carga fija tamaño base.
     * Si ya existe escena, solo cambia el root para no alterar el tamaño actual.
     */
    public void showMenuPrincipal() {
        try {
            Parent root = loadView("MenuPrincipal");

            boolean firstShow = (mainStage.getScene() == null);
            if (firstShow) {
                // primera vez: crea scene con tamaño base
                Scene scene = new Scene(root, BASE_WIDTH, BASE_HEIGHT);
                mainStage.setScene(scene);
                mainStage.setTitle("RestUNA - Menú Principal");
                lastNormalWidth  = BASE_WIDTH;
                lastNormalHeight = BASE_HEIGHT;
                attachRestoreSizeListenerIfNeeded();
                mainStage.centerOnScreen();
                mainStage.show();
            } else {
                // ya hay escena: preserva tamaño/estado actual
                swapRootPreservingStage(root, "RestUNA - Menú Principal");
            }
            mainStage.toFront();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: MenuPrincipal\n" + e.getMessage());
        }
    }

    /**
     * Va a una vista usando el tamaño preferido del FXML, preservando
     * el tamaño/estado actual del Stage (maximizado o manual).
     */
    public void goToView(String fxmlName, String title) {
        try {
            Parent root = loadView(fxmlName);
            // si no existe escena aún (caso raro), créala con el último tamaño normal
            if (mainStage.getScene() == null) {
                Scene scene = new Scene(root, lastNormalWidth, lastNormalHeight);
                mainStage.setScene(scene);
                mainStage.setTitle(title);
                attachRestoreSizeListenerIfNeeded();
                mainStage.show();
            } else {
                swapRootPreservingStage(root, title);
            }
            mainStage.toFront();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    /**
     * Va a una vista forzando un tamaño concreto SOLO si aún no hay Scene.
     * Si ya hay Scene, se preserva el tamaño/estado actual del Stage.
     */
    public void goToView(String fxmlName, String title, double width, double height) {
        try {
            Parent root = loadView(fxmlName);

            if (mainStage.getScene() == null) {
                // No hay escena: crea con el tamaño solicitado
                Scene scene = new Scene(root, width, height);
                mainStage.setScene(scene);
                mainStage.setTitle(title);
                mainStage.setMinWidth(MIN_WIDTH);
                mainStage.setMinHeight(MIN_HEIGHT);
                mainStage.setResizable(true);
                lastNormalWidth  = width;
                lastNormalHeight = height;
                attachRestoreSizeListenerIfNeeded();
                mainStage.centerOnScreen();
                mainStage.show();
            } else {
                // Ya hay escena: preserva tamaño/estado (no lo pisamos)
                swapRootPreservingStage(root, title);
            }
            mainStage.toFront();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    /**
     * Reemplaza el root de la Scene actual preservando:
     * - tamaño del Stage
     * - estado maximizado
     * - mínimos y resizable
     */
    private void swapRootPreservingStage(Parent newRoot, String title) {
        boolean wasMaximized = mainStage.isMaximized();
        double currentW = mainStage.getWidth();
        double currentH = mainStage.getHeight();

        // Si por alguna razón llegan 0s (algunas plataformas previas al show),
        // usa el último tamaño normal recordado.
        if (currentW <= 0) currentW = lastNormalWidth;
        if (currentH <= 0) currentH = lastNormalHeight;

        Scene scene = mainStage.getScene();
        if (scene == null) {
            scene = new Scene(newRoot, currentW, currentH);
            mainStage.setScene(scene);
        } else {
            scene.setRoot(newRoot);
        }

        mainStage.setTitle(title);
        mainStage.setMinWidth(MIN_WIDTH);
        mainStage.setMinHeight(MIN_HEIGHT);
        mainStage.setResizable(true);

        // restablece estado/tamaño exactamente como estaba
        mainStage.setMaximized(wasMaximized);
        if (!wasMaximized) {
            mainStage.setWidth(currentW);
            mainStage.setHeight(currentH);
        }

        attachRestoreSizeListenerIfNeeded();
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
        loaders.remove(fxmlName); // fuerza recarga fresca

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
                mainStage.setWidth(lastNormalWidth);
                mainStage.setHeight(lastNormalHeight);
                mainStage.centerOnScreen();
            }
        });
    }

    /**
     * (Compatibilidad con tu versión anterior.)
     * Actualiza el "tamaño normal" base y adjunta listener.
     */
    private void attachRestoreSizeListener(double width, double height) {
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

    public void setContentPane(StackPane pane) {
        this.contentPane = pane;
    }

    public void captureHomeSnapshot(Node homeRoot) {
        this.homeSnapshot = homeRoot;
    }

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

    public void goHomeWithFade() {
        if (contentPane == null || homeSnapshot == null) {
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