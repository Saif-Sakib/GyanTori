package com.database;

import com.models.Book;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchBooks {
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
            books.createIndex(new Document("categories", 1));
            LOGGER.info("Book collection initialized with indexes");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing book collection", e);
        }
    }

    public List<Book> searchBooks(String searchTerm) {
        List<Book> bookList = new ArrayList<>();
        MongoCursor<Document> cursor = null;

        try {
            // Create more comprehensive filters to match the document structure
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
                Book book = BooksDetailsCollection.convertDocumentToBook(doc);
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

    // Additional search methods for specific fields
    public Document findByIsbn(String isbn) {
        try {
            return books.find(Filters.eq("isbn", isbn)).first();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding book by ISBN: " + e.getMessage(), e);
            return null;
        }
    }

    public List<Document> findByAuthor(String author) {
        List<Document> results = new ArrayList<>();
        try (MongoCursor<Document> cursor = books.find(Filters.regex("author", author, "i")).iterator()) {
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding books by author: " + e.getMessage(), e);
        }
        return results;
    }

    public List<Document> findByCategory(String category) {
        List<Document> results = new ArrayList<>();
        try (MongoCursor<Document> cursor = books.find(Filters.regex("categories", category, "i")).iterator()) {
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding books by category: " + e.getMessage(), e);
        }
        return results;
    }

    public List<Document> findFeaturedBooks() {
        List<Document> results = new ArrayList<>();
        try (MongoCursor<Document> cursor = books.find(Filters.eq("featured", true)).iterator()) {
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error finding featured books: " + e.getMessage(), e);
        }
        return results;
    }
}