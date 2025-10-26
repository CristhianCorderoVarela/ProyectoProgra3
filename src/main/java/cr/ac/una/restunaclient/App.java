package cr.ac.una.restunaclient;
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
        // Configurar el FlowController con el stage principal
        FlowController.getInstance().setMainStage(stage);

        // Navegar a la vista de login
        FlowController.getInstance().goToView("Login", "RestUNA - Login", 1024, 768);
    }
    public static void main(String[] args) {
        launch(args);
    }
}