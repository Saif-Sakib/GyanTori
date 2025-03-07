package com.services;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionManager {
    private static final String FILE_PATH = "session.properties";
    private static SessionManager instance;
    private String userName;
    private String currentBookId;
    private boolean isLoggedIn;
    private List<String> cartItems; // Using List for dynamic sizing

    private SessionManager() {
        cartItems = new ArrayList<>();
        loadSession();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setUser(String name) {
        this.userName = name;
        saveSession();
    }

    public void setCurrentBookId(String book) {
        this.currentBookId = book;
        saveSession();
    }

    public void setIsLoggedIn(Boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
        saveSession();
    }

    public String getUserName() {
        return userName;
    }

    public String getCurrentBookId() {
        return currentBookId;
    }

    public boolean getIsLoggedIn() {
        return isLoggedIn;
    }

    // Cart management methods
    public void addToCart(String bookId) {
        if (!cartItems.contains(bookId)) {
            cartItems.add(bookId);
            saveSession();
        }
    }

    public void removeFromCart(String bookId) {
        cartItems.remove(bookId);
        saveSession();
    }

    public List<String> getCartItems() {
        return new ArrayList<>(cartItems); // Return a copy to prevent external modification
    }

    public void clearCart() {
        cartItems.clear();
        saveSession();
    }

    public void clearSession() {
        userName = null;
        cartItems.clear();
        deleteSessionFile();
    }

    private void saveSession() {
        try (FileOutputStream fileOut = new FileOutputStream(FILE_PATH)) {
            Properties properties = new Properties();
            properties.setProperty("userName", userName != null ? userName : "");

            // Convert cart items to a single string with delimiter
            String cartItemsStr = String.join(",", cartItems);
            properties.setProperty("cartItems", cartItemsStr);

            properties.store(fileOut, "User Session");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSession() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (FileInputStream fileIn = new FileInputStream(file)) {
                Properties properties = new Properties();
                properties.load(fileIn);

                userName = properties.getProperty("userName");

                // Load cart items
                String cartItemsStr = properties.getProperty("cartItems", "");
                if (!cartItemsStr.isEmpty()) {
                    cartItems = new ArrayList<>(Arrays.asList(cartItemsStr.split(",")));
                } else {
                    cartItems = new ArrayList<>();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteSessionFile() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }
}