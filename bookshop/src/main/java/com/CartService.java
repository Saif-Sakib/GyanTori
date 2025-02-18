package com;

import java.util.HashMap;
import java.util.Map;

public class CartService {
    private static CartService instance;
    private final Map<String, CartItem> cartItems = new HashMap<>();

    private CartService() {
    } // Private constructor

    public static CartService getInstance() {
        if (instance == null) {
            instance = new CartService();
        }
        return instance;
    }

    public void addItem(String itemId, String itemName, double price, String imageUrl) {
        if (cartItems.containsKey(itemId)) {
            CartItem existingItem = cartItems.get(itemId);
            existingItem.incrementQuantity();
        } else {
            cartItems.put(itemId, new CartItem(itemId, itemName, price, imageUrl));
        }
    }

    public Map<String, CartItem> getCartItems() {
        return cartItems;
    }

    public void updateItemQuantity(String itemId, int quantity) {
        if (cartItems.containsKey(itemId)) {
            cartItems.get(itemId).setQuantity(quantity);
        }
    }

    public void removeItem(String itemId) {
        cartItems.remove(itemId);
    }

    public void clearCart() {
        cartItems.clear();
    }

    // CartItem inner class
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
            this.quantity = quantity;
        }

        public void incrementQuantity() {
            this.quantity++;
        }
    }
}