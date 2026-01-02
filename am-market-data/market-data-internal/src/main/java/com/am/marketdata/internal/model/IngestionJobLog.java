package com.am.marketdata.internal.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ingestion_job_logs")
public class IngestionJobLog {

    @Id
    private String id;

    private String jobId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String status; // SUCCESS, FAILED, PARTIAL

    private int totalSymbols;
    private int successCount;
    private int failureCount;

    private List<String> failedSymbols;

    private long durationMs;
    private long payloadSize; // Size in bytes
    private String message;

    @org.springframework.data.annotation.Transient
    private List<String> logs;
}
