package com.am.marketdata.api.model;

import com.am.marketdata.common.model.OHLCQuote;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/** Named response contract for GET and POST market quote requests. */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Quote snapshots for requested instruments; optional provider fields are omitted when unavailable.")
public class QuotesResponse {
    @Schema(description = "Quotes keyed by the requested symbol.")
    Map<String, OHLCQuote> quotes;

    String source;
    Integer count;
    Boolean cached;
    Long timestamp;
    String timeFrame;
    List<String> unavailableSymbols;
    String error;
    String message;

    @SuppressWarnings("unchecked")
    public static QuotesResponse fromResult(Map<String, Object> result) {
        Object errorValue = result.get("ERROR");
        if (errorValue instanceof Map<?, ?> details) {
            Object error = details.get("error");
            Object message = details.get("message");
            return error(error != null ? String.valueOf(error) : "MARKET_DATA_ERROR",
                    message != null ? String.valueOf(message) : "Quote request failed");
        }

        Object quotesValue = result.get("quotes");
        Map<String, OHLCQuote> quotes = quotesValue instanceof Map<?, ?>
                ? (Map<String, OHLCQuote>) quotesValue : null;
        Object unavailableValue = result.get("unavailableSymbols");
        List<String> unavailable = unavailableValue instanceof List<?> values
                ? values.stream().filter(String.class::isInstance).map(String.class::cast).toList() : null;

        return QuotesResponse.builder()
                .quotes(quotes)
                .source(asString(result.get("source")))
                .count(asInteger(result.get("count")))
                .cached(result.get("cached") instanceof Boolean value ? value : null)
                .timestamp(asLong(result.get("timestamp")))
                .timeFrame(asString(result.get("timeFrame")))
                .unavailableSymbols(unavailable)
                .build();
    }

    public static QuotesResponse error(String error, String message) {
        return QuotesResponse.builder().error(error).message(message).build();
    }

    private static String asString(Object value) {
        return value instanceof String text ? text : null;
    }

    private static Integer asInteger(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private static Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }
}
