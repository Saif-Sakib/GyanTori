package com.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.models.Book;

public class BookPreviewController implements Initializable {
    @FXML
    private VBox previewRoot;
    @FXML
    private ImageView bookCoverPreview;
    @FXML
    private Label titleLabel;
    @FXML
    private Label authorLabel;
    @FXML
    private Label publisherLabel;
    @FXML
    private Label pagesLabel;
    @FXML
    private Label isbnLabel;
    @FXML
    private Label languageLabel;
    @FXML
    private Label publicationDateLabel;
    @FXML
    private Label originalPriceLabel;
    @FXML
    private Label currentPriceLabel;
    @FXML
    private Label discountLabel;
    @FXML
    private Label bookHubLabel;
    @FXML
    private FlowPane categoriesContainer;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Button closeButton;

    private Book book;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        closeButton.setOnAction(event -> {
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        });
    }

    public void setBook(Book book) {
        this.book = book;
        displayBookDetails();
    }

    private void displayBookDetails() {
        if (book == null)
            return;

        // Set book metadata
        titleLabel.setText(book.getTitle());
        authorLabel.setText(book.getAuthor());

        if (book.getPublisher() != null && !book.getPublisher().isEmpty()) {
            publisherLabel.setText(book.getPublisher());
        } else {
            publisherLabel.setText("N/A");
        }

        if (book.getPages() > 0) {
            pagesLabel.setText(String.valueOf(book.getPages()));
        } else {
            pagesLabel.setText("N/A");
        }

        if (book.getIsbn() != null && !book.getIsbn().isEmpty()) {
            isbnLabel.setText(book.getIsbn());
        } else {
            isbnLabel.setText("N/A");
        }

        languageLabel.setText(book.getLanguage());

        if (book.getPublicationDate() != null && !book.getPublicationDate().isEmpty()) {
            publicationDateLabel.setText(book.getPublicationDate());
        } else {
            publicationDateLabel.setText("N/A");
        }

        // Set price information
        originalPriceLabel.setText(String.format("৳%.2f", book.getOriginalPrice()));
        currentPriceLabel.setText(String.format("৳%.2f", book.getCurrentPrice()));

        if (book.getDiscount() > 0) {
            discountLabel.setText(String.format("%.0f%%", book.getDiscount()));
        } else {
            discountLabel.setText("No Discount");
        }

        // Set BookHub
        bookHubLabel.setText(book.getBookHubId());

        // Set categories
        categoriesContainer.getChildren().clear();
        if (book.getCategories() != null) {
            for (String category : book.getCategories()) {
                Label categoryLabel = new Label(category);
                categoryLabel.getStyleClass().add("category-chip");
                categoriesContainer.getChildren().add(categoryLabel);
            }
        }

        // Set description
        descriptionLabel.setText(book.getDescription());

        // Set image if available
        if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
            try {
                Image image = new Image(book.getImageUrl());
                bookCoverPreview.setImage(image);
            } catch (Exception e) {
                // Use placeholder image if there's an error
                bookCoverPreview
                        .setImage(new Image(getClass().getResourceAsStream("/com/images/books/placeholder-book.png").toString()));
            }
        } else {
            // Use placeholder image if no image URL
            bookCoverPreview
                    .setImage(new Image(getClass().getResourceAsStream("/com/images/books/placeholder-book.png").toString()));
        }
    }
}