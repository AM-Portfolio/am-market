package com.am.marketdata.provider.common;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility for dynamically mapping a broker's JSON response node into a Java Map.
 * Trigger deployment pipeline.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li><b>Snake → CamelCase</b>: converts keys like {@code bid_price} → {@code bidPrice}
 *       so the API contract is always camelCase regardless of the broker's naming.</li>
 *   <li><b>Alias resolution</b>: translates broker-specific key names to contract-standard
 *       names (e.g. Upstox {@code underlying_spot_price} → {@code spotPrice}).</li>
 *   <li><b>Core-type enforcement</b>: guarantees critical numeric fields are always stored
 *       as Double or Long, even if the broker sends them as Strings.</li>
 *   <li><b>Exclusion</b>: drops broker-internal / garbage fields that must not reach
 *       consumers.</li>
 * </ol>
 *
 * <p>This class is intentionally stateless and has no Spring dependency so it can be
 * used in any layer without circular-dependency risk.
 */
public final class DynamicJsonMapper {

    private DynamicJsonMapper() { /* utility class */ }

    /**
     * Extracts every leaf value from {@code node} into {@code target}.
     *
     * @param node      the Jackson JsonNode to extract (must be an OBJECT node)
     * @param target    the Map to populate (values are appended, not replaced)
     * @param aliases   broker-key → contract-key overrides (null-safe, may be empty)
     * @param excludes  set of raw broker keys to completely ignore (null-safe, may be empty)
     * @param coreTypes contract keys that must be stored as {@code Double}; any other
     *                  numeric key that maps to a long-like value is stored as {@code Long}
     */
    public static void extractToMap(
            JsonNode node,
            Map<String, Object> target,
            Map<String, String> aliases,
            Set<String> excludes,
            Set<String> coreDoubleTypes) {

        if (node == null || !node.isObject()) {
            return;
        }

        node.fields().forEachRemaining(entry -> {
            String rawKey = entry.getKey();
            JsonNode value = entry.getValue();

            // 1. Skip excluded (garbage) fields
            if (excludes != null && excludes.contains(rawKey)) {
                return;
            }

            // 2. Resolve the output key: alias takes priority, else camelCase
            String outputKey = (aliases != null && aliases.containsKey(rawKey))
                    ? aliases.get(rawKey)
                    : toCamelCase(rawKey);

            // 3. Recurse into nested objects (e.g. ohlc sub-objects)
            if (value.isObject()) {
                Map<String, Object> nested = new HashMap<>();
                extractToMap(value, nested, aliases, excludes, coreDoubleTypes);
                target.put(outputKey, nested);
                return;
            }

            // 4. Skip null / missing JSON nodes — do NOT silently default to 0.0
            if (value.isNull() || value.isMissingNode()) {
                return;
            }

            // 5. Map scalar values with proper Java types
            Object javaValue = toJavaValue(value, outputKey, coreDoubleTypes);
            target.put(outputKey, javaValue);
        });
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a JSON scalar node to the appropriate Java type.
     * Critical numeric fields (ltp, oi, …) are always coerced to Double.
     */
    private static Object toJavaValue(JsonNode value, String outputKey, Set<String> coreDoubleTypes) {
        if (value.isBoolean()) {
            return value.asBoolean();
        }

        if (value.isTextual()) {
            // Try numeric coercion for text nodes that carry numbers
            String text = value.asText();
            if (coreDoubleTypes != null && coreDoubleTypes.contains(outputKey)) {
                try { return Double.parseDouble(text); } catch (NumberFormatException ignore) {}
            }
            return text;
        }

        // Integral numbers
        if (value.isIntegralNumber()) {
            // If output key is in the double-types set, store as Double for type safety
            if (coreDoubleTypes != null && coreDoubleTypes.contains(outputKey)) {
                return value.asDouble();
            }
            return value.asLong();
        }

        // Floating-point numbers
        if (value.isFloatingPointNumber()) {
            return value.asDouble();
        }

        // Arrays or anything else — return as-is via toString
        return value.asText();
    }

    /**
     * Converts a {@code snake_case} string to {@code camelCase}.
     * Already-camelCase keys are returned unchanged.
     */
    static String toCamelCase(String snakeKey) {
        if (snakeKey == null || !snakeKey.contains("_")) {
            return snakeKey;
        }
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : snakeKey.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }
}
