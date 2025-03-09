package com.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.database.BookDetailsCollection;
import com.models.Book;
import com.services.SearchImplementation;
import com.services.SessionManager;

public class HomeController extends CommonController {

    @FXML
    private TextField searchField;
    @FXML
    private HBox featuredBooks;
    @FXML
    private HBox recommendedBooks;
    @FXML
    private Label navLogo;
    @FXML
    private ImageView bannerImage;
    @FXML
    private HBox bannerBox;
    @FXML
    private HBox searchResultsContainer;
    @FXML
    private Button searchButton;

    private volatile boolean slideshowRunning = true;
    private Thread slideshowThread;
    private int currentImageIndex = 0;

    private final List<String> bannerImages = Arrays.asList(
            "/com/images/img1.png",
            "/com/images/img2.png",
            "/com/images/img3.png",
            "/com/images/img4.png",
            "/com/images/img5.png",
            "/com/images/img6.png");

    private SearchImplementation searchImplementation;

    @FXML
    public void initialize() {
        try {
            // Initialize the search results container
            if (searchResultsContainer != null) {
                searchResultsContainer.setVisible(false);
                searchResultsContainer.setManaged(false);

                // Initialize the search implementation
                searchImplementation = new SearchImplementation(this, searchResultsContainer);
            }

            updateProfileButton(); // Using the method from CommonHome
            loadBooks();
            addSearchListener();

            if (navLogo != null) {
                animateNavLogo();
            }

            initBanner();

            // Initialize search button click handler
            if (searchButton != null) {
                searchButton.setOnAction(e -> performSearch());
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Initialization Error",
                    "Failed to initialize application: " + e.getMessage());
        }
    }

    private void initBanner() {
        if (bannerImage != null && !bannerImages.isEmpty()) {
            // Disable ratio preservation to allow full stretching
            bannerImage.setPreserveRatio(false);

            // Get parent StackPane
            StackPane parent = (StackPane) bannerImage.getParent();

            // Bind ImageView size to parent size
            bannerImage.fitWidthProperty().bind(parent.widthProperty());
            bannerImage.fitHeightProperty().bind(parent.heightProperty());

            // Set minimum dimensions if needed
            parent.setMinWidth(100); // Adjust these values as needed
            parent.setMinHeight(50);

            // Optional: Add resize listeners for additional control
            parent.widthProperty().addListener((obs, oldVal, newVal) -> {
                // Ensure image width never exceeds parent width
                if (bannerImage.getFitWidth() > newVal.doubleValue()) {
                    bannerImage.setFitWidth(newVal.doubleValue());
                }
            });

            parent.heightProperty().addListener((obs, oldVal, newVal) -> {
                // Ensure image height never exceeds parent height
                if (bannerImage.getFitHeight() > newVal.doubleValue()) {
                    bannerImage.setFitHeight(newVal.doubleValue());
                }
            });

            try {
                Image firstImage = loadImage(bannerImages.get(0));
                if (firstImage != null) {
                    bannerImage.setImage(firstImage);
                    startImageSlideshow();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed to load initial banner image: " + e.getMessage());
            }
        }
    }

    private Image loadImage(String imageUrl) {
        try {
            InputStream stream = getClass().getResourceAsStream(imageUrl);
            if (stream != null) {
                return new Image(stream);
            }
            System.err.println("Could not find image resource: " + imageUrl);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading image " + imageUrl + ": " + e.getMessage());
            return null;
        }
    }

    private void startImageSlideshow() {
        slideshowThread = new Thread(() -> {
            while (slideshowRunning) {
                try {
                    Thread.sleep(3000);

                    if (!slideshowRunning)
                        break;

                    Platform.runLater(() -> {
                        // Calculate next image index before loading
                        currentImageIndex = (currentImageIndex + 1) % bannerImages.size();
                        String nextImageUrl = bannerImages.get(currentImageIndex);

                        // Try to load the next image before starting animation
                        Image nextImage = loadImage(nextImageUrl);
                        if (nextImage == null) {
                            // Skip this transition if image couldn't be loaded
                            return;
                        }

                        // Create and play the slide out animation
                        TranslateTransition slideOut = new TranslateTransition(Duration.seconds(0.5), bannerImage);
                        slideOut.setFromX(0);
                        slideOut.setFromY(0);
                        slideOut.setToX(bannerImage.getFitWidth());
                        slideOut.setToY(-bannerImage.getFitHeight());

                        slideOut.setOnFinished(event -> {
                            // Update the image and index
                            bannerImage.setImage(nextImage);

                            // Create and play the slide in animation
                            TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.5), bannerImage);
                            slideIn.setFromX(-bannerImage.getFitWidth());
                            slideIn.setFromY(bannerImage.getFitHeight());
                            slideIn.setToX(0);
                            slideIn.setToY(0);
                            slideIn.play();
                        });

                        slideOut.play();
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        slideshowThread.setDaemon(true);
        slideshowThread.start();
    }

    public void stopSlideshow() {
        slideshowRunning = false;
        if (slideshowThread != null) {
            slideshowThread.interrupt();
        }
    }

    private void animateNavLogo() {
        if (navLogo == null)
            return; // Avoid null pointer exceptions

        // Fade effect
        FadeTransition fade = new FadeTransition(Duration.seconds(3), navLogo);
        fade.setFromValue(0.0);
        fade.setToValue(3.0);

        // Scale effect (slight bounce)
        ScaleTransition scale = new ScaleTransition(Duration.seconds(1), navLogo);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setAutoReverse(true);

        // Translate effect (left to right)
        TranslateTransition translate = new TranslateTransition(Duration.seconds(2), navLogo);
        translate.setFromX(-100);
        translate.setToX(0);

        // Combine animations
        ParallelTransition parallelTransition = new ParallelTransition(fade, scale, translate);
        parallelTransition.play();
    }

    private void addSearchListener() {
        if (searchField != null) {
            // Add a key press event listener to detect Enter key
            searchField.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                    performSearch();
                }
            });

            // Enable live search after typing 3+ characters
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue.length() >= 3) {
                    // Only perform live search if we've changed by at least 3 characters to avoid
                    // too many searches
                    if (oldValue == null || Math.abs(oldValue.length() - newValue.length()) >= 3) {
                        performSearch();
                    }
                } else if (newValue == null || newValue.isEmpty()) {
                    // Clear results when search field is empty
                    if (searchImplementation != null) {
                        searchImplementation.clearResults();
                    }
                }
            });
        }
    }

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
                    System.err.println("Search implementation is not initialized");
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

    private void loadBooks() {
        try {
            // Fetch books from MongoDB
            List<Book> featuredBooksList = BookDetailsCollection.getBooksByPagination(0, 9).stream()
                    .filter(Objects::nonNull) // Ensure no null books are included
                    .sorted((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()))
                    .collect(Collectors.toList());

            List<Book> recommendedBooksList = BookDetailsCollection.getBooksByPagination(9, 9).stream()
                    .filter(Objects::nonNull)
                    .sorted((b1, b2) -> Double.compare(b2.getReviewCount(), b1.getReviewCount()))
                    .collect(Collectors.toList());

            // Load the book sections with data
            loadBookSection(featuredBooks, featuredBooksList);
            loadBookSection(recommendedBooks, recommendedBooksList);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Loading Error", "Failed to load books: " + e.getMessage());
        }
    }

    private void loadBookSection(HBox container, List<Book> books) {
        if (container != null && books != null) {
            container.getChildren().clear();
            books.forEach(book -> {
                if (book != null) {
                    container.getChildren().add(createBookCard(book));
                }
            });
        }
    }

    public void addToCart(Book book) {
        super.handleAddToCart(book);
    }

    // Override handleCartLoad and handleProfileLogin from parent class
    // to make sure slideshow is stopped when navigating away
    @Override
    @FXML
    public void handleCartLoad() {
        stopSlideshow();
        super.handleCartLoad();
    }

    @Override
    @FXML
    public void handleProfileLogin() {
        if (!SessionManager.getInstance().getIsLoggedIn()) {
            stopSlideshow();
        }
        super.handleProfileLogin();
    }

    @Override
    protected void openDashboard() {
        stopSlideshow();
        super.openDashboard();
    }
}