package com;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class LoginController {
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    private HomeController homeController;
    private void loadScene(String fxmlFile, String cssFile, Button sourceButton) {
        try {
            // Get current window dimensions
            Stage currentStage = (Stage) sourceButton.getScene().getWindow();
            double currentWidth = currentStage.getWidth();
            double currentHeight = currentStage.getHeight();
            boolean isMaximized = currentStage.isMaximized();

            // Load new scene
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);

            // Apply all stylesheets
            scene.getStylesheets().addAll(StyleManager.getStylesheet(cssFile));

            // Set the scene
            currentStage.setScene(scene);

            // Restore dimensions
            if (isMaximized) {
                currentStage.setMaximized(true);
            } else {
                currentStage.setWidth(currentWidth);
                currentStage.setHeight(currentHeight);
            }

            currentStage.show();
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "Error loading " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (validateInput(username, password)) {
            // Add your authentication logic here
            // Get controller instance and update boolean value
            homeController.setLoggedIn(true);
            loadScene("home.fxml", "home.css", loginButton);
        }
    }

    @FXML
    private void handleSignUp() {
        loadScene("signup.fxml", "login_signup.css", loginButton);
    }

    private boolean validateInput(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Input Error", "Please fill in all fields");
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}