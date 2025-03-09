package com.database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.models.Book;

/**
 * Handles all database operations for the Explore page by leveraging
 * BookDetailsCollection
 */
public class ExploreDB {
    private static final Logger LOGGER = Logger.getLogger(ExploreDB.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Fetches all books from the database
     * 
     * @return List of Book objects
     */
    public List<Book> getAllBooks() {
        return BookDetailsCollection.getAllBooks();
    }

    /**
     * Finds a book by its ID
     * 
     * @param bookId ID of the book to find
     * @return Book object or null if not found
     */
    public Book findBookById(String bookId) {
        return BookDetailsCollection.getBookById(bookId);
    }

    /**
     * Extracts unique values for filter dropdowns
     * 
     * @param allBooks List of all books
     * @return Map of filter options by category
     */
    public Map<String, Set<String>> extractFilterOptions(List<Book> allBooks) {
        Map<String, Set<String>> filterOptions = new HashMap<>();
        Set<String> languages = new HashSet<>();
        Set<String> categories = new HashSet<>();

        for (Book book : allBooks) {
            // Add language
            String language = book.getLanguage();
            if (language != null) {
                languages.add(language);
            }

            // Add categories
            String[] bookCategories = book.getCategories();
            if (bookCategories != null) {
                categories.addAll(Arrays.asList(bookCategories));
            }
        }

        filterOptions.put("languages", languages);
        filterOptions.put("categories", categories);
        return filterOptions;
    }

    /**
     * Searches books based on the search term using BookDetailsCollection
     * 
     * @param searchTerm The search term to find in books
     * @return List of books matching the search term
     */
    public List<Book> searchBooks(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return BookDetailsCollection.getAllBooks();
        }
        return BookDetailsCollection.searchBooks(searchTerm);
    }

    /**
     * Gets books by author using BookDetailsCollection
     * 
     * @param author The author name to search for
     * @return List of books by the specified author
     */
    public List<Book> getBooksByAuthor(String author) {
        if (author == null || author.isEmpty()) {
            return new ArrayList<>();
        }
        return BookDetailsCollection.getBooksByAuthor(author);
    }

    /**
     * Gets books by category using BookDetailsCollection
     * 
     * @param category The category to filter by
     * @return List of books in the specified category
     */
    public List<Book> getBooksByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return new ArrayList<>();
        }
        return BookDetailsCollection.getBooksByCategory(category);
    }

    /**
     * Applies composite filters that aren't directly available in
     * BookDetailsCollection
     * 
     * @param books        Original list of books
     * @param filterParams Map of filter parameters
     * @return Filtered list of books
     */
    public List<Book> applyCompositeFilters(List<Book> books, Map<String, Object> filterParams) {
        List<Book> result = new ArrayList<>(books);

        // Apply language filter
        String languageFilter = (String) filterParams.get("language");
        if (languageFilter != null && !languageFilter.isEmpty()) {
            result = result.stream()
                    .filter(book -> languageFilter.equals(book.getLanguage()))
                    .collect(Collectors.toList());
        }

        // Apply price range filters
        Double minPrice = (Double) filterParams.get("minPrice");
        if (minPrice != null) {
            result = result.stream()
                    .filter(book -> book.getCurrentPrice() >= minPrice)
                    .collect(Collectors.toList());
        }

        Double maxPrice = (Double) filterParams.get("maxPrice");
        if (maxPrice != null) {
            result = result.stream()
                    .filter(book -> book.getCurrentPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        // Apply rating filter
        Double minRating = (Double) filterParams.get("minRating");
        if (minRating != null && minRating > 0) {
            result = result.stream()
                    .filter(book -> book.getRating() >= minRating)
                    .collect(Collectors.toList());
        }

        // Apply date filters
        LocalDate fromDate = (LocalDate) filterParams.get("fromDate");
        LocalDate toDate = (LocalDate) filterParams.get("toDate");

        if (fromDate != null || toDate != null) {
            result = result.stream()
                    .filter(book -> {
                        try {
                            String dateStr = book.getPublicationDate();
                            if (dateStr == null || dateStr.isEmpty()) {
                                return true; // Keep if no date is available
                            }

                            LocalDate pubDate = LocalDate.parse(dateStr, DATE_FORMATTER);

                            boolean afterFromDate = fromDate == null || !pubDate.isBefore(fromDate);
                            boolean beforeToDate = toDate == null || !pubDate.isAfter(toDate);

                            return afterFromDate && beforeToDate;
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error parsing date for book: " + book.getTitle(), e);
                            return true; // Keep if date parsing fails
                        }
                    })
                    .collect(Collectors.toList());
        }

        // Apply availability filter
        String availabilityFilter = (String) filterParams.get("availability");
        if (availabilityFilter != null && !availabilityFilter.equals("Any")) {
            switch (availabilityFilter) {
                case "Available Now":
                    result = result.stream()
                            .filter(book -> book.getHolderId() == null || book.getHolderId().isEmpty())
                            .collect(Collectors.toList());
                    break;
                case "Available for Purchase":
                    // All books are available for purchase in this system
                    break;
                case "Available for Borrowing":
                    result = result.stream()
                            .filter(book -> book.getHolderId() == null || book.getHolderId().isEmpty())
                            .collect(Collectors.toList());
                    break;
            }
        }

        // Apply discount filter
        Boolean discountOnly = (Boolean) filterParams.get("discountOnly");
        if (discountOnly != null && discountOnly) {
            result = result.stream()
                    .filter(book -> book.getDiscount() > 0)
                    .collect(Collectors.toList());
        }

        return result;
    }

    /**
     * Applies sorting to the book list
     * 
     * @param books     List of books to sort
     * @param sortBy    Field to sort by
     * @param ascending Whether to sort in ascending order
     * @return Sorted list of books
     */
    public List<Book> sortBooks(List<Book> books, String sortBy, Boolean ascending) {
        if (sortBy == null || sortBy.equals("Relevance")) {
            return books; // No sorting
        }

        Comparator<Book> comparator = null;

        switch (sortBy) {
            case "Title":
                comparator = Comparator.comparing(Book::getTitle, Comparator.nullsLast(String::compareTo));
                break;
            case "Author":
                comparator = Comparator.comparing(Book::getAuthor, Comparator.nullsLast(String::compareTo));
                break;
            case "Price":
                comparator = Comparator.comparing(Book::getCurrentPrice, Comparator.nullsLast(Double::compareTo));
                break;
            case "Rating":
                comparator = Comparator.comparing(Book::getRating, Comparator.nullsLast(Double::compareTo));
                break;
            case "Publication Date":
                comparator = Comparator.comparing(Book::getPublicationDate, Comparator.nullsLast(String::compareTo));
                break;
            default:
                return books;
        }

        List<Book> sortedBooks = new ArrayList<>(books);
        if (ascending == null || !ascending) {
            comparator = comparator.reversed();
        }

        sortedBooks.sort(comparator);
        return sortedBooks;
    }

    /**
     * Main method to apply all filters and sorting
     * 
     * @param filterParams Map of filter parameters
     * @return Filtered and sorted list of books
     */
    public List<Book> getFilteredBooks(Map<String, Object> filterParams) {
        List<Book> result;

        // First try to use specialized search methods if applicable
        String searchTerm = (String) filterParams.get("searchTerm");
        String authorFilter = (String) filterParams.get("author");
        String categoryFilter = (String) filterParams.get("category");

        if (searchTerm != null && !searchTerm.isEmpty()) {
            // Use the search capability from BookDetailsCollection
            result = searchBooks(searchTerm);
        } else if (authorFilter != null && !authorFilter.isEmpty()) {
            // Use the author filter from BookDetailsCollection
            result = getBooksByAuthor(authorFilter);
        } else if (categoryFilter != null && !categoryFilter.isEmpty()) {
            // Use the category filter from BookDetailsCollection
            result = getBooksByCategory(categoryFilter);
        } else {
            // Get all books if no specialized filter is applicable
            result = getAllBooks();
        }

        // Apply any additional filters that aren't handled by BookDetailsCollection
        result = applyCompositeFilters(result, filterParams);

        // Apply sorting
        String sortBy = (String) filterParams.get("sortBy");
        Boolean ascending = (Boolean) filterParams.get("ascending");
        result = sortBooks(result, sortBy, ascending);

        return result;
    }
}