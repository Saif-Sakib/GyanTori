package com.controllers;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.models.Book;
import com.services.CartService;
import com.services.SessionManager;

/**
 * Base controller class for common navigation and user session handling
 * This class provides common functionality that can be inherited by other
 * controllers
 */
public abstract class CommonController {
    private static final Logger LOGGER = Logger.getLogger(CommonController.class.getName());

    @FXML
    protected Button profileLoginButton;

    private final CartService cartService = CartService.getInstance();

    /**
     * Initialize method to be called in the initialize method of subclasses
     */
    protected void initializeCommon() {
        updateProfileButton();
    }

    /**
     * Navigate to the home page
     */
    @FXML
    public void navigateToHome() {
        try {
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("home.fxml", "home.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to home", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to home page.");
        }
    }

    @FXML
    public void navigateToCategory() {
        try {
            // Navigate to category page (implementation would depend on app structure)
            LOGGER.info("Navigating to category page");
            // Implementation would be added here when category page is created
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to category", e);
            showAlert(Alert.AlertType.ERROR, "Error navigating to category", e.getMessage());
        }
    }

    @FXML
    public void navigateToExplore() {
        try {
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("explore.fxml", "explore.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to explore", e);
            showAlert(Alert.AlertType.ERROR, "Error navigating to explore", e.getMessage());
        }
    }

    /**
     * Handle navigation to author page.
     */
    @FXML
    public void navigateToAuthor() {
        try {
            // Navigate to author page
            LOGGER.info("Navigating to author page");
            // Implementation would be added here when author page is created
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to author page", e);
            showAlert(Alert.AlertType.ERROR, "Error navigating to author page", e.getMessage());
        }
    }

    /**
     * Handle navigation to cart page
     */
    @FXML
    public void handleCartLoad() {
        try {
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("cart.fxml", "cart.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to cart", e);
            showAlert(Alert.AlertType.ERROR, "Cart Navigation Error", "Failed to navigate to cart page.");
        }
    }

    public CartService getCartService() {
        return cartService;
    }

    /**
     * Handle profile/login button click
     * If user is logged in, shows profile dropdown
     * If user is not logged in, navigates to login page
     */
    @FXML
    public void handleProfileLogin() {
        if (SessionManager.getInstance().getIsLoggedIn()) {
            // This will be handled by the updateProfileButton method
            // which creates a dropdown menu for logged-in users
        } else {
            try {
                Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
                LoadPageController.loadScene("login.fxml", "login_signup.css", currentStage);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error navigating to login", e);
                showAlert(Alert.AlertType.ERROR, "Login Navigation Error", "Failed to navigate to login page.");
            }
        }
    }

    /**
     * Standardized method to handle adding a book to cart
     * Performs login check and shows appropriate messages
     * 
     * @param book The book to add to cart
     */
    @FXML
    public void handleAddToCart(Book book) {
        if (book == null) {
            LOGGER.warning("Attempted to add null book to cart");
            showAlert(Alert.AlertType.ERROR, "Error", "Invalid book selection.");
            return;
        }

        if (!SessionManager.getInstance().getIsLoggedIn()) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please login to add books to your cart.");
            return;
        }

        try {
            getCartService().addItem(book.getId(), book.getTitle(), book.getCurrentPrice(), book.getImageUrl());
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    String.format("%s has been added to your cart!", book.getTitle()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding book to cart: " + book.getId(), e);
            showAlert(Alert.AlertType.ERROR, "Cart Error", "Failed to add book to cart. Please try again.");
        }
    }

    /**
     * Update profile button based on login status
     * If logged in, shows dropdown with Dashboard, Settings, Logout
     * If not logged in, shows Login button
     */
    protected void updateProfileButton() {
        if (profileLoginButton != null) {
            if (SessionManager.getInstance().getIsLoggedIn()) {
                // Create a dropdown menu
                ContextMenu menu = new ContextMenu();

                // Add menu items
                CustomMenuItem dashboard = new CustomMenuItem(new Label("Dashboard"));
                CustomMenuItem settings = new CustomMenuItem(new Label("Settings"));
                CustomMenuItem logout = new CustomMenuItem(new Label("Logout"));

                // Set action handlers for each menu item
                dashboard.setOnAction(e -> openDashboard());
                settings.setOnAction(e -> openSettings());
                logout.setOnAction(e -> logout());

                // Add items to menu
                menu.getItems().addAll(dashboard, settings, logout);

                // Style the context menu
                menu.setStyle(
                        "-fx-background-color: rgba(255, 255, 255, 0.1); -fx-text-fill: white; -fx-padding: 5px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), 8, 0, 0, 3);");
                menu.getItems().forEach(item -> {
                    item.setStyle(
                            "-fx-background-color: #444; -fx-text-fill: white; -fx-padding: 5px; -fx-background-radius: 5px; -fx-cursor: hand;");

                    ((CustomMenuItem) item).getContent().setOnMouseEntered(event -> item
                            .setStyle("-fx-background-color: #555; -fx-text-fill: #ddd; -fx-background-radius: 5px;"));

                    ((CustomMenuItem) item).getContent().setOnMouseExited(event -> item
                            .setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 5px;"));
                });

                // Style the profile login button
                profileLoginButton.setText("Profile");
                profileLoginButton.setOnAction(e -> menu.show(profileLoginButton, javafx.geometry.Side.BOTTOM, 0, 0));

            } else {
                // Styling for the login button when not logged in
                profileLoginButton.setText("Login");
                profileLoginButton.setOnAction(e -> handleProfileLogin());
            }
        }
    }

    /**
     * Create a standardized book card component for displaying throughout the app
     * 
     * @param book The book to create a card for
     * @return VBox containing the book card UI
     */
    public VBox createBookCard(Book book) {
        if (book == null) {
            LOGGER.warning("Attempted to create card for null book");
            return new VBox(); // Return empty VBox instead of null to avoid NPE
        }

        VBox card = new VBox(10);
        card.getStyleClass().add("book-card");
        card.setAlignment(Pos.CENTER);

        // Book Cover Image
        ImageView coverImage = new ImageView();
        try {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(book.getImageUrl())));
            coverImage.setImage(image);
        } catch (Exception e) {
            // Load placeholder image if book image is not found
            try {
                coverImage.setImage(new Image(
                        Objects.requireNonNull(getClass().getResourceAsStream("/images/books/placeholder-book.png"))));
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load placeholder image", ex);
            }
        }
        coverImage.setFitHeight(200);
        coverImage.setFitWidth(140);
        coverImage.setPreserveRatio(true);

        // Make the cover image clickable
        coverImage.setCursor(Cursor.HAND);
        coverImage.setOnMouseClicked(e -> handleBookSelection(book));

        // Book Details
        Label titleLabel = new Label(book.getTitle());
        titleLabel.getStyleClass().add("book-title");
        titleLabel.setWrapText(true);

        // Make the title clickable
        titleLabel.setCursor(Cursor.HAND);
        titleLabel.setOnMouseClicked(e -> handleBookSelection(book));

        Label authorLabel = new Label(book.getAuthor());
        authorLabel.getStyleClass().add("book-author");

        Label ratingLabel = new Label(String.format("%.1f ★", book.getRating()));
        ratingLabel.getStyleClass().add("book-rating");

        Label priceLabel = new Label(String.format("৳ %.2f", book.getCurrentPrice()));
        priceLabel.getStyleClass().add("book-price");

        Button addToCartBtn = new Button("Add to Cart");
        addToCartBtn.getStyleClass().addAll("cart-button", "animated-button");
        addToCartBtn.setOnAction(e -> handleAddToCart(book));

        card.getChildren().addAll(coverImage, titleLabel, authorLabel, ratingLabel, priceLabel, addToCartBtn);

        // Add hover effect
        addHoverEffect(card);

        return card;
    }

    /**
     * Handle book selection to navigate to book details page
     * 
     * @param book The selected book
     */
    protected void handleBookSelection(Book book) {
        try {
            if (book == null) {
                LOGGER.warning("Attempted to select null book");
                return;
            }

            SessionManager.getInstance().setCurrentBookId(book.getId());
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("bookdetails.fxml", "bookdetails.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to book details for book: " +
                    (book != null ? book.getId() : "null"), e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to book details page.");
        }
    }

    /**
     * Add hover effect to a VBox component
     * 
     * @param card The VBox to add hover effect to
     */
    protected void addHoverEffect(VBox card) {
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), card);
        scaleIn.setToX(1.05);
        scaleIn.setToY(1.05);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), card);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        card.setOnMouseEntered(e -> scaleIn.playFromStart());
        card.setOnMouseExited(e -> scaleOut.playFromStart());
    }

    /**
     * Navigate to dashboard page
     */
    protected void openDashboard() {
        try {
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("dashboard.fxml", "dashboard.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to dashboard", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to dashboard page.");
        }
    }

    /**
     * Navigate to settings page
     */
    protected void openSettings() {
        try {
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("settings.fxml", "settings.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to settings", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to navigate to settings page.");
        }
    }

    /**
     * Handle user logout
     * Clears session and updates UI
     */
    public void logout() {
        try {
            SessionManager.getInstance().setIsLoggedIn(false);
            updateProfileButton();
            SessionManager.getInstance().clearSession();
            showAlert(Alert.AlertType.INFORMATION, "Logged Out", "You have been successfully logged out.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error logging out", e);
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to process logout.");
        }
    }

    /**
     * Display an alert dialog
     * 
     * @param type    Alert type (INFO, ERROR, etc.)
     * @param title   Alert title
     * @param content Alert message content
     */
    protected void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Overloaded showAlert method that accepts a header
     * 
     * @param type    Alert type (INFO, ERROR, etc.)
     * @param title   Alert title
     * @param header  Alert header text
     * @param content Alert message content
     */
    protected void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}