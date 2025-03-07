package com.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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

import com.models.Book;

public class BooksDetailsCollection {
    private static final Logger LOGGER = Logger.getLogger(BooksDetailsCollection.class.getName());
    private static final String COLLECTION_NAME = "bookdetails";
    private static final MongoCollection<Document> books = DatabaseManager.getCollection(COLLECTION_NAME);

    // Initialize the collection with indexes if needed
    public static void initialize() {
        try {
            // Create indexes for common queries if they don't exist
            books.createIndex(new Document("isbn", 1), new IndexOptions().unique(true));
            books.createIndex(new Document("title", 1));
            books.createIndex(new Document("author", 1));
            LOGGER.info("Book collection initialized with indexes");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing book collection", e);
        }
    }

    // Insert a new book
    public static boolean insertBook(Book book) {
        try {
            Document doc = convertBookToDocument(book);
            InsertOneResult result = books.insertOne(doc);
            return result.wasAcknowledged();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error inserting book", e);
            return false;
        }
    }

    // Get book by ID
    public static Book getBookById(String id) {
        try {
            if (id == null || id.isEmpty()) {
                LOGGER.warning("Empty or null ID provided");
                return null;
            }

            // Check if the ID is a valid ObjectId
            Document doc = books.find(Filters.eq("_id", isValidObjectId(id) ? new ObjectId(id) : id)).first();

            if (doc != null) {
                return convertDocumentToBook(doc);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting book by ID: " + id, e);
        }
        return null;
    }

    // Get books by seller ID
    public static List<Book> getBooksBySellerId(String id) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            FindIterable<Document> iterable = books.find(Filters.eq("sellerId", id));

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by sequence", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    // Get books by holder ID
    public static List<Book> getBooksByHolderId(String id) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            FindIterable<Document> iterable = books.find(Filters.eq("holderId", id));

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by sequence", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    // Helper function to check if the ID is a valid ObjectId
    private static boolean isValidObjectId(String id) {
        return id != null && id.matches("^[0-9a-fA-F]{24}$");
    }

    // Get book by ISBN
    public static Book getBookByIsbn(String isbn) {
        try {
            Document doc = books.find(Filters.eq("isbn", isbn)).first();
            if (doc != null) {
                return convertDocumentToBook(doc);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting book by ISBN: " + isbn, e);
        }
        return null;
    }

    // Get all books - FIXED to properly handle cursor and close resources
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

    /**
     * Get books by numeric ID range
     * This function uses a custom numeric field instead of ObjectId
     * 
     * @param startId Start of the numeric ID range
     * @param endId   End of the numeric ID range
     * @return List of books within the specified ID range
     */
    public static List<Book> getBooksByNumericIdRange(int startId, int endId) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Create a filter for the range query using a numeric field
            // This assumes your collection has a numeric 'bookId' field
            Bson rangeFilter = Filters.and(
                    Filters.gte("bookId", startId),
                    Filters.lte("bookId", endId));

            // Find books within the ID range
            FindIterable<Document> iterable = books.find(rangeFilter);
            cursor = iterable.iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by numeric ID range", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    /**
     * Get books by sequence (position in the collection)
     * This is an alternative way to get a range of books when you don't have
     * a numeric ID field but want to get books by their position
     * 
     * @param start Starting position (0-based)
     * @param count Number of books to retrieve
     * @return List of books within the specified range
     */
    public static List<Book> getBooksBySequence(int start, int count) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Skip to the starting position and limit the number of results
            FindIterable<Document> iterable = books.find()
                    .skip(start)
                    .limit(count);

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by sequence", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    /**
     * Get a specific number of books starting from a specific ObjectId
     * This can be used when you have a valid ObjectId and want to get a range after
     * it
     * 
     * @param startObjectId The ObjectId to start from
     * @param count         Number of books to retrieve
     * @return List of books starting from the specified ObjectId
     */
    public static List<Book> getBooksStartingFromId(String startObjectId, int count) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Validate the ObjectId format
            ObjectId start = new ObjectId(startObjectId);

            // Create a filter to get documents with _id >= startObjectId
            Bson filter = Filters.gte("_id", start);

            // Find books with IDs greater than or equal to startObjectId
            FindIterable<Document> iterable = books.find(filter).limit(count);
            cursor = iterable.iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid ObjectId format: " + startObjectId, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books starting from ID", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    // Get books by author
    public static List<Book> getBooksByAuthor(String author) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            FindIterable<Document> iterable = books.find(Filters.eq("author", author));
            cursor = iterable.iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by author: " + author, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bookList;
    }

    // Get books by category
    public static List<Book> getBooksByCategory(String category) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            FindIterable<Document> iterable = books.find(Filters.in("categories", category));
            cursor = iterable.iterator();

            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting books by category: " + category, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bookList;
    }

    /**
     * Get top rated books, sorted by rating in descending order
     * 
     * @param limit Number of books to return
     * @return List of books sorted by rating
     */
    public static List<Book> getTopRatedBooks(int limit) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Find books and sort by rating in descending order
            FindIterable<Document> iterable = books.find()
                    .sort(Sorts.descending("rating")) // Use Sorts instead of Filters
                    .limit(limit);

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting top rated books", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    public static List<Book> getMostReviewedBooks(int limit) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Find books and sort by reviewCount in descending order
            FindIterable<Document> iterable = books.find()
                    .sort(Sorts.descending("reviewCount")) // Use Sorts instead of Filters
                    .limit(limit);

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting most reviewed books", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }
    // Alternative implementation if you still want to get books by position/offset
    // instead of sorting
    public static List<Book> getBooksByOffset(int skip, int limit) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Find books with skip and limit to get a specific range
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
            LOGGER.log(Level.SEVERE, "Error getting books by offset", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return bookList;
    }

    // Update book
    public static boolean updateBook(Book book) {
        try {
            if (book.getId() == null || book.getId().isEmpty()) {
                LOGGER.warning("Cannot update book with null or empty ID");
                return false;
            }

            Document updateDoc = convertBookToDocument(book);
            // Remove the _id field from the update document
            updateDoc.remove("_id");

            UpdateResult result = books.updateOne(
                    Filters.eq("_id", new ObjectId(book.getId())),
                    new Document("$set", updateDoc));

            return result.getModifiedCount() > 0;
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid ObjectId format: " + book.getId(), e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating book: " + book.getId(), e);
            return false;
        }
    }

    // Delete book
    public static boolean deleteBook(String id) {
        try {
            if (id == null || id.isEmpty()) {
                LOGGER.warning("Cannot delete book with null or empty ID");
                return false;
            }

            DeleteResult result = books.deleteOne(Filters.eq("_id", new ObjectId(id)));
            return result.getDeletedCount() > 0;
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid ObjectId format: " + id, e);
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting book: " + id, e);
            return false;
        }
    }

    // Convert Book object to MongoDB Document
    private static Document convertBookToDocument(Book book) {
        Document doc = new Document();

        if (book.getId() != null && !book.getId().isEmpty()) {
            try {
                doc.append("_id", new ObjectId(book.getId()));
            } catch (IllegalArgumentException e) {
                // ID is not in proper format, it will be generated by MongoDB
                LOGGER.log(Level.WARNING, "Invalid ObjectId format, will be generated by MongoDB", e);
            }
        }

        doc.append("title", book.getTitle())
                .append("author", book.getAuthor())
                .append("publisher", book.getPublisher())
                .append("publicationDate", book.getPublicationDate())
                .append("language", book.getLanguage())
                .append("pages", book.getPages())
                .append("isbn", book.getIsbn())
                .append("categories", Arrays.asList(book.getCategories()))
                .append("originalPrice", book.getOriginalPrice())
                .append("currentPrice", book.getCurrentPrice())
                .append("discount", book.getDiscount())
                .append("rating", book.getRating())
                .append("reviewCount", book.getReviewCount())
                .append("description", book.getDescription())
                .append("imageUrl", book.getImageUrl());

        return doc;
    }

    // Convert MongoDB Document to Book object
    private static Book convertDocumentToBook(Document doc) {
        if (doc == null) {
            LOGGER.log(Level.SEVERE, "Document is null");
            return null;
        }

        try {
            Book book = new Book();

            // Handle _id field safely (ObjectId or String)
            Object idField = doc.get("_id");
            book.setId(idField instanceof ObjectId ? ((ObjectId) idField).toHexString() : idField.toString());

            book.setTitle(doc.getString("title"));
            book.setAuthor(doc.getString("author"));
            book.setPublisher(doc.getString("publisher"));
            book.setPublicationDate(doc.getString("publicationDate"));
            book.setLanguage(doc.getString("language"));

            // Handle integer fields safely
            book.setPages(doc.containsKey("pages") ? doc.get("pages", Number.class).intValue() : 0);
            book.setReviewCount(doc.containsKey("reviewCount") ? doc.get("reviewCount", Number.class).intValue() : 0);

            book.setIsbn(doc.getString("isbn"));

            // Handle categories array safely
            List<String> categoriesList = doc.getList("categories", String.class);
            book.setCategories(categoriesList != null ? categoriesList.toArray(new String[0]) : new String[0]);

            // Handle double fields safely
            book.setOriginalPrice(
                    doc.containsKey("originalPrice") ? doc.get("originalPrice", Number.class).doubleValue() : 0.0);
            book.setCurrentPrice(
                    doc.containsKey("currentPrice") ? doc.get("currentPrice", Number.class).doubleValue() : 0.0);
            book.setDiscount(doc.containsKey("discount") ? doc.get("discount", Number.class).doubleValue() : 0.0);
            book.setRating(doc.containsKey("rating") ? doc.get("rating", Number.class).doubleValue() : 0.0);

            book.setDescription(doc.getString("description"));
            book.setImageUrl(doc.getString("imageUrl"));

            return book;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error converting document to book: " + e.getMessage(), e);
            return null;
        }
    }

    // Search books by title (partial match)
    public static List<Book> searchBooksByTitle(String titleQuery) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Create a pattern for case-insensitive search
            Pattern pattern = Pattern.compile(titleQuery, Pattern.CASE_INSENSITIVE);
            FindIterable<Document> iterable = books.find(Filters.regex("title", pattern));

            cursor = iterable.iterator();
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Book book = convertDocumentToBook(doc);
                bookList.add(book);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error searching books by title: " + titleQuery, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return bookList;
    }
}