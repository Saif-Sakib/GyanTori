package com;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class LoadPageController {
    public static void loadScene(String fxmlFile, String cssFile, Stage stage) {
        try {
            // Store current window state
            double width = stage.getWidth();
            double height = stage.getHeight();
            double x = stage.getX();
            double y = stage.getY();
            boolean maximized = stage.isMaximized();

            // Load new scene
            FXMLLoader loader = new FXMLLoader(LoadPageController.class.getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(StyleManager.getStylesheet(cssFile));

            // Set scene and restore window state
            stage.setScene(scene);

            if (!maximized) {
                stage.setWidth(width);
                stage.setHeight(height);
                stage.setX(x);
                stage.setY(y);
            }
            stage.setMaximized(maximized);

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