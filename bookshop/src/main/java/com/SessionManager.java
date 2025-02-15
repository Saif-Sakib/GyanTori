package com;

import java.io.*;
import java.util.Properties;

public class SessionManager {
    private static final String FILE_PATH = "session.properties";
    private static SessionManager instance;
    private String userName;

    private SessionManager() {
        loadSession();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setUser(String name) {
        this.userName = name;
        saveSession();
    }

    public String getUserName() {
        return userName;
    }

    public void clearSession() {
        userName = null;
        deleteSessionFile();
    }

    private void saveSession() {
        try (FileOutputStream fileOut = new FileOutputStream(FILE_PATH)) {
            Properties properties = new Properties();
            properties.setProperty("userName", userName);
            properties.store(fileOut, "User Session");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSession() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (FileInputStream fileIn = new FileInputStream(file)) {
                Properties properties = new Properties();
                properties.load(fileIn);
                userName = properties.getProperty("userName");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteSessionFile() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            file.delete();
        }
    }
}
