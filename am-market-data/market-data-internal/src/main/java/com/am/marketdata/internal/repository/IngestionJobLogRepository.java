package com.am.marketdata.internal.repository;

import com.am.marketdata.internal.model.IngestionJobLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository for Ingestion Job Logs.
 * Accessed by MarketDataAdminController.
 */
@Repository
public interface IngestionJobLogRepository extends MongoRepository<IngestionJobLog, String> {
    java.util.Optional<IngestionJobLog> findByJobId(String jobId);

    Page<IngestionJobLog> findByStartTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
