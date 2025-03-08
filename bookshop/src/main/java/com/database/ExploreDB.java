package com.database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bson.Document;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

/**
 * Handles all database operations for the Explore page
 */
public class ExploreDB {
    private static final Logger LOGGER = Logger.getLogger(ExploreDB.class.getName());
    private static final String COLLECTION_NAME = "bookdetails";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final MongoCollection<Document> books;

    public ExploreDB() {
        this.books = DatabaseManager.getCollection(COLLECTION_NAME);
    }

    /**
     * Fetches all books from the database
     * 
     * @return List of book documents
     */
    public List<Document> getAllBooks() {
        List<Document> bookList = new ArrayList<>();
        try {
            FindIterable<Document> cursor = books.find();
            cursor.forEach(bookList::add);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading books from database", e);
        }
        return bookList;
    }

    /**
     * Finds a book by its ID
     * 
     * @param bookId ID of the book to find
     * @return Book document or null if not found
     */
    public Document findBookById(String bookId) {
        try {
            return books.find(Filters.eq("id", bookId)).first();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error finding book by ID: " + bookId, e);
            return null;
        }
    }

    /**
     * Extracts unique values for filter dropdowns
     * 
     * @param allBooks List of all books
     * @return Map of filter options by category
     */
    public Map<String, Set<String>> extractFilterOptions(List<Document> allBooks) {
        Map<String, Set<String>> filterOptions = new HashMap<>();
        Set<String> languages = new HashSet<>();
        Set<String> categories = new HashSet<>();

        for (Document book : allBooks) {
            // Add language
            String language = book.getString("language");
            if (language != null) {
                languages.add(language);
            }

            // Add categories
            List<String> bookCategories = (List<String>) book.get("categories");
            if (bookCategories != null) {
                categories.addAll(bookCategories);
            }
        }

        filterOptions.put("languages", languages);
        filterOptions.put("categories", categories);
        return filterOptions;
    }

    /**
     * Applies all filters to the book list
     * 
     * @param allBooks     Original book list
     * @param filterParams Map of filter parameters
     * @return Filtered book list
     */
    public List<Document> applyFilters(List<Document> allBooks, Map<String, Object> filterParams) {
        List<Document> result = new ArrayList<>(allBooks);

        // Apply text search filters
        result = applyTextFilters(result, filterParams);

        // Apply selection filters
        result = applySelectionFilters(result, filterParams);

        // Apply numeric filters
        result = applyNumericFilters(result, filterParams);

        // Apply date filters
        result = applyDateFilters(result, filterParams);

        // Apply availability and discount filters
        result = applySpecialFilters(result, filterParams);

        // Apply sorting
        result = applySorting(result, filterParams);

        return result;
    }

    private List<Document> applyTextFilters(List<Document> books, Map<String, Object> params) {
        List<Document> result = books;

        // Apply search term filter
        String searchTerm = (String) params.get("searchTerm");
        if (searchTerm != null && !searchTerm.isEmpty()) {
            result = result.stream()
                    .filter(book -> contains(book, "title", searchTerm) ||
                            contains(book, "author", searchTerm) ||
                            contains(book, "description", searchTerm))
                    .collect(Collectors.toList());
        }

        // Apply author filter
        String authorFilter = (String) params.get("author");
        if (authorFilter != null && !authorFilter.isEmpty()) {
            result = result.stream()
                    .filter(book -> contains(book, "author", authorFilter))
                    .collect(Collectors.toList());
        }

        // Apply publisher filter
        String publisherFilter = (String) params.get("publisher");
        if (publisherFilter != null && !publisherFilter.isEmpty()) {
            result = result.stream()
                    .filter(book -> contains(book, "publisher", publisherFilter))
                    .collect(Collectors.toList());
        }

        return result;
    }

    private List<Document> applySelectionFilters(List<Document> books, Map<String, Object> params) {
        List<Document> result = books;

        // Apply language filter
        String languageFilter = (String) params.get("language");
        if (languageFilter != null && !languageFilter.isEmpty()) {
            result = result.stream()
                    .filter(book -> languageFilter.equals(book.getString("language")))
                    .collect(Collectors.toList());
        }

        // Apply category filter
        String categoryFilter = (String) params.get("category");
        if (categoryFilter != null && !categoryFilter.isEmpty()) {
            result = result.stream()
                    .filter(book -> {
                        List<String> categories = (List<String>) book.get("categories");
                        return categories != null && categories.contains(categoryFilter);
                    })
                    .collect(Collectors.toList());
        }

        return result;
    }

    private List<Document> applyNumericFilters(List<Document> books, Map<String, Object> params) {
        List<Document> result = books;

        // Apply price range filters
        Double minPrice = (Double) params.get("minPrice");
        if (minPrice != null) {
            result = result.stream()
                    .filter(book -> book.getInteger("currentPrice") >= minPrice)
                    .collect(Collectors.toList());
        }

        Double maxPrice = (Double) params.get("maxPrice");
        if (maxPrice != null) {
            result = result.stream()
                    .filter(book -> book.getInteger("currentPrice") <= maxPrice)
                    .collect(Collectors.toList());
        }

        // Apply rating filter
        Double minRating = (Double) params.get("minRating");
        if (minRating != null && minRating > 0) {
            result = result.stream()
                    .filter(book -> book.getDouble("rating") >= minRating)
                    .collect(Collectors.toList());
        }

        return result;
    }

    private List<Document> applyDateFilters(List<Document> books, Map<String, Object> params) {
        List<Document> result = books;

        LocalDate fromDate = (LocalDate) params.get("fromDate");
        LocalDate toDate = (LocalDate) params.get("toDate");

        if (fromDate != null || toDate != null) {
            result = result.stream()
                    .filter(book -> {
                        try {
                            String dateStr = book.getString("publicationDate");
                            if (dateStr == null || dateStr.isEmpty()) {
                                return true; // Keep if no date is available
                            }

                            LocalDate pubDate = LocalDate.parse(dateStr, DATE_FORMATTER);

                            boolean afterFromDate = fromDate == null || !pubDate.isBefore(fromDate);
                            boolean beforeToDate = toDate == null || !pubDate.isAfter(toDate);

                            return afterFromDate && beforeToDate;
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error parsing date for book: " + book.getString("title"), e);
                            return true; // Keep if date parsing fails
                        }
                    })
                    .collect(Collectors.toList());
        }

        return result;
    }

    private List<Document> applySpecialFilters(List<Document> books, Map<String, Object> params) {
        List<Document> result = books;

        // Apply availability filter
        String availabilityFilter = (String) params.get("availability");
        if (availabilityFilter != null && !availabilityFilter.equals("Any")) {
            switch (availabilityFilter) {
                case "Available Now":
                    result = result.stream()
                            .filter(book -> book.get("holderId") == null || book.getString("holderId").isEmpty())
                            .collect(Collectors.toList());
                    break;
                case "Available for Purchase":
                    // All books are available for purchase in this system
                    break;
                case "Available for Borrowing":
                    result = result.stream()
                            .filter(book -> book.containsKey("holderId") &&
                                    (book.get("holderId") == null || book.getString("holderId").isEmpty()))
                            .collect(Collectors.toList());
                    break;
            }
        }

        // Apply discount filter
        Boolean discountOnly = (Boolean) params.get("discountOnly");
        if (discountOnly != null && discountOnly) {
            result = result.stream()
                    .filter(book -> book.getInteger("discount") > 0)
                    .collect(Collectors.toList());
        }

        return result;
    }

    private List<Document> applySorting(List<Document> books, Map<String, Object> params) {
        String sortBy = (String) params.get("sortBy");
        Boolean ascending = (Boolean) params.get("ascending");

        if (sortBy == null || sortBy.equals("Relevance")) {
            return books; // No sorting
        }

        Comparator<Document> comparator = null;

        switch (sortBy) {
            case "Title":
                comparator = Comparator.comparing(doc -> doc.getString("title"));
                break;
            case "Author":
                comparator = Comparator.comparing(doc -> doc.getString("author"));
                break;
            case "Price":
                comparator = Comparator.comparing(doc -> doc.getInteger("currentPrice"));
                break;
            case "Rating":
                comparator = Comparator.comparing(doc -> doc.getDouble("rating"));
                break;
            case "Publication Date":
                comparator = Comparator.comparing(doc -> doc.getString("publicationDate"));
                break;
            default:
                return books;
        }

        List<Document> sortedBooks = new ArrayList<>(books);
        if (ascending == null || !ascending) {
            comparator = comparator.reversed();
        }

        sortedBooks.sort(comparator);
        return sortedBooks;
    }

    /**
     * Helper method to check if a field contains a substring (case-insensitive)
     */
    private boolean contains(Document doc, String field, String value) {
        String fieldValue = doc.getString(field);
        return fieldValue != null && fieldValue.toLowerCase().contains(value.toLowerCase());
    }
}