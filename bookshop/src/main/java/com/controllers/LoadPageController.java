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
    // Make class final and constructor private since it's utility class
    private LoadPageController() {}

    /**
     * Loads a new scene with state preservation
     */
    public static <T> T loadScene(String fxmlFile, String cssFile, Stage stage) {
        try {
            // Quick validation - faster than creating separate method call
            if (fxmlFile == null || stage == null) {
                throw new IllegalArgumentException("FXML file or Stage cannot be null");
            }

            // Cache window state - using primitives for better performance
            double width = stage.getWidth();
            double height = stage.getHeight();
            double x = stage.getX();
            double y = stage.getY();
            boolean maximized = stage.isMaximized();

            // Load FXML efficiently
            FXMLLoader loader = new FXMLLoader(LoadPageController.class.getResource("/com/" + fxmlFile));
            Parent root = loader.load();

            // Create scene and apply CSS
            Scene scene = new Scene(root);
            if (cssFile != null && !cssFile.isEmpty()) {
                scene.getStylesheets().add(StyleManager.getStylesheet(cssFile));
            }

            // Apply scene and restore state efficiently
            stage.setScene(scene);
            if (!maximized) {
                // Only set dimensions if not maximized
                stage.setX(x);
                stage.setY(y);
                stage.setWidth(width);
                stage.setHeight(height);
            }
            stage.setMaximized(maximized);

            return loader.getController();

        } catch (IOException | IllegalArgumentException e) {
            // Combined exception handling for better performance
            showError("Error", e.getMessage(), e);
            return null;
        }
    }

    private static void showError(String title, String message, Exception e) {
        e.printStackTrace();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.show(); // Using show() instead of showAndWait() for better responsiveness
    }
}