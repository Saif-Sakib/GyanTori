package com.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

// Assuming StyleManager is in the same package, otherwise adjust the package name
import com.services.StyleManager;

public final class LoadPageController {
    private LoadPageController() {
    }

    /**
     * Loads a new scene with state preservation.
     */
    public static <T> T loadScene(String fxmlFile, String cssFile, Stage stage) {
        try {
            if (fxmlFile == null || stage == null) {
                throw new IllegalArgumentException("FXML file or Stage cannot be null");
            }

            // Store window state
            double width = stage.getWidth();
            double height = stage.getHeight();
            double x = stage.getX();
            double y = stage.getY();
            boolean maximized = stage.isMaximized();

            // Load FXML dynamically
            FXMLLoader loader = new FXMLLoader(LoadPageController.class.getResource("/com/" + fxmlFile));
            Parent root = loader.load();

            // Try using setRoot() if Scene already exists
            Scene scene = stage.getScene();
            if (scene != null) {
                scene.setRoot(root); // Faster than recreating Scene
            } else {
                scene = new Scene(root);
                stage.setScene(scene);
            }

            // Apply CSS only if provided
            if (cssFile != null && !cssFile.isEmpty()) {
                scene.getStylesheets().clear();
                scene.getStylesheets().add(StyleManager.getStylesheet(cssFile));
            }

            // Restore window state
            if (!maximized) {
                stage.setX(x);
                stage.setY(y);
                stage.setWidth(width);
                stage.setHeight(height);
            }
            stage.setMaximized(maximized);

            return loader.getController();

        } catch (IOException | IllegalArgumentException e) {
            showError("Error", e.getMessage(), e);
            return null;
        }
    }

    private static void showError(String title, String message, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show();
    }
}
