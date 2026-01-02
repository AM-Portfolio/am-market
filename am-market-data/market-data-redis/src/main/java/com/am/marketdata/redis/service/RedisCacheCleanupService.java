package com.am.marketdata.redis.service;

import com.am.marketdata.common.log.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for cleaning up old Redis cache data.
 * Identifies keys older than retention period, archives to database, and
 * removes from Redis.
 */
@Service
@RequiredArgsConstructor
public class RedisCacheCleanupService {

    private final AppLogger log = AppLogger.getLogger(RedisCacheCleanupService.class);
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${redis.cleanup.retention.days:7}")
    private int retentionDays;

    @Value("${redis.cleanup.batch.size:1000}")
    private int batchSize;

    // Pattern to extract date from Redis keys:
    // stock:<type>:<symbol>:<interval>:<date>
    private static final Pattern KEY_DATE_PATTERN = Pattern.compile(".*:(\\d{4}-\\d{2}-\\d{2})$");

    /**
     * Identifies old keys matching the pattern that are older than retention
     * period.
     * 
     * @param pattern       Redis key pattern (e.g., "stock:intraday:*")
     * @param retentionDays Number of days to retain
     * @return List of keys to cleanup
     */
    public List<String> identifyOldKeys(String pattern, int retentionDays) {
        log.info("identifyOldKeys", "Scanning Redis for old keys with pattern: {} (retention: {} days)",
                pattern, retentionDays);

        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        List<String> oldKeys = new ArrayList<>();

        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(batchSize)
                    .build();

            Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .scan(options);

            int totalScanned = 0;
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                totalScanned++;

                LocalDate keyDate = extractDateFromKey(key);
                if (keyDate != null && keyDate.isBefore(cutoffDate)) {
                    oldKeys.add(key);
                }

                // Log progress every 10000 keys
                if (totalScanned % 10000 == 0) {
                    log.info("identifyOldKeys", "Scanned {} keys, found {} old keys so far",
                            totalScanned, oldKeys.size());
                }
            }
            cursor.close();

            log.info("identifyOldKeys", "Scan complete. Total scanned: {}, Old keys found: {}",
                    totalScanned, oldKeys.size());
            return oldKeys;

        } catch (Exception e) {
            log.error("identifyOldKeys", "Error scanning Redis keys", e);
            return new ArrayList<>();
        }
    }

    /**
     * Cleans up old keys after archiving to database.
     * 
     * @param pattern       Redis key pattern
     * @param retentionDays Days to retain
     * @return Number of keys cleaned up
     */
    public int cleanupOldKeys(String pattern, int retentionDays) {
        log.info("cleanupOldKeys", "Starting cleanup for pattern: {} (retention: {} days)",
                pattern, retentionDays);

        List<String> oldKeys = identifyOldKeys(pattern, retentionDays);

        if (oldKeys.isEmpty()) {
            log.info("cleanupOldKeys", "No old keys found to cleanup");
            return 0;
        }

        // TODO: Archive to database before deletion
        // This will be implemented when we integrate with MarketDataPersistenceService

        // Delete keys in batches
        int totalDeleted = 0;
        for (int i = 0; i < oldKeys.size(); i += batchSize) {
            int end = Math.min(i + batchSize, oldKeys.size());
            List<String> batch = oldKeys.subList(i, end);

            Long deleted = redisTemplate.delete(batch);
            totalDeleted += (deleted != null ? deleted.intValue() : 0);

            log.info("cleanupOldKeys", "Deleted batch {}/{}: {} keys",
                    (i / batchSize) + 1, (oldKeys.size() / batchSize) + 1, deleted);
        }

        log.info("cleanupOldKeys", "Cleanup complete. Total keys deleted: {}", totalDeleted);
        return totalDeleted;
    }

    /**
     * Extracts date from Redis key.
     * Key format: stock:<type>:<symbol>:<interval>:<YYYY-MM-DD>
     * 
     * @param key Redis key
     * @return LocalDate extracted from key, or null if cannot parse
     */
    private LocalDate extractDateFromKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        try {
            Matcher matcher = KEY_DATE_PATTERN.matcher(key);
            if (matcher.matches()) {
                String dateStr = matcher.group(1);
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            }
        } catch (DateTimeParseException e) {
            log.warn("extractDateFromKey", "Failed to parse date from key: {}", key);
        }

        return null;
    }

    /**
     * Gets statistics about cache keys for a pattern.
     * 
     * @param pattern Redis key pattern
     * @return Map with statistics
     */
    public CacheStats getCacheStats(String pattern) {
        log.info("getCacheStats", "Getting cache statistics for pattern: {}", pattern);

        try {
            Set<String> keys = redisTemplate.keys(pattern);
            int totalKeys = keys != null ? keys.size() : 0;

            LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
            int oldKeyCount = 0;
            int recentKeyCount = 0;

            if (keys != null) {
                for (String key : keys) {
                    LocalDate keyDate = extractDateFromKey(key);
                    if (keyDate != null) {
                        if (keyDate.isBefore(cutoffDate)) {
                            oldKeyCount++;
                        } else {
                            recentKeyCount++;
                        }
                    }
                }
            }

            return new CacheStats(totalKeys, oldKeyCount, recentKeyCount, retentionDays);

        } catch (Exception e) {
            log.error("getCacheStats", "Error getting cache statistics", e);
            return new CacheStats(0, 0, 0, retentionDays);
        }
    }

    /**
     * Cache statistics holder
     */
    public static class CacheStats {
        public final int totalKeys;
        public final int oldKeys;
        public final int recentKeys;
        public final int retentionDays;

        public CacheStats(int totalKeys, int oldKeys, int recentKeys, int retentionDays) {
            this.totalKeys = totalKeys;
            this.oldKeys = oldKeys;
            this.recentKeys = recentKeys;
            this.retentionDays = retentionDays;
        }

        @Override
        public String toString() {
            return String.format("CacheStats{total=%d, old=%d, recent=%d, retention=%d days}",
                    totalKeys, oldKeys, recentKeys, retentionDays);
        }
    }
}
