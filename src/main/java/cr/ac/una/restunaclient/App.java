package cr.ac.una.restunaclient;
import cr.ac.una.restunaclient.util.AppContext;
import cr.ac.una.restunaclient.util.FlowController;
import javafx.application.Application;
import javafx.stage.Stage;
/**
 * Clase principal de la aplicación JavaFX
 * RestUNA - Sistema de Gestión de Restaurante
 */
public class App extends Application {
    @Override
public void start(Stage stage) {
    FlowController.getInstance().setMainStage(stage);

    // Mostrar login como modal
    FlowController.getInstance().goToViewInModal("Login", "RestUNA - Iniciar sesión", stage);

    if (AppContext.getInstance().getUsuarioLogueado() != null) {
        FlowController.getInstance().goToView("MenuPrincipal", "RestUNA - Menú Principal", 1200, 800);
        stage.show();
    } else {
        stage.close();
    }
}



    public static void main(String[] args) {
        launch(args);
    }
}