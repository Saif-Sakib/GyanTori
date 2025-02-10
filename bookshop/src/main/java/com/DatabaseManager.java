package com;

import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/gyantori";
    private static final String USER = "boss";
    private static final String PASSWORD = "mayerdoa";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Driver not found", e);
        }
    }

    public static boolean registerUser(String fullName, String email, String username, String password) {
        String query = "INSERT INTO users (full_name, email, username, password_hash, salt) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            byte[] salt = generateSalt();
            String passwordHash = hashPassword(password, salt);

            stmt.setString(1, fullName);
            stmt.setString(2, email);
            stmt.setString(3, username);
            stmt.setString(4, passwordHash);
            stmt.setString(5, Base64.getEncoder().encodeToString(salt));

            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean validateLogin(String username, String password) {
        String query = "SELECT password_hash, salt FROM users WHERE username = ?";

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                byte[] salt = Base64.getDecoder().decode(rs.getString("salt"));
                String hashAttempt = hashPassword(password, salt);
                return storedHash.equals(hashAttempt);
            }
            return false;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return salt;
    }

    public static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}