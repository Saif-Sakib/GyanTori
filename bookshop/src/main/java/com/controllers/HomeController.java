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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.database.BookDetailsCollection;
import com.models.Book;
import com.services.SearchImplementation;
import com.services.SessionManager;

public class HomeController extends CommonController {
    // Add a logger for this class
    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());

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
    @FXML
    private Button viewAllFeaturedButton;
    @FXML
    private Button viewAllRecommendedButton;
    @FXML
    private VBox searchResultsSection;

    private volatile boolean slideshowRunning = true;
    private Thread slideshowThread;
    private int currentImageIndex = 0;

    // Books pagination parameters
    private final int BOOKS_PER_PAGE = 10;
    private int currentFeaturedPage = 0;
    private int currentRecommendedPage = 0;
    private List<Book> allFeaturedBooks;
    private List<Book> allRecommendedBooks;
    private String currentSection = null;

    // Flag to track if we're showing all books
    private boolean showingAllBooks = false;

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
            // Call base class initialization
            initializeCommon();

            // Initialize the search results container and section
            if (searchResultsContainer != null) {
                searchResultsContainer.setVisible(false);
                searchResultsContainer.setManaged(false);

                if (searchResultsSection != null) {
                    searchResultsSection.setVisible(false);
                    searchResultsSection.setManaged(false);
                }

                // Initialize the search implementation
                searchImplementation = new SearchImplementation(this, searchResultsContainer);
            }

            loadBooks();
            addSearchListener();
            setupViewAllButtons();

            if (navLogo != null) {
                animateNavLogo();
            }

            initBanner();

            // Initialize search button click handler
            if (searchButton != null) {
                searchButton.setOnAction(e -> performSearch());
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize HomeController", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error",
                    "Failed to initialize application: " + e.getMessage());
        }
    }

    private void initBanner() {
        try {
            if (bannerImage != null && !bannerImages.isEmpty()) {
                // Set a fixed height for the banner
                bannerImage.setFitHeight(180); // Adjust this value as needed
                bannerImage.setPreserveRatio(false);

                // Get parent StackPane
                StackPane parent = (StackPane) bannerImage.getParent();

                // Set a fixed height for the parent StackPane
                parent.setMaxHeight(180); // Same as the banner height
                parent.setPrefHeight(180);

                // Bind ImageView width to parent width
                bannerImage.fitWidthProperty().bind(parent.widthProperty());

                // Ensure the parent takes full width
                parent.setMinWidth(100);

                Image firstImage = loadImage(bannerImages.get(0));
                if (firstImage != null) {
                    bannerImage.setImage(firstImage);
                    startImageSlideshow();
                } else {
                    LOGGER.warning("Failed to load initial banner image, no valid image found");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing banner", e);
            // Continue execution as this is not critical
        }
    }

    private void setupViewAllButtons() {
        try {
            // Set up the View All buttons for featured and recommended books
            if (viewAllFeaturedButton != null) {
                viewAllFeaturedButton.setOnAction(e -> loadAllBooks("featured"));
            }

            if (viewAllRecommendedButton != null) {
                viewAllRecommendedButton.setOnAction(e -> loadAllBooks("recommended"));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error setting up view all buttons", e);
            // Continue execution as this is not critical
        }
    }

    private void loadAllBooks(String section) {
        try {
            // Log which section we're trying to load
            LOGGER.info("Loading books for section: " + section);

            // Find the parent sections by looking for them in the scene graph
            VBox featuredSection = findSectionParent(featuredBooks);
            VBox recommendedSection = findSectionParent(recommendedBooks);

            if (featuredSection == null || recommendedSection == null) {
                LOGGER.warning("Could not find parent sections for book containers");
                showAlert(Alert.AlertType.ERROR, "UI Error", "Could not find parent sections for book containers");
                return;
            }

            HBox targetContainer;
            String title;

            // Update button text logic first to determine if we're showing or hiding all
            // books
            Button viewAllButton = (section.equals("featured")) ? viewAllFeaturedButton : viewAllRecommendedButton;

            // If we're already showing all books and the button says "Back", handle going
            // back
            if (showingAllBooks && viewAllButton != null && viewAllButton.getText().equals("Back")) {
                // Reset pagination
                currentFeaturedPage = 0;
                currentRecommendedPage = 0;
                currentSection = null;
                allFeaturedBooks = null;
                allRecommendedBooks = null;

                // Change button text back to "View All"
                viewAllButton.setText("View All");
                showingAllBooks = false;
                LOGGER.info("Changed view button to 'View All'");

                // Restore both sections
                featuredSection.setVisible(true);
                featuredSection.setManaged(true);
                recommendedSection.setVisible(true);
                recommendedSection.setManaged(true);

                // Reset section titles
                updateSectionTitle(featuredSection, "Featured Books");
                updateSectionTitle(recommendedSection, "Recommended For You");

                // Reload the initial limited books
                loadBooks();

                LOGGER.info("Returned to main view successfully");
                return; // Exit early after handling back action
            }

            // Reset pagination when switching sections
            if (currentSection == null || !currentSection.equals(section)) {
                currentFeaturedPage = 0;
                currentRecommendedPage = 0;
                currentSection = section;
            }

            if (section.equals("featured")) {
                targetContainer = featuredBooks;
                title = "Featured Books";

                // Hide recommended section
                recommendedSection.setVisible(false);
                recommendedSection.setManaged(false);

                // Show only featured section
                featuredSection.setVisible(true);
                featuredSection.setManaged(true);

                // Load featured books if not already loaded
                if (allFeaturedBooks == null) {
                    allFeaturedBooks = BookDetailsCollection.getFeaturedBooks().stream()
                            .filter(Objects::nonNull)
                            .sorted((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()))
                            .collect(Collectors.toList());
                    LOGGER.info("Featured books found: " + allFeaturedBooks.size());
                }

                // Get books for the current page
                List<Book> booksToShow = getPaginatedBooks(allFeaturedBooks, currentFeaturedPage);

                // Clear the container first
                targetContainer.getChildren().clear();

                // Load books
                loadBookSection(targetContainer, booksToShow);

                // Add "Load More" button if there are more books
                addLoadMoreButton(targetContainer, section, allFeaturedBooks.size());

                // Update section title
                updateSectionTitle(featuredSection, title);

            } else {
                targetContainer = recommendedBooks;
                title = "Recommended Books";

                // Add debug logging
                LOGGER.info("Loading recommended books section");

                // Hide featured section
                featuredSection.setVisible(false);
                featuredSection.setManaged(false);

                // Show only recommended section
                recommendedSection.setVisible(true);
                recommendedSection.setManaged(true);

                // Load all books if not already loaded
                if (allRecommendedBooks == null) {
                    LOGGER.info("Retrieving books from database...");
                    List<Book> allBooks = BookDetailsCollection.getAllBooks();

                    if (allBooks == null) {
                        LOGGER.severe("getAllBooks returned null");
                        showAlert(Alert.AlertType.ERROR, "Data Error", "Failed to retrieve books from database");
                        return;
                    }

                    LOGGER.info("Total books retrieved: " + allBooks.size());

                    // Implement a safer sorting approach with explicit null checks
                    allRecommendedBooks = allBooks.stream()
                            .filter(book -> book != null)
                            .sorted((b1, b2) -> {
                                // Safe comparison that handles potential null or invalid values
                                try {
                                    return Double.compare(
                                            b2.getReviewCount() != null ? b2.getReviewCount() : 0,
                                            b1.getReviewCount() != null ? b1.getReviewCount() : 0);
                                } catch (Exception e) {
                                    LOGGER.warning("Error comparing review counts: " + e.getMessage());
                                    return 0; // Keep original order if comparison fails
                                }
                            })
                            .collect(Collectors.toList());

                    LOGGER.info("Recommended books sorted: " + allRecommendedBooks.size());
                }

                // Check if we have any books to display
                if (allRecommendedBooks.isEmpty()) {
                    LOGGER.warning("No recommended books to display");
                    showAlert(Alert.AlertType.INFORMATION, "No Books", "No recommended books found to display");
                    return;
                }

                // Get books for the current page
                List<Book> booksToShow = getPaginatedBooks(allRecommendedBooks, currentRecommendedPage);

                // Clear the container first
                targetContainer.getChildren().clear();

                // Load books
                loadBookSection(targetContainer, booksToShow);

                // Add "Load More" button if there are more books
                addLoadMoreButton(targetContainer, section, allRecommendedBooks.size());

                // Update section title
                updateSectionTitle(recommendedSection, title);
                LOGGER.info("Recommended section title updated");
            }

            // Update button text to "Back" if not already showing all books
            if (viewAllButton != null && !showingAllBooks) {
                viewAllButton.setText("Back");
                showingAllBooks = true;
                LOGGER.info("Changed view button to 'Back'");
            }

            LOGGER.info("loadAllBooks completed successfully for section: " + section);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading all books", e);
            showAlert(Alert.AlertType.ERROR, "Loading Error", "Failed to load all books: " + e.getMessage());
        }
    }

    private List<Book> getPaginatedBooks(List<Book> allBooks, int page) {
        int startIndex = page * BOOKS_PER_PAGE;
        int endIndex = Math.min(startIndex + BOOKS_PER_PAGE, allBooks.size());

        if (startIndex >= allBooks.size()) {
            return List.of(); // Return empty list if we're past the end
        }

        return allBooks.subList(startIndex, endIndex);
    }

    private void addLoadMoreButton(HBox container, String section, int totalBooks) {
        int currentPage = section.equals("featured") ? currentFeaturedPage : currentRecommendedPage;
        int currentCount = (currentPage + 1) * BOOKS_PER_PAGE;

        // If we've shown all books, don't add the button
        if (currentCount >= totalBooks) {
            return;
        }

        // Create "Load More" button
        Button loadMoreButton = new Button("Load More");
        loadMoreButton.getStyleClass().add("load-more-button");
        loadMoreButton.setCursor(Cursor.HAND);
        loadMoreButton.setStyle(
                "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20; -fx-font-size: 14px; -fx-background-radius: 5;");

        // Create VBox to contain the button and center it
        VBox buttonContainer = new VBox(loadMoreButton);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPrefWidth(180); // Set width similar to book cards
        buttonContainer.setPrefHeight(280); // Set height similar to book cards

        // Add the button container to the main container
        container.getChildren().add(buttonContainer);

        // Set button action
        loadMoreButton.setOnAction(e -> {
            // Increment the current page
            if (section.equals("featured")) {
                currentFeaturedPage++;

                // Get the next batch of books
                List<Book> moreFeaturedBooks = getPaginatedBooks(allFeaturedBooks, currentFeaturedPage);

                // Remove the "Load More" button first
                container.getChildren().remove(buttonContainer);

                // Add the new books to the container
                loadBookSection(container, moreFeaturedBooks);

                // Add the "Load More" button again if there are still more books
                addLoadMoreButton(container, section, allFeaturedBooks.size());

            } else {
                currentRecommendedPage++;

                // Get the next batch of books
                List<Book> moreRecommendedBooks = getPaginatedBooks(allRecommendedBooks, currentRecommendedPage);

                // Remove the "Load More" button first
                container.getChildren().remove(buttonContainer);

                // Add the new books to the container
                loadBookSection(container, moreRecommendedBooks);

                // Add the "Load More" button again if there are still more books
                addLoadMoreButton(container, section, allRecommendedBooks.size());
            }
        });
    }

    public void handleBackFromViewAll() {
        try {
            // Reset pagination
            currentFeaturedPage = 0;
            currentRecommendedPage = 0;
            currentSection = null;
            allFeaturedBooks = null;
            allRecommendedBooks = null;

            // Find the parent sections
            VBox featuredSection = findSectionParent(featuredBooks);
            VBox recommendedSection = findSectionParent(recommendedBooks);

            if (featuredSection != null && recommendedSection != null) {
                // Restore both sections
                featuredSection.setVisible(true);
                featuredSection.setManaged(true);
                recommendedSection.setVisible(true);
                recommendedSection.setManaged(true);

                // Reset section titles
                updateSectionTitle(featuredSection, "Featured Books");
                updateSectionTitle(recommendedSection, "Recommended For You");
            }

            // Reset view buttons
            if (viewAllFeaturedButton != null) {
                viewAllFeaturedButton.setText("View All");
            }
            if (viewAllRecommendedButton != null) {
                viewAllRecommendedButton.setText("View All");
            }

            showingAllBooks = false;

            // Reload the initial limited books
            loadBooks();

            LOGGER.info("Returned to main view successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error returning to main view", e);
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to return to main view: " + e.getMessage());
        }
    }

    /**
     * Helper method to find the parent VBox section containing the book container
     * This traverses up the scene graph to find the VBox that should be the section
     */
    private VBox findSectionParent(HBox bookContainer) {
        if (bookContainer == null)
            return null;

        // Walk up the parent chain looking for the VBox section
        javafx.scene.Parent parent = bookContainer.getParent();
        while (parent != null) {
            // If we find a ScrollPane, go up one more level to find its container
            if (parent instanceof ScrollPane) {
                parent = parent.getParent();
                // The parent of the ScrollPane should be the VBox we're looking for
                if (parent instanceof VBox) {
                    return (VBox) parent;
                }
            }
            // Keep going up
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Helper method to update the section title
     */
    private void updateSectionTitle(VBox section, String newTitle) {
        if (section == null || section.getChildren().isEmpty())
            return;

        // The first child should be the HBox containing the title
        javafx.scene.Node firstChild = section.getChildren().get(0);
        if (firstChild instanceof HBox) {
            HBox titleBox = (HBox) firstChild;
            if (!titleBox.getChildren().isEmpty()) {
                javafx.scene.Node titleNode = titleBox.getChildren().get(0);
                if (titleNode instanceof Label) {
                    ((Label) titleNode).setText(newTitle);
                }
            }
        }
    }

    private Image loadImage(String imageUrl) {
        try {
            InputStream stream = getClass().getResourceAsStream(imageUrl);
            if (stream != null) {
                return new Image(stream);
            }
            LOGGER.warning("Could not find image resource: " + imageUrl);
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading image " + imageUrl, e);
            return null;
        }
    }

    private void startImageSlideshow() {
        try {
            slideshowThread = new Thread(() -> {
                while (slideshowRunning) {
                    try {
                        Thread.sleep(3000);

                        if (!slideshowRunning)
                            break;

                        Platform.runLater(() -> {
                            try {
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
                                TranslateTransition slideOut = new TranslateTransition(Duration.seconds(0.5),
                                        bannerImage);
                                slideOut.setFromX(0);
                                slideOut.setFromY(0);
                                slideOut.setToX(bannerImage.getFitWidth());
                                slideOut.setToY(-bannerImage.getFitHeight());

                                slideOut.setOnFinished(event -> {
                                    try {
                                        // Update the image and index
                                        bannerImage.setImage(nextImage);

                                        // Create and play the slide in animation
                                        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.5),
                                                bannerImage);
                                        slideIn.setFromX(-bannerImage.getFitWidth());
                                        slideIn.setFromY(bannerImage.getFitHeight());
                                        slideIn.setToX(0);
                                        slideIn.setToY(0);
                                        slideIn.play();
                                    } catch (Exception e) {
                                        LOGGER.log(Level.WARNING, "Error in slideshow animation", e);
                                    }
                                });

                                slideOut.play();
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Error in slideshow thread", e);
                            }
                        });

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error in slideshow thread", e);
                    }
                }
            });

            slideshowThread.setDaemon(true);
            slideshowThread.start();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error starting slideshow thread", e);
        }
    }

    public void stopSlideshow() {
        slideshowRunning = false;
        if (slideshowThread != null) {
            slideshowThread.interrupt();
        }
    }

    private void animateNavLogo() {
        try {
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
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error animating nav logo", e);
            // Continue execution as this is not critical
        }
    }

    private void addSearchListener() {
        try {
            if (searchField != null) {
                // Add a key press event listener to detect Enter key
                searchField.setOnKeyPressed(event -> {
                    if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                        performSearch();
                    }
                });

                // Enable live search after typing 3+ characters
                searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                    try {
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

                                // Also hide the search results section to remove the space
                                if (searchResultsSection != null) {
                                    searchResultsSection.setVisible(false);
                                    searchResultsSection.setManaged(false);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error in search listener", e);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error setting up search listener", e);
        }
    }

    @Override
    @FXML
    public void performSearch() {
        try {
            String searchTerm = searchField.getText().trim();
            if (!searchTerm.isEmpty()) {
                LOGGER.info("Searching for: " + searchTerm);

                // Show the search results section
                if (searchResultsSection != null) {
                    searchResultsSection.setVisible(true);
                    searchResultsSection.setManaged(true);
                }

                // Use the search implementation class to perform the search
                if (searchImplementation != null) {
                    searchImplementation.performSearch(searchTerm);
                } else {
                    LOGGER.warning("Search implementation is not initialized");
                    showAlert(Alert.AlertType.ERROR, "Search Error", "Search implementation is not initialized");
                }
            } else {
                // Handle empty search term
                LOGGER.info("Empty search term entered");
                if (searchImplementation != null) {
                    searchImplementation.clearResults();

                    // Hide the search results section
                    if (searchResultsSection != null) {
                        searchResultsSection.setVisible(false);
                        searchResultsSection.setManaged(false);
                    }
                }
                showAlert(Alert.AlertType.INFORMATION, "Search", "Please enter a search term");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error performing search", e);
            showAlert(Alert.AlertType.ERROR, "Error performing search", e.getMessage());
        }
    }

    private void loadBooks() {
        try {
            // Fetch books from MongoDB
            List<Book> featuredBooksList;

            // Try to get from featured flag first
            featuredBooksList = BookDetailsCollection.getFeaturedBooks().stream()
                    .filter(Objects::nonNull)
                    .sorted((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()))
                    .limit(BOOKS_PER_PAGE)
                    .collect(Collectors.toList());

            // If no featured books found, fall back to the pagination method
            if (featuredBooksList.isEmpty()) {
                featuredBooksList = BookDetailsCollection.getBooksByPagination(0, BOOKS_PER_PAGE).stream()
                        .filter(Objects::nonNull)
                        .sorted((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()))
                        .collect(Collectors.toList());
            }

            List<Book> recommendedBooksList = BookDetailsCollection.getBooksByPagination(0, BOOKS_PER_PAGE * 2)
                    .stream()
                    .filter(Objects::nonNull)
                    .sorted((b1, b2) -> Double.compare(b2.getReviewCount(), b1.getReviewCount()))
                    .limit(BOOKS_PER_PAGE)
                    .collect(Collectors.toList());

            // Load the book sections with data
            loadBookSection(featuredBooks, featuredBooksList);
            loadBookSection(recommendedBooks, recommendedBooksList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading books", e);
            showAlert(Alert.AlertType.ERROR, "Loading Error", "Failed to load books: " + e.getMessage());
        }
    }

    private void loadBookSection(HBox container, List<Book> books) {
        try {
            if (container != null && books != null) {
                container.getChildren().clear();
                books.forEach(book -> {
                    if (book != null) {
                        container.getChildren().add(createBookCard(book));
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading book section", e);
            // Don't show an alert here as it might be too intrusive
        }
    }

    @Override
    public void handleAddToCart(Book book) {
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

    // Add a dispose method to clean up resources
    public void dispose() {
        stopSlideshow();
        // Add any other resource cleanup needed
    }
}