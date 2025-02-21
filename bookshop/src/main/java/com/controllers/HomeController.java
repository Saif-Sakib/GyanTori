package com.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.Region;
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
import com.services.CartService;
import com.services.SessionManager;

public class HomeController {

    @FXML
    private TextField searchField;
    @FXML
    private HBox featuredBooks;
    @FXML
    private HBox recommendedBooks;
    @FXML
    private Button profileLoginButton;
    @FXML
    private Label navLogo;
    @FXML
    private ImageView bannerImage;
    @FXML
    private HBox bannerBox;
    private volatile boolean slideshowRunning = true;
    private Thread slideshowThread;
    private int currentImageIndex = 0;

    private final List<String> bannerImages = Arrays.asList(
            "/com/images/img1.png",
            "/com/images/img2.png",
            "/com/images/img3.png",
            "/com/images/img4.png",
            "/com/images/img5.png"
    );

    private final CartService cartService = CartService.getInstance();

    private static boolean isLoggedIn = false;

    // Book data structure
    private static class Book {
        final String Id;
        final String title;
        final String author;
        final String category;
        final double rating;
        final double price;
        final String imageUrl;
        Book(String Id, String title, String author, String category, double rating, double price, String imageUrl) {
            this.Id = Id;
            this.title = title;
            this.author = author;
            this.category = category;
            this.rating = rating;
            this.price = price;
            this.imageUrl = imageUrl;
        }
    }

    @FXML
    public void initialize() {
        try {
            updateProfileButton();
            loadBooks();
            addSearchListener();
            
            if (navLogo != null) {
                animateNavLogo();
            }
            initBanner();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to initialize application: " + e.getMessage());
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
        parent.setMinWidth(100);  // Adjust these values as needed
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
                
                if (!slideshowRunning) break;
                
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

    private void updateProfileButton() {
        if (profileLoginButton != null) {
            if (isLoggedIn) {
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

    private void animateNavLogo() {
    if (navLogo == null) return; // Avoid null pointer exceptions

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

    private void openDashboard(){
        Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
        LoadPageController.loadScene("dashboard.fxml", "dashboard.css", currentStage);
        stopSlideshow();
    }

    private void openSettings() {
        Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
        LoadPageController.loadScene("settings.fxml", "settings.css", currentStage);

    }

    private void addSearchListener() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && newValue.length() >= 3) {
                    performSearch(newValue);
                }
            });
        }
    }

    @FXML
    public void performSearch() {
        if (searchField != null) {
            String query = searchField.getText();
            performSearch(query);
        }
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        // Implement search functionality
        System.out.println("Searching for: " + query.trim());
        // You would typically call your search service here
    }

    private void loadBooks() {
        try {
                    List<Book> featuredBooksList = Arrays.asList(
                            new Book("1", "ড্রাকুলা", " ব্রাম স্টোকার", "অতিপ্রাকৃত ও ভৌতিক", 4.8, 591,"/com/images/books/Dracula.png"),
                            new Book("2", "বৃহৎ বঙ্গ ২য় খণ্ড", " দীনেশচন্দ্র সেন", "প্রাচীন বাংলার ইতিহাস", 4.5, 741,"/com/images/books/Brihot_bongo.png"),
                            new Book("3", "পথে প্রবাসে", "অন্নদাশঙ্কর রায়", "পশ্চিম বঙ্গের বই: ভ্রমণ ও প্রবাস", 4.6, 205,"/com/images/books/pothe_probashe.png"),
                            new Book("4", "টিমওয়ার্ক ১০১", " জন সি. ম্যাক্সওয়েল ", "আত্ম-উন্নয়ন ও মেডিটেশন", 3, 165,"/com/images/books/Teamwork_101.png"),
                            new Book("5", "রিলেশনশিপস ১০১", " জন সি. ম্যাক্সওয়েল ", "আত্ম-উন্নয়ন ও মেডিটেশন", 4.2, 165,"/com/images/books/Relationship_101.png"),
                            new Book("6", "পদ্মজা", "  ইলমা বেহরোজ ", "সমকালীন উপন্যাস", 4.8, 600, "/com/images/books/Poddoja.png"),
                            new Book("7", "জীবন যেখানে যেমন", " আরিফ আজাদ ", "ইসলামি গল্প", 5, 210,"/com/images/books/jibon_jekhane_jemon.png"),
                            new Book("8", "প্যারাডক্সিক্যাল সাজিদ", " আরিফ আজাদ ", "ইসলামি আদর্শ ও মতবাদ", 4.6, 225,"/com/images/books/paradoxical_sajid.png"),
                            new Book("9", "ড. জেকিল অ্যান্ড মি. হাইড", "  রবার্ট লুই স্টিভেনসন ", "বয়স যখন ১২-১৭: উপন্যাস", 4.2, 113,"/com/images/books/dr_jackyll_and_mr_hyde.png")
                    );
        
                    List<Book> recommendedBooksList = Arrays.asList(
                            new Book("10", "প্রোগ্রামিংয়ের চৌদ্দগোষ্ঠী", " ঝংকার মাহবুব", "প্রোগ্রামিং বেসিক বই", 4.5, 315,"/com/images/books/PRO_gushti.png"),
                            new Book("11", "হামাস", " খালেদ হারুব ,  আযযাম তামিমি ,  রাকিবুল হাসান (অনুবাদক)","রাজনৈতিক গবেষণা ও প্রবন্ধ", 5.0, 214, "/com/images/books/Hamas.png"),
                            new Book("12", "আয়নাঘর", " হুমায়ূন আহমেদ", "অতিপ্রাকৃত ও ভৌতিক", 4.8, 143,"/com/images/books/AynaGhor.png"),
                            new Book("13", "হায়াতের দিন ফুরোলে", " আরিফ আজাদ", "ইসলামি বই: আত্ম-উন্নয়ন", 4.5, 241,"/com/images/books/Hayat.png"),
                            new Book("14", "Breaking Dawn", " Stephenie Meyer", "Novel: Horror and Supernatural", 4.3, 863,"/com/images/books/Breaking_Dawn.png"),
                            new Book("15", "গিট ও গিটহাব ", " জাকির হোসাইন", "ওয়েব ডিজাইন ও ডেভেলপমেন্ট", 4.2, 188,"/com/images/books/Git.png"),
                            new Book("16", "অপেক্ষা", " হুমায়ূন আহমেদ", "সমকালীন উপন্যাস", 4.2, 300, "/com/images/books/Opekkha.png"),
                            new Book("17", "দ্য আলকেমিস্ট", " পাওলো কোয়েলহো ,  সুফাই রুমিন তাজিন (অনুবাদক)", "অনুবাদ উপন্যাস", 3.8,195, "/com/images/books/The_Alchemist.png"),
                            new Book("18", "আরেক ফাল্গুন", " জহির রায়হান", "চিরায়ত উপন্যাস", 4.3, 150,"/com/images/books/Arek_Falgun.png"),
                            new Book("19", "কাজল চোখের মেয়ে", " সাদাত হোসাইন", "রোমান্টিক কবিতা", 3.5, 180,"/com/images/books/Kajol.png")
                    );

            loadBookSection(featuredBooks, featuredBooksList);
            loadBookSection(recommendedBooks, recommendedBooksList);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Loading Error","Failed to load books: " + e.getMessage());
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

    private VBox createBookCard(Book book) {
        VBox card = new VBox(10);
        card.getStyleClass().add("book-card");
        card.setAlignment(Pos.CENTER);

        // Book Cover Image
        ImageView coverImage = new ImageView();
        try {
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(book.imageUrl)));
            coverImage.setImage(image);
        } catch (Exception e) {
            // Load placeholder image if book image is not found
            try {
                coverImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/books/placeholder-book.png"))));
            } catch (Exception ex) {
                System.err.println("Failed to load placeholder image: " + ex.getMessage());
            }
        }
        coverImage.setFitHeight(200);
        coverImage.setFitWidth(140);
        coverImage.setPreserveRatio(true);

        // Book Details
        Label titleLabel = new Label(book.title);
        titleLabel.getStyleClass().add("book-title");
        titleLabel.setWrapText(true);

        Label authorLabel = new Label(book.author);
        authorLabel.getStyleClass().add("book-author");

        Label ratingLabel = new Label(String.format("%.1f ★", book.rating));
        ratingLabel.getStyleClass().add("book-rating");

        Label priceLabel = new Label(String.format("৳ %.2f", book.price));
        priceLabel.getStyleClass().add("book-price");

        Button addToCartBtn = new Button("Add to Cart");
        addToCartBtn.getStyleClass().addAll("cart-button", "animated-button");
        addToCartBtn.setOnAction(e -> handleAddToCart(book));

        card.getChildren().addAll(coverImage, titleLabel, authorLabel, ratingLabel, priceLabel, addToCartBtn);

        // Add hover effect
        addHoverEffect(card);

        return card;
    }

    private void addHoverEffect(VBox card) {
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), card);
        scaleIn.setToX(1.05);
        scaleIn.setToY(1.05);

        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), card);
        scaleOut.setToX(1.0);
        scaleOut.setToY(1.0);

        card.setOnMouseEntered(e -> scaleIn.playFromStart());
        card.setOnMouseExited(e -> scaleOut.playFromStart());
    }

    private void handleAddToCart(Book book) {
        if (!isLoggedIn) {
            showAlert(Alert.AlertType.INFORMATION, "Login Required","Please login to add books to your cart.");
            return;
        }
        cartService.addItem(book.Id, book.title, book.price, book.imageUrl);
        showAlert(Alert.AlertType.INFORMATION, "Success",String.format("%s has been added to your cart!", book.title));
    }

    @FXML
    private void handleCartLoad() {
        Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
        LoadPageController.loadScene("cart.fxml", "cart.css", currentStage);
        stopSlideshow();
    }

    @FXML
    private void handleProfileLogin() {
        if (isLoggedIn) {
            logout();
        } else {
            Stage currentStage = (Stage) profileLoginButton.getScene().getWindow();
            LoadPageController.loadScene("login.fxml", "login_signup.css", currentStage);
            stopSlideshow();
        }
    }

    public void logout() {
        try {
            isLoggedIn = false;
            updateProfileButton();
            SessionManager.getInstance().clearSession();
            showAlert(Alert.AlertType.INFORMATION, "Logged Out", "You have been successfully logged out.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Logout Error", "Failed to process logout.");
        }
        
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Getter and setter for login status
    public static void setLoggedIn(boolean status) {
        isLoggedIn = status;
    }

    public boolean getLoggedIn() {
        return isLoggedIn;
    }
}