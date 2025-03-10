package com.database;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.services.SessionManager;
import com.models.Book;
import com.models.Book.Review;

public class BookDetailsCollection {
    private static final Logger LOGGER = Logger.getLogger(BookDetailsCollection.class.getName());
    private static final String COLLECTION_NAME = "bookdetails";
    private static final MongoCollection<Document> books = DatabaseManager.getCollection(COLLECTION_NAME);

    // Initialize the collection with indexes if needed
    public static void initialize() {
        try {
            // Create indexes for common queries if they don't exist
            books.createIndex(new Document("isbn", 1), new IndexOptions().unique(true));
            books.createIndex(new Document("title", 1));
            books.createIndex(new Document("author", 1));
            books.createIndex(new Document("categories", 1));
            books.createIndex(new Document("sellerId", 1));
            books.createIndex(new Document("featured", 1));
            LOGGER.info("Book collection initialized with indexes");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing book collection", e);
        }
    }

    // =============== CREATE OPERATIONS ===============

    // Insert a new book
    public static boolean insertBook(Book book) {
        try {
            Document doc = convertBookToDocument(book);
            InsertOneResult result = books.insertOne(doc);

            if (result.wasAcknowledged() && result.getInsertedId() != null) {
                // Update the book's ID with the generated MongoDB ID
                book.set_id(result.getInsertedId().asObjectId().getValue().toString());
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inserting book", e);
            return false;
        }
    }

    // =============== READ OPERATIONS ===============

    // Get book by ID
    public static Book getBookById(String id) {
        try {
            if (id == null || id.isEmpty()) {
                LOGGER.warning("Empty or null ID provided");
                return null;
            }

            // Check if the ID is a valid ObjectId
            Document doc = books.find(Filters.eq("id", isValidObjectId(id) ? new ObjectId(id) : id)).first();

            if (doc != null) {
                return convertDocumentToBook(doc);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting book by ID: " + id, e);
        }
        return null;
    }

    // Get all books
    public static List<Book> getAllBooks() {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            FindIterable<Document> iterable = books.find();
            cursor = iterable.iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting all books", e);
        } finally {
            // Close cursor in finally block to ensure resources are released
            if (cursor != null) {
                cursor.close();
            }
        }
        return bookList;
    }

    // Get books by seller ID
    public static List<Book> getBooksBySellerId(String sellerId) {
        return getBooksByFilter(Filters.eq("sellerId", sellerId));
    }

    // Get books by holder ID
    public static List<Book> getBooksByHolderId(String holderId) {
        return getBooksByFilter(Filters.eq("holderId", holderId));
    }

    // Get books by ISBN
    public static Book getBookByIsbn(String isbn) {
        try {
            Document doc = books.find(Filters.eq("isbn", isbn)).first();
            return (doc != null) ? convertDocumentToBook(doc) : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding book by ISBN: " + e.getMessage(), e);
            return null;
        }
    }

    // Get books by author
    public static List<Book> getBooksByAuthor(String author) {
        return getBooksByFilter(Filters.regex("author", Pattern.compile(author, Pattern.CASE_INSENSITIVE)));
    }

    /**
 * Gets all books from a specific publisher
 * 
 * @param publisher The publisher name to filter by
 * @return List of books from the specified publisher
 */
public static List<Book> getBooksByPublisher(String publisher) {
    try {
        Bson filter = Filters.eq("publisher", publisher);
        return books.find(filter).into(new ArrayList<>()).stream()
                .map(BookDetailsCollection::convertDocumentToBook)
                .collect(Collectors.toList());
    } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Error getting books by publisher: " + publisher, e);
        return new ArrayList<>();
    }
}

    // Get books by category
    public static List<Book> getBooksByCategory(String category) {
        return getBooksByFilter(Filters.regex("categories", Pattern.compile(category, Pattern.CASE_INSENSITIVE)));
    }

    // Get featured books
    public static List<Book> getFeaturedBooks() {
        return getBooksByFilter(Filters.eq("featured", true));
    }

    // Get top rated books
    public static List<Book> getTopRatedBooks(int limit) {
        return getBooksByFilterAndSort(Filters.gte("rating", 0), Sorts.descending("rating"), limit);
    }

    // Get most reviewed books
    public static List<Book> getMostReviewedBooks(int limit) {
        return getBooksByFilterAndSort(Filters.gte("reviewCount", 0), Sorts.descending("reviewCount"), limit);
    }

    // Get books by pagination
    public static List<Book> getBooksByPagination(int skip, int limit) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            FindIterable<Document> iterable = books.find()
                    .skip(skip)
                    .limit(limit);

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by pagination", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    // =============== SEARCH OPERATIONS ===============

    // Search books by title (partial match)
    public static List<Book> searchBooksByTitle(String titleQuery) {
        Pattern pattern = Pattern.compile(titleQuery, Pattern.CASE_INSENSITIVE);
        return getBooksByFilter(Filters.regex("title", pattern));
    }

    // Comprehensive search across multiple fields
    public static List<Book> searchBooks(String searchTerm) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Create comprehensive filters to match the document structure
            FindIterable<Document> iterable = books.find(Filters.or(
                    Filters.regex("title", searchTerm, "i"),
                    Filters.regex("author", searchTerm, "i"),
                    Filters.regex("categories", searchTerm, "i"),
                    Filters.regex("publisher", searchTerm, "i"),
                    Filters.regex("description", searchTerm, "i"),
                    Filters.regex("isbn", searchTerm, "i"),
                    Filters.regex("id", searchTerm, "i")));

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                if (book != null) {
                    bookList.add(book);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching books: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    // =============== UPDATE OPERATIONS ===============

    // Update book
    public static boolean updateBook(Book book) {
        try {
            String idToUse = book.get_id() != null ? book.get_id() : book.getId();

            if (idToUse == null || idToUse.isEmpty()) {
                LOGGER.warning("Cannot update book with null or empty ID");
                return false;
            }

            Document updateDoc = convertBookToDocument(book);
            // Remove the _id field from the update document
            updateDoc.remove("_id");

            // Create filter based on the type of ID
            Bson filter;
            if (idToUse.matches("[0-9a-fA-F]{24}")) {
                // It's a valid MongoDB ObjectId
                filter = Filters.eq("_id", new ObjectId(idToUse));
            } else {
                // Treat as a string ID (application ID)
                filter = Filters.eq("id", idToUse);
            }

            UpdateResult result = books.updateOne(filter, new Document("$set", updateDoc));

            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating book with ID: " +
                    (book.get_id() != null ? book.get_id() : book.getId()), e);
            return false;
        }
    }

    // =============== DELETE OPERATIONS ===============

    // Delete book
    public static boolean deleteBook(String id) {
        try {
            if (id == null || id.isEmpty()) {
                LOGGER.warning("Cannot delete book with null or empty ID");
                return false;
            }

            DeleteResult result = books.deleteOne(Filters.eq("id", new ObjectId(id)));
            return result.getDeletedCount() > 0;
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid ObjectId format: " + id, e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting book: " + id, e);
            return false;
        }
    }

    // =============== HELPER METHODS ===============

    // Helper function to check if the ID is a valid ObjectId
    private static boolean isValidObjectId(String id) {
        return id != null && id.matches("^[0-9a-fA-F]{24}$");
    }

    // Generic method to get books by any filter
    private static List<Book> getBooksByFilter(Bson filter) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            FindIterable<Document> iterable = books.find(filter);
            cursor = iterable.iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                if (book != null) {
                    bookList.add(book);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by filter: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    // Generic method to get books by filter and sort
    private static List<Book> getBooksByFilterAndSort(Bson filter, Bson sort, int limit) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            FindIterable<Document> iterable = books.find(filter)
                    .sort(sort)
                    .limit(limit);

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                if (book != null) {
                    bookList.add(book);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by filter and sort: " + e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    public static boolean addBookReview(String bookId, Review newReview) {
        try {
            if (bookId == null || bookId.isEmpty() || newReview == null) {
                LOGGER.warning("Cannot add review: Book ID is null/empty or review is null");
                return false;
            }

            // Create filter based on the format of the ID
            Bson filter;
            if (bookId.matches("[0-9a-fA-F]{24}")) {
                // It's a valid MongoDB ObjectId
                filter = Filters.eq("_id", new ObjectId(bookId));
                LOGGER.info("Using MongoDB ObjectId filter for: " + bookId);
            } else {
                // Treat as an application ID (string)
                filter = Filters.eq("id", bookId);
                LOGGER.info("Using application ID filter for: " + bookId);
            }

            // Try to find the book first
            Document bookDoc = books.find(filter).first();
            if (bookDoc == null) {
                LOGGER.warning("Cannot add review: Book with ID " + bookId + " not found");
                return false;
            }

            String user = SessionManager.getInstance().getUserName();
            String userId = UsersCollection.getUserIdFromUsername(user);
            // Adapt from your Review class to document format
            Document reviewDoc = new Document()
                    .append("reviewerId", userId) // Map reviewerName to reviewerId
                    .append("comment", newReview.getComment())
                    .append("rating", (double) newReview.getRating()) // Convert int to double
                    .append("reviewDate", newReview.getReviewDate().toString());

            // Get current reviews and calculate new average rating
            List<Document> currentReviews = bookDoc.getList("buyerReviews", Document.class);
            int newReviewCount = (currentReviews != null ? currentReviews.size() : 0) + 1;

            double totalRating = 0;
            if (currentReviews != null) {
                for (Document review : currentReviews) {
                    Number rating = review.get("rating", Number.class);
                    totalRating += (rating != null ? rating.doubleValue() : 0);
                }
            }
            totalRating += newReview.getRating();
            double newAverageRating = totalRating / newReviewCount;

            // Log the update information for debugging
            LOGGER.info("Adding review to book: " + bookId +
                    ", New review count: " + newReviewCount +
                    ", New average rating: " + newAverageRating);

            // Perform atomic update operation - push the review and update rating/count
            UpdateResult result = books.updateOne(
                    filter,
                    new Document("$push", new Document("buyerReviews", reviewDoc))
                            .append("$set", new Document("rating", newAverageRating)
                                    .append("reviewCount", newReviewCount)));

            if (result.getModifiedCount() > 0) {
                LOGGER.info("Successfully added review to book with ID: " + bookId);
                return true;
            } else {
                LOGGER.warning("Failed to add review: No document was modified");
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding review to book with ID: " + bookId, e);
            e.printStackTrace(); // Add stack trace for more detailed debugging
            return false;
        }
    }

    // Convert MongoDB Document to Book object
    public static Book convertDocumentToBook(Document doc) {
        if (doc == null) {
            LOGGER.log(Level.SEVERE, "Document is null");
            return null;
        }

        try {
            Book book = new Book();

            // Handle _id field safely (ObjectId or String)
            Object idField = doc.get("_id");
            if (idField != null) {
                book.set_id(idField instanceof ObjectId ? ((ObjectId) idField).toHexString() : idField.toString());
            }

            // Application ID
            book.setId(doc.getString("id"));

            // Book metadata
            book.setTitle(doc.getString("title"));
            book.setAuthor(doc.getString("author"));
            book.setPublisher(doc.getString("publisher"));
            book.setPublicationDate(doc.getString("publicationDate"));
            book.setLanguage(doc.getString("language"));
            book.setIsbn(doc.getString("isbn"));
            book.setDescription(doc.getString("description"));
            book.setImageUrl(doc.getString("imageUrl"));
            book.setBookHubId(doc.getString("bookHubId"));

            // Handle integer fields safely
            book.setPages(doc.containsKey("pages") ? doc.get("pages", Number.class).intValue() : 0);
            book.setReviewCount(doc.containsKey("reviewCount") ? doc.get("reviewCount", Number.class).intValue() : 0);
            book.setTotalPurchases(
                    doc.containsKey("totalPurchases") ? doc.get("totalPurchases", Number.class).intValue() : 0);

            // Handle categories array safely
            List<String> categoriesList = doc.getList("categories", String.class);
            book.setCategories(categoriesList != null ? categoriesList.toArray(new String[0]) : new String[0]);

            // Handle double fields safely - using Number.class to handle both Integer and
            // Double types
            if (doc.containsKey("originalPrice")) {
                Number originalPrice = doc.get("originalPrice", Number.class);
                book.setOriginalPrice(originalPrice != null ? originalPrice.doubleValue() : 0.0);
            } else {
                book.setOriginalPrice(0.0);
            }

            if (doc.containsKey("currentPrice")) {
                Number currentPrice = doc.get("currentPrice", Number.class);
                book.setCurrentPrice(currentPrice != null ? currentPrice.doubleValue() : 0.0);
            } else {
                book.setCurrentPrice(0.0);
            }

            if (doc.containsKey("discount")) {
                Number discount = doc.get("discount", Number.class);
                book.setDiscount(discount != null ? discount.doubleValue() : 0.0);
            } else {
                book.setDiscount(0.0);
            }

            if (doc.containsKey("rating")) {
                Number rating = doc.get("rating", Number.class);
                book.setRating(rating != null ? rating.doubleValue() : 0.0);
            } else {
                book.setRating(0.0);
            }

            // Transaction information
            book.setSellerId(doc.getString("sellerId"));
            book.setUploadDate(doc.getString("uploadDate"));
            book.setHolderId(doc.getString("holderId"));
            book.setBorrowDate(doc.getString("borrowDate"));
            book.setReturnDate(doc.getString("returnDate"));
            book.setFeatured(doc.getBoolean("featured", false));

            // Handle buyer reviews
            if (doc.containsKey("buyerReviews")) {
                List<Document> reviewDocs = doc.getList("buyerReviews", Document.class);
                if (reviewDocs != null) {
                    List<Review> reviews = new ArrayList<>();
                    for (Document reviewDoc : reviewDocs) {
                        Review review = new Review();
                        review.setReviewerId(reviewDoc.getString("reviewerId"));
                        review.setComment(reviewDoc.getString("comment"));

                        // Handle rating safely (could be Integer or Double)
                        if (reviewDoc.containsKey("rating")) {
                            Number reviewRating = reviewDoc.get("rating", Number.class);
                            review.setRating(reviewRating != null ? reviewRating.doubleValue() : 0.0);
                        } else {
                            review.setRating(0.0);
                        }

                        String reviewDateStr = reviewDoc.getString("reviewDate");
                        if (reviewDateStr != null) {
                            review.setReviewDate(LocalDate.parse(reviewDateStr));
                        }

                        reviews.add(review);
                    }
                    book.setBuyerReviews(reviews);
                }
            }

            return book;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error converting document to book: " + e.getMessage(), e);
            return null;
        }
    }

    // Convert Book object to MongoDB Document
    private static Document convertBookToDocument(Book book) {
        Document doc = new Document();

        // Handle MongoDB ID - don't try to convert application IDs to ObjectIds
        if (book.get_id() != null && !book.get_id().isEmpty()) {
            try {
                // Only convert to ObjectId if it's a valid 24-character hex string
                if (book.get_id().matches("[0-9a-fA-F]{24}")) {
                    doc.append("_id", new ObjectId(book.get_id()));
                } else {
                    // Otherwise, store as string
                    doc.append("_id", book.get_id());
                }
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid MongoDB ObjectId format", e);
            }
        }

        // Application ID - store as string, not ObjectId
        if (book.getId() != null) {
            doc.append("id", book.getId());
        }

        // Book metadata
        doc.append("title", book.getTitle())
                .append("author", book.getAuthor())
                .append("publisher", book.getPublisher())
                .append("publicationDate", book.getPublicationDate())
                .append("language", book.getLanguage())
                .append("pages", book.getPages())
                .append("isbn", book.getIsbn())
                .append("description", book.getDescription())
                .append("imageUrl", book.getImageUrl())
                .append("bookHubId", book.getBookHubId());

        // Handle categories array
        if (book.getCategories() != null) {
            doc.append("categories", Arrays.asList(book.getCategories()));
        }

        // Price information
        doc.append("originalPrice", book.getOriginalPrice())
                .append("currentPrice", book.getCurrentPrice())
                .append("discount", book.getDiscount());

        // Review information
        doc.append("rating", book.getRating())
                .append("reviewCount", book.getReviewCount());

        // Transaction information
        doc.append("sellerId", book.getSellerId())
                .append("uploadDate", book.getUploadDate())
                .append("totalPurchases", book.getTotalPurchases())
                .append("holderId", book.getHolderId())
                .append("borrowDate", book.getBorrowDate())
                .append("returnDate", book.getReturnDate())
                .append("featured", book.isFeatured());

        // Handle buyer reviews
        if (book.getBuyerReviews() != null && !book.getBuyerReviews().isEmpty()) {
            List<Document> reviewDocs = new ArrayList<>();
            for (Review review : book.getBuyerReviews()) {
                Document reviewDoc = new Document()
                        .append("reviewerId", review.getReviewerId())
                        .append("comment", review.getComment())
                        .append("rating", review.getRating());

                if (review.getReviewDate() != null) {
                    reviewDoc.append("reviewDate", review.getReviewDate().toString());
                }

                reviewDocs.add(reviewDoc);
            }
            doc.append("buyerReviews", reviewDocs);
        }

        return doc;
    }
}