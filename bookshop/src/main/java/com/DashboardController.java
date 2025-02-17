package com;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class DashboardController {

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userEmailLabel;

    @FXML
    private Button homeButton;

    @FXML
    private Button settingsButton;

    @FXML
    private Button logoutButton;


    private String userName;

    @FXML
    private void initialize() {
        loadUserData();
    }

    private void loadUserData() {
        SessionManager session = SessionManager.getInstance();
        userName = session.getUserName();
        if (userName != null) {
            userNameLabel.setText(userName);
        } else {
            System.out.println("⚠️ No user session found!");
        }
    }

    // Sample handlers (implement functionality accordingly)
    @FXML
    private void openProfile() {
        System.out.println("Profile opened for: " + userName);
    }

    @FXML
    private void goToHome() {
        Stage currentStage = (Stage) settingsButton.getScene().getWindow();
        LoadPageController.loadScene("home.fxml", "home.css", currentStage);
    }

    @FXML
    private void goToSettings() {
        Stage currentStage = (Stage) settingsButton.getScene().getWindow();
        LoadPageController.loadScene("settings.fxml", "settings.css", currentStage);
    }

    @FXML
    private void logout() {
        HomeController homeController = new HomeController();
        homeController.logout();
        Stage currentStage = (Stage) userNameLabel.getScene().getWindow();
        LoadPageController.loadScene("home.fxml", "home.css", currentStage);
    }
}
