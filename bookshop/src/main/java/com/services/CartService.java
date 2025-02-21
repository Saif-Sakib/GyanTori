// CartService.java
package com.services;

import java.util.HashMap;
import java.util.Map;

public class CartService {
    private static CartService instance;
    private final Map<String, CartItem> cartItems = new HashMap<>();
    private static final int MAX_QUANTITY = 99;

    private CartService() {
    }

    public static CartService getInstance() {
        if (instance == null) {
            instance = new CartService();
        }
        return instance;
    }

    public void addItem(String itemId, String itemName, double price, String imageUrl) {
        validateItemInput(itemId, itemName, price);

        if (cartItems.containsKey(itemId)) {
            CartItem item = cartItems.get(itemId);
            if (item.getQuantity() >= MAX_QUANTITY) {
                throw new IllegalStateException("Maximum quantity limit reached for item: " + itemName);
            }
            item.incrementQuantity();
        } else {
            cartItems.put(itemId, new CartItem(itemId, itemName, price, imageUrl));
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

    public void updateItemQuantity(String itemId, int quantity) {
        if (!cartItems.containsKey(itemId)) {
            throw new IllegalArgumentException("Item not found in cart: " + itemId);
        }
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Invalid quantity. Must be between 1 and " + MAX_QUANTITY);
        }
        cartItems.get(itemId).setQuantity(quantity);
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
        private final double price;
        private final String imageUrl;
        private int quantity;

        public CartItem(String id, String name, double price, String imageUrl) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.imageUrl = imageUrl;
            this.quantity = 1;
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

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            if (quantity < 1 || quantity > MAX_QUANTITY) {
                throw new IllegalArgumentException("Invalid quantity. Must be between 1 and " + MAX_QUANTITY);
            }
            this.quantity = quantity;
        }

        public void incrementQuantity() {
            if (quantity >= MAX_QUANTITY) {
                throw new IllegalStateException("Maximum quantity limit reached");
            }
            this.quantity++;
        }
    }
}
