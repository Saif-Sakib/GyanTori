module com {
    // JavaFX requirements
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    // Java base requirements
    requires java.base;
    requires java.sql;

    // MongoDB requirements
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;

    // Logging
    requires org.slf4j;

    requires java.net.http;

    // Open packages for JavaFX FXML
    opens com.start to javafx.fxml, javafx.graphics;
    opens com.controllers to javafx.fxml, javafx.graphics;

    // Export your packages
    exports com.start;
    exports com.controllers;
    exports com.services;
}