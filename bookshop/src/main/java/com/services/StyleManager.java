package com.services;

import java.util.ArrayList;
import java.util.List;

public class StyleManager {
    // List of all application stylesheets
    private static final List<String> STYLESHEETS = new ArrayList<>();

    static {
        // Add all your stylesheets here
        STYLESHEETS.add("/styles/login_signup.css");
        STYLESHEETS.add("/styles/common.css");
        // Add more stylesheets as needed
    }

    public static List<String> getStylesheets() {
        return STYLESHEETS;
    }

    public static String getStylesheet(String name) {
        return StyleManager.class.getResource("/com/styles/" + name).toExternalForm();
    }
}