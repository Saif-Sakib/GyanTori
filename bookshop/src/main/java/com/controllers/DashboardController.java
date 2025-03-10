package com.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.database.BookDetailsCollection;
import com.database.DatabaseManager;
import com.models.Book;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import com.services.SessionManager;
import javafx.scene.image.Image;

import java.io.File;
import javafx.stage.Stage;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

public class DashboardController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String DEFAULT_PROFILE_IMAGE = "/com/images/userDP/default-profile.png";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    // Cache for user names to avoid repeated DB lookups
    private final Map<String, String> userNameCache = new ConcurrentHashMap<>();

    @FXML
    private ImageView profilePic;
    @FXML
    private StackPane profilePicContainer;
    @FXML
    private Label nameLabel;
    @FXML
    private Label locationLabel;
    @FXML
    private Label bookHubIdLabel;
    @FXML
    private Button homeButton;
    @FXML
    private Button editInfoButton;
    @FXML
    private Label totalUploadsLabel;
    @FXML
    private Label totalBorrowedLabel;
    @FXML
    private Label buyerRatingLabel;
    @FXML
    private TableView<BookTableWrapper> uploadedBooksTable;
    @FXML
    private TableView<BookTableWrapper> borrowedBooksTable;
    @FXML
    private ListView<ReviewWrapper> reviewsList;
    @FXML
    private Button logoutButton;
    @FXML
    private Label lastUpdateLabel;
    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private TableColumn<BookTableWrapper, String> uploadedTitleColumn;
    @FXML
    private TableColumn<BookTableWrapper, LocalDate> uploadDateColumn;
    @FXML
    private TableColumn<BookTableWrapper, Double> priceColumn;
    @FXML
    private TableColumn<BookTableWrapper, Integer> totalPurchasesColumn;
    @FXML
    private TableColumn<BookTableWrapper, Double> revenueColumn;

    @FXML
    private TableColumn<BookTableWrapper, String> borrowedTitleColumn;
    @FXML
    private TableColumn<BookTableWrapper, String> sellerNameColumn;
    @FXML
    private TableColumn<BookTableWrapper, LocalDate> borrowDateColumn;
    @FXML
    private TableColumn<BookTableWrapper, Integer> daysRemainingColumn;
    @FXML
    private TableColumn<BookTableWrapper, String> statusColumn;

    // Wrapper class for TableView display
    public static class BookTableWrapper {
        private final Book book;
        private final LocalDate uploadDate;
        private final LocalDate borrowDate;
        private final LocalDate returnDate;
        private final String sellerName;
        private final int daysRemaining;
        private final String status;
        private final double revenue;

        public BookTableWrapper(Book book, String sellerName) {
            this.book = book;
            this.uploadDate = parseDate(book.getUploadDate(), LocalDate.now());
            this.borrowDate = parseDate(book.getBorrowDate(), LocalDate.now().minusDays(30));
            this.returnDate = parseDate(book.getReturnDate(), LocalDate.now().plusDays(14));
            this.sellerName = sellerName != null ? sellerName : "Unknown";
            this.daysRemaining = (int) Math.max(0, ChronoUnit.DAYS.between(LocalDate.now(), this.returnDate));
            this.status = this.daysRemaining > 0 ? "Active" : "Overdue";
            this.revenue = book.getCurrentPrice() * book.getTotalPurchases();
        }

        // Getter methods for TableView
        public String getTitle() {
            return book.getTitle();
        }

        public LocalDate getUploadDate() {
            return uploadDate;
        }

        public double getPrice() {
            return book.getCurrentPrice();
        }

        public int getTotalPurchases() {
            return book.getTotalPurchases();
        }

        public double getRevenue() {
            return revenue;
        }

        public String getSellerName() {
            return sellerName;
        }

        public LocalDate getBorrowDate() {
            return borrowDate;
        }

        public LocalDate getReturnDate() {
            return returnDate;
        }

        public int getDaysRemaining() {
            return daysRemaining;
        }

        public String getStatus() {
            return status;
        }

        public Book getBook() {
            return book;
        }
    }

    // Wrapper class for Review display
    public static class ReviewWrapper {
        private final Book.Review review;
        private final String reviewerName;
        private final String bookTitle;

        public ReviewWrapper(Book.Review review, String reviewerName, String bookTitle) {
            this.review = review;
            this.reviewerName = reviewerName != null ? reviewerName : "Anonymous";
            this.bookTitle = bookTitle != null ? bookTitle : "Unknown Book";
        }

        public String getReviewerName() {
            return reviewerName;
        }

        public int getRating() {
            return safeRating(review.getRating());
        }

        public String getComment() {
            return review.getComment() != null ? review.getComment() : "No comment provided";
        }

        public LocalDate getReviewDate() {
            return review.getReviewDate() != null ? review.getReviewDate() : LocalDate.now();
        }

        public String getBookTitle() {
            return bookTitle;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing dashboard controller...");

        // Show loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }

        // Setup UI components
        setupTables();
        setupReviewsList();
        updateLastUpdateLabel();


        // Load data asynchronously
        String userName = SessionManager.getInstance().getUserName();
        loadDashboardDataAsync(userName);
    }

    private void loadDashboardDataAsync(String userName) {
        Task<Void> loadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    // Load everything in background
                    Document userDoc = loadUserDocument(userName);

                    if (userDoc == null) {
                        Platform.runLater(() -> showError("Data Error", "User not found."));
                        return null;
                    }

                    // Get user ID
                    String userId = getStringId(userDoc, "id");

                    // Load data in parallel
                    CompletableFuture<Document> profileFuture = CompletableFuture.supplyAsync(() -> userDoc);
                    CompletableFuture<List<Book>> uploadedBooksFuture = CompletableFuture
                            .supplyAsync(() -> BookDetailsCollection.getBooksBySellerId(userId));
                    CompletableFuture<List<Book>> borrowedBooksFuture = CompletableFuture
                            .supplyAsync(() -> BookDetailsCollection.getBooksByHolderId(userId));

                    // When profile data is ready, update UI on JavaFX thread
                    profileFuture.thenAcceptAsync(user -> {
                        updateProfileUI(user);
                        updateUserStatistics(user);
                    }, Platform::runLater);

                    // When uploaded books are ready
                    uploadedBooksFuture.thenAcceptAsync(books -> {
                        updateUploadedBooksTable(books);
                        loadReviews(books);
                    }, Platform::runLater);

                    // When borrowed books are ready
                    borrowedBooksFuture.thenAcceptAsync(books -> updateBorrowedBooksTable(books), Platform::runLater);

                    // Wait for all async operations to complete
                    CompletableFuture.allOf(profileFuture, uploadedBooksFuture, borrowedBooksFuture).join();

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error loading dashboard data", e);
                    Platform.runLater(
                            () -> showError("Data Error", "Failed to load dashboard data: " + e.getMessage()));
                }
                return null;
            }

            @Override
            protected void succeeded() {
                // Hide loading indicator when done
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
            }

            @Override
            protected void failed() {
                // Hide loading indicator on failure
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                LOGGER.log(Level.SEVERE, "Failed to load dashboard data", getException());
                Platform.runLater(() -> showError("Data Error", "Failed to load dashboard data"));
            }
        };

        // Start background task
        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private Document loadUserDocument(String userName) {
        if (userName == null || userName.isEmpty()) {
            LOGGER.log(Level.SEVERE, "Username is null or empty");
            return null;
        }

        try {
            MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
            return users.find(Filters.eq("username", userName)).first();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading user document", e);
            return null;
        }
    }

    private void setupReviewsList() {
        // Use a more efficient cell factory with view recycling
        reviewsList.setCellFactory(param -> new ListCell<ReviewWrapper>() {
            private final VBox container = new VBox(5);
            private final HBox header = new HBox(10);
            private final Label nameLabel = new Label();
            private final Label dateLabel = new Label();
            private final Label bookLabel = new Label();
            private final HBox stars = new HBox(2);
            private final Label[] starLabels = new Label[5];
            private final Label commentLabel = new Label();

            {
                // Initialize components once
                container.setPadding(new Insets(10));
                container.getStyleClass().add("review-cell");

                nameLabel.setStyle("-fx-font-weight: bold;");
                dateLabel.setStyle("-fx-text-fill: #666;");
                header.getChildren().addAll(nameLabel, dateLabel);

                bookLabel.setStyle("-fx-font-style: italic;");

                stars.setAlignment(Pos.CENTER_LEFT);
                for (int i = 0; i < 5; i++) {
                    starLabels[i] = new Label("☆");
                    starLabels[i].setStyle("-fx-text-fill: #ccc; -fx-font-size: 14px;");
                    stars.getChildren().add(starLabels[i]);
                }

                commentLabel.setWrapText(true);

                container.getChildren().addAll(header, bookLabel, stars, commentLabel);
            }

            @Override
            protected void updateItem(ReviewWrapper review, boolean empty) {
                super.updateItem(review, empty);

                if (empty || review == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // Update existing components instead of creating new ones
                nameLabel.setText(review.getReviewerName());
                dateLabel.setText(review.getReviewDate().format(DATE_FORMATTER));
                bookLabel.setText("Book: " + review.getBookTitle());

                // Update star ratings
                for (int i = 0; i < 5; i++) {
                    boolean filled = i < review.getRating();
                    starLabels[i].setText(filled ? "★" : "☆");
                    starLabels[i].setStyle(filled ? "-fx-text-fill: gold; -fx-font-size: 14px;"
                            : "-fx-text-fill: #ccc; -fx-font-size: 14px;");
                }

                commentLabel.setText(review.getComment());

                setGraphic(container);
                setText(null);
            }
        });

        reviewsList.getStyleClass().add("reviews-list");
    }

    private void setupTables() {
        // Setup Uploaded Books Table
        uploadedTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        uploadDateColumn.setCellValueFactory(new PropertyValueFactory<>("uploadDate"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        totalPurchasesColumn.setCellValueFactory(new PropertyValueFactory<>("totalPurchases"));
        revenueColumn.setCellValueFactory(new PropertyValueFactory<>("revenue"));

        // Setup Borrowed Books Table
        borrowedTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        sellerNameColumn.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        borrowDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        daysRemainingColumn.setCellValueFactory(new PropertyValueFactory<>("daysRemaining"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Make tables responsive
        uploadedBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        borrowedBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void updateProfileUI(Document user) {
        try {
            // Extract user info with null checks
            String name = user.getString("full_name");
            String location = user.containsKey("location") ? user.getString("location") : "Not Set";
            String id = user.getString("id");
            String imagePath = user.containsKey("imgPath") ? user.getString("imgPath") : DEFAULT_PROFILE_IMAGE;

            // Update UI
            nameLabel.setText(name != null ? name : "Unknown");
            locationLabel.setText(location);
            bookHubIdLabel.setText("ID: " + id);
            updateProfilePicture(imagePath);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating profile UI", e);
        }
    }

    private void updateUserStatistics(Document user) {
        try {
            // Extract statistics from user document with safe handling
            int uploads = user.containsKey("uploaded_books") ? ((List<?>) user.get("uploaded_books")).size() : 0;
            int borrowed = user.containsKey("borrowed_books") ? ((List<?>) user.get("borrowed_books")).size() : 0;
            Double ratingObj = user.getDouble("rating");
            double rating = (ratingObj != null) ? ratingObj : 0.0;

            // Update UI
            totalUploadsLabel.setText(String.valueOf(uploads));
            totalBorrowedLabel.setText(String.valueOf(borrowed));
            buyerRatingLabel.setText(String.format("%.1f", rating));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating user statistics", e);
        }
    }

    private void updateUploadedBooksTable(List<Book> uploadedBooks) {
        try {
            // Skip if no books
            if (uploadedBooks == null || uploadedBooks.isEmpty()) {
                uploadedBooksTable.setItems(FXCollections.observableArrayList());
                return;
            }

            // Sort books by review count (high to low) and wrap with our adapter
            List<BookTableWrapper> bookWrappers = uploadedBooks.stream()
                    .sorted((b1, b2) -> Double.compare(b2.getReviewCount(), b1.getReviewCount()))
                    .map(book -> new BookTableWrapper(book, null))
                    .collect(Collectors.toList());

            // Update table in one batch operation
            uploadedBooksTable.setItems(FXCollections.observableArrayList(bookWrappers));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating uploaded books table", e);
        }
    }

    private void updateBorrowedBooksTable(List<Book> borrowedBooks) {
        try {
            // Skip if no books
            if (borrowedBooks == null || borrowedBooks.isEmpty()) {
                borrowedBooksTable.setItems(FXCollections.observableArrayList());
                return;
            }

            // Wrap books with our adapter
            List<BookTableWrapper> bookWrappers = borrowedBooks.stream()
                    .map(book -> {
                        // Get seller name (using cache)
                        String sellerName = getSellerNameFromCache(book.getSellerId());
                        return new BookTableWrapper(book, sellerName);
                    })
                    .collect(Collectors.toList());

            // Update table in one batch operation
            borrowedBooksTable.setItems(FXCollections.observableArrayList(bookWrappers));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating borrowed books table", e);
        }
    }

    private void loadReviews(List<Book> books) {
        try {
            if (books == null || books.isEmpty()) {
                // Add placeholder if no books
                addPlaceholderReview();
                return;
            }

            // Extract and wrap all reviews at once
            List<ReviewWrapper> allReviewWrappers = books.stream()
                    .filter(book -> book.getBuyerReviews() != null && !book.getBuyerReviews().isEmpty())
                    .flatMap(book -> book.getBuyerReviews().stream()
                            .filter(review -> review != null)
                            .map(review -> {
                                String reviewerName = getReviewerNameFromCache(review.getReviewerId());
                                return new ReviewWrapper(review, reviewerName, book.getTitle());
                            }))
                    .collect(Collectors.toList());

            // Add placeholder if no reviews
            if (allReviewWrappers.isEmpty()) {
                addPlaceholderReview();
                return;
            }

            // Update list in one batch operation
            reviewsList.setItems(FXCollections.observableArrayList(allReviewWrappers));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading reviews", e);
        }
    }

    private void addPlaceholderReview() {
        // Create a placeholder Book.Review
        Book.Review placeholderReview = new Book.Review(
                "no-reviewer",
                "No reviews have been posted for your books yet.",
                0,
                LocalDate.now());

        // Wrap it and add to the list
        ReviewWrapper wrapper = new ReviewWrapper(placeholderReview, "No Reviews Yet", "N/A");
        reviewsList.setItems(FXCollections.observableArrayList(wrapper));
    }

    // Cached name lookup
    private String getSellerNameFromCache(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) {
            return "Unknown";
        }

        return userNameCache.computeIfAbsent(sellerId, id -> {
            try {
                MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
                Document seller = users.find(Filters.eq("id", id)).first();
                return (seller != null && seller.getString("name") != null) ? seller.getString("name") : id;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error fetching seller name", e);
                return id;
            }
        });
    }

    // Cached reviewer name lookup
    private String getReviewerNameFromCache(String reviewerId) {
        if (reviewerId == null || reviewerId.isEmpty()) {
            return "Anonymous";
        }

        return userNameCache.computeIfAbsent(reviewerId, id -> {
            try {
                MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
                Document reviewer = users.find(Filters.eq("id", id)).first();
                return (reviewer != null && reviewer.getString("name") != null) ? reviewer.getString("name") : id;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error fetching reviewer name", e);
                return id;
            }
        });
    }

    public void updateProfilePicture(String imagePath) {
        try {
            // Load image asynchronously
            Task<Image> imageTask = new Task<Image>() {
                @Override
                protected Image call() throws Exception {
                    try {
                        if (imagePath == null || imagePath.isEmpty()) {
                            throw new IllegalArgumentException("Image path is empty");
                        }

                        File imageFile = new File(imagePath);
                        if (!imageFile.exists()) {
                            throw new IllegalArgumentException("Image not found");
                        }

                        return new Image(imageFile.toURI().toString(),
                                100, 100, true, true, true); // Use background loading
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Using default profile image", e);
                        return new Image(getClass().getResourceAsStream(DEFAULT_PROFILE_IMAGE),
                                100, 100, true, true);
                    }
                }

                @Override
                protected void succeeded() {
                    // Apply the image with circular clip on UI thread
                    Image newImage = getValue();
                    profilePicContainer.setAlignment(Pos.CENTER);
                    profilePic.setImage(newImage);
                    profilePic.setFitHeight(100);
                    profilePic.setFitWidth(100);
                    profilePic.setPreserveRatio(true);

                    // Create circular clip
                    Circle clip = new Circle(50, 50, 50);
                    profilePic.setClip(clip);

                    // Add hover effect
                    addHoverEffect(profilePic);
                }
            };

            Thread imageThread = new Thread(imageTask);
            imageThread.setDaemon(true);
            imageThread.start();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating profile picture", e);
        }
    }

    private void addHoverEffect(ImageView imageView) {
        imageView.setOnMouseEntered(event -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), imageView);
            scaleTransition.setToX(1.1);
            scaleTransition.setToY(1.1);
            scaleTransition.play();
        });

        imageView.setOnMouseExited(event -> {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), imageView);
            scaleTransition.setToX(1.0);
            scaleTransition.setToY(1.0);
            scaleTransition.play();
        });
    }

    private void updateLastUpdateLabel() {
        lastUpdateLabel.setText("Last updated: " + LocalDate.now().format(DATE_FORMATTER));
    }

    @FXML
    private void handleEditInfo() {
        try {
            Stage currentStage = (Stage) editInfoButton.getScene().getWindow();
            LoadPageController.loadScene("editProfile.fxml", "editProfile.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to edit profile", e);
            showError("Navigation Error", "Failed to open edit profile screen.");
        }
    }

    @FXML
    private void handleHome() {
        try {
            Stage currentStage = (Stage) homeButton.getScene().getWindow();
            LoadPageController.loadScene("home.fxml", "home.css", currentStage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating to home", e);
            showError("Navigation Error", "Failed to navigate to home screen.");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            new HomeController().logout();
            handleHome();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during logout", e);
            showError("Logout Error", "Failed to process logout.");
        }
    }

    // Helper methods

    private static int safeRating(double rawRating) {
        int rating = (int) Math.round(rawRating);
        return Math.max(1, Math.min(5, rating));
    }

    private static LocalDate parseDate(String dateStr, LocalDate defaultDate) {
        if (dateStr == null || dateStr.isEmpty()) {
            return defaultDate;
        }

        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            Logger.getLogger(DashboardController.class.getName())
                    .log(Level.WARNING, "Invalid date format: " + dateStr, e);
            return defaultDate;
        }
    }

    private String getStringId(Document doc, String fieldName) {
        Object idObj = doc.get(fieldName);
        if (idObj == null) {
            return "";
        } else if (idObj instanceof String) {
            return (String) idObj;
        } else if (idObj instanceof ObjectId) {
            return ((ObjectId) idObj).toHexString();
        } else {
            return idObj.toString();
        }
    }

    private void showError(String title, String content) {
        try {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error showing error dialog", e);
            System.err.println("Failed to show error dialog: " + title + " - " + content);
        }
    }
}