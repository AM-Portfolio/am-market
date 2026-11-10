package com.am.marketdata.provider.upstox.service;

import com.am.marketdata.common.model.UpstoxInstrument;
import com.am.marketdata.common.provider.InstrumentDataProvider;
import com.am.marketdata.provider.upstox.repo.UpstoxInstrumentRepository;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpstoxInstrumentService implements InstrumentDataProvider {

    /**
     * Official daily NSE instrument master hosted on Upstox CDN.
     * Contains 85,000+ contracts across Equities, Indices, Futures, and Options.
     * Updated every night by Upstox at ~23:57 GMT (5:27 AM IST).
     */
    public static final String OFFICIAL_NSE_INSTRUMENTS_URL =
            "https://assets.upstox.com/market-quote/instruments/exchange/NSE.json.gz";

    private static final int BATCH_SIZE = 1000;
    private static final int HTTP_CONNECT_TIMEOUT_MS = 30_000;
    private static final int HTTP_READ_TIMEOUT_MS = 60_000;

    private final UpstoxInstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;

    /**
     * Prevents overlapping executions of large instrument sync jobs.
     */
    private final AtomicBoolean isSyncRunning = new AtomicBoolean(false);

    @Override
    public String getProviderName() {
        return "UPSTOX";
    }

    /**
     * Returns true if a background instrument sync job is currently executing.
     */
    public boolean isSyncRunning() {
        return isSyncRunning.get();
    }

    /**
     * Unified entry point for syncing instruments.
     * If source is null, blank, or an HTTP(S) URL, streams directly from Upstox CDN (or provided URL).
     * Otherwise, reads from the specified local file path.
     */
    @Override
    public void updateInstruments(String source) throws IOException {
        if (source == null || source.isBlank() || source.startsWith("http://") || source.startsWith("https://")) {
            String url = (source == null || source.isBlank()) ? OFFICIAL_NSE_INSTRUMENTS_URL : source;
            syncInstrumentsFromRemoteUrl(url);
        } else {
            updateInstrumentsFromFile(source);
        }
    }

    /**
     * Streams and ingests instruments from a remote gzipped JSON URL (e.g. official Upstox CDN).
     * Employs chunked streaming via Jackson JsonParser without loading the full ~35MB JSON into memory.
     */
    public void syncInstrumentsFromRemoteUrl(String urlString) throws IOException {
        if (!isSyncRunning.compareAndSet(false, true)) {
            log.warn("[INSTRUMENT_SYNC] Instrument sync already running. Skipping duplicate trigger for URL: {}", urlString);
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("[INSTRUMENT_SYNC] Initiating instrument sync from remote URL: {}", urlString);

        HttpURLConnection connection = null;
        try {
            URL url = URI.create(urlString).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "am-market-data/1.0");
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("[INSTRUMENT_SYNC] Remote server returned HTTP " + responseCode + " for " + urlString);
            }

            InputStream rawStream = connection.getInputStream();
            boolean isGzip = urlString.endsWith(".gz") || "gzip".equalsIgnoreCase(connection.getContentEncoding());
            InputStream decompressedStream = isGzip ? new GZIPInputStream(rawStream) : rawStream;

            try (BufferedInputStream bis = new BufferedInputStream(decompressedStream, 65536)) {
                int totalProcessed = streamAndSaveInstruments(bis, "remote URL: " + urlString);
                log.info("[INSTRUMENT_SYNC] Completed streaming from remote URL. Total processed: {} records in {} ms",
                        totalProcessed, (System.currentTimeMillis() - startTime));
            }

            // Post-sync cleanup of expired derivative contracts
            purgeExpiredDerivatives();

        } catch (Exception e) {
            log.error("[INSTRUMENT_SYNC] Failed during remote instrument sync from URL: {}", urlString, e);
            throw (e instanceof IOException ioException) ? ioException : new IOException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            isSyncRunning.set(false);
        }
    }

    /**
     * Streams and ingests instruments from a local file path.
     */
    public void updateInstrumentsFromFile(String filePath) throws IOException {
        if (!isSyncRunning.compareAndSet(false, true)) {
            log.warn("[INSTRUMENT_SYNC] Instrument sync already running. Skipping duplicate trigger for file: {}", filePath);
            return;
        }

        long startTime = System.currentTimeMillis();
        log.info("[INSTRUMENT_SYNC] Starting instrument update from file: {}", filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            isSyncRunning.set(false);
            throw new IOException("File not found: " + filePath);
        }

        try {
            InputStream rawStream = new FileInputStream(file);
            InputStream decompressedStream = filePath.endsWith(".gz") ? new GZIPInputStream(rawStream) : rawStream;

            try (BufferedInputStream bis = new BufferedInputStream(decompressedStream, 65536)) {
                int totalProcessed = streamAndSaveInstruments(bis, "file: " + filePath);
                log.info("[INSTRUMENT_SYNC] Completed streaming from file. Total processed: {} records in {} ms",
                        totalProcessed, (System.currentTimeMillis() - startTime));
            }

            // Post-sync cleanup of expired derivative contracts
            purgeExpiredDerivatives();

        } catch (Exception e) {
            log.error("[INSTRUMENT_SYNC] Failed during file instrument sync from: {}", filePath, e);
            throw (e instanceof IOException ioException) ? ioException : new IOException(e);
        } finally {
            isSyncRunning.set(false);
        }
    }

    /**
     * Core streaming engine: consumes JSON array token by token and performs bulk upserts.
     * Memory complexity is strictly bounded by BATCH_SIZE (1,000 documents).
     */
    private int streamAndSaveInstruments(InputStream inputStream, String sourceDesc) throws IOException {
        JsonFactory jsonFactory = objectMapper.getFactory();
        try (JsonParser jsonParser = jsonFactory.createParser(inputStream)) {

            // Expected root structure is a JSON array: [ { ... }, { ... } ]
            if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected root content to be a JSON array from " + sourceDesc);
            }

            List<UpstoxInstrument> batch = new ArrayList<>(BATCH_SIZE);
            int totalProcessed = 0;
            long startProgress = System.currentTimeMillis();

            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                UpstoxInstrument instrument = objectMapper.readValue(jsonParser, UpstoxInstrument.class);

                if (instrument != null) {
                    // Critical: explicitly assign MongoDB _id to instrumentKey (e.g. "NSE_FO|61724")
                    // to guarantee deterministic upsert semantics and avoid stale duplicate keys.
                    String instrumentKey = instrument.getInstrumentKey();
                    if (instrumentKey != null && !instrumentKey.isBlank()) {
                        instrument.setId(instrumentKey);
                    }

                    if (instrument.getIsin() == null || instrument.getIsin().isEmpty()) {
                        String isin = extractIsin(instrumentKey);
                        if (isin == null) {
                            isin = extractIsin(instrument.getUnderlyingKey());
                        }
                        instrument.setIsin(isin);
                    }
                    batch.add(instrument);
                }

                if (batch.size() >= BATCH_SIZE) {
                    saveBatch(batch);
                    totalProcessed += batch.size();
                    batch.clear();
                    if (totalProcessed % 10000 == 0) {
                        log.info("[INSTRUMENT_SYNC] Processed {} instruments from {} (elapsed: {} ms)...",
                                totalProcessed, sourceDesc, (System.currentTimeMillis() - startProgress));
                    }
                }
            }

            // Save any remaining items in the trailing batch
            if (!batch.isEmpty()) {
                saveBatch(batch);
                totalProcessed += batch.size();
                batch.clear();
            }

            return totalProcessed;
        }
    }

    /**
     * High-throughput bulk upsert into MongoDB.
     * Uses MongoCollection bulkWrite with ReplaceOneModel for single round-trip batch insertion,
     * with graceful fallback to instrumentRepository.saveAll on error.
     */
    private void saveBatch(List<UpstoxInstrument> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        try {
            List<WriteModel<Document>> operations = new ArrayList<>(batch.size());
            for (UpstoxInstrument inst : batch) {
                String docId = inst.getId() != null ? inst.getId() : inst.getInstrumentKey();
                if (docId == null || docId.isBlank()) {
                    continue;
                }

                Document doc = new Document();
                mongoTemplate.getConverter().write(inst, doc);
                operations.add(new ReplaceOneModel<>(
                        Filters.eq("_id", docId),
                        doc,
                        new ReplaceOptions().upsert(true)
                ));
            }

            if (!operations.isEmpty()) {
                mongoTemplate.getCollection("upstock_instruments").bulkWrite(operations);
            }
        } catch (Exception e) {
            log.warn("[INSTRUMENT_SYNC] BulkWrite failed for batch size {}, falling back to repository.saveAll: {}",
                    batch.size(), e.getMessage());
            try {
                instrumentRepository.saveAll(batch);
            } catch (Exception fallbackEx) {
                log.error("[INSTRUMENT_SYNC] Fallback saveAll also failed for batch size {}", batch.size(), fallbackEx);
            }
        }
    }

    /**
     * Post-sync purge of expired derivative contracts from MongoDB.
     * Retains contracts expired within the last 7 days as a grace period for reporting/reconciliation.
     */
    public long purgeExpiredDerivatives() {
        try {
            long gracePeriodMs = 7L * 24 * 60 * 60 * 1000L;
            long cutoffEpochMs = System.currentTimeMillis() - gracePeriodMs;

            Query query = new Query();
            query.addCriteria(Criteria.where("segment").is("NSE_FO")
                    .and("expiry").gt(0).lt(cutoffEpochMs));

            var result = mongoTemplate.remove(query, UpstoxInstrument.class);
            long deletedCount = result.getDeletedCount();
            log.info("[INSTRUMENT_SYNC] Expired derivatives purge complete. Deleted {} expired contracts older than {} (cutoff: {})",
                    deletedCount, new Date(cutoffEpochMs), cutoffEpochMs);
            return deletedCount;
        } catch (Exception e) {
            log.error("[INSTRUMENT_SYNC] Error purging expired derivatives", e);
            return 0;
        }
    }

    /**
     * Search instruments with filters.
     */
    @Override
    public List<UpstoxInstrument> searchInstruments(com.am.marketdata.common.dto.InstrumentSearchCriteria criteria) {
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        List<org.springframework.data.mongodb.core.query.Criteria> criteriaList = new ArrayList<>();

        // 1. Apply Filters (Exchange, Instrument Type)
        if (criteria.getExchanges() != null && !criteria.getExchanges().isEmpty()) {
            criteriaList.add(
                    org.springframework.data.mongodb.core.query.Criteria.where("exchange").in(criteria.getExchanges()));
        }
        if (criteria.getInstrumentTypes() != null && !criteria.getInstrumentTypes().isEmpty()) {
            List<String> types = new ArrayList<>(criteria.getInstrumentTypes());
            // Upstox stores equity instruments with instrument_type = "EQ".
            // To ensure full backward compatibility with callers passing "EQUITY",
            // automatically alias "EQUITY" to include "EQ" in the Mongo criteria search.
            if (types.contains("EQUITY") && !types.contains("EQ")) {
                types.add("EQ");
            }
            criteriaList.add(org.springframework.data.mongodb.core.query.Criteria.where("instrument_type")
                    .in(types));
        }
        if (criteria.getSegments() != null && !criteria.getSegments().isEmpty()) {
            criteriaList.add(
                    org.springframework.data.mongodb.core.query.Criteria.where("segment").in(criteria.getSegments()));
        }
        if (criteria.getWeekly() != null) {
            criteriaList
                    .add(org.springframework.data.mongodb.core.query.Criteria.where("weekly").is(criteria.getWeekly()));
        }
        if (criteria.getIsins() != null && !criteria.getIsins().isEmpty()) {
            criteriaList
                    .add(org.springframework.data.mongodb.core.query.Criteria.where("isin").in(criteria.getIsins()));
        }
        if (criteria.getTradingSymbols() != null && !criteria.getTradingSymbols().isEmpty()) {
            criteriaList.add(org.springframework.data.mongodb.core.query.Criteria.where("trading_symbol")
                    .in(criteria.getTradingSymbols()));
        }

        // 2. Apply Search Queries ("Gym balls" + Semantic)
        if (criteria.getQueries() != null && !criteria.getQueries().isEmpty()) {
            List<org.springframework.data.mongodb.core.query.Criteria> orCriteria = new ArrayList<>();

            // 1. Exact match on ISIN or Asset Symbol (Priority)
            orCriteria.add(
                    org.springframework.data.mongodb.core.query.Criteria.where("isin").in(criteria.getQueries()));
            orCriteria.add(org.springframework.data.mongodb.core.query.Criteria.where("asset_symbol")
                    .in(criteria.getQueries()));

            // 2. Regex text search for Name, Asset Symbol, Trading Symbol, and Underlying Key/Symbol
            for (String text : criteria.getQueries()) {
                String regex = ".*" + java.util.regex.Pattern.quote(text) + ".*";
                orCriteria.add(org.springframework.data.mongodb.core.query.Criteria.where("name").regex(regex, "i"));
                orCriteria.add(
                        org.springframework.data.mongodb.core.query.Criteria.where("asset_symbol").regex(regex, "i"));
                orCriteria.add(
                        org.springframework.data.mongodb.core.query.Criteria.where("trading_symbol").regex(regex, "i"));
                orCriteria.add(
                        org.springframework.data.mongodb.core.query.Criteria.where("underlying_key").regex(regex, "i"));
                orCriteria.add(
                        org.springframework.data.mongodb.core.query.Criteria.where("underlying_symbol").regex(regex, "i"));
            }

            // Combine with OR
            if (!orCriteria.isEmpty()) {
                criteriaList.add(new org.springframework.data.mongodb.core.query.Criteria()
                        .orOperator(orCriteria.toArray(new org.springframework.data.mongodb.core.query.Criteria[0])));
            }
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new org.springframework.data.mongodb.core.query.Criteria()
                    .andOperator(criteriaList.toArray(new org.springframework.data.mongodb.core.query.Criteria[0])));
        } else {
            // Default limit if no filter is provided to avoid fetching all records
            query.limit(100);
        }

        return mongoTemplate.find(query, UpstoxInstrument.class);
    }

    private String extractIsin(String key) {
        if (key != null && key.contains("|")) {
            String[] parts = key.split("\\|");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }
}
