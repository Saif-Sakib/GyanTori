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
            Stage currentStage = (Stage) sourceButton.getScene().getWindow();
            double currentWidth = currentStage.getWidth();
            double currentHeight = currentStage.getHeight();
            boolean isMaximized = currentStage.isMaximized();

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(StyleManager.getStylesheet(cssFile));

            currentStage.setScene(scene);
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
    private void handleSignup() {
        if (validateInput()) {
            String fullName = fullNameField.getText().trim();
            String email = emailField.getText().trim();
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (DatabaseManager.registerUser(fullName, email, username, password)) {
                showAlert(AlertType.INFORMATION, "Success", "Account created successfully!");
                loadScene("login.fxml", "login_signup.css", signupButton);
            } else {
                showAlert(AlertType.ERROR, "Error", "Error creating account! Username or Email might be taken.");
            }
        }
    }

    @FXML
    private void handleBackToLogin() {
        Stage currentStage = (Stage) signupButton.getScene().getWindow();
        LoadPageController.loadScene("login.fxml", "login_signup.css", currentStage);
    }

    private boolean validateInput() {
        if (fullNameField.getText().trim().isEmpty() ||
                emailField.getText().trim().isEmpty() ||
                usernameField.getText().trim().isEmpty() ||
                passwordField.getText().isEmpty() ||
                confirmPasswordField.getText().isEmpty()) {
            showAlert(AlertType.ERROR, "Error", "Please fill in all fields");
            return false;
        }

        if (!isValidEmail(emailField.getText().trim())) {
            showAlert(AlertType.ERROR, "Error", "Please enter a valid email address");
            return false;
        }

        if (passwordField.getText().length() < 8) {
            showAlert(AlertType.ERROR, "Error", "Password must be at least 8 characters long");
            return false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showAlert(AlertType.ERROR, "Error", "Passwords do not match");
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}