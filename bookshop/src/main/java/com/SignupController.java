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

public class SignupController {
    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Button signupButton;

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
            showError("Error loading " + fxmlFile + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignup() {
        if (validateInput()) {
            // Add your registration logic here
            showSuccess("Account created successfully!");
            loadScene("login.fxml", "login_signup.css", signupButton);
        }
    }

    @FXML
    private void handleBackToLogin() {
        loadScene("login.fxml", "login_signup.css", signupButton);
    }

    private boolean validateInput() {
        if (fullNameField.getText().isEmpty() ||
                emailField.getText().isEmpty() ||
                usernameField.getText().isEmpty() ||
                passwordField.getText().isEmpty() ||
                confirmPasswordField.getText().isEmpty()) {
            showError("Please fill in all fields");
            return false;
        }

        if (!isValidEmail(emailField.getText())) {
            showError("Please enter a valid email address");
            return false;
        }

        if (passwordField.getText().length() < 6) {
            showError("Password must be at least 6 characters long");
            return false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showError("Passwords do not match");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.matches(emailRegex);
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}