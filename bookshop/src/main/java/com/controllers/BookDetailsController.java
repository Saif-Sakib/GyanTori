package com.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.database.BookDetailsCollection;
import com.models.Book;
import com.services.SessionManager;
import com.services.SearchImplementation;
import com.models.Review;

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

    // New FXML elements for reviews section
    @FXML
    private VBox reviewsContainer;
    @FXML
    private Button viewAllReviewsButton;

    // Book data model (simplified)
    private Book currentBook;
    private boolean isInWishlist = false;
    private SearchImplementation searchImplementation;

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

            // Load and display reviews
            loadReviews();
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
            currentBook = BookDetailsCollection.getBookById(sessionManager.getCurrentBookId());
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

            // FIX: Check if viewAllReviewsButton is null before setting action
            if (viewAllReviewsButton != null) {
                viewAllReviewsButton.setOnAction(event -> viewAllReviews());
            } else {
                logger.warning("viewAllReviewsButton is null in FXML. Check your FXML file.");
            }
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

            // Update star ratings UI
            if (starsContainer != null) {
                updateStarRatings(starsContainer, currentBook.getRating());
            }
            if (averageStarsContainer != null) {
                updateStarRatings(averageStarsContainer, currentBook.getRating());
            }
        } catch (Exception e) {
            handleException("Error updating UI with book data", e);
        }
    }

    /**
     * Update star ratings UI based on the rating value
     */
    private void updateStarRatings(HBox starsContainer, double rating) {
        try {
            if (starsContainer == null) {
                logger.warning("Stars container is null. Check your FXML file.");
                return;
            }

            starsContainer.getChildren().clear();
            int fullStars = (int) rating;
            boolean hasHalfStar = rating - fullStars >= 0.5;

            // FIX: Add null checks and fallback images
            Image fullStarImage = getStarImage("/images/star_full.png", "⭐");
            Image halfStarImage = getStarImage("/images/star_half.png", "✭");
            Image emptyStarImage = getStarImage("/images/star_empty.png", "☆");

            // Add full stars
            for (int i = 0; i < fullStars; i++) {
                ImageView star = new ImageView(fullStarImage);
                star.setFitWidth(16);
                star.setFitHeight(16);
                starsContainer.getChildren().add(star);
            }

            // Add half star if needed
            if (hasHalfStar) {
                ImageView halfStar = new ImageView(halfStarImage);
                halfStar.setFitWidth(16);
                halfStar.setFitHeight(16);
                starsContainer.getChildren().add(halfStar);
            }

            // Add empty stars to complete 5 stars
            int remainingStars = 5 - fullStars - (hasHalfStar ? 1 : 0);
            for (int i = 0; i < remainingStars; i++) {
                ImageView emptyStar = new ImageView(emptyStarImage);
                emptyStar.setFitWidth(16);
                emptyStar.setFitHeight(16);
                starsContainer.getChildren().add(emptyStar);
            }
        } catch (Exception e) {
            handleException("Error updating star ratings", e);
        }
    }

    /**
     * Helper method to safely get star images with fallback
     */
    private Image getStarImage(String resourcePath, String fallbackText) {
        try {
            // Try to load the image from resources
            java.io.InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is != null) {
                return new Image(is);
            } else {
                // If resource not found, create a text-based fallback
                logger.warning("Image resource not found: " + resourcePath + ". Using fallback.");

                // Create a simple text-based star using JavaFX
                Text starText = new Text(fallbackText);
                javafx.scene.Scene scene = new javafx.scene.Scene(new StackPane(starText), 16, 16);
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

                // Render the scene to an image
                javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(16, 16);
                scene.snapshot(writableImage);

                return writableImage;
            }
        } catch (Exception e) {
            logger.warning("Failed to load star image: " + e.getMessage());
            // Return a 1x1 transparent image as absolute fallback
            return new WritableImage(1, 1);
        }
    }

    /**
     * Load and display reviews for the current book
     */
    private void loadReviews() {
        try {
            if (reviewsContainer != null) {
                reviewsContainer.getChildren().clear();

                List<Review> reviews = currentBook.getBuyerReviews().stream()
                        .map(br -> new Review(br.getReviewerId(), br.getComment(), br.getRating(), br.getReviewDate()))
                        .collect(Collectors.toList());
                if (reviews == null || reviews.isEmpty()) {
                    Label noReviewsLabel = new Label("No reviews yet. Be the first to review this book!");
                    noReviewsLabel.getStyleClass().add("no-reviews-label");
                    reviewsContainer.getChildren().add(noReviewsLabel);
                    return;
                }

                // Show only first 3 reviews in the main view
                int displayCount = Math.min(reviews.size(), 3);
                for (int i = 0; i < displayCount; i++) {
                    VBox reviewBox = createReviewBox(reviews.get(i));
                    reviewsContainer.getChildren().add(reviewBox);

                    // Add separator except for the last review
                    if (i < displayCount - 1) {
                        Separator separator = new Separator();
                        separator.setPadding(new Insets(10, 0, 10, 0));
                        reviewsContainer.getChildren().add(separator);
                    }
                }

                // Show "View All" button if there are more reviews
                if (reviews.size() > 3 && viewAllReviewsButton != null) {
                    viewAllReviewsButton.setVisible(true);
                    viewAllReviewsButton.setText("View All " + reviews.size() + " Reviews");
                } else if (viewAllReviewsButton != null) {
                    viewAllReviewsButton.setVisible(false);
                }
            } else {
                logger.warning("reviewsContainer is null. Check your FXML file.");
            }
        } catch (Exception e) {
            handleException("Error loading reviews", e);
        }
    }

    /**
     * Create a review box for display
     */
    private VBox createReviewBox(Review review) {
        VBox reviewBox = new VBox(5);
        reviewBox.getStyleClass().add("review-box");
        reviewBox.setPadding(new Insets(10));

        // Review header with user info and date
        HBox reviewHeader = new HBox(10);
        reviewHeader.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("User: " + review.getReviewerId());
        userLabel.getStyleClass().add("review-user");

        Label dateLabel = new Label(review.getReviewDate().toString());
        dateLabel.getStyleClass().add("review-date");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        reviewHeader.getChildren().addAll(userLabel, spacer, dateLabel);

        // Stars for this review
        HBox reviewStars = new HBox(2);
        updateStarRatings(reviewStars, review.getRating());

        // Review comment
        Text commentText = new Text(review.getComment());
        commentText.getStyleClass().add("review-comment");
        TextFlow commentFlow = new TextFlow(commentText);
        commentFlow.getStyleClass().add("review-comment-flow");

        reviewBox.getChildren().addAll(reviewHeader, reviewStars, commentFlow);
        return reviewBox;
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
            if (!SessionManager.getInstance().getIsLoggedIn()) {
                showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please login to add books to your cart.");
                return;
            } else {
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
            if (!SessionManager.getInstance().getIsLoggedIn()) {
                showAlert(Alert.AlertType.INFORMATION, "Login Required", "Please login to write a review.");
                return;
            }

            // Create a dialog for writing reviews
            Dialog<Review> dialog = new Dialog<>();
            dialog.setTitle("Write a Review");
            dialog.setHeaderText("Share your thoughts about \"" + currentBook.getTitle() + "\"");

            // Set the button types
            ButtonType submitButtonType = new ButtonType("Submit Review", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

            // Create the rating and comment fields
            VBox content = new VBox(10);
            content.setPadding(new Insets(20, 10, 10, 10));

            Label ratingLabel = new Label("Your Rating (1-5):");
            Slider ratingSlider = new Slider(1, 5, 5);
            ratingSlider.setMajorTickUnit(1);
            ratingSlider.setMinorTickCount(0);
            ratingSlider.setSnapToTicks(true);
            ratingSlider.setShowTickLabels(true);
            ratingSlider.setShowTickMarks(true);

            Label commentLabel = new Label("Your Review:");
            TextArea commentArea = new TextArea();
            commentArea.setPrefRowCount(5);
            commentArea.setPromptText("Tell others what you thought about this book...");

            content.getChildren().addAll(ratingLabel, ratingSlider, commentLabel, commentArea);
            dialog.getDialogPane().setContent(content);

            // Convert the result when the submit button is clicked
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == submitButtonType) {
                    String userId = SessionManager.getInstance().getUserId();
                    String comment = commentArea.getText().trim();
                    double rating = ratingSlider.getValue();

                    if (comment.isEmpty()) {
                        showAlert(Alert.AlertType.WARNING, "Empty Review",
                                "Please write your thoughts about the book.");
                        return null;
                    }

                    return new Review(userId, comment, rating, LocalDate.now());
                }
                return null;
            });

            // Show the dialog and process the result
            Optional<Review> result = dialog.showAndWait();
            result.ifPresent(review -> {
                // Save the review to the database
                Book.Review bookReview = new Book.Review(review.getReviewerId(), review.getComment(),
                        review.getRating(), review.getReviewDate());
                boolean success = BookDetailsCollection.addBookReview(currentBook.getId(), bookReview);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Review Submitted", "Thank you for your review!");

                    // Reload the book to get updated ratings and reviews
                    loadBookData();
                    updateUI();
                    loadReviews();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to submit your review. Please try again later.");
                }
            });
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

            // Create a dialog to display all reviews
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("All Reviews");
            dialog.setHeaderText("Reviews for \"" + currentBook.getTitle() + "\"");

            // Set button types
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            // Create scrollable content for reviews
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(400);

            VBox allReviewsContainer = new VBox(10);
            allReviewsContainer.setPadding(new Insets(10));

            List<Book.Review> bookReviews = currentBook.getBuyerReviews();
            List<Review> reviews = bookReviews.stream()
                    .map(br -> new Review(br.getReviewerId(), br.getComment(), br.getRating(), br.getReviewDate()))
                    .collect(Collectors.toList());
            if (reviews == null || reviews.isEmpty()) {
                Label noReviewsLabel = new Label("No reviews yet for this book.");
                noReviewsLabel.getStyleClass().add("no-reviews-label");
                allReviewsContainer.getChildren().add(noReviewsLabel);
            } else {
                // Add header with summary info
                HBox summary = new HBox(20);
                summary.setAlignment(Pos.CENTER_LEFT);
                summary.setPadding(new Insets(0, 0, 10, 0));

                Label ratingLabel = new Label(String.format("Average Rating: %.1f", currentBook.getRating()));
                ratingLabel.getStyleClass().add("rating-summary");

                Label countLabel = new Label(reviews.size() + " total reviews");
                countLabel.getStyleClass().add("review-count-summary");

                summary.getChildren().addAll(ratingLabel, countLabel);
                allReviewsContainer.getChildren().add(summary);

                // Add separator
                allReviewsContainer.getChildren().add(new Separator());

                // Add all reviews
                for (int i = 0; i < reviews.size(); i++) {
                    VBox reviewBox = createReviewBox(reviews.get(i));
                    allReviewsContainer.getChildren().add(reviewBox);

                    // Add separator except for the last review
                    if (i < reviews.size() - 1) {
                        Separator separator = new Separator();
                        separator.setPadding(new Insets(10, 0, 10, 0));
                        allReviewsContainer.getChildren().add(separator);
                    }
                }
            }

            scrollPane.setContent(allReviewsContainer);
            dialog.getDialogPane().setContent(scrollPane);

            // Set dialog size
            dialog.getDialogPane().setPrefWidth(600);

            // Show the dialog
            dialog.showAndWait();
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