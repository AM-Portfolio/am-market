package com.am.marketdata.service.service;

import com.am.marketdata.service.model.UpstoxInstrument;
import com.am.marketdata.service.repo.UpstoxInstrumentRepository;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.am.marketdata.service.provider.InstrumentDataProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpstoxInstrumentService implements InstrumentDataProvider {

    private final UpstoxInstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    private static final int BATCH_SIZE = 1000;

    @Override
    public String getProviderName() {
        return "UPSTOX";
    }

    @Override
    public void updateInstruments(String filePath) throws IOException {
        updateInstrumentsFromFile(filePath);
    }

    public void updateInstrumentsFromFile(String filePath) throws IOException {
        log.info("Starting instrument update from file: {}", filePath);
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        JsonFactory jsonFactory = objectMapper.getFactory();
        try (JsonParser jsonParser = jsonFactory.createParser(file)) {

            // Expected structure is an array of objects: [ { ... }, { ... } ]
            if (jsonParser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected content to be an array");
            }

            List<UpstoxInstrument> batch = new ArrayList<>();
            int totalProcessed = 0;

            while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                // Parse each object into Instrument model using ObjectMapper
                UpstoxInstrument instrument = objectMapper.readValue(jsonParser, UpstoxInstrument.class);

                if (instrument != null) {
                    if (instrument.getIsin() == null || instrument.getIsin().isEmpty()) {
                        String isin = extractIsin(instrument.getInstrumentKey());
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
                    log.info("Processed {} instruments...", totalProcessed);
                }
            }

            // Save remaining
            if (!batch.isEmpty()) {
                saveBatch(batch);
                totalProcessed += batch.size();
            }

            log.info("Finished updating instruments. Total processed: {}", totalProcessed);
        }
    }

    private void saveBatch(List<UpstoxInstrument> batch) {
        // saveAll works for specific batches, or we can use bulkOps if needed for
        // performance.
        // Repository saveAll is sufficient for this size (35MB ~ 100k records maybe?)
        try {
            instrumentRepository.saveAll(batch);
        } catch (Exception e) {
            log.error("Error saving batch of size {}", batch.size(), e);
            // Fallback or re-throw, depending on requirement. For now, continue.
        }
    }

    /**
     * Search instruments with filters.
     */
    @Override
    public List<UpstoxInstrument> searchInstruments(com.am.marketdata.service.dto.InstrumentSearchCriteria criteria) {
        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query();
        List<org.springframework.data.mongodb.core.query.Criteria> criteriaList = new ArrayList<>();

        // 1. Apply Filters (Exchange, Instrument Type)
        if (criteria.getExchanges() != null && !criteria.getExchanges().isEmpty()) {
            criteriaList.add(
                    org.springframework.data.mongodb.core.query.Criteria.where("exchange").in(criteria.getExchanges()));
        }
        if (criteria.getInstrumentTypes() != null && !criteria.getInstrumentTypes().isEmpty()) {
            criteriaList.add(org.springframework.data.mongodb.core.query.Criteria.where("instrumentType")
                    .in(criteria.getInstrumentTypes()));
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
            criteriaList.add(org.springframework.data.mongodb.core.query.Criteria.where("tradingSymbol")
                    .in(criteria.getTradingSymbols()));
        }

        // 2. Apply Search Queries ("Gym balls" + Semantic)
        if (criteria.getQueries() != null && !criteria.getQueries().isEmpty()) {
            List<org.springframework.data.mongodb.core.query.Criteria> orCriteria = new ArrayList<>();

            // 1. Exact match on ISIN or Asset Symbol (Priority)
            orCriteria.add(
                    org.springframework.data.mongodb.core.query.Criteria.where("isin").in(criteria.getQueries()));
            orCriteria.add(org.springframework.data.mongodb.core.query.Criteria.where("assetSymbol")
                    .in(criteria.getQueries()));

            // 2. Regex text search for Name and Asset Symbol (excluding Trading Symbol)
            for (String text : criteria.getQueries()) {
                String regex = ".*" + java.util.regex.Pattern.quote(text) + ".*";
                orCriteria.add(org.springframework.data.mongodb.core.query.Criteria.where("name").regex(regex, "i"));
                orCriteria.add(
                        org.springframework.data.mongodb.core.query.Criteria.where("assetSymbol").regex(regex, "i"));
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
