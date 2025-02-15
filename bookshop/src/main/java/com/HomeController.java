package com;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class HomeController {

    @FXML
    private TextField searchField;
    @FXML
    private HBox featuredBooks;
    @FXML
    private HBox recommendedBooks;
    @FXML
    private Button profileLoginButton;

    private static boolean isLoggedIn = false;

    // Book data structure
    private static class Book {
        final String title;
        final String author;
        final String category;
        final double rating;
        final double price;
        final String imageUrl;

        Book(String title, String author, String category, double rating, double price, String imageUrl) {
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
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to initialize application: " + e.getMessage());
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
                menu.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 5;");
                menu.getItems().forEach(item -> {item.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-padding: 5px;");
                ((CustomMenuItem) item).getContent().setOnMouseEntered(event -> item.setStyle("-fx-background-color: #555; -fx-text-fill: #ddd;"));
                ((CustomMenuItem) item).getContent().setOnMouseExited(event -> item.setStyle("-fx-background-color: #444; -fx-text-fill: white;"));
                });

                // Style the profile login button
                profileLoginButton.setText("Profile");
                profileLoginButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 10px; -fx-border-radius: 5px;");
                profileLoginButton.setOnAction(e -> menu.show(profileLoginButton, javafx.geometry.Side.BOTTOM, 0, 0));

            } else {
                // Styling for the login button when not logged in
                profileLoginButton.setText("Login");
                profileLoginButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 10px; -fx-border-radius: 5px;");
                profileLoginButton.setOnAction(e -> handleProfileLogin());
            }
        }
    }

    private void openDashboard(){
        LoadPageController.loadScene("dashboard.fxml", "dashboard.css", profileLoginButton);
    }

    private void openSettings() {
        LoadPageController.loadScene("settings.fxml", "settings.css", profileLoginButton);
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
            // Featured Books Data
            List<Book> featuredBooksList = Arrays.asList(
                    new Book("ড্রাকুলা", " ব্রাম স্টোকার", "অতিপ্রাকৃত ও ভৌতিক", 4.8, 591,"/com/images/books/Dracula.png"),
                    new Book("বৃহৎ বঙ্গ ২য় খণ্ড", " দীনেশচন্দ্র সেন", "প্রাচীন বাংলার ইতিহাস", 4.5, 741,"/com/images/books/Brihot_bongo.png"),
                    new Book("পথে প্রবাসে", "অন্নদাশঙ্কর রায়", "পশ্চিম বঙ্গের বই: ভ্রমণ ও প্রবাস", 4.6, 205,"/com/images/books/pothe_probashe.png"),
                    new Book("টিমওয়ার্ক ১০১", " জন সি. ম্যাক্সওয়েল ", "আত্ম-উন্নয়ন ও মেডিটেশন", 3, 165,"/com/images/books/Teamwork_101.png"),
                    new Book("রিলেশনশিপস ১০১", " জন সি. ম্যাক্সওয়েল ", "আত্ম-উন্নয়ন ও মেডিটেশন", 4.2, 165,"/com/images/books/Relationship_101.png"),
                    new Book("পদ্মজা", "  ইলমা বেহরোজ ", "সমকালীন উপন্যাস", 4.8, 600, "/com/images/books/Poddoja.png"),
                    new Book("জীবন যেখানে যেমন", " আরিফ আজাদ ", "ইসলামি গল্প", 5, 210,"/com/images/books/jibon_jekhane_jemon.png"),
                    new Book("প্যারাডক্সিক্যাল সাজিদ", " আরিফ আজাদ ", "ইসলামি আদর্শ ও মতবাদ", 4.6, 225,"/com/images/books/paradoxical_sajid.png"),
                    new Book("ড. জেকিল অ্যান্ড মি. হাইড", "  রবার্ট লুই স্টিভেনসন ", "বয়স যখন ১২-১৭: উপন্যাস", 4.2, 113,"/com/images/books/dr_jackyll_and_mr_hyde.png"));

            // Recommended Books Data
            List<Book> recommendedBooksList = Arrays.asList(
                    new Book("প্রোগ্রামিংয়ের চৌদ্দগোষ্ঠী", " ঝংকার মাহবুব", "প্রোগ্রামিং বেসিক বই", 4.5, 315,"/com/images/books/PRO_gushti.png"),
                    new Book("হামাস", " খালেদ হারুব ,  আযযাম তামিমি ,  রাকিবুল হাসান (অনুবাদক)","রাজনৈতিক গবেষণা ও প্রবন্ধ", 5.0, 214, "/com/images/books/Hamas.png"),
                    new Book("আয়নাঘর", " হুমায়ূন আহমেদ", "অতিপ্রাকৃত ও ভৌতিক", 4.8, 143,"/com/images/books/AynaGhor.png"),
                    new Book("হায়াতের দিন ফুরোলে", " আরিফ আজাদ", "ইসলামি বই: আত্ম-উন্নয়ন", 4.5, 241,"/com/images/books/Hayat.png"),
                    new Book("Breaking Dawn", " Stephenie Meyer", "Novel: Horror and Supernatural", 4.3, 863,"/com/images/books/Breaking_Dawn.png"),
                    new Book("গিট ও গিটহাব ", " জাকির হোসাইন", "ওয়েব ডিজাইন ও ডেভেলপমেন্ট", 4.2, 188,"/com/images/books/Git.png"),
                    new Book("অপেক্ষা", " হুমায়ূন আহমেদ", "সমকালীন উপন্যাস", 4.2, 300, "/com/images/books/Opekkha.png"),
                    new Book("দ্য আলকেমিস্ট", " পাওলো কোয়েলহো ,  সুফাই রুমিন তাজিন (অনুবাদক)", "অনুবাদ উপন্যাস", 3.8,195, "/com/images/books/The_Alchemist.png"),
                    new Book("আরেক ফাল্গুন", " জহির রায়হান", "চিরায়ত উপন্যাস", 4.3, 150,"/com/images/books/Arek_Falgun.png"),
                    new Book("কাজল চোখের মেয়ে", " সাদাত হোসাইন", "রোমান্টিক কবিতা", 3.5, 180,"/com/images/books/Kajol.png"));

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
        showAlert(Alert.AlertType.INFORMATION, "Success",String.format("%s has been added to your cart!", book.title));
    }

    @FXML
    private void handleProfileLogin() {
        if (isLoggedIn) {
            logout();
        } else {
            LoadPageController.loadScene("login.fxml", "login_signup.css", profileLoginButton);
        }
    }

    public void logout() {
        isLoggedIn = false;
        updateProfileButton();
        SessionManager.getInstance().clearSession();
        showAlert(Alert.AlertType.INFORMATION, "Logged Out","You have been successfully logged out.");
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