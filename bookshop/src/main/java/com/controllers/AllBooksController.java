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
import com.services.SessionManager;

import java.util.Comparator;
import java.util.List;
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

    // Possible view types
    public static final String VIEW_ALL_BOOKS = "all_books";
    public static final String VIEW_HIGHLY_RATED = "highly_rated_books";
    public static final String VIEW_CATEGORY = "category_books";
    public static final String VIEW_PUBLISHER = "publisher_books";

    @FXML
    public void initialize() {
        // Initialize common elements from parent class
        initializeCommon();

        // Determine the view type and category/publisher from SessionManager
        String viewType = SessionManager.getPressedView();
        String category = SessionManager.getCurrentCategory();
        //String publisher = SessionManager.getCurrentPublisher();
        String publisher = null;
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
                "Popularity",
                "Price: Low to High",
                "Price: High to Low",
                "Rating",
                "Newest First");

        // Set default sort option based on view type
        if (VIEW_HIGHLY_RATED.equals(viewType)) {
            sortOptions.setValue("Rating");
        } else if (VIEW_PUBLISHER.equals(viewType)) {
            sortOptions.setValue("Popularity");
        } else {
            sortOptions.setValue("Popularity");
        }

        // Add listener for sort option changes
        sortOptions.setOnAction(event -> sortBooks());
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
        filterOptions.setOnAction(event -> filterBooks());
    }

    private void loadBooks(String viewType, String category, String publisher) {
        try {
            // Get books based on the view type, category, and publisher
            if (VIEW_HIGHLY_RATED.equals(viewType)) {
                allBooks = BookDetailsCollection.getTopRatedBooks(50); // Limit to top 50 rated books
                LOGGER.info("Loaded " + allBooks.size() + " highly rated books");
            } else if (VIEW_CATEGORY.equals(viewType) && category != null) {
                allBooks = BookDetailsCollection.getBooksByCategory(category);
                LOGGER.info("Loaded " + allBooks.size() + " books in category: " + category);
            } else if (VIEW_PUBLISHER.equals(viewType) && publisher != null) {
                //allBooks = BookDetailsCollection.getBooksByPublisher(publisher);
                LOGGER.info("Loaded " + allBooks.size() + " books from publisher: " + publisher);
            } else {
                allBooks = BookDetailsCollection.getAllBooks();
                LOGGER.info("Loaded " + allBooks.size() + " books");
            }

            // Set up pagination
            int pageCount = (allBooks.size() + BOOKS_PER_PAGE - 1) / BOOKS_PER_PAGE;
            booksPagination.setPageCount(pageCount > 0 ? pageCount : 1);
            booksPagination.setCurrentPageIndex(0);

            // Set page factory to display books for each page
            booksPagination.setPageFactory(this::createPage);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading books", e);
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Error", "Failed to load books");
        }
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

    private void sortBooks() {
        try {
            String sortOption = sortOptions.getValue();

            switch (sortOption) {
                case "Price: Low to High":
                    allBooks.sort(Comparator.comparing(Book::getCurrentPrice));
                    break;
                case "Price: High to Low":
                    allBooks.sort(Comparator.comparing(Book::getCurrentPrice).reversed());
                    break;
                case "Rating":
                    allBooks.sort(Comparator.comparing(Book::getRating).reversed());
                    break;
                case "Newest First":
                    allBooks.sort(Comparator.comparing(Book::getPublicationDate).reversed());
                    break;
                case "Popularity":
                default:
                    // For popularity, we could use a combination of ratings and number of reviews
                    allBooks.sort(
                            Comparator.comparing((Book book) -> book.getRating() * book.getReviewCount()).reversed());
                    break;
            }

            // Refresh the current page
            booksPagination.setCurrentPageIndex(booksPagination.getCurrentPageIndex());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sorting books", e);
        }
    }

    private void filterBooks() {
        try {
            // Get books based on view type, category, and publisher
            String viewType = SessionManager.getPressedView();
            String category = SessionManager.getCurrentCategory();
            //String publisher = SessionManager.getCurrentPublisher();
            String publisher = null;
            List<Book> filteredBooks;

            if (VIEW_HIGHLY_RATED.equals(viewType)) {
                filteredBooks = BookDetailsCollection.getTopRatedBooks(50);
            } else if (VIEW_CATEGORY.equals(viewType) && category != null) {
                filteredBooks = BookDetailsCollection.getBooksByCategory(category);
            } else {
                filteredBooks = BookDetailsCollection.getAllBooks();
            }

            String filterOption = filterOptions.getValue();

            // Apply filters
            switch (filterOption) {
                case "Available":
                    filteredBooks.removeIf(book -> !book.isFeatured());
                    break;
                case "Under ৳500":
                    filteredBooks.removeIf(book -> book.getCurrentPrice() >= 500);
                    break;
                case "Under ৳1000":
                    filteredBooks.removeIf(book -> book.getCurrentPrice() >= 1000);
                    break;
                case "5-Star Rated":
                    filteredBooks.removeIf(book -> book.getRating() < 4.5); // Consider 4.5+ as 5 star
                    break;
                case "All":
                default:
                    // No filtering needed
                    break;
            }

            // Update the allBooks list with filtered books
            allBooks = filteredBooks;

            // Update pagination
            int pageCount = (allBooks.size() + BOOKS_PER_PAGE - 1) / BOOKS_PER_PAGE;
            booksPagination.setPageCount(pageCount > 0 ? pageCount : 1);
            booksPagination.setCurrentPageIndex(0);

            // Refresh the display
            createPage(0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error filtering books", e);
        }
    }

    @FXML
    private void applyFilters() {
        // First filter books
        filterBooks();

        // Then sort the filtered books
        sortBooks();
    }
}