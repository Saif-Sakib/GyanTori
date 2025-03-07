package com.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;

import com.database.DatabaseManager;
import com.database.UsersCollection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.services.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class EditProfileController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(EditProfileController.class.getName());
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024; // 2MB in bytes
    private static final String UPLOAD_DIR = "uploads/profile_images/";
    
    @FXML private ImageView profileImageView;
    @FXML private Label idLabel;
    @FXML private Label usernameLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField locationField;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button uploadImageButton;

    private String currentUsername;
    private Document currentUser;
    private String tempImagePath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            // Ensure upload directory exists
            createUploadDirectory();
            
            currentUsername = SessionManager.getInstance().getUserName();
            if (currentUsername == null) {
                throw new IllegalStateException("No user logged in");
            }
            
            loadUserData();
            setupEventHandlers();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize edit profile", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load profile data");
            returnToDashboard();
        }
    }

    private void createUploadDirectory() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create upload directory", e);
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    private void loadUserData() {
        try {
            MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
            currentUser = users.find(Filters.eq("username", currentUsername)).first();

            if (currentUser == null) {
                throw new IllegalStateException("User data not found");
            }

            idLabel.setText(currentUser.getString("id"));
            usernameLabel.setText(currentUsername);
            fullNameField.setText(currentUser.getString("full_name"));
            emailField.setText(currentUser.getString("email"));
            locationField.setText(currentUser.getString("location"));
            
            String imagePath = currentUser.getString("imgPath");
            if (imagePath != null) {
                loadProfileImage(imagePath);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading user data", e);
            throw new RuntimeException("Failed to load user data", e);
        }
    }

    private void loadProfileImage(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                profileImageView.setImage(image);
                profileImageView.setFitWidth(120);
                profileImageView.setFitHeight(120);
                profileImageView.setPreserveRatio(true);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load profile image", e);
            // Don't throw - just log the error and continue without the image
        }
    }

    private void handleImageUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                // Validate file size
                if (file.length() > MAX_IMAGE_SIZE) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Image size must be less than 2MB");
                    return;
                }

                // Validate image format
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null || !contentType.startsWith("image/")) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Invalid image format");
                    return;
                }

                // Generate unique filename
                String fileName = UUID.randomUUID().toString() + getFileExtension(file.getName());
                tempImagePath = UPLOAD_DIR + fileName;

                // Copy file to temporary location
                Files.copy(file.toPath(), Paths.get(tempImagePath), StandardCopyOption.REPLACE_EXISTING);
                
                // Update preview
                loadProfileImage(tempImagePath);
                
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error handling image upload", e);
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to upload image");
            }
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }

    private void setupEventHandlers() {
        saveButton.setOnAction(e -> handleSave());
        uploadImageButton.setOnAction(e -> handleImageUpload());
        cancelButton.setOnAction(e -> handleCancel());
    }

    @FXML
    private void handleSave() {
        try {
            if (!validateInputs()) {
                return;
            }

            MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
            Document updates = new Document();

            // Update basic information
            updates.append("full_name", fullNameField.getText().trim())
                   .append("email", emailField.getText().trim())
                   .append("location", locationField.getText().trim());

            // Update image path if a new image was uploaded
            if (tempImagePath != null) {
                // Delete old image if it exists
                String oldImagePath = currentUser.getString("imgPath");
                if (oldImagePath != null) {
                    try {
                        Files.deleteIfExists(Paths.get(oldImagePath));
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to delete old profile image", e);
                    }
                }
                updates.append("imgPath", tempImagePath);
            }

            // Handle password update if provided
            if (!newPasswordField.getText().isEmpty()) {
                if (!UsersCollection.validateLogin(currentUsername, currentPasswordField.getText())) {
                    return;
                }
                
                byte[] salt = UsersCollection.generateSalt();
                String hashedPassword = UsersCollection.hashPassword(newPasswordField.getText(), salt);
                
                updates.append("password_hash", hashedPassword)
                       .append("salt", Base64.getEncoder().encodeToString(salt));
            }

            users.updateOne(
                Filters.eq("username", currentUsername),
                new Document("$set", updates)
            );

            showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully");
            returnToDashboard();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving profile changes", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to save changes");
        }
    }


    private boolean validateInputs() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();

        if (fullName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Full name cannot be empty");
            fullNameField.requestFocus();
            return false;
        }

        if (email.isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid email format");
            emailField.requestFocus();
            return false;
        }

        return true;
    }

    private void returnToDashboard() {
        try {
            Stage currentStage = (Stage) cancelButton.getScene().getWindow();
            LoadPageController.loadScene("dashboard.fxml", "dashboard.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error returning to dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to return to dashboard");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void handleCancel() {
        returnToDashboard();
    }
}