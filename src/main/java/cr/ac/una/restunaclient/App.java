package cr.ac.una.restunaclient;

import cr.ac.una.restunaclient.util.FlowController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Clase principal de la aplicación JavaFX
 * RestUNA - Sistema de Gestión de Restaurante
 */
public class App extends Application {
    @Override
    public void start(Stage stage) {
        // Evita que la app se cierre si se cierra el Login antes de mostrar el mainStage
        Platform.setImplicitExit(false);

        // Configurar el Stage principal (no se muestra todavía)
        FlowController.getInstance().setMainStage(stage);

        // Mostrar el Login en una ventana aparte (Stage distinto)
        FlowController.getInstance().showLoginWindow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}