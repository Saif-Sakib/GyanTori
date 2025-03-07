package com.controllers;

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

import com.database.BooksDetailsCollection;
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
    private TableView<UploadedBook> uploadedBooksTable;
    @FXML
    private TableView<BorrowedBook> borrowedBooksTable;
    @FXML
    private ListView<Review> reviewsList;
    @FXML
    private Button logoutButton;
    @FXML
    private Label lastUpdateLabel;

    @FXML
    private TableColumn<UploadedBook, String> uploadedTitleColumn;
    @FXML
    private TableColumn<UploadedBook, LocalDate> uploadDateColumn;
    @FXML
    private TableColumn<UploadedBook, Double> priceColumn;
    @FXML
    private TableColumn<UploadedBook, Integer> totalPurchasesColumn;
    @FXML
    private TableColumn<UploadedBook, Double> revenueColumn;

    @FXML
    private TableColumn<BorrowedBook, String> borrowedTitleColumn;
    @FXML
    private TableColumn<BorrowedBook, String> sellerNameColumn;
    @FXML
    private TableColumn<BorrowedBook, LocalDate> borrowDateColumn;
    @FXML
    private TableColumn<BorrowedBook, Integer> daysRemainingColumn;
    @FXML
    private TableColumn<BorrowedBook, String> statusColumn;

    // Data classes
    public static class UploadedBook {
        private final String title;
        private final LocalDate uploadDate;
        private final double price;
        private final int totalPurchases;
        private final double revenue;

        public UploadedBook(String title, LocalDate uploadDate, double price, int totalPurchases) {
            if (title == null || uploadDate == null) {
                throw new IllegalArgumentException("Title and upload date cannot be null");
            }
            if (price < 0 || totalPurchases < 0) {
                throw new IllegalArgumentException("Price and total purchases cannot be negative");
            }
            this.title = title;
            this.uploadDate = uploadDate;
            this.price = price;
            this.totalPurchases = totalPurchases;
            this.revenue = price * totalPurchases;
        }

        public String getTitle() {
            return title;
        }

        public LocalDate getUploadDate() {
            return uploadDate;
        }

        public double getPrice() {
            return price;
        }

        public int getTotalPurchases() {
            return totalPurchases;
        }

        public double getRevenue() {
            return revenue;
        }
    }

    public static class BorrowedBook {
        private final String title;
        private final String sellerName;
        private final LocalDate borrowDate;
        private final LocalDate returnDate;
        private final int daysRemaining;
        private final String status;

        public BorrowedBook(String title, String sellerName, LocalDate borrowDate, LocalDate returnDate,
                int daysRemaining, String status) {
            if (title == null || sellerName == null || borrowDate == null || returnDate == null || status == null) {
                throw new IllegalArgumentException(
                        "Title, seller name, borrow date, return date, and status cannot be null");
            }
            this.title = title;
            this.sellerName = sellerName;
            this.borrowDate = borrowDate;
            this.returnDate = returnDate;
            this.daysRemaining = Math.max(0, daysRemaining); // Ensure non-negative
            this.status = status;
        }

        public String getTitle() {
            return title;
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
    }

    public static class Review {
        private final String reviewerName;
        private final int rating;
        private final String comment;
        private final LocalDate reviewDate;
        private final String bookTitle;

        public Review(String reviewerName, int rating, String comment, LocalDate reviewDate, String bookTitle) {
            this.reviewerName = reviewerName != null ? reviewerName : "Anonymous";
            this.rating = (rating >= 1 && rating <= 5) ? rating : 1;
            this.comment = comment != null ? comment : "No comment provided";
            this.reviewDate = reviewDate != null ? reviewDate : LocalDate.now();
            this.bookTitle = bookTitle != null ? bookTitle : "Unknown Book";
        }

        public String getReviewerName() {
            return reviewerName;
        }

        public int getRating() {
            return rating;
        }

        public String getComment() {
            return comment;
        }

        public String getBookTitle() {
            return bookTitle;
        }

        public LocalDate getReviewDate() {
            return reviewDate;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing dashboard controller...");

        try {
            String userName = SessionManager.getInstance().getUserName();
            setupTables();
            setProfileInfo(userName);
            loadUserData(userName);
            setupReviewsList();
            updateLastUpdateLabel();

            LOGGER.info("Dashboard initialization completed successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing dashboard", e);
            showError("Initialization Error", "Failed to initialize dashboard: " + e.getMessage());
        }
    }

    private void setupReviewsList() {
        reviewsList.setCellFactory(param -> new ListCell<Review>() {
            @Override
            protected void updateItem(Review review, boolean empty) {
                super.updateItem(review, empty);

                if (empty || review == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                VBox container = new VBox(5);
                container.setPadding(new Insets(10));
                container.getStyleClass().add("review-cell");

                // Header with name and date
                HBox header = new HBox(10);
                Label nameLabel = new Label(review.getReviewerName());
                nameLabel.setStyle("-fx-font-weight: bold;");

                Label dateLabel = new Label(review.getReviewDate().format(DATE_FORMATTER));
                dateLabel.setStyle("-fx-text-fill: #666;");

                header.getChildren().addAll(nameLabel, dateLabel);

                // Book title
                Label bookLabel = new Label("Book: " + review.getBookTitle());
                bookLabel.setStyle("-fx-font-style: italic;");

                // Rating stars
                HBox stars = new HBox(2);
                stars.setAlignment(Pos.CENTER_LEFT);

                for (int i = 0; i < 5; i++) {
                    Label star = new Label(i < review.getRating() ? "★" : "☆");
                    star.setStyle(i < review.getRating() ? "-fx-text-fill: gold; -fx-font-size: 14px;"
                            : "-fx-text-fill: #ccc; -fx-font-size: 14px;");
                    stars.getChildren().add(star);
                }

                // Comment
                Label commentLabel = new Label(review.getComment());
                commentLabel.setWrapText(true);

                container.getChildren().addAll(header, bookLabel, stars, commentLabel);
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

    private void loadUserData(String userName) {
        try {
            if (userName == null || userName.isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty");
            }

            MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
            Document user = users.find(Filters.eq("username", userName)).first();

            if (user == null) {
                showError("Data Error", "User not found.");
                return;
            }

            // Get user ID
            String userId = getStringId(user, "id");

            // Load uploaded books, borrowed books, and statistics
            loadUploadedBooks(userId);
            loadBorrowedBooks(userId);
            loadUserStatistics(user);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading user data", e);
            showError("Data Error", "Failed to load user data: " + e.getMessage());
        }
    }

    private void loadUploadedBooks(String userId) {
        try {
            // Get books sorted by review count (high to low)
            List<Book> uploadedBooks = BooksDetailsCollection.getBooksBySellerId(userId).stream()
                    .sorted((b1, b2) -> Double.compare(b2.getReviewCount(), b1.getReviewCount()))
                    .collect(Collectors.toList());

            ObservableList<UploadedBook> uploadedBooksObservable = FXCollections.observableArrayList();

            // Convert Book objects to UploadedBook objects
            for (Book book : uploadedBooks) {
                // Safe date parsing with fallback
                LocalDate uploadDate;
                try {
                    String uploadDateStr = book.getUploadDate();
                    uploadDate = (uploadDateStr == null || uploadDateStr.isEmpty()) ? LocalDate.now()
                            : LocalDate.parse(uploadDateStr);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Invalid date format for book: " + book.getTitle(), e);
                    uploadDate = LocalDate.now();
                }

                uploadedBooksObservable.add(new UploadedBook(
                        book.getTitle(),
                        uploadDate,
                        book.getCurrentPrice(),
                        book.getTotalPurchases()));
            }

            uploadedBooksTable.setItems(uploadedBooksObservable);

            // Load reviews for these books
            loadReviews(uploadedBooks);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading uploaded books", e);
            showError("Data Error", "Failed to load uploaded books: " + e.getMessage());
        }
    }

    private void loadBorrowedBooks(String userId) {
        try {
            List<Book> borrowedBooks = BooksDetailsCollection.getBooksByHolderId(userId);
            ObservableList<BorrowedBook> borrowedBooksObservable = FXCollections.observableArrayList();

            for (Book book : borrowedBooks) {
                // Get seller name
                String sellerName = getSafeSellerName(book.getSellerId());

                // Safe date parsing with fallbacks
                LocalDate borrowDate = parseDate(book.getBorrowDate(), LocalDate.now().minusDays(30));
                LocalDate returnDate = parseDate(book.getReturnDate(), LocalDate.now().plusDays(14));

                // Calculate days remaining and status
                long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), returnDate);
                String status = (daysRemaining > 0) ? "Active" : "Overdue";

                borrowedBooksObservable.add(new BorrowedBook(
                        book.getTitle(),
                        sellerName,
                        borrowDate,
                        returnDate,
                        (int) daysRemaining,
                        status));
            }

            borrowedBooksTable.setItems(borrowedBooksObservable);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading borrowed books", e);
            showError("Data Error", "Failed to load borrowed books: " + e.getMessage());
        }
    }

    private void loadReviews(List<Book> books) {
        try {
            ObservableList<Review> reviewsObservable = FXCollections.observableArrayList();

            for (Book book : books) {
                String bookTitle = book.getTitle();

                if (book.getBuyerReviews() == null || book.getBuyerReviews().isEmpty()) {
                    continue;
                }

                for (Book.Review bookReview : book.getBuyerReviews()) {
                    if (bookReview == null)
                        continue;

                    String reviewerName = getSafeReviewerName(bookReview.getReviewerId());
                    int rating = safeRating(bookReview.getRating());
                    String comment = bookReview.getComment() != null ? bookReview.getComment() : "No comment provided";
                    LocalDate reviewDate = bookReview.getReviewDate() != null ? bookReview.getReviewDate()
                            : LocalDate.now();

                    reviewsObservable.add(new Review(
                            reviewerName, rating, comment, reviewDate, bookTitle));
                }
            }

            // Add placeholder if no reviews found
            if (reviewsObservable.isEmpty()) {
                reviewsObservable.add(new Review(
                        "No Reviews Yet",
                        0,
                        "No reviews have been posted for your books yet.",
                        LocalDate.now(),
                        "N/A"));
            }

            reviewsList.setItems(reviewsObservable);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading reviews", e);
        }
    }

    private void loadUserStatistics(Document user) {
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
            LOGGER.log(Level.SEVERE, "Error loading statistics", e);
        }
    }

    private void setProfileInfo(String userName) {
        try {
            MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
            Document user = users.find(Filters.eq("username", userName)).first();

            if (user == null) {
                showError("Profile Error", "User not found.");
                return;
            }

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
            LOGGER.log(Level.SEVERE, "Error setting profile info", e);
            showError("Profile Error", "Failed to load profile information.");
        }
    }

    public void updateProfilePicture(String imagePath) {
        try {
            // Use default image if path is invalid
            Image newImage;
            try {
                if (imagePath == null || imagePath.isEmpty()) {
                    throw new IllegalArgumentException("Image path is empty");
                }

                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    throw new IllegalArgumentException("Image not found");
                }

                newImage = new Image(imageFile.toURI().toString());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Using default profile image", e);
                newImage = new Image(getClass().getResourceAsStream(DEFAULT_PROFILE_IMAGE));
            }

            // Apply the image with circular clip
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

    private String getSafeSellerName(String sellerId) {
        if (sellerId == null || sellerId.isEmpty()) {
            return "Unknown";
        }

        try {
            MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
            Document seller = users.find(Filters.eq("id", sellerId)).first();
            return (seller != null && seller.getString("name") != null) ? seller.getString("name") : sellerId;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error fetching seller name", e);
            return sellerId;
        }
    }

    private String getSafeReviewerName(String reviewerId) {
        if (reviewerId == null || reviewerId.isEmpty()) {
            return "Anonymous";
        }

        try {
            MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
            Document reviewer = users.find(Filters.eq("id", reviewerId)).first();
            return (reviewer != null && reviewer.getString("name") != null) ? reviewer.getString("name") : reviewerId;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error fetching reviewer name", e);
            return reviewerId;
        }
    }

    private int safeRating(double rawRating) {
        int rating = (int) Math.round(rawRating);
        return Math.max(1, Math.min(5, rating));
    }

    private LocalDate parseDate(String dateStr, LocalDate defaultDate) {
        if (dateStr == null || dateStr.isEmpty()) {
            return defaultDate;
        }

        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Invalid date format: " + dateStr, e);
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