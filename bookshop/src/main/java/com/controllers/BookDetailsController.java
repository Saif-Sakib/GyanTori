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
import com.services.SearchImplementation;

public class BookDetailsController extends CommonController {
    private static final Logger logger = Logger.getLogger(BookDetailsController.class.getName());

    // FXML elements as defined in the FXML file
    @FXML
    private VBox rootPane;
    @FXML
    private Label navLogo;
    @FXML
    private TextField searchField;
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
    private SearchImplementation searchImplementation; // Add this line

    /**
     * Initialize the controller.
     * This method is automatically called after the FXML file is loaded.
     */
    @FXML
    public void initialize() {
        try {
            // Initialize common components (profile button, etc.)
            initializeCommon();

            // Load dummy book data
            loadBookData();

            // Setup event listeners
            setupEventListeners();

            // Update UI with book data
            updateUI();
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
            if (currentBook.getImageUrl().startsWith("/")) { // Check if it's a resource path
                Image image = new Image(
                        Objects.requireNonNull(getClass().getResourceAsStream(currentBook.getImageUrl())));
                bookCoverImage.setImage(image);
            } else { // Assume it's a URL
                Image image = new Image(currentBook.getImageUrl());
                bookCoverImage.setImage(image);
            }
            // In a real app, you would also load images from URLs or resources
        } catch (Exception e) {
            handleException("Error updating UI with book data", e);
        }
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

                // Use the search implementation class to perform the search
                if (searchImplementation != null) {
                    searchImplementation.performSearch(searchTerm);
                } else {
                    System.err.println("Search implementation is not initialized here Bro -_- hahaha");
                    showAlert(Alert.AlertType.ERROR, "Search Error", "Search implementation is not initialized");
                }
            } else {
                // Handle empty search term
                System.out.println("Please enter a search term");
                if (searchImplementation != null) {
                    searchImplementation.clearResults();
                }
                showAlert(Alert.AlertType.INFORMATION, "Search", "Please enter a search term");
            }
        } catch (Exception e) {
            System.err.println("Error performing search: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error performing search", e.getMessage());
        }
    }

    /**
     * Add current book to cart.
     */
    @FXML
    public void addToCart() {
        try {
            handleAddToCart(currentBook);
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

            // Navigate to checkout page
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("checkout.fxml", "checkout.css", currentStage);
        } catch (Exception e) {
            handleException("Checkout is under development -_-", e);
        }
    }

    /**
     * Toggle wishlist status.
     */
    @FXML
    public void toggleWishlist() {
        try {
            isInWishlist = !isInWishlist;
            if(!SessionManager.getInstance().getIsLoggedIn()){
                showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please login to add books to your cart.");
                return;
            }
            else{
                if (isInWishlist) {
                    System.out.println("Added to wishlist: " + currentBook.getTitle());
                    showAlert(Alert.AlertType.INFORMATION, "Wishlist", "Added to your wishlist.");
                    // Update wishlist button style or icon
                } else {
                    System.out.println("Removed from wishlist: " + currentBook.getTitle());
                    showAlert(Alert.AlertType.INFORMATION, "Wishlist", "Removed from your wishlist.");
                    // Update wishlist button style or icon
                }
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
     * Handle exceptions in a centralized way.
     */
    private void handleException(String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
        showAlert(Alert.AlertType.ERROR, "Error", message, e.getMessage());
    }
}