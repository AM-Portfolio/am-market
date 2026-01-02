package com.am.marketdata.common.model.tradeB.financials;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.am.marketdata.common.model.tradeB.financials.adapter.FinancialDataJsonAdapter;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

/**
 * Abstract base class for financial data with dynamic quarterly data structure
 */
@Data
public abstract class AbstractFinancialData<T> {
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonIgnore
    protected ObjectNode data = objectMapper.createObjectNode();

    @JsonAnyGetter
    public JsonNode getData() {
        return data;
    }

    @JsonAnySetter
    public void setData(String key, Object value) {
        if (value instanceof JsonNode) {
            data.set(key, (JsonNode) value);
        } else if (value instanceof String) {
            try {
                // Try to parse the string as JSON
                JsonNode jsonValue = objectMapper.readTree((String) value);
                data.set(key, jsonValue);
            } catch (Exception e) {
                // If it's not JSON, store it as a simple string
                data.put(key, value.toString());
            }
        } else {
            data.put(key, value.toString());
        }
    }

    /**
     * Get a specific quarter's data as a JsonNode
     * @param quarterKey The quarter key (e.g., "202412")
     * @return JsonNode containing the quarter's data
     */
    public JsonNode getData(String quarterKey) {
        return data.get(quarterKey);
    }

    /**
     * Get all quarter keys
     * @return Set of all quarter keys
     */
    public Set<String> getQuarterKeys() {
        Set<String> keys = new HashSet<>();
        Iterator<String> fieldNames = data.fieldNames();
        while (fieldNames.hasNext()) {
            keys.add(fieldNames.next());
        }
        return keys;
    }

    /**
     * Get a specific field value from a quarter
     * @param quarterKey The quarter key
     * @param fieldName The field name
     * @return Value of the field as String
     */
    public String getFieldValue(String quarterKey, String fieldName) {
        JsonNode quarterNode = data.get(quarterKey);
        return FinancialDataJsonAdapter.getFieldValue(quarterNode, fieldName);
    }

    /**
     * Convert a quarter's data into a metrics object
     * @param quarterKey The quarter key (e.g., "202412")
     * @return Metrics object containing the quarter's data
     */
    public abstract T getMetrics(String quarterKey);

    /**
     * Parse a string to Double, handling null and exceptions
     * @param value String value to parse
     * @return Double value or null if parsing fails
     */
    protected Double parseDouble(String value) {
        return FinancialDataJsonAdapter.parseDouble(value);
    }
}
