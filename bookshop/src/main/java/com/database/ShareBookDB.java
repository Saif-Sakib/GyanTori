package com.database;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import com.models.Book;
import com.models.Book.Review;

public class ShareBookDB {
    private static final Logger LOGGER = Logger.getLogger(ShareBookDB.class.getName());
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

            UpdateResult result = books.updateOne(
                    Filters.eq("_id", new ObjectId(idToUse)),
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

    // Convert Book object to MongoDB Document
    private static Document convertBookToDocument(Book book) {
        Document doc = new Document();

        // Handle MongoDB ID
        if (book.get_id() != null && !book.get_id().isEmpty()) {
            try {
                doc.append("_id", new ObjectId(book.get_id()));
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Invalid MongoDB ObjectId format, will be generated", e);
            }
        } else if (book.getId() != null && !book.getId().isEmpty()) {
            try {
                // If _id is not set but id is, use id as _id
                doc.append("_id", new ObjectId(book.getId()));
            } catch (IllegalArgumentException e) {
                // Application ID is not in proper ObjectId format
                LOGGER.log(Level.WARNING, "Invalid application ID format, MongoDB ID will be generated", e);
            }
        }

        // Application ID
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

    // Helper method to convert Document to Review
    private static Review documentToReview(Document doc) {
        String reviewerId = doc.getString("reviewerId");
        String comment = doc.getString("comment");
        double rating = doc.getDouble("rating");

        String reviewDateStr = doc.getString("reviewDate");
        LocalDate reviewDate = reviewDateStr != null ? LocalDate.parse(reviewDateStr) : null;

        return new Review(reviewerId, comment, rating, reviewDate);
    }
}