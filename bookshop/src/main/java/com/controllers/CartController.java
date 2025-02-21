package com.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.util.HashMap;
import java.util.Map;
import java.text.DecimalFormat;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.input.KeyCode;
import com.services.CartService;

public class CartController {
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

    private final Map<String, CartItem> cartItems = new HashMap<>();
    private final SimpleDoubleProperty subtotal = new SimpleDoubleProperty(0);
    private final CartService cartService = CartService.getInstance();
    private final SimpleDoubleProperty onlineFee = new SimpleDoubleProperty(58);
    private final SimpleDoubleProperty deliveryFee = new SimpleDoubleProperty(60);
    private final DecimalFormat decimalFormat;
    private static final double PROMO_DISCOUNT = 0.10;
    private static final int MAX_QUANTITY = 99;
    private boolean isPromoApplied = false;

    public CartController() {
        decimalFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        decimalFormat.applyPattern("#,##0.00");
    }

    @FXML
    public void initialize() {
        try {
            validateFXMLInjection();
            setupEventHandlers();
            setupBindings();
            updateCartVisibility();
        } catch (Exception e) {
            handleException("Initialization Error", e);
        }
    }

    private void validateFXMLInjection() throws IllegalStateException {
        if (cartItemsContainer == null || emptyCartContainer == null ||
                subtotalLabel == null || onlineFeeLabel == null ||
                deliveryFeeLabel == null || totalLabel == null ||
                checkoutButton == null || promoCodeField == null ||
                applyPromoButton == null) {
            throw new IllegalStateException("FXML injection failed. Required fields are null.");
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
    private void loadHome(){
        Stage currentStage = (Stage) checkoutButton.getScene().getWindow();
        LoadPageController.loadScene("home.fxml", "home.css", currentStage);
    }

    public void addItemToCart(String itemId, String itemName, double price, String imageUrl) {
        try {
            validateItemInput(itemId, itemName, price);

            Platform.runLater(() -> {
                if (cartItems.containsKey(itemId)) {
                    CartItem existingItem = cartItems.get(itemId);
                    if (existingItem.getQuantity() < MAX_QUANTITY) {
                        existingItem.incrementQuantity();
                        updateSubtotal();
                    } else {
                        showAlert("Maximum Quantity",
                                "Cannot add more than " + MAX_QUANTITY + " items.",
                                Alert.AlertType.WARNING);
                    }
                } else {
                    CartItem newItem = new CartItem(itemId, itemName, price, imageUrl);
                    cartItems.put(itemId, newItem);
                    Node itemNode = createCartItemNode(newItem);
                    cartItemsContainer.getChildren().add(itemNode);
                    updateSubtotal();
                }

                updateCartVisibility();
            });

        } catch (Exception e) {
            handleException("Add Item Error", e);
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

    private void handleExistingItem(String itemId) throws IllegalStateException {
        CartItem existingItem = cartItems.get(itemId);
        if (existingItem.getQuantity() < MAX_QUANTITY) {
            existingItem.incrementQuantity();
        } else {
            throw new IllegalStateException("Maximum quantity limit reached");
        }
    }

    private void createNewCartItem(String itemId, String itemName, double price, String imageUrl) {
        try {
            CartItem newItem = new CartItem(itemId, itemName, price, imageUrl);
            cartItems.put(itemId, newItem);
            cartItemsContainer.getChildren().add(createCartItemNode(newItem));
        } catch (Exception e) {
            handleException("createNewCartItem Error", e);
        }
    }

    private Node createCartItemNode(CartController.CartItem item) {
        try {
            // Create main container
            HBox itemContainer = new HBox();
            itemContainer.setId("item_" + item.getId());
            itemContainer.setSpacing(10);
            itemContainer.setPadding(new Insets(10));
            itemContainer.setAlignment(Pos.CENTER_LEFT);
            itemContainer.setPrefWidth(Region.USE_COMPUTED_SIZE);
            itemContainer.getStyleClass().add("cart-item");

            // Create details container
            VBox detailsBox = new VBox(5);
            detailsBox.setAlignment(Pos.CENTER_LEFT);
            
            // Item name
            Label nameLabel = new Label(item.getName());
            nameLabel.getStyleClass().add("item-name");
            
            // Price with formatting
            Label priceLabel = new Label(formatPrice(item.getPrice()));
            priceLabel.getStyleClass().add("item-price");
            
            detailsBox.getChildren().addAll(nameLabel, priceLabel);
            
            // Create quantity spinner
            Spinner<Integer> quantitySpinner = new Spinner<>();
            SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, MAX_QUANTITY, item.getQuantity());
            quantitySpinner.setValueFactory(valueFactory);
            quantitySpinner.setEditable(true);
            quantitySpinner.setPrefWidth(80);
            
            // Add quantity change listener
            quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    if (newVal >= 1 && newVal <= MAX_QUANTITY) {
                        item.setQuantity(newVal);
                        updateSubtotal();
                    } else {
                        // Reset to valid value
                        Platform.runLater(() -> quantitySpinner.getValueFactory().setValue(oldVal));
                    }
                }
            });

            // Create remove button
            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().add("remove-button");
            removeButton.setOnAction(e -> removeItemFromCart(item.getId()));

            // Add spacing between elements
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Add all elements to the container
            itemContainer.getChildren().addAll(
                detailsBox,
                spacer,
                quantitySpinner,
                removeButton
            );

            return itemContainer;

        } catch (Exception e) {
            handleException("Create Item Node Error", e);
            return new HBox(); // Return empty container in case of error
        }
    }

    private VBox createDetailsBox(CartItem item) {
        VBox detailsBox = new VBox(5);
        Label nameLabel = new Label(item.getName());
        Label priceLabel = new Label(formatPrice(item.getPrice()));
        detailsBox.getChildren().addAll(nameLabel, priceLabel);
        return detailsBox;
    }

    private Spinner<Integer> createQuantitySpinner(CartItem item) {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, MAX_QUANTITY,
                item.getQuantity());
        Spinner<Integer> spinner = new Spinner<>(valueFactory);
        spinner.setEditable(true);

        spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (newVal != null && newVal >= 1 && newVal <= MAX_QUANTITY) {
                    item.setQuantity(newVal);
                    updateSubtotal();
                } else {
                    spinner.getValueFactory().setValue(oldVal);
                }
            } catch (Exception e) {
                handleException("Quantity Update Error", e);
                spinner.getValueFactory().setValue(oldVal);
            }
        });

        return spinner;
    }

    private Button createRemoveButton(CartItem item) {
        Button removeButton = new Button("Remove");
        removeButton.setOnAction(e -> removeItemFromCart(item.getId()));
        return removeButton;
    }

    private void removeItemFromCart(String itemId) {
        try {
            cartService.removeItem(itemId);
            //loadCartItems();
            updateCartVisibility();
        } catch (Exception e) {
            handleException("Remove Item Error", e);
        }
    }

    private void updateSubtotal() {
        try {
            double newSubtotal = cartItems.values().stream()
                    .mapToDouble(item -> item.getPrice() * item.getQuantity())
                    .sum();

            if (isPromoApplied) {
                newSubtotal *= (1 - PROMO_DISCOUNT);
            }

            subtotal.set(Math.max(0, newSubtotal));
        } catch (Exception e) {
            handleException("Update Subtotal Error", e);
        }
    }

    private void setupBindings() {
        subtotalLabel.textProperty().bind(subtotal.asString("%.2f TK"));
        onlineFeeLabel.textProperty().bind(onlineFee.asString("%.2f TK"));
        deliveryFeeLabel.textProperty().bind(deliveryFee.asString("%.2f TK"));
        totalLabel.textProperty().bind(
                subtotal.add(onlineFee).add(deliveryFee).asString("%.2f TK"));
    }

    private void updateCartVisibility() {
        Platform.runLater(() -> {
            try {
                boolean isEmpty = cartItems.isEmpty();

                // Update empty cart container visibility
                emptyCartContainer.setVisible(isEmpty);
                emptyCartContainer.setManaged(isEmpty);

                // Update cart items container visibility
                cartItemsContainer.setVisible(!isEmpty);
                cartItemsContainer.setManaged(!isEmpty);

                // Update buttons state
                if (checkoutButton != null) {
                    checkoutButton.setDisable(isEmpty);
                }

                // Ensure proper layout
                if (cartItemsContainer.getParent() != null) {
                    cartItemsContainer.getParent().layout();
                }

            } catch (Exception e) {
                handleException("Update Cart Visibility Error", e);
            }
        });
    }

    private void handlePromoCode() {
        try {
            String promoCode = promoCodeField.getText().trim();
            if (!isPromoApplied && isValidPromoCode(promoCode)) {
                isPromoApplied = true;
                updateSubtotal();
                showAlert("Success", "10% discount applied!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Invalid Code", "Please enter a valid promo code.",
                        Alert.AlertType.WARNING);
            }
        } catch (Exception e) {
            handleException("Promo Code Error", e);
        }
    }

    private boolean isValidPromoCode(String code) {
        return code != null && code.equalsIgnoreCase("SAVE10");
    }

    private void handleCheckout() {
        try {
            if (cartItems.isEmpty()) {
                showAlert("Empty Cart", "Please add items before checkout.",
                        Alert.AlertType.WARNING);
                return;
            }

            double total = subtotal.get() + onlineFee.get() + deliveryFee.get();
            showAlert("Checkout", "Processing checkout: " + formatPrice(total),
                    Alert.AlertType.INFORMATION);
            // Add actual checkout logic here

        } catch (Exception e) {
            handleException("Checkout Error", e);
        }
    }

    private void handleException(String context, Exception e) {
        Platform.runLater(() -> {
            String errorMessage = String.format("%s: %s", context, e.getMessage());
            showAlert("Error", errorMessage, Alert.AlertType.ERROR);
        });
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

    private static class CartItem {
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