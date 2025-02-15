package com;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class LoadPageController {

    public static void loadScene(String fxmlFile, String cssFile, Button sourceButton) {
        try {
            // Get current window dimensions
            Stage currentStage = (Stage) sourceButton.getScene().getWindow();
            double currentWidth = currentStage.getWidth();
            double currentHeight = currentStage.getHeight();
            boolean isMaximized = currentStage.isMaximized();

            // Load new scene
            FXMLLoader loader = new FXMLLoader(LoadPageController.class.getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Apply stylesheets
            scene.getStylesheets().addAll(StyleManager.getStylesheet(cssFile));

            // Set the scene
            currentStage.setScene(scene);

            // Restore previous dimensions
            if (isMaximized) {
                currentStage.setMaximized(true);
            } else {
                currentStage.setWidth(currentWidth);
                currentStage.setHeight(currentHeight);
            }

            currentStage.show();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error loading " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
