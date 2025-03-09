package com.database;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.UpdateResult;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsersCollection {
    private static final Logger LOGGER = Logger.getLogger(UsersCollection.class.getName());
    private static final String COLLECTION_NAME = "users";
    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String USER_ID_PREFIX = "BH";

    private static final MongoCollection<Document> users = DatabaseManager.getCollection(COLLECTION_NAME);

    // Initialize indexes
    static {
        try {
            setupIndexes();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to setup indexes", e);
        }
    }

    // Setup indexes for better performance and constraints
    private static void setupIndexes() {
        users.createIndex(new Document("username", 1), new IndexOptions().unique(true));
        users.createIndex(new Document("email", 1), new IndexOptions().unique(true));
        users.createIndex(new Document("id", 1), new IndexOptions().unique(true));
    }

    /**
     * Get user details by user ID
     * 
     * @param userId The unique identifier of the user
     * @return Document containing all user information or null if not found
     */
    public static Document getUserDetails(String userId) {
        try {
            return users.find(Filters.eq("id", userId)).first();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving user details for ID: " + userId, e);
            return null;
        }
    }

    /**
     * Get user ID from username
     * 
     * @param username The username to look up
     * @return The user's ID or null if not found
     */
    public static String getUserIdFromUsername(String username) {
        try {
            Document user = users.find(Filters.eq("username", username.toLowerCase())).first();
            return user != null ? user.getString("id") : null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error retrieving user ID for username: " + username, e);
            return null;
        }
    }

    /**
     * Share a book by adding a book ID to the user's uploaded_books list
     * 
     * @param userId The user's ID
     * @param bookId The book ID to add to uploaded books
     * @return true if update was successful
     */
    public static boolean shareBook(String userId, String bookId) {
        try {
            if (userId == null || bookId == null || bookId.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Invalid input for sharing book: userId={0}, bookId={1}",
                        new Object[] { userId, bookId });
                return false;
            }

            // Add book ID to the uploaded_books array using $addToSet to avoid duplicates
            Document update = new Document("$addToSet",
                    new Document("uploaded_books", bookId));

            UpdateResult result = users.updateOne(
                    Filters.eq("id", userId),
                    update);

            boolean success = result.getModifiedCount() > 0 || result.getMatchedCount() > 0;

            if (success) {
                LOGGER.log(Level.INFO, "Successfully shared book ID: {0} for user: {1}",
                        new Object[] { bookId, userId });
            } else {
                LOGGER.log(Level.WARNING, "Failed to share book ID: {0} for user: {1}. User not found.",
                        new Object[] { bookId, userId });
            }

            return success;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error sharing book ID: " + bookId + " for user: " + userId, e);
            return false;
        }
    }

    public static boolean registerUser(String fullName, String email, String username, String password) {
        try {
            // Validate input
            if (!isValidInput(fullName, email, username, password)) {
                return false;
            }

            // Check if username or email already exists
            if (userExists(username, email)) {
                return false;
            }

            // Generate salt and hash password
            byte[] salt = generateSalt();
            String passwordHash = hashPassword(password, salt);

            // Create new user document
            Document newUser = new Document()
                    .append("id", generateUserId())
                    .append("full_name", fullName)
                    .append("email", email.toLowerCase())
                    .append("username", username.toLowerCase())
                    .append("password_hash", passwordHash)
                    .append("salt", Base64.getEncoder().encodeToString(salt))
                    .append("location", null)
                    .append("rating", null)
                    .append("uploaded_books", new ArrayList<>())
                    .append("borrowed_books", new ArrayList<>())
                    .append("buyers_reviews", new ArrayList<>())
                    .append("created_at", System.currentTimeMillis())
                    .append("imgPath", null);

            users.insertOne(newUser);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error registering user", e);
            return false;
        }
    }

    /**
     * Update user's borrowed books list by adding new book IDs
     * 
     * @param userId  The user's ID
     * @param bookIds List of book IDs to add to borrowed books
     * @return true if update was successful
     */
    public static boolean updateUserBorrowedBooks(String userId, List<String> bookIds) {
        try {
            Document update = new Document("$addToSet",
                    new Document("borrowed_books",
                            new Document("$each", bookIds)));

            UpdateResult result = users.updateOne(
                    Filters.eq("id", userId),
                    update);

            return result.getModifiedCount() > 0 || result.getMatchedCount() > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating user's borrowed books for user: " + userId, e);
            return false;
        }
    }

    /**
     * Generate a unique user ID with format BH followed by 6 digits
     * 
     * @return Generated user ID
     */
    public static String generateUserId() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int number = 100000 + random.nextInt(900000); // Generates a 6-digit number
        return USER_ID_PREFIX + number;
    }

    /**
     * Validate user login credentials
     * 
     * @param username Username or email
     * @param password Password to validate
     * @return true if credentials are valid
     */
    public static boolean validateLogin(String username, String password) {
        try {
            // Allow login with either username or email
            Document user = null;
            if (username.contains("@")) {
                user = users.find(Filters.eq("email", username)).first();
            } else {
                user = users.find(Filters.eq("username", username)).first();
            }

            if (user != null) {
                String storedHash = user.getString("password_hash");
                byte[] salt = Base64.getDecoder().decode(user.getString("salt"));
                String hashAttempt = hashPassword(password, salt);
                return storedHash.equals(hashAttempt);
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating login for: " + username, e);
            return false;
        }
    }

    /**
     * Update user profile information
     * 
     * @param userId  User ID
     * @param updates Map of field names to new values
     * @return true if update was successful
     */
    public static boolean updateUserProfile(String userId, Map<String, Object> updates) {
        try {
            Document updateDoc = new Document("$set", new Document(updates));
            UpdateResult result = users.updateOne(Filters.eq("id", userId), updateDoc);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating user profile for: " + userId, e);
            return false;
        }
    }

    private static boolean isValidInput(String fullName, String email, String username, String password) {
        return fullName != null && !fullName.trim().isEmpty() &&
                email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$") &&
                username != null && username.matches("^[a-zA-Z0-9_]{3,20}$") &&
                password != null && password.length() >= 8;
    }

    private static boolean userExists(String username, String email) {
        return users.find(Filters.or(
                Filters.eq("username", username.toLowerCase()),
                Filters.eq("email", email.toLowerCase()))).first() != null;
    }

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    public static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Error hashing password", e);
            throw new RuntimeException("Error hashing password", e);
        }
    }
}