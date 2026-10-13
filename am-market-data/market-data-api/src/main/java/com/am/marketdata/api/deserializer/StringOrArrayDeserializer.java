package com.am.marketdata.api.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson deserializer that handles both String values ("A,B") 
 * and JSON Array values (["A", "B"]), converting them into a single comma-separated String.
 */
public class StringOrArrayDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.START_ARRAY) {
            List<String> list = new ArrayList<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                String text = p.getText();
                if (text != null && !text.trim().isEmpty()) {
                    list.add(text.trim());
                }
            }
            return String.join(",", list);
        } else if (p.currentToken() == JsonToken.VALUE_STRING) {
            return p.getText();
        }
        return p.getValueAsString();
    }
}
