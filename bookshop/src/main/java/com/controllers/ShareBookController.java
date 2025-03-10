package com.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.types.ObjectId;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import com.database.DatabaseManager;
import com.database.UsersCollection;
import com.database.BookDetailsCollection;
import com.models.Book;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.services.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

import com.services.BookAPIService;

public class ShareBookController extends CommonController implements Initializable {
    private BookAPIService bookAPIService = new BookAPIService();
    private static final Logger LOGGER = Logger.getLogger(ShareBookController.class.getName());
    // Update the path based on your actual project structure
    private static final String DEFAULT_IMAGE_PATH = "/com/images/books/placeholder-book.png";

    // Main container
    @FXML
    private VBox rootPane;

    // Book form fields
    @FXML
    private TextField titleField;
    @FXML
    private TextField isbnField;
    @FXML
    private TextField pagesField;
    @FXML
    private TextField authorField;
    @FXML
    private TextField publisherField;
    @FXML
    private DatePicker publicationDatePicker;
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private TextField originalPriceField;
    @FXML
    private TextField discountField;
    @FXML
    private TextField currentPriceField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private Button addCategoryButton;
    @FXML
    private FlowPane categoryContainer;
    @FXML
    private ImageView bookImagePreview;
    @FXML
    private Button uploadImageButton;
    @FXML
    private Label imageNameLabel;
    @FXML
    private ComboBox<String> bookHubComboBox;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Button shareButton;
    @FXML
    private Button previewButton;
    @FXML
    private Button fetchDetailsButton;

    // Internal state
    private File selectedImageFile;
    private Set<String> selectedCategories = new HashSet<>();
    private boolean isImageSelected = false;
    private String apiImageUrl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            LOGGER.info("Initializing ShareBookController...");

            // Debug: Check if form elements are found
            if (titleField == null || authorField == null || descriptionArea == null) {
                LOGGER.severe("Critical form elements are null. FXML may not be loaded correctly.");
                showDebugAlert("Form Initialization Error", "Critical form elements are null.");
                return;
            }

            // Fix: The content container is within a ScrollPane, don't try to look it up
            // It's already in the FXML structure, so no need to set visibility
            LOGGER.info("Using FXML-defined content container structure");

            // Initialize dropdown options
            languageComboBox.setItems(FXCollections.observableArrayList(
                    "English", "Bengali", "Hindi", "Urdu", "Arabic", "Spanish", "French", "German", "Japanese",
                    "Chinese", "Other"));

            categoryComboBox.setItems(FXCollections.observableArrayList(
                    "Fiction", "Non-Fiction", "Science", "Technology", "Business", "Self-Help",
                    "Biography", "History", "Arts", "Literature", "Cooking", "Travel",
                    "Religion", "Philosophy", "Psychology", "Education", "Children"));

            bookHubComboBox.setItems(FXCollections.observableArrayList(
                    "Main Campus", "Downtown Branch", "East Side Location", "West End", "North Hub"));

            // Auto-calculate price with discount
            discountField.textProperty().addListener((observable, oldValue, newValue) -> calculateCurrentPrice());
            originalPriceField.textProperty().addListener((observable, oldValue, newValue) -> calculateCurrentPrice());

            // Validate numeric inputs
            pagesField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    pagesField.setText(newValue.replaceAll("[^\\d]", ""));
                }
            });

            originalPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*\\.?\\d*")) {
                    originalPriceField.setText(oldValue);
                }
            });

            discountField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*\\.?\\d*")) {
                    discountField.setText(oldValue);
                }
            });

            // Load default book cover image
            loadDefaultImage();

            // Fix: Ensure the buttons have actions
            if (addCategoryButton != null) {
                addCategoryButton.setOnAction(this::addCategory);
            }

            if (uploadImageButton != null) {
                uploadImageButton.setOnAction(this::chooseImage);
            }

            if (shareButton != null) {
                shareButton.setOnAction(this::shareBook);
            }

            if (previewButton != null) {
                previewButton.setOnAction(this::previewBook);
            }

            if (fetchDetailsButton != null) {
                fetchDetailsButton.setOnAction(this::fetchBookDetails);
            }
            updateProfileButton();
            LOGGER.info("ShareBookController initialization completed successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing ShareBookController", e);
            showAlert(AlertType.ERROR, "Initialization Error", "Could not initialize the form: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Debug alert with stack trace
    private void showDebugAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Get the exception stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder stackTraceText = new StringBuilder();
        for (int i = 1; i < stackTrace.length && i < 10; i++) {
            stackTraceText.append(stackTrace[i].toString()).append("\n");
        }

        alert.setContentText(message + "\n\nStack trace:\n" + stackTraceText.toString());
        alert.showAndWait();
    }

    private void loadDefaultImage() {
        try {
            // Try multiple paths to find the image
            String[] possiblePaths = {
                    DEFAULT_IMAGE_PATH,
                    "/com/images/books/placeholder-book.png"
            };

            boolean imageLoaded = false;

            for (String path : possiblePaths) {
                try {
                    URL imageUrl = getClass().getResource(path);
                    if (imageUrl != null) {
                        Image defaultImage = new Image(imageUrl.toString());
                        bookImagePreview.setImage(defaultImage);
                        LOGGER.info("Default image loaded successfully from: " + path);
                        imageLoaded = true;
                        break;
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load image from path: " + path, e);
                }
            }

            if (!imageLoaded) {
                LOGGER.warning("Could not load default image from any path. Using fallback.");
                // Fallback - create a simple colored rectangle as placeholder
                bookImagePreview.setFitHeight(150);
                bookImagePreview.setFitWidth(120);
                bookImagePreview.setStyle("-fx-background-color: #CCCCCC;");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load default image", e);
            bookImagePreview.setStyle("-fx-background-color: #CCCCCC;");
        }
    }

    @FXML
    private void addCategory(ActionEvent event) {
        String category = categoryComboBox.getValue();
        if (category == null || category.trim().isEmpty()) {
            return;
        }

        if (selectedCategories.contains(category)) {
            showAlert(AlertType.INFORMATION, "Duplicate Category", "This category is already added.");
            return;
        }

        if (selectedCategories.size() >= 5) {
            showAlert(AlertType.WARNING, "Too Many Categories", "You can add up to 5 categories.");
            return;
        }

        selectedCategories.add(category);

        // Create a tag-like UI element
        HBox categoryTag = new HBox();
        categoryTag.getStyleClass().add("category-tag");
        categoryTag.setPadding(new Insets(5, 8, 5, 8));
        categoryTag.setSpacing(5);

        Label categoryLabel = new Label(category);
        categoryLabel.getStyleClass().add("category-label");

        Button removeButton = new Button("×");
        removeButton.getStyleClass().add("remove-category-button");
        removeButton.setOnAction(e -> {
            categoryContainer.getChildren().remove(categoryTag);
            selectedCategories.remove(category);
        });

        categoryTag.getChildren().addAll(categoryLabel, removeButton);
        categoryContainer.getChildren().add(categoryTag);
    }

    @FXML
    private void chooseImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Book Cover Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) rootPane.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                // Check file size (limit to 5MB)
                if (file.length() > 5 * 1024 * 1024) {
                    showAlert(AlertType.ERROR, "File Too Large", "Please choose an image under 5MB.");
                    return;
                }

                Image image = new Image(file.toURI().toString());
                bookImagePreview.setImage(image);
                selectedImageFile = file;
                imageNameLabel.setText(file.getName());
                isImageSelected = true;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading image", e);
                showAlert(AlertType.ERROR, "Image Error", "Could not load the selected image.");
            }
        }
    }

    @FXML
    private void fetchBookDetails(ActionEvent event) {
        LOGGER.info("Fetching book details...");
        String isbn = isbnField.getText().trim();
        if (isbn.isEmpty()) {
            showAlert(AlertType.WARNING, "ISBN Required", "Please enter an ISBN to fetch book details.");
            return;
        }

        try {
            Book bookDetails = bookAPIService.fetchBookByISBN(isbn);
            if (bookDetails != null) {
                // Populate form with fetched data
                titleField.setText(bookDetails.getTitle() != null ? bookDetails.getTitle() : "");
                authorField.setText(bookDetails.getAuthor() != null ? bookDetails.getAuthor() : "");
                publisherField.setText(bookDetails.getPublisher() != null ? bookDetails.getPublisher() : "");

                if (bookDetails.getPublicationDate() != null && !bookDetails.getPublicationDate().isEmpty()) {
                    try {
                        LocalDate date = LocalDate.parse(bookDetails.getPublicationDate());
                        publicationDatePicker.setValue(date);
                    } catch (DateTimeParseException e) {
                        LOGGER.log(Level.WARNING,
                                "Could not parse publication date: " + bookDetails.getPublicationDate(), e);
                        // Try alternative date formats
                        try {
                            DateTimeFormatter[] formatters = {
                                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                                    DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                                    DateTimeFormatter.ofPattern("yyyy"),
                                    DateTimeFormatter.ofPattern("MMMM d, yyyy"), // Added this line
                                    DateTimeFormatter.ofPattern("MMM d, yyyy"), // Added this line
                                    DateTimeFormatter.ofPattern("MMMM dd, yyyy") // Added this line
                            };

                            for (DateTimeFormatter formatter : formatters) {
                                try {
                                    LocalDate date = LocalDate.parse(bookDetails.getPublicationDate(), formatter);
                                    publicationDatePicker.setValue(date);
                                    break;
                                } catch (DateTimeParseException ex) {
                                    // Continue to next formatter
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.log(Level.WARNING, "Failed all date parsing attempts", ex);
                        }
                    }
                }

                if (bookDetails.getLanguage() != null && !bookDetails.getLanguage().isEmpty()) {
                    if (languageComboBox.getItems().contains(bookDetails.getLanguage())) {
                        languageComboBox.setValue(bookDetails.getLanguage());
                    } else {
                        languageComboBox.setValue("Other");
                    }
                }

                if (bookDetails.getPages() > 0) {
                    pagesField.setText(String.valueOf(bookDetails.getPages()));
                } else {
                    pagesField.clear();
                }

                if (bookDetails.getDescription() != null) {
                    descriptionArea.setText(bookDetails.getDescription());
                } else {
                    descriptionArea.clear();
                }

                // Handle categories
                if (bookDetails.getCategories() != null && bookDetails.getCategories().length > 0) {
                    selectedCategories.clear();
                    categoryContainer.getChildren().clear();
                    for (String category : bookDetails.getCategories()) {
                        if (category != null && !category.trim().isEmpty()) {
                            addCategoryTag(category);
                        }
                    }
                }

                // Set image if available
                if (bookDetails.getImageUrl() != null && !bookDetails.getImageUrl().isEmpty()) {
                    try {
                        Image image = new Image(bookDetails.getImageUrl(), true); // true enables background loading
                        image.errorProperty().addListener((observable, oldValue, newValue) -> {
                            if (newValue) {
                                loadDefaultImage();
                                isImageSelected = false;
                                apiImageUrl = null;
                                imageNameLabel.setText("No file chosen");
                            }
                        });

                        bookImagePreview.setImage(image);
                        imageNameLabel.setText("Image from API");
                        isImageSelected = true;
                        apiImageUrl = bookDetails.getImageUrl();

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Could not load image from URL: " + bookDetails.getImageUrl(), e);
                        loadDefaultImage();
                        isImageSelected = false;
                        apiImageUrl = null;
                    }
                }

                showAlert(AlertType.INFORMATION, "Book Details", "Book details fetched successfully!");
            } else {
                showAlert(AlertType.WARNING, "Book Not Found", "No book found with the provided ISBN.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching book details", e);
            showAlert(AlertType.ERROR, "API Error", "Failed to fetch book details: " + e.getMessage());
        }
    }

    private void calculateCurrentPrice() {
        try {
            String originalPriceText = originalPriceField.getText().trim();
            if (originalPriceText.isEmpty()) {
                currentPriceField.setText("");
                return;
            }

            double originalPrice = Double.parseDouble(originalPriceText);
            String discountText = discountField.getText().trim();

            if (discountText.isEmpty()) {
                currentPriceField.setText(String.format("%.2f", originalPrice));
                return;
            }

            double discount = Double.parseDouble(discountText);
            if (discount < 0)
                discount = 0;
            if (discount > 100)
                discount = 100;

            double currentPrice = originalPrice - (originalPrice * discount / 100);
            currentPriceField.setText(String.format("%.2f", currentPrice));
        } catch (NumberFormatException e) {
            currentPriceField.setText("");
        }
    }

    private void addCategoryTag(String category) {
        selectedCategories.add(category);

        try {
            // Create a tag-like UI element
            HBox categoryTag = new HBox();
            categoryTag.getStyleClass().add("category-tag");
            categoryTag.setPadding(new Insets(5, 10, 5, 10));
            categoryTag.setSpacing(5);

            Label categoryLabel = new Label(category);
            categoryLabel.getStyleClass().add("category-label");

            Button removeButton = new Button("×");
            removeButton.getStyleClass().add("remove-category-button");
            removeButton.setOnAction(e -> {
                categoryContainer.getChildren().remove(categoryTag);
                selectedCategories.remove(category);
            });

            categoryTag.getChildren().addAll(categoryLabel, removeButton);
            categoryContainer.getChildren().add(categoryTag);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error adding category tag to UI", e);
            // Add to data model even if UI fails
        }
    }

    @FXML
    private void shareBook(ActionEvent event) {
        LOGGER.info("Share book button clicked");
        // Check if user is logged in
        if (!SessionManager.getInstance().getIsLoggedIn()) {
            showAlert(AlertType.WARNING, "Login Required", "Please login to share a book.");
            return;
        }

        if (!validateForm()) {
            return;
        }

        try {
            Book book = buildBookFromForm();

            // Save image if selected
            if (selectedImageFile != null) {
                String imagePath = saveImage();
                if (imagePath != null) {
                    book.setImageUrl(imagePath);
                }
            }

            if (book.getImageUrl() == null) {
                book.setImageUrl(apiImageUrl);
            }
            // Set seller info and metadata
            String userName = SessionManager.getInstance().getUserName();
            MongoCollection<Document> users = DatabaseManager.getDatabase().getCollection("users");
            Document user = users.find(Filters.eq("username", userName)).first();

            // Get user ID
            String userId = getStringId(user, "id");
            book.setSellerId(userId);
            book.setUploadDate(LocalDate.now().format(DateTimeFormatter.ISO_DATE));

            // Initialize statistics
            book.setTotalPurchases(0);
            book.setRating(0.0);
            book.setReviewCount(0);

            // Save to database
            if (BookDetailsCollection.insertBook(book)) {
                if (UsersCollection.shareBook(userId, book.getId())) {
                    showAlert(AlertType.INFORMATION, "Success", "Your book has been shared successfully!");
                    clearForm();
                } else {
                    showAlert(AlertType.ERROR, "Database Error", "Failed to share your book. Please try again.");
                }
            } else {
                showAlert(AlertType.ERROR, "Database Error", "Failed to share your book. Please try again.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sharing book", e);
            showAlert(AlertType.ERROR, "Error", "Failed to share book: " + e.getMessage());
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

    private String saveImage() {
        if (selectedImageFile == null) {
            return null;
        }

        try {
            // Create a unique filename
            String filename = UUID.randomUUID().toString() + "-" + selectedImageFile.getName();
            Path uploadDir = Paths.get("uploads", "book-covers");

            // Create directory if needed
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path destination = uploadDir.resolve(filename);
            Files.copy(selectedImageFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "uploads/book-covers/" + filename;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving image", e);
            return null;
        }
    }

    @FXML
    private void previewBook(ActionEvent event) {
        LOGGER.info("Preview book button clicked");
        if (!validateForm()) {
            return; // Validation failed
        }

        Book book = buildBookFromForm();

        try {
            // If we're using an image from API, set it directly
            if (apiImageUrl != null && selectedImageFile == null && isImageSelected) {
                book.setImageUrl(apiImageUrl);
            } else if (selectedImageFile != null) {
                // For local file, we'll use its URI for preview
                book.setImageUrl(selectedImageFile.toURI().toString());
            }

            // Load the preview screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/views/BookPreview.fxml"));
            Parent previewRoot = loader.load();

            // Pass the book to the preview controller
            BookPreviewController previewController = loader.getController();
            previewController.setBook(book);

            // Show the preview in a new window
            Stage previewStage = new Stage();
            previewStage.initModality(Modality.APPLICATION_MODAL);
            previewStage.setTitle("Book Preview");
            previewStage.setScene(new Scene(previewRoot));
            previewStage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading preview", e);
            showAlert(AlertType.ERROR, "Preview Error", "Could not display book preview: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in preview", e);
            showAlert(AlertType.ERROR, "Preview Error", "An unexpected error occurred while creating the preview.");
        }
    }

    private Book buildBookFromForm() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String language = languageComboBox.getValue();
        String publisher = publisherField.getText().trim();
        String isbn = isbnField.getText().trim();

        // Get publication date if set
        String publicationDate = "";
        if (publicationDatePicker.getValue() != null) {
            publicationDate = publicationDatePicker.getValue().format(DateTimeFormatter.ISO_DATE);
        }

        int pages = 0;
        try {
            pages = Integer.parseInt(pagesField.getText().trim());
        } catch (NumberFormatException e) {
            // Default to 0 if not a valid number
        }

        // Parse numeric values
        double originalPrice = parseDouble(originalPriceField.getText().trim(), 0);
        double discount = parseDouble(discountField.getText().trim(), 0);
        double currentPrice = parseDouble(currentPriceField.getText().trim(), originalPrice);

        String description = descriptionArea.getText().trim();
        String bookHubId = bookHubComboBox.getValue();
        String[] categories = selectedCategories.toArray(new String[0]);

        // String imageUrl = book.getImageUrl();

        // Build and return book object
        return new Book.Builder()
                .id(UUID.randomUUID().toString())
                .title(title)
                .author(author)
                .publisher(publisher)
                .isbn(isbn)
                .publicationDate(publicationDate)
                .language(language)
                .pages(pages)
                .originalPrice(originalPrice)
                .currentPrice(currentPrice)
                .discount(discount)
                .description(description)
                .bookHubId(bookHubId)
                .categories(categories)
                .build();
    }

    private double parseDouble(String value, double defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean validateForm() {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (titleField.getText().trim().isEmpty()) {
            errors.add("Title is required");
        }

        if (authorField.getText().trim().isEmpty()) {
            errors.add("Author is required");
        }

        if (languageComboBox.getValue() == null) {
            errors.add("Language is required");
        }

        if (originalPriceField.getText().trim().isEmpty()) {
            errors.add("Original price is required");
        }

        if (selectedCategories.isEmpty()) {
            errors.add("At least one category is required");
        }

        // Modified validation for image - allow API images
        if (!isImageSelected && selectedImageFile == null && apiImageUrl == null) {
            errors.add("Book image is required");
        }

        if (bookHubComboBox.getValue() == null) {
            errors.add("BookHub location is required");
        }

        if (descriptionArea.getText().trim().length() < 20) {
            errors.add("Book description is too short (minimum 20 characters)");
        }

        if (!errors.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", String.join("\n", errors));
            return false;
        }

        return true;
    }

    private void clearForm() {
        titleField.clear();
        authorField.clear();
        publisherField.clear();
        isbnField.clear();
        pagesField.clear();
        publicationDatePicker.setValue(null);
        languageComboBox.setValue(null);
        originalPriceField.clear();
        discountField.clear();
        currentPriceField.clear();
        categoryComboBox.setValue(null);
        selectedCategories.clear();
        categoryContainer.getChildren().clear();
        loadDefaultImage();
        imageNameLabel.setText("No file chosen");
        selectedImageFile = null;
        isImageSelected = false;
        apiImageUrl = null;
        bookHubComboBox.setValue(null);
        descriptionArea.clear();
    }
}