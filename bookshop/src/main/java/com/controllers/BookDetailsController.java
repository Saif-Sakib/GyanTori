package com.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.database.BooksDetailsCollection;
import com.models.Book;
import com.services.SessionManager;

public class BookDetailsController {

    private static final Logger logger = Logger.getLogger(BookDetailsController.class.getName());

    // FXML elements as defined in the FXML file
    @FXML
    private VBox rootPane;
    @FXML
    private Label navLogo;
    @FXML
    private TextField searchField;
    @FXML
    private Button profileLoginButton;
    @FXML
    private Label categoryBreadcrumb;
    @FXML
    private Label bookTitleBreadcrumb;
    @FXML
    private ImageView bookCoverImage;
    @FXML
    private Label discountLabel;
    @FXML
    private Button previewButton;
    @FXML
    private Label bookTitle;
    @FXML
    private Label authorName;
    @FXML
    private HBox starsContainer;
    @FXML
    private Label ratingValue;
    @FXML
    private Label reviewCount;
    @FXML
    private Label publisherValue;
    @FXML
    private Label publicationDateValue;
    @FXML
    private Label languageValue;
    @FXML
    private Label pagesValue;
    @FXML
    private Label isbnValue;
    @FXML
    private FlowPane categoriesContainer;
    @FXML
    private Label currentPrice;
    @FXML
    private Label originalPrice;
    @FXML
    private Label discountPercent;
    @FXML
    private Button addToCartButton;
    @FXML
    private Button buyNowButton;
    @FXML
    private Button wishlistButton;
    @FXML
    private TextFlow bookDescription;
    @FXML
    private Text descriptionText;
    @FXML
    private ImageView authorImage;
    @FXML
    private Label authorNameFull;
    @FXML
    private TextFlow authorBio;
    @FXML
    private Text authorBioText;
    @FXML
    private Label averageRating;
    @FXML
    private HBox averageStarsContainer;
    @FXML
    private Label totalReviewCount;
    @FXML
    private HBox similarBooks;

    // Book data model (simplified)
    private Book currentBook;
    private boolean isInWishlist = false;

    HomeController homeController = new HomeController();
    /**
     * Initialize the controller.
     * This method is automatically called after the FXML file is loaded.
     */
    @FXML
    public void initialize() {
        try {
            // Load dummy book data
            loadBookData();

            // Setup event listeners
            setupEventListeners();

            // Update UI with book data
            updateUI();
            updateProfileButton();
        } catch (Exception e) {
            handleException("Error initializing BookDetailsController", e);
        }
    }

    /**
     * Load book data from database or service.
     * Using dummy data for demonstration.
     */
    private void loadBookData() {
        try {
            SessionManager sessionManager = SessionManager.getInstance();
            currentBook = BooksDetailsCollection.getBookById(sessionManager.getCurrentBookId());
        } catch (Exception e) {
            handleException("Error loading book data", e);
        }
    }

    /**
     * Set up event listeners for UI components.
     */
    private void setupEventListeners() {
        try {
            // Add any additional event listeners not defined in FXML
            wishlistButton.setOnMouseEntered(event -> {
                // Show tooltip or animation
            });
        } catch (Exception e) {
            handleException("Error setting up event listeners", e);
        }
    }

    /**
     * Update UI components with book data.
     */
    private void updateUI() {
        try {
            // Set values from the book model to the UI components
            bookTitle.setText(currentBook.getTitle());
            bookTitleBreadcrumb.setText(currentBook.getTitle());
            authorName.setText(currentBook.getAuthor());
            authorNameFull.setText(currentBook.getAuthor());
            publisherValue.setText(currentBook.getPublisher());
            publicationDateValue.setText(currentBook.getPublicationDate());
            languageValue.setText(currentBook.getLanguage());
            pagesValue.setText(String.valueOf(currentBook.getPages()));
            isbnValue.setText(currentBook.getIsbn());
            currentPrice.setText("TK. " + currentBook.getCurrentPrice());
            originalPrice.setText("TK. " + currentBook.getOriginalPrice());
            discountPercent.setText("(" + currentBook.getDiscount() + "% off)");
            ratingValue.setText(String.valueOf(currentBook.getRating()));
            reviewCount.setText(currentBook.getReviewCount() + " reviews");
            totalReviewCount.setText(currentBook.getReviewCount() + " reviews");
            averageRating.setText(String.valueOf(currentBook.getRating()));
            descriptionText.setText(currentBook.getDescription());
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(currentBook.getImageUrl())));
            bookCoverImage.setImage(image);
            // In a real app, you would also load images from URLs or resources
        } catch (Exception e) {
            handleException("Error updating UI with book data", e);
        }
    }

    private void updateProfileButton() {
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

    private void openDashboard() {
        Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
        LoadPageController.loadScene("dashboard.fxml", "dashboard.css", currentStage);
    }

    private void openSettings() {
        Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
        LoadPageController.loadScene("settings.fxml", "settings.css", currentStage);

    }

    public void logout() {
        try {
            SessionManager.getInstance().setIsLoggedIn(false);
            updateProfileButton();
            SessionManager.getInstance().clearSession();
            showAlert(Alert.AlertType.INFORMATION, "Logged Out", "You have been successfully logged out.", "");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to process logout.", "");
        }

    }
    
    /**
     * Handle navigation to home page.
     */
    @FXML
    public void navigateToHome() {
        Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
        LoadPageController.loadScene("home.fxml", "home.css", currentStage);
    }

    /**
     * Handle navigation to category page.
     */
    @FXML
    public void navigateToCategory() {
        try {
            // Navigate to category page (implementation would depend on app structure)
            System.out.println("Navigating to category: " + categoryBreadcrumb.getText());
        } catch (Exception e) {
            handleException("Error navigating to category", e);
        }
    }

    /**
     * Handle navigation to author page.
     */
    @FXML
    public void navigateToAuthor() {
        try {
            // Navigate to author page
            System.out.println("Navigating to author: " + authorName.getText());
        } catch (Exception e) {
            handleException("Error navigating to author page", e);
        }
    }

    /**
     * Handle search functionality.
     */
    @FXML
    public void performSearch() {
        try {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                System.out.println("Searching for: " + searchTerm);
                // Implement search functionality
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Error performing search", e.getMessage());
        }
    }

    /**
     * Handle cart loading.
     */
    @FXML
    public void handleCartLoad() {
        try {
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("cart.fxml", "cart.css", currentStage);
        } catch (Exception e) {
            handleException("Error loading cart", e);
        }
    }

    /**
     * Handle profile or login.
     */
    @FXML
    public void handleProfileLogin() {
        try {
            if(SessionManager.getInstance().getIsLoggedIn()){
                homeController.logout();
            }
            else{
                Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
                LoadPageController.loadScene("login.fxml", "login_signup.css", currentStage);
            }
        } catch (Exception e) {
            handleException("Error handling profile/login", e);
        }
    }

    /**
     * Add current book to cart.
     */
    @FXML
    public void addToCart() {
        try {
            homeController.handleAddToCart(currentBook);
        } catch (Exception e) {
            handleException("Error adding book to cart", e);
        }
    }

    /**
     * Buy the book now.
     */
    @FXML
    public void buyNow() {
        try {
            // Add to cart and proceed to checkout
            addToCart();
            CartController cartController = new CartController();
            cartController.handleCheckout();

            // Navigate to checkout page
            // This would be implemented based on your app's navigation structure
        } catch (Exception e) {
            handleException("Error processing buy now", e);
        }
    }

    /**
     * Toggle wishlist status.
     */
    @FXML
    public void toggleWishlist() {
        try {
            isInWishlist = !isInWishlist;

            if (isInWishlist) {
                System.out.println("Added to wishlist: " + currentBook.getTitle());
                // Update wishlist button style or icon
            } else {
                System.out.println("Removed from wishlist: " + currentBook.getTitle());
                // Update wishlist button style or icon
            }
        } catch (Exception e) {
            handleException("Error toggling wishlist status", e);
        }
    }

    /**
     * Show all reviews for the book.
     */
    @FXML
    public void showReviews() {
        try {
            // This might scroll to the reviews section or open a dialog with all reviews
            System.out.println("Showing reviews for: " + currentBook.getTitle());
        } catch (Exception e) {
            handleException("Error showing reviews", e);
        }
    }

    /**
     * View all books by the current author.
     */
    @FXML
    public void viewAuthorBooks() {
        try {
            System.out.println("Viewing all books by author: " + currentBook.getAuthor());
            // Navigate to author books page
        } catch (Exception e) {
            handleException("Error viewing author books", e);
        }
    }

    /**
     * Open review writing interface.
     */
    @FXML
    public void writeReview() {
        try {
            System.out.println("Opening review form for: " + currentBook.getTitle());
            // Open review dialog or navigate to review page
        } catch (Exception e) {
            handleException("Error opening review form", e);
        }
    }

    /**
     * View all reviews for the book.
     */
    @FXML
    public void viewAllReviews() {
        try {
            System.out.println("Viewing all reviews for: " + currentBook.getTitle());
            // Navigate to full reviews page or open dialog
        } catch (Exception e) {
            handleException("Error viewing all reviews", e);
        }
    }

    /**
     * Display an alert dialog.
     */
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        try {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception e) {
            // Fallback to console if alert cannot be shown
            System.err.println("Could not show alert: " + title + " - " + content);
            e.printStackTrace();
        }
    }

    /**
     * Handle exceptions in a centralized way.
     */
    private void handleException(String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
        showAlert(Alert.AlertType.ERROR, "Error", message, e.getMessage());
    }
}