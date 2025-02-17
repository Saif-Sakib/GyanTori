package com;

import javafx.fxml.FXML;
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

    @FXML
    public void initialize() {
        homeController = new HomeController();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (!validateInput(username, password)) {
            return;
        }

        if (DatabaseManager.validateLogin(username, password)) {
            SessionManager.getInstance().setUser(username);
            homeController.setLoggedIn(true);
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            LoadPageController.loadScene("home.fxml", "home.css", currentStage);
        } else {
            showAlert(AlertType.ERROR, "Error", "Invalid Username or Password!");
        }
    }

    @FXML
    private void handleSignUp() {
        Stage currentStage = (Stage) loginButton.getScene().getWindow();
        LoadPageController.loadScene("signup.fxml", "login_signup.css", currentStage);
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