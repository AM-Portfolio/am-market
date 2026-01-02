package com.am.marketdata.api.dto;

import com.am.marketdata.common.model.TimeFrame;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Custom JSON deserializer for TimeFrame that accepts both:
 * - Enum names: "DAY", "HOUR", "MINUTE", etc.
 * - API values: "1D", "1H", "5m", etc.
 */
public class TimeFrameDeserializer extends JsonDeserializer<TimeFrame> {

    @Override
    public TimeFrame deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            return TimeFrame.MINUTE; // Default value
        }

        try {
            return TimeFrame.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid TimeFrame value: " + value +
                    ". Expected enum name (DAY, HOUR, MINUTE) or API value (1D, 1H, 5m)", e);
        }
    }
}
