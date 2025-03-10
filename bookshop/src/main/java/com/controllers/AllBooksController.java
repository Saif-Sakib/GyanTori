package com.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.models.Book;
import com.database.BookDetailsCollection;
import com.database.ExploreDB;
import com.services.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AllBooksController extends CommonController {

    private static final Logger LOGGER = Logger.getLogger(AllBooksController.class.getName());
    private static final int BOOKS_PER_PAGE = 12;

    @FXML
    private Label categoryTitle;
    @FXML
    private FlowPane booksContainer;
    @FXML
    private Pagination booksPagination;
    @FXML
    private ComboBox<String> sortOptions;
    @FXML
    private ComboBox<String> filterOptions;
    @FXML
    private Button allBooksButton;
    @FXML
    private Button profileLoginButton;

    private List<Book> allBooks;
    private ExploreDB exploreDB;
    private Map<String, Object> filterParams;

    // Possible view types
    public static final String VIEW_ALL_BOOKS = "all_books";
    public static final String VIEW_HIGHLY_RATED = "highly_rated_books";
    public static final String VIEW_CATEGORY = "category_books";
    public static final String VIEW_PUBLISHER = "publisher_books";

    @FXML
    public void initialize() {
        // Initialize common elements from parent class
        initializeCommon();

        // Initialize ExploreDB and filter parameters
        exploreDB = new ExploreDB();
        filterParams = new HashMap<>();

        // Determine the view type and category/publisher from SessionManager
        String viewType = SessionManager.getPressedView();
        String category = SessionManager.getCurrentCategory();
        String publisher = null; // SessionManager.getCurrentPublisher();

        // Set the category title based on view type and category
        setCategoryTitle(viewType, category, publisher);

        // Setup sort options
        setupSortOptions(viewType);

        // Setup filter options
        setupFilterOptions();

        // Load books based on the view type and category/publisher
        loadBooks(viewType, category, publisher);
    }

    private void setCategoryTitle(String viewType, String category, String publisher) {
        if (VIEW_HIGHLY_RATED.equals(viewType)) {
            categoryTitle.setText("Highly Rated Books");
        } else if (VIEW_CATEGORY.equals(viewType) && category != null) {
            categoryTitle.setText(category + " Books");
        } else if (VIEW_PUBLISHER.equals(viewType) && publisher != null) {
            categoryTitle.setText(publisher + " Publications");
        } else {
            categoryTitle.setText("All Books");
        }
    }

    private void setupSortOptions(String viewType) {
        sortOptions.getItems().addAll(
                "Relevance",
                "Title",
                "Author",
                "Price",
                "Rating",
                "Publication Date");

        // Set default sort option based on view type
        if (VIEW_HIGHLY_RATED.equals(viewType)) {
            sortOptions.setValue("Rating");
            filterParams.put("sortBy", "Rating");
            filterParams.put("ascending", false);
        } else if (VIEW_PUBLISHER.equals(viewType)) {
            sortOptions.setValue("Relevance");
            filterParams.put("sortBy", "Relevance");
        } else {
            sortOptions.setValue("Relevance");
            filterParams.put("sortBy", "Relevance");
        }

        // Add listener for sort option changes
        sortOptions.setOnAction(event -> {
            filterParams.put("sortBy", sortOptions.getValue());
            applyFilters();
        });
    }

    private void setupFilterOptions() {
        filterOptions.getItems().addAll(
                "All",
                "Available",
                "Under ৳500",
                "Under ৳1000",
                "5-Star Rated");
        filterOptions.setValue("All");

        // Add listener for filter option changes
        filterOptions.setOnAction(event -> {
            applyFilterOptionToParams();
            applyFilters();
        });
    }

    private void applyFilterOptionToParams() {
        // Clear previous filter values
        filterParams.remove("minPrice");
        filterParams.remove("maxPrice");
        filterParams.remove("minRating");
        filterParams.remove("availability");

        String filterOption = filterOptions.getValue();
        switch (filterOption) {
            case "Available":
                filterParams.put("availability", "Available Now");
                break;
            case "Under ৳500":
                filterParams.put("maxPrice", 500.0);
                break;
            case "Under ৳1000":
                filterParams.put("maxPrice", 1000.0);
                break;
            case "5-Star Rated":
                filterParams.put("minRating", 4.5);
                break;
            case "All":
            default:
                // No filters needed
                break;
        }
    }

    private void loadBooks(String viewType, String category, String publisher) {
        try {
            // Reset filter parameters
            filterParams.clear();

            // Set the appropriate filter parameters based on view type
            if (VIEW_HIGHLY_RATED.equals(viewType)) {
                filterParams.put("minRating", 4.0);
                filterParams.put("sortBy", "Rating");
                filterParams.put("ascending", false);

                allBooks = exploreDB.getFilteredBooks(filterParams);
                LOGGER.info("Loaded " + allBooks.size() + " highly rated books");
            } else if (VIEW_CATEGORY.equals(viewType) && category != null) {
                filterParams.put("category", category);

                allBooks = exploreDB.getFilteredBooks(filterParams);
                LOGGER.info("Loaded " + allBooks.size() + " books in category: " + category);
            } else if (VIEW_PUBLISHER.equals(viewType) && publisher != null) {
                // Add publisher filter to ExploreDB if it's implemented
                // filterParams.put("publisher", publisher);

                allBooks = exploreDB.getFilteredBooks(filterParams);
                LOGGER.info("Loaded " + allBooks.size() + " books from publisher: " + publisher);
            } else {
                allBooks = exploreDB.getAllBooks();
                LOGGER.info("Loaded " + allBooks.size() + " books");
            }

            // Set up pagination
            updatePagination();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading books", e);
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Failed to load books");
        }
    }

    private void updatePagination() {
        int pageCount = (allBooks.size() + BOOKS_PER_PAGE - 1) / BOOKS_PER_PAGE;
        booksPagination.setPageCount(pageCount > 0 ? pageCount : 1);
        booksPagination.setCurrentPageIndex(0);

        // Set page factory to display books for each page
        booksPagination.setPageFactory(this::createPage);
    }

    private VBox createPage(int pageIndex) {
        try {
            booksContainer.getChildren().clear();

            int fromIndex = pageIndex * BOOKS_PER_PAGE;
            int toIndex = Math.min(fromIndex + BOOKS_PER_PAGE, allBooks.size());

            // Create book cards for the current page
            for (int i = fromIndex; i < toIndex; i++) {
                Book book = allBooks.get(i);
                VBox bookCard = createBookCard(book);
                booksContainer.getChildren().add(bookCard);
            }

            // Return the container with the books for this page
            VBox pageContainer = new VBox();
            pageContainer.getChildren().add(booksContainer);
            return pageContainer;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating page", e);
            return new VBox(); // Return empty VBox to avoid null
        }
    }

    @FXML
    private void applyFilters() {
        try {
            // Get view type and category/publisher for context
            String viewType = SessionManager.getPressedView();
            String category = SessionManager.getCurrentCategory();
            String publisher = null; // SessionManager.getCurrentPublisher();

            // Preserve context in filter parameters
            if (VIEW_CATEGORY.equals(viewType) && category != null) {
                filterParams.put("category", category);
            }

            if (VIEW_PUBLISHER.equals(viewType) && publisher != null) {
                // Add publisher filter to ExploreDB if it's implemented
                // filterParams.put("publisher", publisher);
            }

            if (VIEW_HIGHLY_RATED.equals(viewType)) {
                filterParams.put("minRating", 4.0);
            }

            // Apply current filter option
            applyFilterOptionToParams();

            // Apply ascending/descending based on sort type
            // Price is typically ascending, everything else is descending by default
            String sortBy = (String) filterParams.get("sortBy");
            if ("Price".equals(sortBy)) {
                filterParams.put("ascending", true);
            } else if (sortBy != null && !sortBy.equals("Relevance")) {
                filterParams.put("ascending", false);
            }

            // Get the filtered and sorted books
            allBooks = exploreDB.getFilteredBooks(filterParams);

            // Update pagination and refresh display
            updatePagination();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error applying filters", e);
        }
    }
}