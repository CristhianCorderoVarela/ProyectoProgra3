package cr.ac.una.restunaclient.util;

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

    // Tamaños base de la app (tu layout "bonito")
    private final double BASE_WIDTH  = 1000;
    private final double BASE_HEIGHT = 560;

    // Límite mínimo permitido para que no se deforme
    private final double MIN_WIDTH   = 750;
    private final double MIN_HEIGHT  = 480;

    // Para no registrar el listener mil veces
    private boolean resizeListenerAttached = false;

    // === NUEVO: contenedor central dinámico y snapshot del home ===
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
     * Stage principal de la app (la ventana grande donde vive el menú)
     */
    public void setMainStage(Stage stage) {
        mainStage = stage;
        mainStage.setResizable(true);
        mainStage.setMinWidth(MIN_WIDTH);
        mainStage.setMinHeight(MIN_HEIGHT);
    }

    public Stage getMainStage() {
        return mainStage;
    }

    /**
     * Muestra el menú principal con el tamaño base (1000x560),
     * aplica mínimos (750x480) y el listener para restaurar tamaño
     * bonito cuando se desmaximiza.
     */
    public void showMenuPrincipal() {
        goToViewInternal("MenuPrincipal",
                "RestUNA - Menú Principal",
                BASE_WIDTH,
                BASE_HEIGHT,
                true);
    }

    /**
     * Va a una vista usando el tamaño preferido del FXML,
     * sin forzar width/height manualmente.
     */
    public void goToView(String fxmlName, String title) {
        try {
            Parent root = loadView(fxmlName);
            Scene scene = new Scene(root);

            mainStage.setScene(scene);
            mainStage.setTitle(title);

            mainStage.setMinWidth(MIN_WIDTH);
            mainStage.setMinHeight(MIN_HEIGHT);

            attachRestoreSizeListenerIfNeeded();

            mainStage.centerOnScreen();
            mainStage.show();
            mainStage.toFront();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    /**
     * Va a una vista forzando un tamaño concreto.
     */
    public void goToView(String fxmlName, String title, double width, double height) {
        goToViewInternal(fxmlName, title, width, height, false);
    }

    /**
     * Lógica compartida entre showMenuPrincipal() y goToView(...,width,height)
     */
    private void goToViewInternal(String fxmlName,
                                  String title,
                                  double width,
                                  double height,
                                  boolean forceBaseRestoreListener) {
        try {
            Parent root = loadView(fxmlName);
            Scene scene = new Scene(root, width, height);

            mainStage.setScene(scene);
            mainStage.setTitle(title);

            // tamaño inicial visible
            mainStage.setWidth(width);
            mainStage.setHeight(height);

            // mínimos para que no se deforme
            mainStage.setMinWidth(MIN_WIDTH);
            mainStage.setMinHeight(MIN_HEIGHT);

            mainStage.setResizable(true);

            if (forceBaseRestoreListener) {
                attachRestoreSizeListener(width, height);
            } else {
                attachRestoreSizeListenerIfNeeded();
            }

            mainStage.centerOnScreen();
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

            // hacerlo hijo del parent REAL que le pasamos
            stage.initOwner(parent);
            stage.initModality(Modality.WINDOW_MODAL);

            stage.initStyle(StageStyle.DECORATED);

            stage.setResizable(false);
            stage.setWidth(width);
            stage.setHeight(height);

            stage.centerOnScreen();

            // bloquea hasta que el login cierre
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
     * Carga el FXML y aplica ResourceBundle para i18n, para usarse en escenas completas.
     * Esto limpia el cache para que siempre tengas versión fresca.
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
     * Carga FXML como Node (Parent) SIN tocar la Scene del Stage.
     * Esto es para pantallas internas (Productos, Usuarios, etc.) que
     * se van a incrustar en contentPane con fade.
     *
     * A diferencia de loadView(), aquí NO limpiamos loaders,
     * porque no estamos reutilizando esos controladores globalmente.
     * (si quisieras cachear vistas internas, podrías guardarlas aparte).
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
     * Listener global (solo una vez) para que,
     * al restaurar desde maximizado, el Stage vuelva
     * al tamaño base (BASE_WIDTH x BASE_HEIGHT).
     */
    private void attachRestoreSizeListenerIfNeeded() {
        if (resizeListenerAttached) {
            return;
        }
        resizeListenerAttached = true;

        mainStage.maximizedProperty().addListener((obs, wasMax, isNowMax) -> {
            if (!isNowMax) {
                mainStage.setWidth(BASE_WIDTH);
                mainStage.setHeight(BASE_HEIGHT);
            }
        });
    }

    /**
     * Listener que restaura a un tamaño específico (width,height).
     * Lo usamos en showMenuPrincipal() para que siempre vuelva al
     * tamaño bonito que definimos ahí.
     */
    private void attachRestoreSizeListener(double width, double height) {
        if (!resizeListenerAttached) {
            resizeListenerAttached = true;

            mainStage.maximizedProperty().addListener((obs, wasMax, isNowMax) -> {
                if (!isNowMax) {
                    mainStage.setWidth(width);
                    mainStage.setHeight(height);
                }
            });
        }
        // Si ya estaba adjunto, lo dejamos.
    }

    // =========================
    // NUEVO BLOQUE: Navegación interna con fade
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
     * Carga una vista interna (por nombre FXML sin "View.fxml" completo),
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
     * Si por alguna razón no tenemos snapshot, como fallback
     * volvemos a cargar todo el menú principal con showMenuPrincipal().
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