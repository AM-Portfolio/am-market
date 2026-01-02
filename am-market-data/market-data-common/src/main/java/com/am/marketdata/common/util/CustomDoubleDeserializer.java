package com.am.marketdata.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Custom deserializer for handling special string values that should be converted to Double.
 * Handles cases like "-" which should be treated as null.
 */
public class CustomDoubleDeserializer extends JsonDeserializer<Double> {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomDoubleDeserializer.class);
    
    @Override
    public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        
        if (value == null || value.isEmpty() || value.equals("-")) {
            return null;
        }
        
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse value '{}' as Double, returning null", value);
            return null;
        }
    }
}
