package com.am.marketdata.common.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for number formatting and parsing operations
 */
public class NumberFormatter {
    
    private static final NumberFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final NumberFormat PERCENT_FORMAT = new DecimalFormat("#,##0.00'%'");
    
    /**
     * Parse a string to Double, handling null and exceptions
     * @param value String value to parse
     * @return Double value or null if parsing fails
     */
    public static Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Format a Double value as a decimal string with 2 decimal places
     * @param value Double value to format
     * @return Formatted string or empty string if value is null
     */
    public static String formatDecimal(Double value) {
        if (value == null) {
            return "";
        }
        return DECIMAL_FORMAT.format(value);
    }
    
    /**
     * Format a Double value as a percentage string with 2 decimal places
     * @param value Double value to format (0.01 = 1.00%)
     * @return Formatted percentage string or empty string if value is null
     */
    public static String formatPercent(Double value) {
        if (value == null) {
            return "";
        }
        return PERCENT_FORMAT.format(value);
    }
    
    /**
     * Format a Double value as a currency string with the specified locale
     * @param value Double value to format
     * @param locale Locale to use for currency formatting
     * @return Formatted currency string or empty string if value is null
     */
    public static String formatCurrency(Double value, Locale locale) {
        if (value == null) {
            return "";
        }
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);
        return currencyFormat.format(value);
    }
    
    /**
     * Format a Double value as an Indian Rupee currency string
     * @param value Double value to format
     * @return Formatted INR string or empty string if value is null
     */
    public static String formatIndianRupee(Double value) {
        if (value == null) {
            return "";
        }
        return "₹" + formatDecimal(value);
    }
    
    private NumberFormatter() {
        // Private constructor to prevent instantiation
    }
}
