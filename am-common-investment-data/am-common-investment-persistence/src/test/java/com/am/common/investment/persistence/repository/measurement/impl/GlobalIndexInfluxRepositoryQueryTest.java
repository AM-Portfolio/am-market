package com.am.common.investment.persistence.repository.measurement.impl;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalIndexInfluxRepositoryQueryTest {

    @Test
    void globalHistoryUsesOhlcAggregationsAndEscapesInstrumentIdentity() {
        String query = GlobalIndexInfluxRepository.buildOhlcAggregationQuery(
                "market-data", "global_market_index", "GLOBAL_INDEX|^DJI\" ",
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-25T00:00:00Z"),
                "1w", "-14h30m");

        assertTrue(query.contains("fn: first"));
        assertTrue(query.contains("fn: max"));
        assertTrue(query.contains("fn: min"));
        assertTrue(query.contains("fn: last"));
        assertTrue(query.contains("GLOBAL_INDEX|^DJI\\\" "));
        assertFalse(query.contains("fn: mean"));
    }
}
