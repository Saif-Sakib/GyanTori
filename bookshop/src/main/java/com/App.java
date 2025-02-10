package com;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static final String HOME_FXML = "/com/home.fxml";
    private static final String STYLES_CSS = "/com/styles/home.css";
    private static final String APP_TITLE = "GyanTori";
    private static final double INITIAL_WIDTH = 1024;
    private static final double INITIAL_HEIGHT = 768;

    @Override
    public void start(Stage stage) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(HOME_FXML));
            Parent root = loader.load();

            // Create scene
            Scene scene = createScene(root);

            // Configure stage
            configureStage(stage, scene);
        } catch (IOException e) {
            handleStartupError(e);
        }
    }

    private Scene createScene(Parent root) {
        Scene scene = new Scene(root, INITIAL_WIDTH, INITIAL_HEIGHT);
        
        // Load CSS
        URL cssURL = getClass().getResource(STYLES_CSS);
        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
        } else {
            System.err.println("Error: CSS file not found!");
        }
        
        return scene;
    }

    private void configureStage(Stage stage, Scene scene) {
        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.centerOnScreen();
        stage.show();
    }

    private void handleStartupError(Exception e) {
        System.err.println("Failed to start application: " + e.getMessage());
        e.printStackTrace();
        // Optionally, show an error dialog
        javafx.application.Platform.exit();
    }

    public static void main(String[] args) {
        // Set system properties or perform any pre-launch configurations
        System.setProperty("prism.lcdtext", "false"); // Optional: improve text rendering
        
        // Launch the application
        launch(args);
    }

    @Override
    public void init() throws Exception {
        // Perform any initialization before the JavaFX application is launched
        super.init();
    }

    @Override
    public void stop() throws Exception {
        // Perform cleanup or save application state
        super.stop();
    }
}