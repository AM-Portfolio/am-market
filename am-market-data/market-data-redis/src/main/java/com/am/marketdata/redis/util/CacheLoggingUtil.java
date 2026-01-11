package com.am.marketdata.redis.util;

import com.am.marketdata.common.log.AppLogger;

import com.am.common.investment.model.historical.OHLCVTPoint;
import com.am.marketdata.common.util.LoggingUtil;
import com.am.marketdata.redis.model.OHLCV;

import org.slf4j.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Specialized logging utility for Redis cache operations.
 * Extends the base LoggingUtil with cache-specific logging methods.
 */
public class CacheLoggingUtil {

        private static final String CACHE_COMPONENT = "RedisCache";
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

        /**
         * Log OHLC data caching operation
         *
         * @param logger   The SLF4J logger instance
         * @param symbol   The market symbol
         * @param interval The time interval
         * @param date     The date for the data
         * @param point    The OHLC data point
         */
        public static void logOHLCCaching(Logger logger, String symbol, String interval,
                        LocalDate date, OHLCVTPoint point) {
                String key = generateCacheKey("intraday", symbol, interval, date);
                String value = LoggingUtil.formatOHLCData(
                                point.getOpen(), point.getHigh(), point.getLow(), point.getClose());

                LoggingUtil.logCacheOperation(logger, "PUT", key, value);

                // Additional detailed logging at debug level
                if (logger.isDebugEnabled()) {
                        logger.debug("Cached data point for {}: time={}, open={}, high={}, low={}, close={}, volume={}",
                                        symbol, point.getTime(), point.getOpen(), point.getHigh(),
                                        point.getLow(), point.getClose(), point.getVolume());
                }
        }

        public static void logOHLCCaching(AppLogger logger, String symbol, String interval,
                        LocalDate date, OHLCVTPoint point) {
                String key = generateCacheKey("intraday", symbol, interval, date);
                String value = LoggingUtil.formatOHLCData(
                                point.getOpen(), point.getHigh(), point.getLow(), point.getClose());

                LoggingUtil.logCacheOperation(logger, "PUT", key, value);

                // Additional detailed logging at debug level
                logger.debug("logOHLCCaching",
                                String.format(
                                                "Cached data point for %s: time=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f, volume=%d",
                                                symbol, point.getTime(), point.getOpen(), point.getHigh(),
                                                point.getLow(), point.getClose(), point.getVolume()));
        }

        /**
         * Log batch OHLC data caching operation
         *
         * @param logger       The SLF4J logger instance
         * @param symbolPrices Map of symbols to their price points
         * @param date         The date for the data
         */
        public static void logBatchOHLCCaching(Logger logger, Map<String, List<OHLCV>> symbolPrices,
                        LocalDate date) {
                if (symbolPrices.isEmpty()) {
                        return;
                }

                // Build a list of actual Redis keys and values for logging
                List<String> keyValuePairs = symbolPrices.entrySet().stream()
                                .flatMap(entry -> {
                                        String symbol = entry.getKey();
                                        List<OHLCV> points = entry.getValue();

                                        if (points == null || points.isEmpty()) {
                                                return List.<String>of().stream();
                                        }

                                        OHLCV latestPoint = points.get(points.size() - 1);

                                        return CacheKeyGenerator.getIntradayIntervals().stream()
                                                        .map(interval -> {
                                                                String key = generateCacheKey("intraday", symbol,
                                                                                interval, date);
                                                                String value = LoggingUtil.formatOHLCData(
                                                                                latestPoint.getOpen(),
                                                                                latestPoint.getHigh(),
                                                                                latestPoint.getLow(),
                                                                                latestPoint.getClose());
                                                                return LoggingUtil.formatCacheKeyValue(key, value);
                                                        });
                                })
                                .collect(Collectors.toList());

                // Always log just the count in INFO
                logger.info("Successfully cached OHLC data for {} symbols in Redis with {} key-value pairs",
                                symbolPrices.size(), keyValuePairs.size());

                // Log detailed key-value pairs in DEBUG mode
                if (logger.isDebugEnabled() && !keyValuePairs.isEmpty()) {
                        logger.debug("Cached key-value pairs: {}", String.join(", ", keyValuePairs));
                }

                // Log detailed information at debug level
                if (logger.isDebugEnabled()) {
                        symbolPrices.forEach((symbol, points) -> {
                                if (points != null && !points.isEmpty()) {
                                        points.forEach(point -> {
                                                logger.debug(
                                                                "Cached data point for {}: open={}, high={}, low={}, close={}, volume={}, lastPrice={}",
                                                                symbol, point.getOpen(), point.getHigh(),
                                                                point.getLow(), point.getClose(), point.getVolume(),
                                                                point.getLastPrice());
                                        });
                                }
                        });
                }
        }

        public static void logBatchOHLCCaching(AppLogger logger, Map<String, List<OHLCV>> symbolPrices,
                        LocalDate date) {
                String methodName = "logBatchOHLCCaching";
                if (symbolPrices.isEmpty()) {
                        return;
                }

                // Build a list of actual Redis keys and values for logging
                List<String> keyValuePairs = symbolPrices.entrySet().stream()
                                .flatMap(entry -> {
                                        String symbol = entry.getKey();
                                        List<OHLCV> points = entry.getValue();

                                        if (points == null || points.isEmpty()) {
                                                return List.<String>of().stream();
                                        }

                                        OHLCV latestPoint = points.get(points.size() - 1);

                                        return CacheKeyGenerator.getIntradayIntervals().stream()
                                                        .map(interval -> {
                                                                String key = generateCacheKey("intraday", symbol,
                                                                                interval, date);
                                                                String value = LoggingUtil.formatOHLCData(
                                                                                latestPoint.getOpen(),
                                                                                latestPoint.getHigh(),
                                                                                latestPoint.getLow(),
                                                                                latestPoint.getClose());
                                                                return LoggingUtil.formatCacheKeyValue(key, value);
                                                        });
                                })
                                .collect(Collectors.toList());

                // Always log just the count in INFO
                logger.info(methodName,
                                String.format("Successfully cached OHLC data for %d symbols in Redis with %d key-value pairs",
                                                symbolPrices.size(), keyValuePairs.size()));

                // Log detailed key-value pairs in DEBUG mode
                if (!keyValuePairs.isEmpty()) {
                        logger.debug(methodName, "Cached key-value pairs: " + String.join(", ", keyValuePairs));
                }

                // Log detailed information at debug level
                symbolPrices.forEach((symbol, points) -> {
                        if (points != null && !points.isEmpty()) {
                                points.forEach(point -> {
                                        logger.debug(methodName, String.format(
                                                        "Cached data point for %s: open=%.2f, high=%.2f, low=%.2f, close=%.2f, volume=%d, lastPrice=%.2f",
                                                        symbol, point.getOpen(), point.getHigh(),
                                                        point.getLow(), point.getClose(), point.getVolume(),
                                                        point.getLastPrice()));
                                });
                        }
                });
        }

        /**
         * Log historical data caching operation
         *
         * @param logger   The SLF4J logger instance
         * @param symbol   The market symbol
         * @param interval The time interval
         * @param points   The list of historical data points
         */
        public static void logHistoricalDataCaching(Logger logger, String symbol, String interval,
                        List<OHLCVTPoint> points) {
                if (points == null || points.isEmpty()) {
                        logger.warn("No historical data to cache for symbol: {}", symbol);
                        return;
                }

                // Get a preview of the data for logging
                OHLCVTPoint firstPoint = points.get(0);
                String dataPreview = LoggingUtil.formatOHLCData(
                                firstPoint.getOpen(), firstPoint.getHigh(), firstPoint.getLow(), firstPoint.getClose());

                String redisKey = generateCacheKey("historical", symbol, interval, LocalDate.now());

                LoggingUtil.logMarketDataOperation(
                                logger, CACHE_COMPONENT, "CACHE_HISTORICAL", symbol, interval,
                                "Redis key: " + redisKey + " value: " + dataPreview);

                // Log key-value pairs for each data point
                List<String> keyValuePairs = points.stream()
                                .map(point -> {
                                        String key = generateCacheKey("historical", symbol, interval,
                                                        point.getTime().toLocalDate());
                                        String value = LoggingUtil.formatOHLCData(
                                                        point.getOpen(), point.getHigh(), point.getLow(),
                                                        point.getClose());
                                        return LoggingUtil.formatCacheKeyValue(key, value);
                                })
                                .collect(Collectors.toList());

                // Always log just the count in INFO
                logger.info("Successfully cached {} historical bars for {} in Redis with {} key-value pairs",
                                points.size(), symbol, keyValuePairs.size());

                // Log detailed key-value pairs in DEBUG mode
                if (logger.isDebugEnabled() && !keyValuePairs.isEmpty()) {
                        logger.debug("Cached key-value pairs: {}", String.join(", ", keyValuePairs));
                }

                // Log detailed information at debug level
                if (logger.isDebugEnabled()) {
                        points.forEach(point -> {
                                logger.debug(
                                                "Cached historical data point for {}: time={}, open={}, high={}, low={}, close={}, volume={}",
                                                symbol, point.getTime(), point.getOpen(), point.getHigh(),
                                                point.getLow(), point.getClose(), point.getVolume());
                        });
                }
        }

        public static void logHistoricalDataCaching(AppLogger logger, String symbol, String interval,
                        List<OHLCVTPoint> points) {
                String methodName = "logHistoricalDataCaching";
                if (points == null || points.isEmpty()) {
                        logger.warn(methodName, "No historical data to cache for symbol: " + symbol);
                        return;
                }

                // Get a preview of the data for logging
                OHLCVTPoint firstPoint = points.get(0);
                String dataPreview = LoggingUtil.formatOHLCData(
                                firstPoint.getOpen(), firstPoint.getHigh(), firstPoint.getLow(), firstPoint.getClose());

                String redisKey = generateCacheKey("historical", symbol, interval, LocalDate.now());

                LoggingUtil.logMarketDataOperation(
                                logger, CACHE_COMPONENT, "CACHE_HISTORICAL", symbol, interval,
                                "Redis key: " + redisKey + " value: " + dataPreview);

                // Log key-value pairs for each data point
                List<String> keyValuePairs = points.stream()
                                .map(point -> {
                                        String key = generateCacheKey("historical", symbol, interval,
                                                        point.getTime().toLocalDate());
                                        String value = LoggingUtil.formatOHLCData(
                                                        point.getOpen(), point.getHigh(), point.getLow(),
                                                        point.getClose());
                                        return LoggingUtil.formatCacheKeyValue(key, value);
                                })
                                .collect(Collectors.toList());

                // Always log just the count in INFO
                logger.info(methodName,
                                String.format("Successfully cached %d historical bars for %s in Redis with %d key-value pairs",
                                                points.size(), symbol, keyValuePairs.size()));

                // Log detailed key-value pairs in DEBUG mode
                if (!keyValuePairs.isEmpty()) {
                        logger.debug(methodName, "Cached key-value pairs: " + String.join(", ", keyValuePairs));
                }

                // Log detailed information at debug level
                points.forEach(point -> {
                        logger.debug(methodName, String.format(
                                        "Cached historical data point for %s: time=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f, volume=%d",
                                        symbol, point.getTime(), point.getOpen(), point.getHigh(),
                                        point.getLow(), point.getClose(), point.getVolume()));
                });
        }

        /**
         * Log exception during cache operation
         *
         * @param logger    The SLF4J logger instance
         * @param operation The cache operation that failed
         * @param symbol    The market symbol (optional)
         * @param message   Error message
         * @param exception The exception that occurred
         */
        public static void logCacheException(Logger logger, String operation, String symbol,
                        String message, Throwable exception) {
                LoggingUtil.logException(logger, CACHE_COMPONENT, operation,
                                symbol != null ? "Symbol: " + symbol + " - " + message : message, exception);
        }

        public static void logCacheException(AppLogger logger, String operation, String symbol,
                        String message, Throwable exception) {
                LoggingUtil.logException(logger, CACHE_COMPONENT, operation,
                                symbol != null ? "Symbol: " + symbol + " - " + message : message, exception);
        }

        /**
         * Generate a standardized Redis cache key
         *
         * @param prefix   The key prefix (e.g., "intraday" or "historical")
         * @param symbol   The market symbol
         * @param interval The time interval
         * @param date     The date for the data
         * @return Formatted Redis key
         */
        public static String generateCacheKey(String prefix, String symbol, String interval, LocalDate date) {
                return String.format("stock:%s:%s:%s:%s",
                                prefix, symbol.toUpperCase(), interval, date.format(DATE_FORMATTER));
        }
}
