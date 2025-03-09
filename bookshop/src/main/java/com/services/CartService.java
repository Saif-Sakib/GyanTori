package com.services;

import java.util.HashMap;
import java.util.Map;
import com.controllers.CommonController;

import javafx.scene.control.Alert;

public class CartService {
    private static CartService instance;
    private final Map<String, CartItem> cartItems = new HashMap<>();
    private static final int DEFAULT_BORROW_DAYS = 30;
    private static final int MIN_BORROW_DAYS = 1;
    private static final int MAX_BORROW_DAYS = 90;

    private CartService() {
    }

    public static CartService getInstance() {
        if (instance == null) {
            instance = new CartService();
        }
        return instance;
    }

    public Boolean addItem(String itemId, String itemName, double price, String imageUrl) {
        validateItemInput(itemId, itemName, price);

        if (cartItems.containsKey(itemId)) {
            // For books, we don't allow duplicates (since each book is unique)
            return false;
        } else {
            cartItems.put(itemId, new CartItem(itemId, itemName, price, imageUrl));
            return true;
        }
    }

    private void validateItemInput(String itemId, String itemName, double price) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
    }

    public Map<String, CartItem> getCartItems() {
        return new HashMap<>(cartItems); // Return a copy to prevent external modification
    }

    public void updateBorrowDays(String itemId, int days) {
        if (!cartItems.containsKey(itemId)) {
            throw new IllegalArgumentException("Item not found in cart: " + itemId);
        }
        if (days < MIN_BORROW_DAYS || days > MAX_BORROW_DAYS) {
            throw new IllegalArgumentException("Invalid borrowing period. Must be between " +
                    MIN_BORROW_DAYS + " and " + MAX_BORROW_DAYS + " days");
        }
        cartItems.get(itemId).setBorrowDays(days);
    }

    public void removeItem(String itemId) {
        if (!cartItems.containsKey(itemId)) {
            throw new IllegalArgumentException("Item not found in cart: " + itemId);
        }
        cartItems.remove(itemId);
    }

    public void clearCart() {
        cartItems.clear();
    }

    public static class CartItem {
        private final String id;
        private final String name;
        private final double price; // Price for the default borrowing period (30 days)
        private final String imageUrl;
        private int borrowDays; // Number of days to borrow the book

        public CartItem(String id, String name, double price, String imageUrl) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
            this.borrowDays = DEFAULT_BORROW_DAYS; // Default to 30 days
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public int getBorrowDays() {
            return borrowDays;
        }

        public void setBorrowDays(int days) {
            if (days < MIN_BORROW_DAYS || days > MAX_BORROW_DAYS) {
                throw new IllegalArgumentException("Invalid borrowing period. Must be between " +
                        MIN_BORROW_DAYS + " and " + MAX_BORROW_DAYS + " days");
            }
            this.borrowDays = days;
        }
    }
    
    public void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}