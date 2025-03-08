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
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.input.KeyCode;
import com.services.CartService;

public class CartController {
    @FXML
    private Button returnButton;
    @FXML
    private VBox cartItemsContainer;
    @FXML
    private VBox emptyCartContainer;
    @FXML
    private Label subtotalLabel;
    @FXML
    private Label onlineFeeLabel;
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

    private final CartService cartService = CartService.getInstance();
    private final SimpleDoubleProperty subtotal = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty total = new SimpleDoubleProperty(0);
    private final DecimalFormat decimalFormat;
    private static final double PROMO_DISCOUNT = 0.10;
    private static final double ONLINE_FEE = 5.00;
    private static final double DELIVERY_FEE = 15.00;
    private boolean isPromoApplied = false;

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

    private Node createCartItemNode(CartService.CartItem item) {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(10));
        container.setPrefHeight(60);
        container.getStyleClass().add("cart-item");

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label priceLabel = new Label(formatPrice(item.getPrice()));

        Spinner<Integer> quantitySpinner = createQuantitySpinner(item);
        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().add("secondary-button");
        removeButton.setOnAction(e -> removeItem(item.getId()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        container.getChildren().addAll(
                new VBox(5, nameLabel, priceLabel),
                spacer,
                quantitySpinner,
                removeButton);

        return container;
    }

    private Spinner<Integer> createQuantitySpinner(CartService.CartItem item) {
        Spinner<Integer> spinner = new Spinner<>(1, 99, item.getQuantity());
        spinner.setEditable(true);
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                try {
                    cartService.updateItemQuantity(item.getId(), newVal);
                    updateTotals();
                } catch (Exception e) {
                    handleError("Failed to update quantity", e);
                    spinner.getValueFactory().setValue(oldVal);
                }
            }
        });
        return spinner;
    }

    private void removeItem(String itemId) {
        try {
            cartService.removeItem(itemId);
            refreshCartView();
        } catch (Exception e) {
            handleError("Failed to remove item", e);
        }
    }

    public void refreshCartView() {
        // Clear container first
        cartItemsContainer.getChildren().clear();

        // Get cart items
        List<CartService.CartItem> items = new ArrayList<>(cartService.getCartItems().values());

        // Update visibility based on cart state
        boolean isEmpty = items.isEmpty();

        // This is the main fix - make sure both containers are visible
        // but only populate the appropriate one
        emptyCartContainer.setVisible(isEmpty);
        cartItemsContainer.setVisible(!isEmpty);

        if (!isEmpty) {
            // Add items to the container if cart is not empty
            for (CartService.CartItem item : items) {
                cartItemsContainer.getChildren().add(createCartItemNode(item));
            }
        }

        // Disable checkout button if cart is empty
        checkoutButton.setDisable(isEmpty);

        // Update totals based on cart contents
        updateTotals();
    }

    private void updateTotals() {
        double newSubtotal = cartService.getCartItems().values().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

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
        if (cartService.getCartItems().isEmpty()) {
            showAlert("Empty Cart", "Please add items before checkout.", Alert.AlertType.WARNING);
            return;
        }
        // Add checkout logic here
        showAlert("Checkout", "Processing checkout: " + formatPrice(total.get()), Alert.AlertType.INFORMATION);
    }

    private void handleError(String message, Exception e) {
        Platform.runLater(() -> showAlert("Error", message + ": " + e.getMessage(), Alert.AlertType.ERROR));
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
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