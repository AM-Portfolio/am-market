package com.am.marketdata.service.service;

import com.am.marketdata.service.model.ZerodhaInstrument;
import com.am.marketdata.service.repo.ZerodhaInstrumentRepository;
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

import com.am.marketdata.common.provider.InstrumentDataProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZerodhaInstrumentService implements InstrumentDataProvider {

    private final ZerodhaInstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    private static final int BATCH_SIZE = 1000;

    @Override
    public String getProviderName() {
        return "ZERODHA";
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

        if (!file.exists()) {
            throw new IOException("File not found: " + filePath);
        }

        com.fasterxml.jackson.dataformat.csv.CsvMapper csvMapper = new com.fasterxml.jackson.dataformat.csv.CsvMapper();
        // Configure schema based on model fields order or use header
        com.fasterxml.jackson.dataformat.csv.CsvSchema schema = com.fasterxml.jackson.dataformat.csv.CsvSchema
                .emptySchema().withHeader();

        try (java.io.Reader reader = new java.io.FileReader(file)) {
            com.fasterxml.jackson.databind.MappingIterator<ZerodhaInstrument> it = csvMapper
                    .readerFor(ZerodhaInstrument.class)
                    .with(schema)
                    .readValues(reader);

            List<ZerodhaInstrument> batch = new ArrayList<>();
            int totalProcessed = 0;

            while (it.hasNext()) {
                ZerodhaInstrument instrument = it.next();
                if (instrument != null) {
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

    private void saveBatch(List<ZerodhaInstrument> batch) {
        try {
            instrumentRepository.saveAll(batch);
        } catch (Exception e) {
            log.error("Error saving batch of size {}", batch.size(), e);
        }
    }

    /**
     * Search instruments with filters.
     */
    @Override
    public List<ZerodhaInstrument> searchInstruments(com.am.marketdata.common.dto.InstrumentSearchCriteria criteria) {
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

        // 2. Apply Search Queries ("Gym balls" + Semantic)
        if (criteria.getQueries() != null && !criteria.getQueries().isEmpty()) {
            // If queries look like symbols (uppercase, short), try exact match
            List<ZerodhaInstrument> results = new ArrayList<>();

            // Strategy 1: Exact match on asset symbol (mapped to 'name' in Zerodha)
            List<ZerodhaInstrument> byAsset = instrumentRepository.findByNameIn(criteria.getQueries());
            results.addAll(byAsset);

            // Strategy 2: Text search (name, tradingSymbol, assetSymbol)
            List<org.springframework.data.mongodb.core.query.Criteria> orCriteria = new ArrayList<>();
            // B. Regex text search for each query
            for (String text : criteria.getQueries()) {
                String regex = ".*" + java.util.regex.Pattern.quote(text) + ".*";
                // Search in name, assetSymbol, tradingSymbol
                orCriteria.add(org.springframework.data.mongodb.core.query.Criteria.where("name").regex(regex, "i"));
                orCriteria.add(
                        org.springframework.data.mongodb.core.query.Criteria.where("assetSymbol").regex(regex, "i"));
                orCriteria.add(
                        org.springframework.data.mongodb.core.query.Criteria.where("tradingSymbol").regex(regex, "i"));
            }

            // Combine with OR
            criteriaList.add(new org.springframework.data.mongodb.core.query.Criteria()
                    .orOperator(orCriteria.toArray(new org.springframework.data.mongodb.core.query.Criteria[0])));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new org.springframework.data.mongodb.core.query.Criteria()
                    .andOperator(criteriaList.toArray(new org.springframework.data.mongodb.core.query.Criteria[0])));
        }

        return mongoTemplate.find(query, ZerodhaInstrument.class);
    }
}
