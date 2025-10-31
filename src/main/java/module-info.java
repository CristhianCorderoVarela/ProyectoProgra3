module cr.ac.una.restunaclient {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    
    // HTTP Client
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;
    
    // Gson
    requires com.google.gson;
    
    // ControlsFX
    requires org.controlsfx.controls;
    requires java.base;
    requires java.desktop;
    
    // Opens para reflexión de JavaFX
    opens cr.ac.una.restunaclient to javafx.fxml;
    opens cr.ac.una.restunaclient.controller to javafx.fxml;  // ← AGREGAR ESTA LÍNEA
    opens cr.ac.una.restunaclient.util to javafx.fxml;
    
    // Opens para Gson (para serializar/deserializar tus modelos)
    opens cr.ac.una.restunaclient.model to com.google.gson;
    
    
    // Exports
    exports cr.ac.una.restunaclient;
    exports cr.ac.una.restunaclient.controller;  // ← TAMBIÉN AGREGAR ESTA (opcional pero recomendado)
    exports cr.ac.una.restunaclient.model;
    exports cr.ac.una.restunaclient.service;
    exports cr.ac.una.restunaclient.util;
}
    

