package com.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    // Pattern for valid email format
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    
    // Pattern for price validation (positive number with optional decimal places)
    private static final Pattern PRICE_PATTERN = 
        Pattern.compile("^\\d+(\\.\\d{1,2})?$");
    
    // Pattern for discount validation (number between 0 and 100 with optional decimal places)
    private static final Pattern DISCOUNT_PATTERN = 
        Pattern.compile("^(100(\\.0{1,2})?|\\d{1,2}(\\.\\d{1,2})?)$");
    
    // Pattern for ISBN validation
    private static final Pattern ISBN_PATTERN = 
        Pattern.compile("^(?:ISBN(?:-10)?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$");
    
    /**
     * Validates if the provided string is a valid email format.
     * 
     * @param email the email string to validate
     * @return true if the email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validates if the provided string is a valid price format.
     * Valid prices are positive numbers with optional decimal places (up to 2).
     * 
     * @param price the price string to validate
     * @return true if the price is valid, false otherwise
     */
    public static boolean isValidPrice(String price) {
        if (price == null || price.trim().isEmpty()) {
            return false;
        }
        return PRICE_PATTERN.matcher(price).matches();
    }
    
    /**
     * Validates if the provided string is a valid discount percentage.
     * Valid discounts are numbers between 0 and 100 with optional decimal places.
     * 
     * @param discount the discount string to validate
     * @return true if the discount is valid, false otherwise
     */
    public static boolean isValidDiscount(String discount) {
        if (discount == null || discount.trim().isEmpty()) {
            return false;
        }
        return DISCOUNT_PATTERN.matcher(discount).matches();
    }
    
    /**
     * Validates if the provided string is a valid ISBN format.*/
}
