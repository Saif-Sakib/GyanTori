package com.database;

import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsersCollection {
    private static final Logger LOGGER = Logger.getLogger(UsersCollection.class.getName());
    private static final String COLLECTION_NAME = "users";
    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";

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
        .append("id", generateUserId())  // Generate a unique ID, e.g., "BH123456"
        .append("full_name", fullName)
        .append("email", email.toLowerCase())
        .append("username", username.toLowerCase())
        .append("password_hash", passwordHash)
        .append("salt", Base64.getEncoder().encodeToString(salt))
        .append("location", null)  // Can be updated later
        .append("rating", null)  // Starts as null, can be updated when reviews come
        .append("uploaded_books", new ArrayList<>())  // Empty list initially
        .append("borrowed_books", new ArrayList<>())  // Empty list initially
        .append("buyers_reviews", new ArrayList<>())  // Empty list initially
        .append("created_at", System.currentTimeMillis())
        .append("imgPath", null);

            users.insertOne(newUser);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error registering user", e);
            return false;
        }
    }
    public static String generateUserId() {
    Random random = new Random();
    int number = 100000 + random.nextInt(900000); // Generates a 6-digit number (100000 - 999999)
    return "BH" + number;
    }

    public static boolean validateLogin(String username, String password) {
        try {
            Document user = users.find(Filters.eq("username", username.toLowerCase())).first();

            if (user != null) {
                String storedHash = user.getString("password_hash");
                byte[] salt = Base64.getDecoder().decode(user.getString("salt"));
                String hashAttempt = hashPassword(password, salt);
                return storedHash.equals(hashAttempt);
            }
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error validating login", e);
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