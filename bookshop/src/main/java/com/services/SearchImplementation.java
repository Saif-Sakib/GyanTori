package com.services;

import java.util.List;

import com.database.BookDetailsCollection;
import com.models.Book;
import com.controllers.HomeController;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Handles search functionality for the application
 */
public class SearchImplementation {

    private final BookDetailsCollection searchBooks;
    private final HomeController homeController;
    private HBox searchResultsContainer;

    public SearchImplementation(HomeController homeController, HBox searchResultsContainer) {
        this.searchBooks = new BookDetailsCollection();
        this.homeController = homeController;
        this.searchResultsContainer = searchResultsContainer;
    }

    /**
     * Perform search with the provided query
     * 
     * @param query The search term
     * @return True if search was successful, false otherwise
     */
    public boolean performSearch(String query) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return false;
            }

            System.out.println("Searching for: " + query.trim());
            List<Book> results = searchBooks.searchBooks(query.trim());

            if (results.isEmpty()) {
                System.out.println("No books found matching: " + query);
                // Clear previous results and show no results message
                Platform.runLater(() -> {
                    clearResults();
                    showNoResultsMessage(query);
                });
                return false;
            } else {
                System.out.println("Found " + results.size() + " books matching: " + query);
                // Convert documents to Book objects
                List<Book> bookResults = results;

                // Display the results in the UI
                Platform.runLater(() -> {
                    displaySearchResults(bookResults);
                });
                return true;
            }
        } catch (Exception e) {
            System.err.println("Error performing search: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Convert MongoDB documents to Book objects
     * 
     * @param documents List of document results from database
     * @return List of Book objects
     */

    /**
     * Display search results in the UI
     * 
     * @param books List of books to display
     */
    private void displaySearchResults(List<Book> books) {
        // Clear previous results
        clearResults();

        // Add header label
        Label headerLabel = new Label("Search Results");
        headerLabel.getStyleClass().add("section-header");

        VBox headerBox = new VBox(headerLabel);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPrefWidth(searchResultsContainer.getWidth());

        searchResultsContainer.getChildren().add(headerBox);

        // Create HBox for actual results
        HBox resultsBox = new HBox(20);
        resultsBox.getStyleClass().add("book-container");

        // Add book cards to the results container
        for (Book book : books) {
            resultsBox.getChildren().add(homeController.createBookCard(book));
        }

        searchResultsContainer.getChildren().add(resultsBox);

        // Make the search results container visible
        searchResultsContainer.setVisible(true);
        searchResultsContainer.setManaged(true);
    }

    /**
     * Clear search results from the UI
     */
    public void clearResults() {
        if (searchResultsContainer != null) {
            searchResultsContainer.getChildren().clear();
            searchResultsContainer.setVisible(false);
            searchResultsContainer.setManaged(false);
        }
    }

    /**
     * Show message when no results are found
     * 
     * @param searchTerm The search term used
     */
    private void showNoResultsMessage(String searchTerm) {
        // Clear previous results
        clearResults();

        // Create no results message
        Label noResultsLabel = new Label("No books found matching: " + searchTerm);
        noResultsLabel.getStyleClass().add("no-results-message");

        VBox messageBox = new VBox(noResultsLabel);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setPrefWidth(searchResultsContainer.getWidth());
        messageBox.setPrefHeight(100);

        searchResultsContainer.getChildren().add(messageBox);
        searchResultsContainer.setVisible(true);
        searchResultsContainer.setManaged(true);
    }
}