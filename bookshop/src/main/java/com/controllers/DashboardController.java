package com.controllers;

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
import java.time.ZoneId;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import com.database.DatabaseManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.logging.Level;
import com.services.SessionManager;
import javafx.scene.image.Image;

import java.io.File;
import java.io.InputStream;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

public class DashboardController implements Initializable {
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final String DEFAULT_PROFILE_IMAGE = "/com/images/userDP/default-profile.png";

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
        // Verify FXML injection
        if (uploadedBooksTable == null || borrowedBooksTable == null) {
            throw new IllegalStateException("Tables not properly injected from FXML");
        }
        try {
            SessionManager sessionManager = SessionManager.getInstance();
            String userName = sessionManager.getUserName();
            setupTables();
            setProfileInfo(userName);
            setupTableInfo(userName);
            setStatistics(userName);
            // Verify data loaded
            updateLastUpdateLabel();
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

    private void setupTableInfo(String userName) {
    try {
        if (userName == null || userName.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");

        // Fetch user details from DB based on username
        Document user = users.find(Filters.eq("username", userName)).first();

        if (user == null) {
            showError("Data Error", "User not found.");
            return;
        }

        // Fetch uploaded books
        ObservableList<UploadedBook> uploadedBooks = FXCollections.observableArrayList();
        List<Document> uploadedBooksList = (List<Document>) user.get("uploaded_books");

        if (uploadedBooksList != null) {
            for (Document book : uploadedBooksList) {
                uploadedBooks.add(new UploadedBook(
                        book.getString("title"),
                        book.getDate("upload_date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        book.getDouble("price"),
                        book.getInteger("total_purchases")
                ));
            }
        }

        uploadedBooksTable.setItems(uploadedBooks);

        // Fetch borrowed books
        ObservableList<BorrowedBook> borrowedBooks = FXCollections.observableArrayList();
        List<Document> borrowedBooksList = (List<Document>) user.get("borrowed_books");

        if (borrowedBooksList != null) {
            for (Document book : borrowedBooksList) {
                borrowedBooks.add(new BorrowedBook(
                        book.getString("title"),
                        book.getString("seller_id"),
                        book.getDate("borrow_date").toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        book.getInteger("return_date"),
                        book.getString("status")
                ));
            }
        }

        borrowedBooksTable.setItems(borrowedBooks);

    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error fetching table information", e);
        showError("Data Error", "Failed to load book data.");
    }
}

    private void setProfileInfo(String userName) {
    try {
        if (userName == null || userName.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");

        // Fetch user details from DB based on ID
        Document user = users.find(Filters.eq("username", userName)).first();

        if (user == null) {
            showError("Profile Error", "User not found.");
            return;
        }

        // Extract fields from the document (handling possible missing values)
        String name = user.getString("full_name");
        String location = user.containsKey("location") ? user.getString("location") : "Not Set";
        String id = user.getString("id");
        String dp = user.containsKey("imgPath")?user.getString("imgPath"): DEFAULT_PROFILE_IMAGE;
        // Set the labels
        nameLabel.setText(name != null ? name : "Unknown");
        locationLabel.setText(location);
        bookHubIdLabel.setText("ID: " + id);
        updateProfilePicture(dp);
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error fetching profile information", e);
        showError("Profile Error", "Failed to load profile information.");
    }
}


    private void setStatistics(String userName) {
    try {
        MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
        Document user = users.find(Filters.eq("username", userName.toLowerCase())).first();

        if (user == null) {
            throw new IllegalArgumentException("User not found in database");
        }

        // Extract values from the user document
        int uploads = user.containsKey("uploaded_books") ? ((List<Document>) user.get("uploaded_books")).size() : 0;
        int borrowed = user.containsKey("borrowed_books") ? ((List<Document>) user.get("borrowed_books")).size() : 0;
        double rating = (user.getDouble("rating")!=null) ? user.getDouble("rating") : 0.0;
        System.out.println("userName");
        // Set labels with retrieved values
        totalUploadsLabel.setText(String.valueOf(uploads));
        totalBorrowedLabel.setText(String.valueOf(borrowed));
        buyerRatingLabel.setText(String.format("%.1f", rating));

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

    public void updateProfilePicture(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                throw new IllegalArgumentException("Image path cannot be null or empty");
            }
            
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                    Image newImage = new Image(imageFile.toURI().toString());
                    profilePic.setImage(newImage);
                    // Configure the image view
                    profilePic.setFitHeight(100);
                    profilePic.setFitWidth(100);
                    profilePic.setPreserveRatio(true);
                    profilePic.setSmooth(true);
                    System.out.println("Profile section initialized successfully");
                    // Don't set the clip here since it's already set in FXML
                } else {
                    throw new IllegalArgumentException("Image not found: " + imagePath);
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
            LOGGER.log(Level.SEVERE, "Error updating profile picture", e);
        }
    }

    @FXML
    private void handleEditInfo() {
        try {
            Stage currentStage = (Stage) editInfoButton.getScene().getWindow();
            LoadPageController.loadScene("editProfile.fxml", "editProfile.css", currentStage);
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