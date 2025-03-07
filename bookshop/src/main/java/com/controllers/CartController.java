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
    private Label totalLabel;
    @FXML
    private Button checkoutButton;
    @FXML
    private TextField promoCodeField;
    @FXML
    private Button applyPromoButton;

    private final CartService cartService = CartService.getInstance();
    private final SimpleDoubleProperty subtotal = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty total = new SimpleDoubleProperty(0);
    private final DecimalFormat decimalFormat;
    private static final double PROMO_DISCOUNT = 0.10;
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
            loadCartItems();
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
        try {
            Stage stage = (Stage) checkoutButton.getScene().getWindow();
            LoadPageController.loadScene("home.fxml", "home.css", stage);
        } catch (Exception e) {
            handleError("Failed to return to home page", e);
        }
    }

    private void setupBindings() {
        totalLabel.textProperty().bind(total.asString("%.2f TK"));
        updateTotals();
    }

    private void loadCartItems() {
        cartItemsContainer.getChildren().clear();
        cartService.getCartItems()
                .forEach((id, item) -> cartItemsContainer.getChildren().add(createCartItemNode(item)));
        updateCartVisibility();
        updateTotals();
    }

    private Node createCartItemNode(CartService.CartItem item) {
        HBox container = new HBox(10);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(10));
        container.getStyleClass().add("cart-item");

        Label nameLabel = new Label(item.getName());
        Label priceLabel = new Label(formatPrice(item.getPrice()));

        Spinner<Integer> quantitySpinner = createQuantitySpinner(item);
        Button removeButton = new Button("Remove");
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
            try {
                cartService.updateItemQuantity(item.getId(), newVal);
                updateTotals();
            } catch (Exception e) {
                handleError("Failed to update quantity", e);
                spinner.getValueFactory().setValue(oldVal);
            }
        });
        return spinner;
    }

    private void removeItem(String itemId) {
        try {
            cartService.removeItem(itemId);
            loadCartItems();
        } catch (Exception e) {
            handleError("Failed to remove item", e);
        }
    }

    private void updateTotals() {
        double newSubtotal = cartService.getCartItems().values().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        if (isPromoApplied) {
            newSubtotal *= (1 - PROMO_DISCOUNT);
        }

        subtotal.set(newSubtotal);
        total.set(newSubtotal);
    }

    private void updateCartVisibility() {
        boolean isEmpty = cartService.getCartItems().isEmpty();
        emptyCartContainer.setVisible(isEmpty);
        cartItemsContainer.setVisible(!isEmpty);
        checkoutButton.setDisable(isEmpty);
    }

    private void handlePromoCode() {
        String code = promoCodeField.getText().trim();
        if (!isPromoApplied && "SAVE10".equalsIgnoreCase(code)) {
            isPromoApplied = true;
            updateTotals();
            showAlert("Success", "10% discount applied!", Alert.AlertType.INFORMATION);
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
    private void loadHome() {
        try {
            Stage stage = (Stage) checkoutButton.getScene().getWindow();
            LoadPageController.loadScene("home.fxml", "home.css", stage);
        } catch (Exception e) {
            handleError("Failed to load home page", e);
        }
    }
}