package cr.ac.una.restunaclient.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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
    
    private FlowController() {
    }
    
    /**
     * Obtiene la instancia única del FlowController
     */
    public static FlowController getInstance() {
        if (instance == null) {
            instance = new FlowController();
        }
        return instance;
    }
    
    /**
     * Establece el Stage principal de la aplicación
     */
    public void setMainStage(Stage stage) {
        mainStage = stage;
        mainStage.setResizable(false); // Opcional: evitar redimensionamiento
    }
    
    /**
     * Obtiene el Stage principal
     */
    public Stage getMainStage() {
        return mainStage;
    }
    
    /**
     * Navega a una nueva vista en el Stage principal
     * @param fxmlName Nombre del archivo FXML (sin .fxml)
     * @param title Título de la ventana
     */
    public void goToView(String fxmlName, String title) {
        try {
            Parent root = loadView(fxmlName);
            Scene scene = new Scene(root);
            mainStage.setScene(scene);
            mainStage.setTitle(title);
            mainStage.centerOnScreen();
            mainStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Mensaje.showError("Error", "No se pudo cargar la vista: " + fxmlName + "\n" + e.getMessage());
        }
    }
    
    /**
     * Navega a una nueva vista con tamaño específico
     * @param fxmlName Nombre del archivo FXML
     * @param title Título de la ventana
     * @param width Ancho de la ventana
     * @param height Alto de la ventana
     */
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
    
    /**
     * Abre una vista en una ventana modal
     * @param fxmlName Nombre del archivo FXML
     * @param title Título de la ventana
     * @param parent Stage padre
     * @return Stage de la ventana modal
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
     * Carga una vista FXML
     * @param fxmlName Nombre del archivo FXML (sin .fxml)
     * @return Parent root de la vista
     */
    private Parent loadView(String fxmlName) throws IOException {
        // Limpiar cache para siempre cargar la vista fresca
        loaders.remove(fxmlName);
        
        FXMLLoader loader = new FXMLLoader();
        
        // Ruta corregida: buscar en resources/view/
        String fxmlPath = "/cr/ac/una/restunaclient/view/" + fxmlName + "View.fxml";
        
        System.out.println("Intentando cargar: " + fxmlPath);
        
        loader.setLocation(FlowController.class.getResource(fxmlPath));
        
        if (loader.getLocation() == null) {
            throw new IOException("No se pudo encontrar el archivo FXML: " + fxmlPath);
        }
        
        // ⭐ AGREGAR ESTA LÍNEA - Cargar el ResourceBundle para traducciones
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
     * Obtiene el controlador de una vista cargada
     * @param fxmlName Nombre de la vista
     * @return Controlador de la vista
     */
    public Object getController(String fxmlName) {
        if (loaders.containsKey(fxmlName)) {
            return loaders.get(fxmlName).getController();
        }
        return null;
    }
    
    /**
     * Limpia la caché de loaders
     */
    public void clearCache() {
        loaders.clear();
    }
    
    /**
     * Cierra la aplicación
     */
    public void exitApp() {
        AppContext.getInstance().logout();
        System.exit(0);
    }
}