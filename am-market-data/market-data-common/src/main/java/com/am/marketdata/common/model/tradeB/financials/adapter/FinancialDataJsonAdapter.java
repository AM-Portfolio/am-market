package com.am.marketdata.common.model.tradeB.financials.adapter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter class for handling JSON data extraction from financial statements
 */
public class FinancialDataJsonAdapter {
    /**
     * Get a specific field value from a JsonNode
     * @param node The JsonNode containing the data
     * @param fieldName The field name to extract
     * @return The field value as String, or null if not found
     */
    public static String getFieldValue(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }

        if (node.isObject() && node.has(fieldName)) {
            JsonNode fieldValue = node.get(fieldName);
            if (fieldValue.isTextual()) {
                return fieldValue.asText();
            } else if (fieldValue.isNumber()) {
                return fieldValue.asText();
            } else {
                return null;
            }
        } else if (node.isTextual()) {
            return getFieldValueFromText(node.asText(), fieldName);
        }
        return null;
    }

    /**
     * Parse a field value from a textual JSON representation
     * @param text The textual JSON representation
     * @param fieldName The field name to extract
     * @return The field value as String, or null if not found
     */
    private static String getFieldValueFromText(String text, String fieldName) {
        // Remove the outer quotes and braces if present
        text = text.replaceAll("^\"|\"$", "");
        text = text.replaceAll("^\\{|\\}$", "");
        
        // Split by comma and equals sign
        String[] pairs = text.split(", ");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2 && keyValue[0].equals(fieldName)) {
                return keyValue[1];
            }
        }
        return null;
    }

    /**
     * Parse a string to Double, handling null and exceptions
     * @param value String value to parse
     * @return Double value or null if parsing fails
     */
    public static Double parseDouble(String value) {
        if (value == null || value.isEmpty() || value.equals("null")) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse quarter data from JsonNode into a Map if it's stored as a text string
     * @param quarterNode The JsonNode containing quarter data
     * @return Map containing parsed key-value pairs or empty map if parsing fails
     */
    public static Map<String, String> parseQuarterDataToMap(JsonNode quarterNode) {
        Map<String, String> quarterData = new HashMap<>();
        if (quarterNode == null) {
            return quarterData;
        }
        
        if (quarterNode.isTextual()) {
            String quarterText = quarterNode.asText();
            // Remove the outer quotes and braces if present
            quarterText = quarterText.replaceAll("^\"|\"$", "");
            quarterText = quarterText.replaceAll("^\\{|\\}$", "");
            
            // Split by comma and equals sign
            String[] pairs = quarterText.split(", ");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    quarterData.put(keyValue[0], keyValue[1]);
                }
            }
        }
        
        return quarterData;
    }
}
