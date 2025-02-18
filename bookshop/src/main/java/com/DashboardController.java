package com;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.logging.Level;
import javafx.scene.image.Image;
import java.io.InputStream;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

public class DashboardController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String DEFAULT_PROFILE_IMAGE = "/com/images/default-profile.png";

    @FXML private ImageView profilePic;
    @FXML private Circle profilePicClip;
    @FXML private Label nameLabel;
    @FXML private Label locationLabel;
    @FXML private Label bookHubIdLabel;
    @FXML private Button homeButton;
    @FXML private Button editInfoButton;
    @FXML private Label totalUploadsLabel;
    @FXML private Label totalBorrowedLabel;
    @FXML private Label buyerRatingLabel;
    @FXML private TableView<UploadedBook> uploadedBooksTable;
    @FXML private TableView<BorrowedBook> borrowedBooksTable;
    @FXML private ListView<Review> reviewsList;
    @FXML private Button logoutButton;
    @FXML private Label lastUpdateLabel;
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

        public String getTitle() { return title; }
        
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
        private final int daysRemaining;
        private final String status;

        public BorrowedBook(String title, String sellerName, LocalDate borrowDate, int daysRemaining, String status) {
            if (title == null || sellerName == null || borrowDate == null || status == null) {
                throw new IllegalArgumentException("Title, seller name, borrow date, and status cannot be null");
            }
            if (daysRemaining < 0) {
                throw new IllegalArgumentException("Days remaining cannot be negative");
            }
            this.title = title;
            this.sellerName = sellerName;
            this.borrowDate = borrowDate;
            this.daysRemaining = daysRemaining;
            this.status = status;
        }

        public String getTitle() { return title; }
        
        public String getSellerName() {
            return sellerName;
        }

        public LocalDate getBorrowDate() {
            return borrowDate;
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

        public Review(String reviewerName, int rating, String comment, LocalDate reviewDate) {
            if (reviewerName == null || comment == null || reviewDate == null) {
                throw new IllegalArgumentException("Reviewer name, comment, and review date cannot be null");
            }
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }
            this.reviewerName = reviewerName;
            this.rating = rating;
            this.comment = comment;
            this.reviewDate = reviewDate;
        }

        @Override
        public String toString() {
            try {
                return reviewerName + " (" + rating + "★) - " + reviewDate + "\n" + comment;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error formatting review string", e);
                return "Error displaying review";
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing dashboard controller...");
        System.out.println("uploadedBooksTable: " + (uploadedBooksTable != null));
        System.out.println("borrowedBooksTable: " + (borrowedBooksTable != null));
        // Verify FXML injection
        if (uploadedBooksTable == null || borrowedBooksTable == null) {
            throw new IllegalStateException("Tables not properly injected from FXML");
        }
        try {
            setupTables();
            loadDummyData();
            // Verify data loaded
            System.out.println("Uploaded books count: " + uploadedBooksTable.getItems().size());
            System.out.println("Borrowed books count: " + borrowedBooksTable.getItems().size());
            setupEventHandlers();
            initializeProfileSection();
            System.out.println("Initialization completed successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing dashboard", e);
            e.printStackTrace();
            showError("Initialization Error", "Failed to initialize dashboard: " + e.getMessage());
        }
    }

    private void setupTables() {
        try {
            System.out.println("Setting up tables...");

            // Debug print initial state
            System.out.println("Initial uploaded books columns: " + uploadedBooksTable.getColumns().size());
            System.out.println("Initial borrowed books columns: " + borrowedBooksTable.getColumns().size());

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
            // Make sure columns take up the full width of the table
            uploadedBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            borrowedBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            // Set a minimum width for the tables
            uploadedBooksTable.setMinWidth(600);
            borrowedBooksTable.setMinWidth(600);
            System.out.println("Tables setup completed successfully");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up tables", e);
            e.printStackTrace(); // Add stack trace for debugging
            showError("Setup Error", "Failed to setup tables properly: " + e.getMessage());
        }
    }
    private void loadDummyData() {
        try {
            // Load profile information
            setProfileInfo("John Doe", "New York, USA", "BH123456");
            setStatistics("15", "7", "4.5/5");

            // Load dummy uploaded books
            ObservableList<UploadedBook> uploadedBooks = FXCollections.observableArrayList(
                    new UploadedBook("The Great Gatsby", LocalDate.now().minusDays(30), 29.99, 15),
                    new UploadedBook("1984", LocalDate.now().minusDays(20), 24.99, 10));

            // Debug print
            System.out.println("Loading uploaded books: " + uploadedBooks.size());
            uploadedBooksTable.setItems(uploadedBooks);
            System.out.println("Current items in uploadedBooksTable: " + uploadedBooksTable.getItems().size());

            // Load dummy borrowed books
            ObservableList<BorrowedBook> borrowedBooks = FXCollections.observableArrayList(
                    new BorrowedBook("Lord of the Rings", "Alice Smith", LocalDate.now().minusDays(5), 25, "Active"),
                    new BorrowedBook("Harry Potter", "Bob Johnson", LocalDate.now().minusDays(10), 20, "Active"));

            // Debug print
            System.out.println("Loading borrowed books: " + borrowedBooks.size());
            borrowedBooksTable.setItems(borrowedBooks);
            System.out.println("Current items in borrowedBooksTable: " + borrowedBooksTable.getItems().size());

            // Debug print for columns
            System.out.println("Uploaded books columns: " + uploadedBooksTable.getColumns().size());
            System.out.println("Borrowed books columns: " + borrowedBooksTable.getColumns().size());

            updateLastUpdateLabel();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading dummy data", e);
            e.printStackTrace(); // Add stack trace for debugging
            showError("Loading Error", "Failed to load dashboard data: " + e.getMessage());
        }
    }

    private void setProfileInfo(String name, String location, String id) {
        try {
            if (name == null || location == null || id == null) {
                throw new IllegalArgumentException("Profile information cannot be null");
            }
            nameLabel.setText(name);
            locationLabel.setText(location);
            bookHubIdLabel.setText("ID: " + id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting profile information", e);
            showError("Profile Error", "Failed to update profile information.");
        }
    }

    private void setStatistics(String uploads, String borrowed, String rating) {
        try {
            if (uploads == null || borrowed == null || rating == null) {
                throw new IllegalArgumentException("Statistics cannot be null");
            }
            totalUploadsLabel.setText(uploads);
            totalBorrowedLabel.setText(borrowed);
            buyerRatingLabel.setText(rating);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting statistics", e);
            showError("Statistics Error", "Failed to update statistics.");
        }
    }

    private void updateLastUpdateLabel() {
        try {
            lastUpdateLabel.setText("Last updated: " + LocalDate.now());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating last update label", e);
        }
    }

    private void setupEventHandlers() {
        try {
            editInfoButton.setOnAction(event -> handleEditInfo());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting up event handlers", e);
            showError("Setup Error", "Failed to setup button handlers.");
        }
    }

    private void initializeProfileSection() {
        try {
            // Verify FXML injection
            if (profilePic == null || profilePicClip == null) {
                throw new IllegalStateException("Profile image components not injected");
            }

            // Load default profile image
            try (InputStream imageStream = getClass().getResourceAsStream(DEFAULT_PROFILE_IMAGE)) {
                if (imageStream != null) {
                    Image defaultImage = new Image(imageStream);
                    profilePic.setImage(defaultImage);

                    // Configure the image view
                    profilePic.setFitHeight(100);
                    profilePic.setFitWidth(100);
                    profilePic.setPreserveRatio(true);
                    profilePic.setSmooth(true);
                    System.out.println("Profile section initialized successfully");
                    // Don't set the clip here since it's already set in FXML

                } else {
                    LOGGER.warning("Default profile image not found: " + DEFAULT_PROFILE_IMAGE);
                    // Create a default colored circle instead
                    profilePic.setImage(null);
                    profilePicClip.setFill(Color.LIGHTGRAY);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load default profile image", e);
            }

            // Add hover effect
            profilePic.setOnMouseEntered(event -> {
                ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), profilePic);
                scaleTransition.setToX(1.1);
                scaleTransition.setToY(1.1);
                scaleTransition.play();
            });

            profilePic.setOnMouseExited(event -> {
                ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(200), profilePic);
                scaleTransition.setToX(1.0);
                scaleTransition.setToY(1.0);
                scaleTransition.play();
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing profile section", e);
            showError("Profile Error",
                    "Failed to initialize profile section. Profile picture may not display correctly.");
        }
    }

    public void updateProfilePicture(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                throw new IllegalArgumentException("Image path cannot be null or empty");
            }

            try (InputStream imageStream = getClass().getResourceAsStream(imagePath)) {
                if (imageStream != null) {
                    Image newImage = new Image(imageStream);
                    profilePic.setImage(newImage);
                } else {
                    throw new IllegalArgumentException("Image not found: " + imagePath);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating profile picture", e);
            showError("Profile Update Error", "Failed to update profile picture: " + e.getMessage());
        }
    }

    private void handleEditInfo() {
        try {
            System.out.println("Edit profile clicked");
            // Add your edit profile logic here
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling edit info", e);
            showError("Edit Error", "Failed to open edit profile dialog.");
        }
    }

    @FXML
    private void handleHome() {
        Stage currentStage = (Stage) homeButton.getScene().getWindow();
        LoadPageController.loadScene("home.fxml", "home.css", currentStage);
    }

    @FXML
    private void handleLogout() {
        try {
            HomeController homeController = new HomeController();
            homeController.logout();
            handleHome();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling logout", e);
            showError("Logout Error", "Failed to process logout.");
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