package com.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.input.KeyCode;

import com.database.BookDetailsCollection;
import com.database.DatabaseManager;
import com.models.Book;
import com.database.UsersCollection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import com.services.CartService;
import com.services.SessionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CartController {
    private static final Logger LOGGER = Logger.getLogger(CartController.class.getName());
    @FXML
    private Button returnButton;
    @FXML
    private VBox cartItemsContainer;
    @FXML
    private VBox emptyCartContainer;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label onlineFeeLabel; // Used for online service fee
    @FXML
    private Label deliveryFeeLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Button checkoutButton;
    @FXML
    private TextField promoCodeField;
    @FXML
    private Button applyPromoButton;
    @FXML
    private ScrollPane cartScrollPane;

    // Removed redundant declaration
    private final CartService cartService = CartService.getInstance();
    private final SimpleDoubleProperty subtotal = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty total = new SimpleDoubleProperty(0);
    private final DecimalFormat decimalFormat;
    private static final double PROMO_DISCOUNT = 0.10;
    private static final double ONLINE_FEE = 5.00;
    private static final double DELIVERY_FEE = 15.00;
    private boolean isPromoApplied = false;

    // Predefined borrowing day options - matching CartService min/max
    private static final int[] BORROW_DAY_OPTIONS = { 10, 20, 30 };
    private static final int DEFAULT_BORROW_DAYS = 30;

    public CartController() {
        decimalFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        decimalFormat.applyPattern("#,##0.00");
    }

    @FXML
    public void initialize() {
        try {
            setupEventHandlers();
            setupBindings();
            refreshCartView();
        } catch (Exception e) {
            handleError("Initialization failed", e);
        }
    }

    private void setupEventHandlers() {
        checkoutButton.setOnAction(event -> handleCheckout());
        applyPromoButton.setOnAction(event -> handlePromoCode());
        promoCodeField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handlePromoCode();
            }
        });
        returnButton.setOnAction(event -> handleReturn());
    }

    @FXML
    private void handleReturn() {
        loadHome();
    }

    private void setupBindings() {
        subtotalLabel.textProperty().bind(subtotal.asString("%.2f TK"));
        onlineFeeLabel.setText(formatPrice(ONLINE_FEE));
        deliveryFeeLabel.setText(formatPrice(DELIVERY_FEE));
        totalLabel.textProperty().bind(total.asString("%.2f TK"));
        updateTotals();
    }

    private Node createCartItemNode(Book book) {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(10));
        container.setPrefHeight(70); // Slightly increased height for more space
        container.getStyleClass().add("cart-item");

        // Book information section
        VBox infoBox = new VBox(5);
        Label nameLabel = new Label(book.getTitle());
        nameLabel.setStyle("-fx-font-weight: bold;");

        // Daily rate calculation
        double dailyRate = book.getCurrentPrice() / DEFAULT_BORROW_DAYS;
        Label rateLabel = new Label(formatPrice(dailyRate) + " / day");
        Label priceLabel = new Label(formatPrice(calculateItemPrice(book)));

        infoBox.getChildren().addAll(nameLabel, rateLabel);

        // Create borrowing days control
        HBox daysControlBox = new HBox(5);
        daysControlBox.setAlignment(Pos.CENTER);
        Label daysLabel = new Label("Borrow for: ");

        // ComboBox for common day options
        ComboBox<String> daysComboBox = new ComboBox<>();
        for (int days : BORROW_DAY_OPTIONS) {
            daysComboBox.getItems().add(days + " days");
        }
        daysComboBox.getItems().add("Custom...");

        // Set default value based on global borrowing days
        int currentDays = cartService.getBorrowDays();
        boolean isCustomDays = true;
        for (int option : BORROW_DAY_OPTIONS) {
            if (currentDays == option) {
                daysComboBox.setValue(option + " days");
                isCustomDays = false;
                break;
            }
        }
        if (isCustomDays) {
            daysComboBox.setValue("Custom...");
        }

        // TextField for custom days
        TextField customDaysField = new TextField(String.valueOf(currentDays));
        customDaysField.setPrefWidth(60);
        customDaysField.setVisible(isCustomDays);

        // Handle combobox selection changes
        daysComboBox.setOnAction(e -> {
            String selected = daysComboBox.getValue();
            if ("Custom...".equals(selected)) {
                customDaysField.setVisible(true);
                customDaysField.requestFocus();
            } else {
                customDaysField.setVisible(false);
                int days = Integer.parseInt(selected.split(" ")[0]);
                updateBorrowDays(days);
                priceLabel.setText(formatPrice(calculateItemPrice(book)));
            }
        });

        // Handle custom days field changes
        customDaysField.setOnAction(e -> {
            try {
                int days = Integer.parseInt(customDaysField.getText().trim());
                if (days > 0) {
                    updateBorrowDays(days);
                    priceLabel.setText(formatPrice(calculateItemPrice(book)));
                } else {
                    customDaysField.setText(String.valueOf(cartService.getBorrowDays()));
                    showAlert("Invalid Input", "Borrowing days must be greater than 0.", Alert.AlertType.WARNING);
                }
            } catch (NumberFormatException ex) {
                customDaysField.setText(String.valueOf(cartService.getBorrowDays()));
                showAlert("Invalid Input", "Please enter a valid number.", Alert.AlertType.WARNING);
            }
        });

        daysControlBox.getChildren().addAll(daysLabel, daysComboBox, customDaysField);

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("secondary-button");
        removeButton.setOnAction(e -> removeItem(book.getId()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Right side with price and controls
        VBox controlsBox = new VBox(5);
        controlsBox.setAlignment(Pos.CENTER_RIGHT);
        controlsBox.getChildren().addAll(priceLabel, daysControlBox);

        container.getChildren().addAll(
                infoBox,
                spacer,
                controlsBox,
                removeButton);

        return container;
    }

    private double calculateItemPrice(Book book) {
        double dailyRate = book.getCurrentPrice() / DEFAULT_BORROW_DAYS;
        return dailyRate * cartService.getBorrowDays();
    }

    private void updateBorrowDays(int days) {
        try {
            cartService.setGlobalBorrowDays(days);
            updateTotals();
        } catch (Exception e) {
            handleError("Failed to update borrowing days", e);
        }
    }

    public void handleDaysChange(int selectedDays) {
        // Update the borrowing days in the cart service
        cartService.setBorrowingDays(selectedDays);

        // Recalculate and update all prices
        updateCartTotals();

        // Update the UI to show the new total
        refreshCartView();
    }

    private void updateCartTotals() {
        // Recalculate all items with the new duration
        cartService.recalculateAllPrices();

        // Update the UI totals
        subtotal.set(cartService.getSubtotal());
        total.set(cartService.getTotal());

        // Update fixed fee labels
        onlineFeeLabel.setText(formatPrice(cartService.getServiceFee()));
        deliveryFeeLabel.setText(formatPrice(cartService.getDeliveryFee()));
    }

    private void removeItem(String itemId) {
        try {
            cartService.removeItem(itemId);
            refreshCartView();
        } catch (Exception e) {
            handleError("Failed to remove item", e);
        }
    }

    // Consolidated refreshCartView method combining both implementations
    public void refreshCartView() {
        // Clear container first
        cartItemsContainer.getChildren().clear();

        // Get cart items
        List<Book> items = cartService.getCartItems();

        // Update visibility based on cart state
        boolean isEmpty = items.isEmpty();

        // Set visibility of containers
        emptyCartContainer.setVisible(isEmpty);
        cartItemsContainer.setVisible(!isEmpty);

        if (!isEmpty) {
            // Add items to the container if cart is not empty
            for (Book book : items) {
                cartItemsContainer.getChildren().add(createCartItemNode(book));
            }
        }

        // Disable checkout button if cart is empty
        checkoutButton.setDisable(isEmpty);

        // Update cart totals
        updateCartTotals();
    }

    // Helper method to remove a book from cart
    public void handleRemoveItem(Book book) {
        cartService.removeFromCart(book);
        refreshCartView();
    }

    private void updateTotals() {
        double newSubtotal = cartService.getCartTotal();
        subtotal.set(newSubtotal);

        // Calculate total including fees
        double newTotal = newSubtotal;

        // Apply promo code discount if applicable
        if (isPromoApplied) {
            newTotal = newTotal * (1 - PROMO_DISCOUNT);
        }

        // Add fixed fees
        if (newSubtotal > 0) {
            newTotal += ONLINE_FEE + DELIVERY_FEE;
        }

        total.set(newTotal);
    }

    private void handlePromoCode() {
        String code = promoCodeField.getText().trim();
        if (!isPromoApplied && "SAVE10".equalsIgnoreCase(code)) {
            isPromoApplied = true;
            updateTotals();
            promoCodeField.setDisable(true);
            applyPromoButton.setDisable(true);
            showAlert("Success", "10% discount applied!", Alert.AlertType.INFORMATION);
        } else if (isPromoApplied) {
            showAlert("Already Applied", "Promo code already applied.", Alert.AlertType.WARNING);
        } else {
            showAlert("Invalid Code", "Please enter a valid promo code.", Alert.AlertType.WARNING);
        }
    }

    public void handleCheckout() {
        if (cartService.getItemCount() == 0) {
            showAlert("Empty Cart", "Please add items before checkout.", Alert.AlertType.WARNING);
            return;
        }

        try {
            // Get current user ID
            String userName = SessionManager.getInstance().getUserName();

            // We need userId for processing checkout
            String userId = UsersCollection.getUserIdFromUsername(userName); // Assuming this method exists
            if (userId == null || userId.isEmpty()) {
                showAlert("Authentication Error", "User is not logged in properly.", Alert.AlertType.ERROR);
                return;
            }

            // Calculate return date
            String borrowDate = LocalDate.now().toString();
            String returnDate = cartService.getExpectedReturnDate().toString();

            // Process each book in the cart
            boolean allSuccess = true;
            List<String> borrowedBookIds = new ArrayList<>();

            for (Book book : cartService.getCartItems()) {
                // Update book details in memory
                book.setHolderId(userId);
                book.setBorrowDate(borrowDate);
                book.setReturnDate(returnDate);

                // Update the book in the database
                boolean updated = BookDetailsCollection.updateBook(book);

                if (updated) {
                    borrowedBookIds.add(book.getId());
                } else {
                    allSuccess = false;
                    LOGGER.log(Level.WARNING, "Failed to update book: " + book.getId());
                }
            }

            // Update the user's borrowed books list
            if (!borrowedBookIds.isEmpty()) {
                // We need a method to update the user's borrowed books
                UsersCollection usersCollection = new UsersCollection();
                boolean userUpdated = usersCollection.updateUserBorrowedBooks(userId, borrowedBookIds);
                if (!userUpdated) {
                    allSuccess = false;
                    LOGGER.log(Level.WARNING, "Failed to update user's borrowed books");
                }
            }

            if (allSuccess) {
                showAlert("Checkout Successful",
                        "Books have been borrowed successfully.\nReturn by: " + returnDate,
                        Alert.AlertType.INFORMATION);

                // Clear the cart after successful checkout
                cartService.clearCart();
                refreshCartView();
            } else {
                showAlert("Checkout Issue",
                        "Some items couldn't be processed. Please try again.",
                        Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during checkout", e);
            showAlert("Checkout Error",
                    "An error occurred during checkout: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    private void handleError(String message, Exception e) {
        Platform.runLater(() -> showAlert("Error", message + ": " + e.getMessage(), Alert.AlertType.ERROR));
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        cartService.showAlert(type, title, null, content);
    }

    private String formatPrice(double price) {
        return decimalFormat.format(price) + " TK";
    }

    @FXML
    public void loadHome() {
        try {
            Stage stage = (Stage) returnButton.getScene().getWindow();
            LoadPageController.loadScene("home.fxml", "home.css", stage);
        } catch (Exception e) {
            handleError("Failed to load home page", e);
        }
    }
}