package com.am.marketdata.common.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SafeDoubleDeserializer extends JsonDeserializer<Double> {
    private static final Logger log = LoggerFactory.getLogger(SafeDoubleDeserializer.class);

    @Override
    public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse double value: '{}', defaulting to 0.0", value);
            return 0.0;
        }
    }
}
