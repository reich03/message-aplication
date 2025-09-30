package com.messaging.client;

import com.messaging.client.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Aplicación principal del cliente de mensajería
 * Implementa patrón MVC con JavaFX
 */
public class MessagingClientApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingClientApp.class);
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Cargar FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load());
            
            // Configurar controlador
            MainController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);
            
            // Configurar escena con CSS
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Configurar ventana principal
            primaryStage.setTitle("Messaging Client - Sistema de Mensajería");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.setWidth(1200);
            primaryStage.setHeight(800);
            
            // Icono de la aplicación
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
            } catch (Exception e) {
                logger.warn("No se pudo cargar el icono de la aplicación");
            }
            
            // Configurar cierre de aplicación
            primaryStage.setOnCloseRequest(event -> {
                controller.shutdown();
                System.exit(0);
            });
            
            primaryStage.show();
            
            logger.info("Aplicación cliente iniciada correctamente");
            
        } catch (IOException e) {
            logger.error("Error iniciando aplicación: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void stop() {
        logger.info("Cerrando aplicación cliente");
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
