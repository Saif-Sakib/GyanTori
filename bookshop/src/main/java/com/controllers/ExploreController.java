package com.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.bson.Document;

import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.database.ExploreDB;
import com.services.SessionManager;
import com.models.Book;
import com.database.BooksDetailsCollection;

public class ExploreController extends CommonController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ExploreController.class.getName());
    private static final int BOOKS_PER_PAGE = 12;

    // Services
    private final ExploreDB exploreDB = new ExploreDB();

    // Data Storage
    private ObservableList<Document> allBooks = FXCollections.observableArrayList();
    private ObservableList<Document> filteredBooks = FXCollections.observableArrayList();
    private Map<String, Set<String>> filterOptions = new HashMap<>();
    private boolean isAdvancedSearchVisible = false;

    // FXML Component References
    @FXML
    private TextField searchField;
    @FXML
    private Button toggleAdvancedSearchBtn;
    @FXML
    private VBox advancedSearchFilters;

    // Advanced Search Fields
    @FXML
    private TextField authorFilterField;
    @FXML
    private ComboBox<String> languageFilterCombo;
    @FXML
    private TextField publisherFilterField;
    @FXML
    private ComboBox<String> categoryFilterCombo;
    @FXML
    private TextField minPriceField;
    @FXML
    private TextField maxPriceField;
    @FXML
    private Slider ratingSlider;
    @FXML
    private Label ratingValueLabel;
    @FXML
    private DatePicker fromDatePicker;
    @FXML
    private DatePicker toDatePicker;
    @FXML
    private ComboBox<String> availabilityFilterCombo;
    @FXML
    private CheckBox discountFilterCheck;

    // Sort Controls
    @FXML
    private ComboBox<String> sortByCombo;
    @FXML
    private ComboBox<String> sortOrderCombo;
    @FXML
    private Label resultCountLabel;

    // Results and Pagination
    @FXML
    private TilePane booksGridPane;
    @FXML
    private Pagination booksPagination;
    @FXML
    private VBox noResultsBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            // Initialize common components from parent class
            initializeCommon();

            // Initialize UI components
            setupComboBoxes();
            setupListeners();
            setupPagination();

            // Load initial data
            loadAllBooks();

            // Set up the books display
            displayBooks(0);
            updateResultCount();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing ExploreController", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error",
                    "Failed to initialize the explore page. Please try again later.");
        }
    }

    private void loadAllBooks() {
        try {
            // Get books from database
            List<Document> bookList = exploreDB.getAllBooks();
            allBooks.clear();
            allBooks.addAll(bookList);
            filteredBooks.setAll(allBooks);

            // Extract filter options from books
            filterOptions = exploreDB.extractFilterOptions(bookList);
            populateFilterDropdowns();

            // Update pagination
            updatePagination();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading books", e);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load books from the database.");
        }
    }

    private void populateFilterDropdowns() {
        // Populate language dropdown
        Set<String> languages = filterOptions.get("languages");
        if (languages != null) {
            List<String> sortedLanguages = new ArrayList<>(languages);
            Collections.sort(sortedLanguages);
            languageFilterCombo.setItems(FXCollections.observableArrayList(sortedLanguages));
        }

        // Populate category dropdown
        Set<String> categories = filterOptions.get("categories");
        if (categories != null) {
            List<String> sortedCategories = new ArrayList<>(categories);
            Collections.sort(sortedCategories);
            categoryFilterCombo.setItems(FXCollections.observableArrayList(sortedCategories));
        }
    }

    private void setupComboBoxes() {
        // Sort options
        sortByCombo.setItems(FXCollections.observableArrayList(
                "Relevance", "Title", "Author", "Price", "Rating", "Publication Date"));
        sortByCombo.getSelectionModel().selectFirst();

        sortOrderCombo.setItems(FXCollections.observableArrayList("Ascending", "Descending"));
        sortOrderCombo.getSelectionModel().select(1); // Default to Descending

        // Availability options
        availabilityFilterCombo.setItems(FXCollections.observableArrayList(
                "Any", "Available Now", "Available for Purchase", "Available for Borrowing"));
        availabilityFilterCombo.getSelectionModel().selectFirst();
    }

    private void setupListeners() {
        // Rating slider listener
        ratingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double roundedValue = Math.round(newVal.doubleValue() * 2) / 2.0; // Round to nearest 0.5
            ratingValueLabel.setText(roundedValue + "+");
        });

        // Listen for sort changes
        sortByCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                applyFilters();
            }
        });

        sortOrderCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                applyFilters();
            }
        });

        // Search field listener
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                performSearch();
            }
        });

        // Numeric only for price fields
        setupNumericTextField(minPriceField);
        setupNumericTextField(maxPriceField);
    }

    private void setupNumericTextField(TextField textField) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                textField.setText(oldVal);
            }
        });
    }

    private void setupPagination() {
        booksPagination.setPageCount(1);
        booksPagination.setCurrentPageIndex(0);
        booksPagination.setPageFactory(this::createPage);
    }

    private Node createPage(int pageIndex) {
        booksGridPane.getChildren().clear();

        if (filteredBooks.isEmpty()) {
            noResultsBox.setVisible(true);
            return booksGridPane;
        }

        noResultsBox.setVisible(false);

        int fromIndex = pageIndex * BOOKS_PER_PAGE;
        int toIndex = Math.min(fromIndex + BOOKS_PER_PAGE, filteredBooks.size());

        for (int i = fromIndex; i < toIndex; i++) {
            try {
                Book book = BooksDetailsCollection.convertDocumentToBook(filteredBooks.get(i));
                VBox bookCard = createBookCard(book); // Using inherited method from CommonController
                booksGridPane.getChildren().add(bookCard);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error creating book card at index " + i, e);
            }
        }

        return booksGridPane;
    }

    @FXML
    private void toggleAdvancedSearch() {
        isAdvancedSearchVisible = !isAdvancedSearchVisible;
        advancedSearchFilters.setVisible(isAdvancedSearchVisible);
        advancedSearchFilters.setManaged(isAdvancedSearchVisible);
        toggleAdvancedSearchBtn.setText(isAdvancedSearchVisible ? "Hide Filters" : "Show Filters");
    }

    @FXML
    private void performSearch() {
        applyFilters();
    }

    @FXML
    private void clearFilters() {
        try {
            // Clear search field
            searchField.clear();

            // Reset advanced search filters
            authorFilterField.clear();
            languageFilterCombo.getSelectionModel().clearSelection();
            publisherFilterField.clear();
            categoryFilterCombo.getSelectionModel().clearSelection();
            minPriceField.clear();
            maxPriceField.clear();
            ratingSlider.setValue(0);
            fromDatePicker.setValue(null);
            toDatePicker.setValue(null);
            availabilityFilterCombo.getSelectionModel().selectFirst();
            discountFilterCheck.setSelected(false);

            // Reset results
            filteredBooks.setAll(allBooks);
            updatePagination();
            updateResultCount();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error clearing filters", e);
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to clear filters. Please try again.");
        }
    }

    @FXML
    private void applyFilters() {
        try {
            Map<String, Object> filterParams = new HashMap<>();

            // Add text search filters
            filterParams.put("searchTerm", searchField.getText().trim());
            filterParams.put("author", authorFilterField.getText().trim());
            filterParams.put("publisher", publisherFilterField.getText().trim());

            // Add selection filters
            filterParams.put("language", languageFilterCombo.getValue());
            filterParams.put("category", categoryFilterCombo.getValue());

            // Add numeric filters
            try {
                if (!minPriceField.getText().isEmpty()) {
                    filterParams.put("minPrice", Double.parseDouble(minPriceField.getText()));
                }
                if (!maxPriceField.getText().isEmpty()) {
                    filterParams.put("maxPrice", Double.parseDouble(maxPriceField.getText()));
                }
            } catch (NumberFormatException e) {
                LOGGER.log(Level.WARNING, "Invalid price format", e);
            }

            filterParams.put("minRating", ratingSlider.getValue());

            // Add date filters
            filterParams.put("fromDate", fromDatePicker.getValue());
            filterParams.put("toDate", toDatePicker.getValue());

            // Add availability and discount filters
            filterParams.put("availability", availabilityFilterCombo.getValue());
            filterParams.put("discountOnly", discountFilterCheck.isSelected());

            // Add sorting parameters
            filterParams.put("sortBy", sortByCombo.getValue());
            filterParams.put("ascending", "Ascending".equals(sortOrderCombo.getValue()));

            // Apply all filters using the ExploreDB service
            List<Document> result = exploreDB.applyFilters(new ArrayList<>(allBooks), filterParams);

            // Update filtered books
            filteredBooks.setAll(result);

            // Update UI
            updatePagination();
            updateResultCount();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error applying filters", e);
            showAlert(Alert.AlertType.ERROR, "Filter Error", "An error occurred while applying filters.");
        }
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) filteredBooks.size() / BOOKS_PER_PAGE);
        booksPagination.setPageCount(Math.max(1, pageCount));
        booksPagination.setCurrentPageIndex(0);
        displayBooks(0);
    }

    private void updateResultCount() {
        resultCountLabel.setText("Showing " + filteredBooks.size() + " book(s)");
    }

    private void displayBooks(int pageIndex) {
        booksPagination.setPageFactory(this::createPage);
    }

    /**
     * This overrides the duplicate method previously defined
     * Now it uses the parent class method directly
     */
    @FXML
    private void handleAddToCart(String bookId) {
        try {
            // Check if user is logged in - this is redundant as the parent method does this
            // check
            // but we need to get the Book object first

            // Get book details from database
            Document bookDoc = exploreDB.findBookById(bookId);
            if (bookDoc != null) {
                Book book = BooksDetailsCollection.convertDocumentToBook(bookDoc);
                super.handleAddToCart(book); // Call the parent method with the Book object
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Book not found.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error adding book to cart: " + bookId, e);
            showAlert(Alert.AlertType.ERROR, "Cart Error", "Failed to add book to cart. Please try again.");
        }
    }
}