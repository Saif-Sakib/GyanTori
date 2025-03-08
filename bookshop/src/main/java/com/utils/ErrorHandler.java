package com.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Utility class for handling and displaying errors
 */
public class ErrorHandler {
    private static final Logger LOGGER = Logger.getLogger(ErrorHandler.class.getName());

    /**
     * Logs an error and displays an alert dialog
     * 
     * @param context The class where the error occurred
     * @param title   Alert dialog title
     * @param header  Alert header text
     * @param content Alert content text
     * @param ex      The exception (optional)
     */
    public static void handleError(Class<?> context, String title, String header, String content, Exception ex) {
        // Log the error
        if (ex != null) {
            LOGGER.log(Level.SEVERE, content, ex);
        } else {
            LOGGER.log(Level.SEVERE, content);
        }

        // Create and configure alert dialog
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // If there's an exception, add expandable content with stack trace
        if (ex != null) {
            // Create expandable Exception section
            String exceptionText = getStackTraceAsString(ex);

            Label label = new Label("Exception stacktrace:");

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);

            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);

            alert.getDialogPane().setExpandableContent(expContent);
        }

        // Show the alert dialog
        alert.showAndWait();
    }

    /**
     * Logs and shows a simple error message
     */
    public static void showError(Class<?> context, String message) {
        handleError(context, "Error", "An error occurred", message, null);
    }

    /**
     * Logs and shows an error message with exception details
     */
    public static void showException(Class<?> context, String message, Exception ex) {
        handleError(context, "Error", "An error occurred", message, ex);
    }

    /**
     * Converts exception stack trace to a string
     */
    private static String getStackTraceAsString(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.toString()).append("\n");

        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }

        return sb.toString();
    }
}