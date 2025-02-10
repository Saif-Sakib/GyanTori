package com;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
            Parent root = loader.load();

            // Create the scene
            Scene scene = new Scene(root);

            // Add CSS files
            scene.getStylesheets().add(getClass().getResource("styles/login_signup.css").toExternalForm());

            // Configure the stage
            primaryStage.setTitle("GyanTori - Login");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000); // Set minimum width
            primaryStage.setMinHeight(600); // Set minimum height

            // Make the window draggable
            root.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });

            root.setOnMouseDragged(event -> {
                primaryStage.setX(event.getScreenX() - xOffset);
                primaryStage.setY(event.getScreenY() - yOffset);
            });

            // Center the stage on screen
            primaryStage.centerOnScreen();

            // Show the stage
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}