package com.am.marketdata.scheduler.service;

import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.redis.service.RedisCacheCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Scheduled job for cleaning up old Redis cache data.
 * Runs daily to remove data older than retention period.
 */
@Service
@RequiredArgsConstructor
public class RedisCacheCleanupScheduler {

    private final AppLogger log = AppLogger.getLogger(RedisCacheCleanupScheduler.class);
    private final RedisCacheCleanupService cleanupService;

    @Value("${scheduler.redis.cleanup.enabled:true}")
    private boolean enabled;

    @Value("${redis.cleanup.retention.days:7}")
    private int retentionDays;

    /**
     * Scheduled cleanup job - runs daily at 2:00 AM
     */
    /**
     * Scheduled cleanup job - runs daily at 2:00 AM
     */
    public void executeCleanup() {
        if (!enabled) {
            log.debug("scheduledCleanup", "Redis cleanup is disabled");
            return;
        }

        log.info("scheduledCleanup", "Starting scheduled Redis cache cleanup (retention: {} days)", retentionDays);

        try {
            // Get initial statistics
            RedisCacheCleanupService.CacheStats intradayStatsBefore = cleanupService.getCacheStats("stock:intraday:*");
            RedisCacheCleanupService.CacheStats historicalStatsBefore = cleanupService
                    .getCacheStats("stock:historical:*");

            log.info("scheduledCleanup", "Before cleanup - Intraday: {}", intradayStatsBefore);
            log.info("scheduledCleanup", "Before cleanup - Historical: {}", historicalStatsBefore);

            // Cleanup intraday data
            int intradayDeleted = cleanupService.cleanupOldKeys("stock:intraday:*", retentionDays);
            log.info("scheduledCleanup", "Cleanup intraday complete: {} keys deleted", intradayDeleted);

            // Cleanup historical data
            int historicalDeleted = cleanupService.cleanupOldKeys("stock:historical:*", retentionDays);
            log.info("scheduledCleanup", "Cleanup historical complete: {} keys deleted", historicalDeleted);

            // Get final statistics
            RedisCacheCleanupService.CacheStats intradayStatsAfter = cleanupService.getCacheStats("stock:intraday:*");
            RedisCacheCleanupService.CacheStats historicalStatsAfter = cleanupService
                    .getCacheStats("stock:historical:*");

            log.info("scheduledCleanup", "After cleanup - Intraday: {}", intradayStatsAfter);
            log.info("scheduledCleanup", "After cleanup - Historical: {}", historicalStatsAfter);

            log.info("scheduledCleanup", "Scheduled Redis cache cleanup completed successfully. Total deleted: {}",
                    intradayDeleted + historicalDeleted);

        } catch (Exception e) {
            log.error("scheduledCleanup", "Error during scheduled Redis cleanup", e);
        }
    }

    /**
     * Manual cleanup trigger (can be called via admin endpoint)
     * 
     * @param pattern Redis key pattern to cleanup
     * @param days    Number of days to retain
     * @return Number of keys deleted
     */
    public int manualCleanup(String pattern, int days) {
        log.info("manualCleanup", "Manual cleanup triggered for pattern: {} (retention: {} days)", pattern, days);

        try {
            int deleted = cleanupService.cleanupOldKeys(pattern, days);
            log.info("manualCleanup", "Manual cleanup complete: {} keys deleted", deleted);
            return deleted;
        } catch (Exception e) {
            log.error("manualCleanup", "Error during manual cleanup", e);
            return 0;
        }
    }

    /**
     * Get cache statistics for monitoring
     * 
     * @param pattern Redis key pattern
     * @return Cache statistics
     */
    public RedisCacheCleanupService.CacheStats getStats(String pattern) {
        return cleanupService.getCacheStats(pattern);
    }
}
