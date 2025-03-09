package com.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.controllers.CommonController;
import com.models.Book;

import javafx.scene.control.Alert;
import java.time.LocalDate;

public class CartService {
    private static CartService instance;
    private final Map<String, Book> cartItems = new HashMap<>();
    private static final int DEFAULT_BORROW_DAYS = 30;
    private static final int MIN_BORROW_DAYS = 1;
    private static final int MAX_BORROW_DAYS = 90;
    private int borrowDays = DEFAULT_BORROW_DAYS;

    private CartService() {
    }

    public static CartService getInstance() {
        if (instance == null) {
            instance = new CartService();
        }
        return instance;
    }

    public Boolean addBookToCart(Book book) {
        if (book == null || book.getId() == null || book.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Book or book ID cannot be null or empty");
        }

        if (cartItems.containsKey(book.getId())) {
            // For books, we don't allow duplicates (since each book is unique)
            return false;
        } else {
            cartItems.put(book.getId(), book);
            return true;
        }
    }

    // Keeping this method for backward compatibility
    public Boolean addItem(String itemId, String itemName, double price, String imageUrl) {
        validateItemInput(itemId, itemName, price);

        if (cartItems.containsKey(itemId)) {
            // For books, we don't allow duplicates (since each book is unique)
            return false;
        } else {
            Book book = new Book();
            book.setId(itemId);
            book.setTitle(itemName);
            book.setCurrentPrice(price);
            book.setImageUrl(imageUrl);
            cartItems.put(itemId, book);
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

    public Map<String, Book> getCartItemsMap() {
        return new HashMap<>(cartItems); // Return a copy to prevent external modification
    }

    public List<Book> getCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    public double getCartTotal() {
        double total = 0.0;
        for (Book book : cartItems.values()) {
            total += book.getCurrentPrice();
        }
        return total;
    }

    public void setGlobalBorrowDays(int days) {
        if (days < MIN_BORROW_DAYS || days > MAX_BORROW_DAYS) {
            throw new IllegalArgumentException("Invalid borrowing period. Must be between " +
                    MIN_BORROW_DAYS + " and " + MAX_BORROW_DAYS + " days");
        }
        this.borrowDays = days;
    }

    public int getBorrowDays() {
        return this.borrowDays;
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

    public int getItemCount() {
        return cartItems.size();
    }

    public LocalDate getExpectedReturnDate() {
        return LocalDate.now().plusDays(borrowDays);
    }

    public boolean isBookInCart(String bookId) {
        return cartItems.containsKey(bookId);
    }

    public void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}