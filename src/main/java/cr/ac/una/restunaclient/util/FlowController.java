package cr.ac.una.restunaclient.util;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Controlador de flujo para manejar la navegación entre pantallas
 */
public class FlowController {

    private static FlowController instance;
    private static Stage mainStage;
    private static HashMap<String, FXMLLoader> loaders = new HashMap<>();

    // Contenedor opcional para embebido en una sola ventana (no usado si navegas por escenas)
    private Pane contentHost;

    // Stage del login (para poder cerrarlo tras autenticación)
    private Stage loginStage;

    private FlowController() {}

    public static FlowController getInstance() {
        if (instance == null) {
            instance = new FlowController();
        }
        return instance;
    }

    public void setMainStage(Stage stage) {
    mainStage = stage;
    mainStage.setResizable(true);
    // 👇 Evita que “aplasten” tu layout
    mainStage.setMinWidth(1100);
    mainStage.setMinHeight(700);
}

    /** Devuelve el Stage principal */
    public Stage getMainStage() {
        return mainStage;
    }

    // ==========================
    //  API estilo "UNA Planilla"
    // ==========================

    public void showLoginWindow() {
    try {
        Parent content = loadView("Login");
        double baseW = 1024, baseH = 768;
        double initialW = 850, initialH = 638; // ✅ escala 1:1

        loginStage = new Stage();
        Scene scene = createScaledScene(content, initialW, initialH, baseW, baseH);
        loginStage.setScene(scene);
        loginStage.setTitle("RestUNA - Login");
        loginStage.setResizable(true);
        loginStage.centerOnScreen();
        loginStage.show();
    } catch (Exception e) {
        e.printStackTrace();
        Mensaje.showError("Error", "No se pudo abrir la ventana de Login.\n" + e.getMessage());
    }
}

    public void showLoginModal() {
    try {
        Parent content = loadView("Login");
        double baseW = 1024, baseH = 768;
        double initialW = 850, initialH = 638; // ✅ escala 1:1

        if (loginStage != null) { try { loginStage.close(); } catch (Exception ignore) {} }

        loginStage = new Stage();
        Scene scene = createScaledScene(content, initialW, initialH, baseW, baseH);
        loginStage.setScene(scene);
        loginStage.setTitle("RestUNA - Login");
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setResizable(true);
        Platform.setImplicitExit(false);

        loginStage.showAndWait();

        if (AppContext.getInstance().getUsuarioLogueado() == null) {
            exitApp();
        }
    } catch (Exception e) {
        e.printStackTrace();
        Mensaje.showError("Error", "No se pudo abrir la ventana de Login (modal).\n" + e.getMessage());
    }
}

    /** Oculta el main y relanza el login modal (para logout) */
    public void startLogoutFlow() {
        if (mainStage != null && mainStage.isShowing()) {
            mainStage.hide();
        }
        showLoginModal();
    }

    public void openMainFromLoginAndClose(Scene anySceneInLogin) {
    // ✅ inicial = base (1200x800) para que el primer render del menú sea 1:1 y NO borroso
    goToViewWithFadeScaled("MenuPrincipal", "RestUNA - Menú Principal",
            1200, 800,
            1050, 700
    );

    // cerrar login (igual a tu código actual) ...
    try {
        if (anySceneInLogin != null && anySceneInLogin.getWindow() instanceof Stage stage) {
            if (stage != mainStage) stage.close();
        } else if (loginStage != null && loginStage.isShowing()) {
            loginStage.close();
        }
    } catch (Exception ignore) {
    } finally {
        loginStage = null;
    }

    Platform.setImplicitExit(true);
    if (!mainStage.isShowing()) mainStage.show();
    mainStage.toFront();
}

    // ==========================
    //  Navegación normal (no escalada)
    // ==========================

    public void goToView(String fxmlName, String title) {
        try {
            Parent root = loadView(fxmlName);
            Scene scene;
            if (mainStage != null && mainStage.getScene() != null) {
                double w = mainStage.getScene().getWidth();
                double h = mainStage.getScene().getHeight();
                scene = new Scene(root, w, h);
            } else {
                scene = new Scene(root);
            }
            mainStage.setScene(scene);
            mainStage.setTitle(title);
            mainStage.centerOnScreen();
            mainStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    public void goToView(String fxmlName, String title, double width, double height) {
        try {
            Parent root = loadView(fxmlName);
            Scene scene = new Scene(root, width, height);
            mainStage.setScene(scene);
            mainStage.setTitle(title);
            mainStage.setWidth(width);
            mainStage.setHeight(height);
            mainStage.centerOnScreen();
            mainStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: " + fxmlName + "\n" + e.getMessage());
        }
    }

    // Reemplaza TODO el método por este
public void goToViewKeepSize(String fxmlName, String title) {
    try {
        if (mainStage == null) throw new IllegalStateException("mainStage no ha sido configurado.");
        double w = (mainStage.getScene() != null) ? mainStage.getScene().getWidth() : mainStage.getWidth();
        double h = (mainStage.getScene() != null) ? mainStage.getScene().getHeight() : mainStage.getHeight();
        if (w <= 0 || h <= 0) { w = 1200; h = 800; }

        Parent newRoot = loadView(fxmlName);
        if (mainStage.getScene() == null) {
            Scene scene = new Scene(newRoot, w, h);
            mainStage.setScene(scene);
            mainStage.setTitle(title);
            mainStage.centerOnScreen();
            mainStage.show();
            playFadeIn(newRoot, 140); // fade-in simple al primer montaje
            return;
        }

        // ⬇️ Aquí el cambio: setRoot + fade-in (sin fade-out/cross-fade)
        mainStage.setTitle(title);
        Scene scene = mainStage.getScene();
        newRoot.setOpacity(0.0);
        scene.setRoot(newRoot);
        playFadeIn(newRoot, 140);

    } catch (Exception e) {
        e.printStackTrace();
        Mensaje.showError("Error", "No se pudo cargar la vista (keep size): " + fxmlName + "\n" + e.getMessage());
    }
}

    public void goToViewWithFade(String fxmlName, String title, double width, double height) {
        try {
            if (mainStage == null) throw new IllegalStateException("mainStage no ha sido configurado.");
            Parent newRoot = loadView(fxmlName);

            if (mainStage.getScene() == null) {
                Scene scene = new Scene(newRoot, width, height);
                mainStage.setScene(scene);
                mainStage.setTitle(title);
                mainStage.setWidth(width);
                mainStage.setHeight(height);
                mainStage.centerOnScreen();
                mainStage.show();
                playFadeIn(newRoot, 140);
                return;
            }
            mainStage.setTitle(title);
            mainStage.setWidth(width);
            mainStage.setHeight(height);
            mainStage.centerOnScreen();
            crossFadeReplaceRoot(mainStage.getScene(), newRoot, 110);
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista (fade+resize): " + fxmlName + "\n" + e.getMessage());
        }
    }

    // ==========================
    //  Navegación RESPONSIVE (escalada)
    // ==========================

    /** Crea una Scene con escala proporcional (contenido dentro de Group) */
    private Scene createScaledScene(Parent content, double initialW, double initialH, double baseW, double baseH) {
        Group scalableGroup = new Group(content);
        StackPane wrapper = new StackPane(scalableGroup); // centra el contenido
        Scene scene = new Scene(wrapper, initialW, initialH);

        var scale = Bindings.min(
                scene.widthProperty().divide(baseW),
                scene.heightProperty().divide(baseH)
        );
        scalableGroup.scaleXProperty().bind(scale);
        scalableGroup.scaleYProperty().bind(scale);

        wrapper.setMinSize(0, 0);
        return scene;
    }

    /** Busca el Group escalable dentro de la Scene responsive */
    private Group findScalableGroup(Scene scene) {
        if (scene == null) return null;
        if (scene.getRoot() instanceof StackPane sp && !sp.getChildren().isEmpty() && sp.getChildren().get(0) instanceof Group g) {
            return g;
        }
        return null;
    }

    /** Reemplaza el contenido dentro del Group escalable, con fade suave */
    private void crossFadeReplaceScaledContent(Scene scene, Parent newContent, int baseMillis) {
        Group g = findScalableGroup(scene);
        if (g == null) {
            // Si por alguna razón no hay Group (no responsive), caemos al root normal
            crossFadeReplaceRoot(scene, newContent, baseMillis);
            return;
        }

        Node old = g.getChildren().isEmpty() ? null : g.getChildren().get(0);
        if (old == null) {
            g.getChildren().setAll(newContent);
            playFadeIn(newContent, Math.max(baseMillis, 100));
            return;
        }

        StackPane stack = new StackPane(old, newContent);
        newContent.setOpacity(0.0);
        g.getChildren().setAll(stack);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(baseMillis), old);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(baseMillis + 20), newContent);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition pt = new ParallelTransition(fadeOut, fadeIn);
        pt.setOnFinished(e -> g.getChildren().setAll(newContent));
        pt.play();
    }

    /** Abre vista escalada fijando tamaño de ventana (ideal Login -> MenuPrincipal) */
    public void goToViewWithFadeScaled(String fxmlName, String title,
                                       double baseW, double baseH,
                                       double initialW, double initialH) {
        try {
            if (mainStage == null) throw new IllegalStateException("mainStage no ha sido configurado.");
            Parent newContent = loadView(fxmlName);

            if (mainStage.getScene() == null) {
                Scene scene = createScaledScene(newContent, initialW, initialH, baseW, baseH);
                mainStage.setScene(scene);
    mainStage.setTitle(title);

    
    mainStage.sizeToScene();

    mainStage.centerOnScreen();
    mainStage.show();
                // el Group ya está escalado, solo fade-in del contenido actual
                playFadeIn(findScalableGroup(scene), 140);
                return;
            }

            mainStage.setTitle(title);
            mainStage.centerOnScreen();
            // Usar fade-out -> swap -> fade-in (misma sensación en todo el flujo)
            fadeSwapReplaceScaledContent(mainStage.getScene(), newContent, 140);

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista responsive (fade+resize): " + fxmlName + "\n" + e.getMessage());
        }
    }

    // Reemplaza TODO el método por este
public void goToViewKeepSizeScaled(String fxmlName, String title, double baseW, double baseH) {
    try {
        if (mainStage == null) throw new IllegalStateException("mainStage no ha sido configurado.");
        Parent newContent = loadView(fxmlName);

        if (mainStage.getScene() == null) {
            // Primera vez: crea escena responsive y aplica fade-in simple
            double w = mainStage.getWidth() > 0 ? mainStage.getWidth() : baseW;
            double h = mainStage.getHeight() > 0 ? mainStage.getHeight() : baseH;
            Scene scene = createScaledScene(newContent, w, h, baseW, baseH);
            mainStage.setScene(scene);
            mainStage.setTitle(title);
            mainStage.centerOnScreen();
            mainStage.show();
            Group g = findScalableGroup(scene);
            if (g != null) playFadeIn(g, 140); else playFadeIn(newContent, 140);
            return;
        }

        // Si la escena actual NO es responsive, conviértela una única vez
        if (findScalableGroup(mainStage.getScene()) == null) {
            Scene old = mainStage.getScene();
            double w = old.getWidth(), h = old.getHeight();
            Scene scene = createScaledScene((Parent) old.getRoot(), w, h, baseW, baseH);
            mainStage.setScene(scene);
        }

        // ⬇️ Aquí el cambio: reemplazo del contenido escalado con solo fade-in
        mainStage.setTitle(title);
        Scene scene = mainStage.getScene();
        Group g = findScalableGroup(scene);
        if (g == null) {
            newContent.setOpacity(0.0);
            scene.setRoot(newContent);
            playFadeIn(newContent, 140);
        } else {
            newContent.setOpacity(0.0);
            g.getChildren().setAll(newContent);
            playFadeIn(newContent, 140);
        }

    } catch (Exception e) {
        e.printStackTrace();
        Mensaje.showError("Error", "No se pudo cargar la vista responsive (keep size): " + fxmlName + "\n" + e.getMessage());
    }
}

    // ================== Modales y carga ==================

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

    // MISMA TRANSICIÓN QUE EL INGRESO (fade-out -> swap -> fade-in) dentro de la escena responsive
    private void fadeSwapReplaceScaledContent(Scene scene, Parent newContent, int millis) {
        Group g = findScalableGroup(scene);
        if (g == null) {
            // Si no es responsive, usa el cross-fade actual como fallback
            crossFadeReplaceRoot(scene, newContent, millis);
            return;
        }

        Node old = g.getChildren().isEmpty() ? null : g.getChildren().get(0);
        if (old == null) {
            newContent.setOpacity(0.0);
            g.getChildren().setAll(newContent);
            playFadeIn(newContent, Math.max(millis, 100));
            return;
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(millis), old);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_BOTH);
        fadeOut.setOnFinished(ev -> {
            g.getChildren().setAll(newContent);
            newContent.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(millis), newContent);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setInterpolator(Interpolator.EASE_BOTH);
            fadeIn.play();
        });
        fadeOut.play();
    }

    /** Carga una vista FXML */
    private Parent loadView(String fxmlName) throws IOException {
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

    public Object getController(String fxmlName) {
        if (loaders.containsKey(fxmlName)) {
            return loaders.get(fxmlName).getController();
        }
        return null;
    }

    public void clearCache() {
        loaders.clear();
    }

    public void exitApp() {
        AppContext.getInstance().logout();
        System.exit(0);
    }

    public Parent loadViewNode(String fxmlName) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        String fxmlPath = "/cr/ac/una/restunaclient/view/" + fxmlName + "View.fxml";
        loader.setLocation(FlowController.class.getResource(fxmlPath));
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

    public void setContentHost(Pane pane) { this.contentHost = pane; }

    public void showInContent(String fxmlName) {
        try {
            if (contentHost == null) throw new IllegalStateException("No se ha configurado el contentHost en FlowController.");
            Parent view = loadView(fxmlName);

            Node old = contentHost.getChildren().isEmpty() ? null : contentHost.getChildren().get(0);
            if (old == null) {
                contentHost.getChildren().setAll(view);
                playFadeIn(view, 140);
                return;
            }

            StackPane stack = new StackPane(old, view);
            view.setOpacity(0.0);
            contentHost.getChildren().setAll(stack);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(100), old);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setInterpolator(Interpolator.EASE_BOTH);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(120), view);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.setInterpolator(Interpolator.EASE_BOTH);

            ParallelTransition pt = new ParallelTransition(fadeOut, fadeIn);
            pt.setOnFinished(e -> contentHost.getChildren().setAll(view));

            if (view instanceof AnchorPane) {
                AnchorPane.setTopAnchor(view, 0.0);
                AnchorPane.setRightAnchor(view, 0.0);
                AnchorPane.setBottomAnchor(view, 0.0);
                AnchorPane.setLeftAnchor(view, 0.0);
            }

            pt.play();

        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista embebida: " + fxmlName + "\n" + e.getMessage());
        }
    }

    // ================== Helpers de transición ==================
    private void crossFadeReplaceRoot(Scene scene, Parent newRoot, int baseMillis) {
        Node oldRoot = scene.getRoot();
        if (oldRoot == null) {
            scene.setRoot(newRoot);
            playFadeIn(newRoot, Math.max(baseMillis, 100));
            return;
        }

        StackPane stack = new StackPane();
        stack.getChildren().addAll(oldRoot, newRoot);
        newRoot.setOpacity(0.0);
        scene.setRoot(stack);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(baseMillis), oldRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(baseMillis + 20), newRoot);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setInterpolator(Interpolator.EASE_BOTH);

        ParallelTransition pt = new ParallelTransition(fadeOut, fadeIn);
        pt.setOnFinished(e -> {
            stack.getChildren().clear();
            scene.setRoot(newRoot);
        });
        pt.play();
    }

    private void playFadeIn(Node node, int millis) {
    if (node == null) return;
    node.setOpacity(0.0);
    // node.setCache(false); // opcional: explícito
    FadeTransition fadeIn = new FadeTransition(Duration.millis(millis), node);
    fadeIn.setFromValue(0.0);
    fadeIn.setToValue(1.0);
    fadeIn.setInterpolator(Interpolator.EASE_BOTH);
    fadeIn.setOnFinished(ev -> {
        node.setOpacity(1.0);
        node.setCache(false); // ✅ asegura nitidez post-animación
    });
    fadeIn.play();
}
}