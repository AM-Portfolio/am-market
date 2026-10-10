package com.am.marketdata.common.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared utility for determining expected and minimum constituent counts for stock indices.
 * Prevents accepting truncated rosters or evaluating incomplete index rankings.
 */
public final class IndexMembershipUtils {

    private IndexMembershipUtils() {}

    private static final Pattern SIZE_PATTERN = Pattern.compile("\\b(500|250|200|150|100|50)\\b");

    /**
     * Determines the minimum required constituent count for a given index name
     * to prevent accepting truncated scrapes or incomplete constituent rosters.
     *
     * @param indexName The index name (e.g., "NIFTY 50", "NIFTY MIDCAP 150")
     * @return Minimum required member count
     */
    public static int minimumMembershipSize(String indexName) {
        if (indexName == null || indexName.isBlank()) {
            return 1;
        }
        String normalized = indexName.trim().toUpperCase(Locale.ROOT);

        // Check for broad-market size tokens with word boundaries in descending order
        Matcher matcher = SIZE_PATTERN.matcher(normalized);
        if (matcher.find()) {
            String token = matcher.group(1);
            switch (token) {
                case "500": return 490;
                case "250": return 240;
                case "200": return 190;
                case "150": return 140;
                case "100": return 95;
                case "50":  return 45;
                default: break;
            }
        }

        // Sectoral and thematic indices defaults
        if (normalized.contains("COMMODITIES") || normalized.contains("INFRA") || normalized.contains("SERVICES")) {
            return 25;
        }
        if (normalized.contains("FIN") || normalized.contains("PHARMA") || normalized.contains("HEALTHCARE") || normalized.contains("PSE")) {
            return 15;
        }
        if (normalized.contains("AUTO") || normalized.contains("FMCG") || normalized.contains("METAL") || normalized.contains("OIL")) {
            return 12;
        }
        if (normalized.contains("BANK") || normalized.contains("IT") || normalized.contains("MEDIA")
                || normalized.contains("REALTY") || normalized.contains("ENERGY") || normalized.contains("CPSE")
                || normalized.contains("DURABLE")) {
            return 8;
        }

        return 1;
    }
}
