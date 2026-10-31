package com.am.marketdata.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeFrame Comprehensive Unit Tests")
class TimeFrameTest {

    @Nested
    @DisplayName("Exact API Value Resolution")
    class ExactApiValueTests {

        @Test
        @DisplayName("Every enum constant resolves from its exact api value")
        void allEnumConstantsResolveFromExactApiValue() {
            for (TimeFrame tf : TimeFrame.values()) {
                TimeFrame resolved = TimeFrame.fromApiValue(tf.getApiValue());
                assertEquals(tf, resolved, "Failed to resolve " + tf.name() + " from " + tf.getApiValue());
            }
        }

        @Test
        @DisplayName("Disambiguate 1m (minute) vs 1M (month)")
        void disambiguateOneMinuteVsOneMonth() {
            TimeFrame minute = TimeFrame.fromApiValue("1m");
            TimeFrame month = TimeFrame.fromApiValue("1M");

            assertEquals(TimeFrame.MINUTE, minute, "'1m' must resolve to MINUTE");
            assertEquals(TimeFrame.MONTH, month, "'1M' must resolve to MONTH");
            assertNotEquals(minute, month, "MINUTE and MONTH must never be identical");
        }

        @Test
        @DisplayName("Disambiguate 3m (3 minutes) vs 3M (3 months)")
        void disambiguateThreeMinuteVsThreeMonth() {
            TimeFrame threeMin = TimeFrame.fromApiValue("3m");
            TimeFrame threeMonth = TimeFrame.fromApiValue("3M");

            assertEquals(TimeFrame.THREE_MINUTE, threeMin, "'3m' must resolve to THREE_MINUTE");
            assertEquals(TimeFrame.THREE_MONTH, threeMonth, "'3M' must resolve to THREE_MONTH");
            assertNotEquals(threeMin, threeMonth, "THREE_MINUTE and THREE_MONTH must never be identical");
        }

        @Test
        @DisplayName("Six month resolution")
        void sixMonthResolution() {
            assertEquals(TimeFrame.SIX_MONTH, TimeFrame.fromApiValue("6M"));
            assertEquals(TimeFrame.SIX_MONTH, TimeFrame.fromApiValue("6m"));
        }
    }

    @Nested
    @DisplayName("Case-Insensitive Fallback for Non-Ambiguous Values")
    class CaseInsensitiveFallbackTests {

        @ParameterizedTest(name = "Input ''{0}'' should resolve to {1}")
        @CsvSource({
                "1D, DAY",
                "1d, DAY",
                "1W, WEEK",
                "1w, WEEK",
                "1H, HOUR",
                "1h, HOUR",
                "4H, FOUR_HOUR",
                "4h, FOUR_HOUR",
                "1Y, YEAR",
                "1y, YEAR",
                "5Y, FIVE_YEAR",
                "5y, FIVE_YEAR",
                "5m, FIVE_MINUTE",
                "10m, TEN_MINUTE",
                "15m, FIFTEEN_MINUTE",
                "30m, THIRTY_MINUTE"
        })
        void nonAmbiguousCaseInsensitiveResolution(String input, TimeFrame expected) {
            assertEquals(expected, TimeFrame.fromApiValue(input));
        }
    }

    @Nested
    @DisplayName("Aliases Resolution")
    class AliasesTests {

        @ParameterizedTest(name = "Alias ''{0}'' should resolve to {1}")
        @CsvSource({
                "I1, MINUTE",
                "i1, MINUTE",
                "I30, THIRTY_MINUTE",
                "i30, THIRTY_MINUTE",
                "5D, WEEK",
                "5d, WEEK"
        })
        void aliasResolution(String alias, TimeFrame expected) {
            assertEquals(expected, TimeFrame.fromApiValue(alias));
        }
    }

    @Nested
    @DisplayName("FromString (Enum Names and API Values)")
    class FromStringTests {

        @Test
        @DisplayName("fromString correctly resolves 1m and 1M without collision")
        void fromStringOneMinuteAndOneMonth() {
            assertEquals(TimeFrame.MINUTE, TimeFrame.fromString("1m"));
            assertEquals(TimeFrame.MONTH, TimeFrame.fromString("1M"));
        }

        @Test
        @DisplayName("fromString correctly resolves 3m and 3M without collision")
        void fromStringThreeMinuteAndThreeMonth() {
            assertEquals(TimeFrame.THREE_MINUTE, TimeFrame.fromString("3m"));
            assertEquals(TimeFrame.THREE_MONTH, TimeFrame.fromString("3M"));
        }

        @ParameterizedTest(name = "Enum name ''{0}'' should resolve to {1}")
        @CsvSource({
                "MINUTE, MINUTE",
                "minute, MINUTE",
                "Minute, MINUTE",
                "MONTH, MONTH",
                "month, MONTH",
                "Month, MONTH",
                "THREE_MONTH, THREE_MONTH",
                "three_month, THREE_MONTH",
                "DAY, DAY",
                "day, DAY",
                "WEEK, WEEK",
                "week, WEEK",
                "YEAR, YEAR",
                "year, YEAR"
        })
        void fromStringWithEnumNames(String name, TimeFrame expected) {
            assertEquals(expected, TimeFrame.fromString(name));
        }

        @Test
        @DisplayName("fromString resolves every enum name via valueOf")
        void fromStringResolvesAllEnumNames() {
            for (TimeFrame tf : TimeFrame.values()) {
                assertEquals(tf, TimeFrame.fromString(tf.name()));
                assertEquals(tf, TimeFrame.fromString(tf.name().toLowerCase()));
            }
        }
    }

    @Nested
    @DisplayName("Trimming and Whitespace")
    class WhitespaceTests {

        @Test
        @DisplayName("Handles leading and trailing whitespace")
        void handlesWhitespace() {
            assertEquals(TimeFrame.MINUTE, TimeFrame.fromApiValue("  1m  "));
            assertEquals(TimeFrame.MONTH, TimeFrame.fromApiValue("  1M  "));
            assertEquals(TimeFrame.DAY, TimeFrame.fromApiValue(" 1D "));
            assertEquals(TimeFrame.MINUTE, TimeFrame.fromString("  1m  "));
            assertEquals(TimeFrame.MONTH, TimeFrame.fromString("  1M  "));
            assertEquals(TimeFrame.DAY, TimeFrame.fromString("  DAY  "));
        }
    }

    @Nested
    @DisplayName("Invalid and Null Input Validation")
    class InvalidInputTests {

        @Test
        @DisplayName("fromApiValue throws on null")
        void fromApiValueThrowsOnNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> TimeFrame.fromApiValue(null));
            assertTrue(ex.getMessage().contains("null"));
        }

        @Test
        @DisplayName("fromString throws on null")
        void fromStringThrowsOnNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> TimeFrame.fromString(null));
            assertTrue(ex.getMessage().contains("null"));
        }

        @ParameterizedTest(name = "Empty or whitespace ''{0}'' should throw IllegalArgumentException")
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        void emptyOrBlankThrows(String blank) {
            assertThrows(IllegalArgumentException.class, () -> TimeFrame.fromApiValue(blank));
            assertThrows(IllegalArgumentException.class, () -> TimeFrame.fromString(blank));
        }

        @ParameterizedTest(name = "Invalid string ''{0}'' should throw IllegalArgumentException")
        @ValueSource(strings = {"invalid", "0m", "99X", "2D", "10Y", "XYZ"})
        void invalidValuesThrow(String invalid) {
            assertThrows(IllegalArgumentException.class, () -> TimeFrame.fromApiValue(invalid));
            assertThrows(IllegalArgumentException.class, () -> TimeFrame.fromString(invalid));
        }
    }

    @Nested
    @DisplayName("Provider Mappings Verification")
    class ProviderMappingTests {

        @Test
        @DisplayName("Zerodha mapping preserves minute vs month values")
        void zerodhaMappings() {
            assertEquals("minute", TimeFrame.toZerodhaValue("1m"));
            assertEquals("60minute", TimeFrame.toZerodhaValue("1M"));
            assertEquals("3minute", TimeFrame.toZerodhaValue("3m"));
            assertEquals("day", TimeFrame.toZerodhaValue("3M"));
            assertEquals("day", TimeFrame.toZerodhaValue("6M"));
            assertEquals("60minute", TimeFrame.toZerodhaValue("1D"));
            assertEquals("60minute", TimeFrame.toZerodhaValue("1W"));
        }

        @Test
        @DisplayName("Upstox mapping preserves minute vs month values")
        void upstoxMappings() {
            assertEquals("I1", TimeFrame.toUpStockValue("1m"));
            assertEquals("month", TimeFrame.toUpStockValue("1M"));
            assertEquals("I1", TimeFrame.toUpStockValue("3m"));
            assertEquals("day", TimeFrame.toUpStockValue("3M"));
            assertEquals("day", TimeFrame.toUpStockValue("6M"));
            assertEquals("1d", TimeFrame.toUpStockValue("1D"));
            assertEquals("week", TimeFrame.toUpStockValue("1W"));
        }
    }

    @Nested
    @DisplayName("Aggregation and Base TimeFrame Properties")
    class AggregationPropertiesTests {

        @Test
        @DisplayName("Verify aggregation requirements and base timeframes")
        void aggregationRules() {
            // Non-aggregated
            assertFalse(TimeFrame.MINUTE.requiresAggregation());
            assertEquals(1, TimeFrame.MINUTE.getAggregationFactor());
            assertEquals(TimeFrame.MINUTE, TimeFrame.MINUTE.getBaseTimeFrame());

            assertFalse(TimeFrame.DAY.requiresAggregation());
            assertEquals(TimeFrame.DAY, TimeFrame.DAY.getBaseTimeFrame());

            assertFalse(TimeFrame.HOUR.requiresAggregation());
            assertEquals(TimeFrame.HOUR, TimeFrame.HOUR.getBaseTimeFrame());

            // Aggregated
            assertTrue(TimeFrame.FOUR_HOUR.requiresAggregation());
            assertEquals(4, TimeFrame.FOUR_HOUR.getAggregationFactor());
            assertEquals(TimeFrame.HOUR, TimeFrame.FOUR_HOUR.getBaseTimeFrame());

            assertTrue(TimeFrame.WEEK.requiresAggregation());
            assertEquals(7, TimeFrame.WEEK.getAggregationFactor());
            assertEquals(TimeFrame.DAY, TimeFrame.WEEK.getBaseTimeFrame());

            assertTrue(TimeFrame.MONTH.requiresAggregation());
            assertEquals(30, TimeFrame.MONTH.getAggregationFactor());
            assertEquals(TimeFrame.DAY, TimeFrame.MONTH.getBaseTimeFrame());

            assertTrue(TimeFrame.THREE_MONTH.requiresAggregation());
            assertEquals(90, TimeFrame.THREE_MONTH.getAggregationFactor());
            assertEquals(TimeFrame.DAY, TimeFrame.THREE_MONTH.getBaseTimeFrame());

            assertTrue(TimeFrame.SIX_MONTH.requiresAggregation());
            assertEquals(180, TimeFrame.SIX_MONTH.getAggregationFactor());
            assertEquals(TimeFrame.DAY, TimeFrame.SIX_MONTH.getBaseTimeFrame());

            assertTrue(TimeFrame.YEAR.requiresAggregation());
            assertEquals(365, TimeFrame.YEAR.getAggregationFactor());
            assertEquals(TimeFrame.DAY, TimeFrame.YEAR.getBaseTimeFrame());

            assertTrue(TimeFrame.FIVE_YEAR.requiresAggregation());
            assertEquals(1825, TimeFrame.FIVE_YEAR.getAggregationFactor());
            assertEquals(TimeFrame.DAY, TimeFrame.FIVE_YEAR.getBaseTimeFrame());
        }
    }
}

