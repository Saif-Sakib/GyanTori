package com.database;

import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    // Configuration constants
    private static final String CONNECTION_STRING = "mongodb+srv://sakib:ZLsT19blLzqcMsNd@cluster0.s32ut.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
    private static final String DATABASE_NAME = "gyantori";

    // MongoDB client instances
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    // Initialize database connection
    static {
        try {
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database connection", e);
            throw new RuntimeException("Failed to connect to MongoDB", e);
        }
    }

    // Get database instance
    public static MongoDatabase getDatabase() {
        return database;
    }

    // Get collection
    public static MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    // Test database connection
    public static boolean testConnection() {
        try {
            getDatabase().runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Database connection test failed", e);
            return false;
        }
    }

    // Close database connection
    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}