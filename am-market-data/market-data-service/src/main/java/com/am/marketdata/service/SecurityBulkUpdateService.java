package com.am.marketdata.service;

import com.am.marketdata.common.dto.BulkUpdateRequest;
import com.am.marketdata.common.dto.BulkUpdateResponse;
import com.am.marketdata.common.dto.BulkUpdateResponse.UpdateResult;
import com.am.marketdata.common.dto.BulkUpdateResponse.MatchStatus;
import com.am.marketdata.common.log.AppLogger;
import com.am.marketdata.service.dto.SecurityUpdateDto;
import com.am.marketdata.service.model.security.SecurityDocument;
import com.am.marketdata.service.repo.SecurityRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for bulk updating securities from CSV/Excel files
 */
@Service
@RequiredArgsConstructor
public class SecurityBulkUpdateService {

    private final AppLogger log = AppLogger.getLogger();
    private final SecurityRepository securityRepository;

    /**
     * Process bulk update from uploaded file - WITH BATCH PROCESSING
     */
    public BulkUpdateResponse processBulkUpdate(
            MultipartFile file,
            Set<String> fieldsToUpdate,
            BulkUpdateRequest.MatchingStrategy strategy,
            boolean dryRun) {

        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        List<UpdateResult> results = new ArrayList<>();

        log.info("processBulkUpdate", "========== BULK UPDATE STARTED ==========");
        log.info("processBulkUpdate", "Strategy: " + strategy + ", DryRun: " + dryRun + ", Fields: " + fieldsToUpdate);

        try {
            // STAGE 1: Parse file
            log.info("processBulkUpdate", "STAGE 1: Parsing file...");
            List<SecurityUpdateDto> updates = parseFile(file);
            log.info("processBulkUpdate", "STAGE 1 COMPLETE: Parsed " + updates.size() + " records from file");

            // STAGE 2: Process in batches of 100
            log.info("processBulkUpdate", "STAGE 2: Processing in batches of 100...");
            int batchSize = 100;
            int totalBatches = (int) Math.ceil(updates.size() / (double) batchSize);

            for (int i = 0; i < updates.size(); i += batchSize) {
                int batchNum = (i / batchSize) + 1;
                int endIdx = Math.min(i + batchSize, updates.size());
                List<SecurityUpdateDto> batch = updates.subList(i, endIdx);

                long batchStartTime = System.currentTimeMillis();
                log.info("processBulkUpdate", String.format("Processing Batch %d/%d (%d records)...",
                        batchNum, totalBatches, batch.size()));

                // Process this batch
                List<UpdateResult> batchResults = processBatch(batch, fieldsToUpdate, strategy, dryRun);
                results.addAll(batchResults);

                // Log batch statistics
                long batchDuration = System.currentTimeMillis() - batchStartTime;
                int batchMatched = (int) batchResults.stream()
                        .filter(r -> r.getStatus() == MatchStatus.MATCHED || r.getStatus() == MatchStatus.UPDATED)
                        .count();
                int batchUpdated = (int) batchResults.stream()
                        .filter(r -> r.getStatus() == MatchStatus.UPDATED)
                        .count();
                int batchNotFound = (int) batchResults.stream()
                        .filter(r -> r.getStatus() == MatchStatus.NOT_FOUND)
                        .count();

                log.info("processBulkUpdate", String.format(
                        "Batch %d/%d COMPLETE: Matched: %d, Updated: %d, Not Found: %d, Duration: %.2fs",
                        batchNum, totalBatches, batchMatched, batchUpdated, batchNotFound, batchDuration / 1000.0));
            }

            log.info("processBulkUpdate", "STAGE 2 COMPLETE: Processed all batches");

            // STAGE 3: Build response
            log.info("processBulkUpdate", "STAGE 3: Building response...");
            BulkUpdateResponse response = buildResponse(results, errors);

            long duration = System.currentTimeMillis() - startTime;
            log.info("processBulkUpdate", "========== BULK UPDATE COMPLETE ==========");
            log.info("processBulkUpdate", String.format(
                    "Total: %d, Matched: %d, Updated: %d, Not Found: %d, Failed: %d, Duration: %.2fs",
                    response.getTotalRecords(), response.getMatchedRecords(), response.getUpdatedRecords(),
                    response.getTotalRecords() - response.getMatchedRecords() - response.getFailedRecords(),
                    response.getFailedRecords(), duration / 1000.0));

            return response;

        } catch (Exception e) {
            log.error("processBulkUpdate", "FATAL ERROR during bulk update", e);
            errors.add("Fatal error: " + e.getMessage());
            return BulkUpdateResponse.builder()
                    .totalRecords(0)
                    .matchedRecords(0)
                    .updatedRecords(0)
                    .failedRecords(1)
                    .results(new ArrayList<>())
                    .errors(errors)
                    .build();
        }
    }

    /**
     * Process a batch of records with BULK DB query and in-memory matching
     */
    private List<UpdateResult> processBatch(
            List<SecurityUpdateDto> batch,
            Set<String> fieldsToUpdate,
            BulkUpdateRequest.MatchingStrategy strategy,
            boolean dryRun) {

        List<UpdateResult> results = new ArrayList<>();

        // Extract all symbols/ISINs from this batch
        List<String> symbols = batch.stream()
                .map(SecurityUpdateDto::getSymbol)
                .filter(s -> s != null && !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        List<String> isins = batch.stream()
                .map(SecurityUpdateDto::getIsin)
                .filter(i -> i != null && !i.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        // Fetch all matching securities in ONE query per batch
        Map<String, SecurityDocument> securityMap = fetchSecuritiesForBatch(symbols, isins, strategy);

        // Process each record in the batch using the pre-fetched map
        for (SecurityUpdateDto dto : batch) {
            try {
                UpdateResult result = processRecordWithMap(dto, securityMap, fieldsToUpdate, strategy, dryRun);
                results.add(result);
            } catch (Exception e) {
                log.error("processBatch", "Error processing row " + dto.getRowNumber() +
                        " (Symbol: " + dto.getSymbol() + ")", e);
                results.add(UpdateResult.builder()
                        .symbol(dto.getSymbol())
                        .isin(dto.getIsin())
                        .status(MatchStatus.ERROR)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        return results;
    }

    /**
     * Fetch all securities for a batch in ONE query (performance optimization)
     */
    private Map<String, SecurityDocument> fetchSecuritiesForBatch(
            List<String> symbols,
            List<String> isins,
            BulkUpdateRequest.MatchingStrategy strategy) {

        Map<String, SecurityDocument> map = new HashMap<>();

        switch (strategy) {
            case STRICT_SYMBOL:
            case LOOSE_SYMBOL:
                if (!symbols.isEmpty()) {
                    List<SecurityDocument> docs = securityRepository.findBySymbolIn(symbols);
                    for (SecurityDocument doc : docs) {
                        String key = strategy == BulkUpdateRequest.MatchingStrategy.STRICT_SYMBOL
                                ? doc.getKey().getSymbol()
                                : doc.getKey().getSymbol().toUpperCase();
                        // For duplicates, first one wins
                        map.putIfAbsent(key, doc);
                    }
                }
                break;

            case STRICT_ISIN:
            case LOOSE_ISIN:
                if (!isins.isEmpty()) {
                    for (String isin : isins) {
                        List<SecurityDocument> docs = securityRepository.findByKeyIsin(isin);
                        if (!docs.isEmpty()) {
                            String key = strategy == BulkUpdateRequest.MatchingStrategy.STRICT_ISIN
                                    ? isin
                                    : isin.toUpperCase();
                            map.putIfAbsent(key, docs.get(0));
                        }
                    }
                }
                break;

            case STRICT_BOTH:
                if (!symbols.isEmpty()) {
                    List<SecurityDocument> docs = securityRepository.findBySymbolIn(symbols);
                    for (SecurityDocument doc : docs) {
                        String key = doc.getKey().getSymbol() + ":" + doc.getKey().getIsin();
                        map.putIfAbsent(key, doc);
                    }
                }
                break;
        }

        return map;
    }

    /**
     * Process a single record using pre-fetched security map (no DB query here)
     */
    private UpdateResult processRecordWithMap(
            SecurityUpdateDto dto,
            Map<String, SecurityDocument> securityMap,
            Set<String> fieldsToUpdate,
            BulkUpdateRequest.MatchingStrategy strategy,
            boolean dryRun) {

        // Find matching security from map
        SecurityDocument existing = matchSecurityFromMap(dto, securityMap, strategy);

        if (existing == null) {
            return UpdateResult.builder()
                    .symbol(dto.getSymbol())
                    .isin(dto.getIsin())
                    .status(MatchStatus.NOT_FOUND)
                    .errorMessage("No matching security found")
                    .build();
        }

        // Apply updates
        Map<String, Object> changes = new HashMap<>();
        boolean hasChanges = false;

        if (fieldsToUpdate != null && !fieldsToUpdate.isEmpty()) {
            hasChanges = applyUpdates(existing, dto, fieldsToUpdate, changes);
        }

        // Save if not dry run and has changes
        if (!dryRun && hasChanges) {
            securityRepository.save(existing);
            return UpdateResult.builder()
                    .symbol(existing.getKey().getSymbol())
                    .isin(existing.getKey().getIsin())
                    .status(MatchStatus.UPDATED)
                    .changes(changes)
                    .build();
        }

        MatchStatus status = hasChanges ? MatchStatus.MATCHED : MatchStatus.SKIPPED;
        return UpdateResult.builder()
                .symbol(existing.getKey().getSymbol())
                .isin(existing.getKey().getIsin())
                .status(status)
                .changes(changes)
                .build();
    }

    /**
     * Match security from pre-fetched map based on strategy
     */
    private SecurityDocument matchSecurityFromMap(
            SecurityUpdateDto dto,
            Map<String, SecurityDocument> securityMap,
            BulkUpdateRequest.MatchingStrategy strategy) {

        String key = null;

        switch (strategy) {
            case STRICT_SYMBOL:
                key = dto.getSymbol();
                break;
            case LOOSE_SYMBOL:
                key = dto.getSymbol() != null ? dto.getSymbol().toUpperCase() : null;
                break;
            case STRICT_ISIN:
                key = dto.getIsin();
                break;
            case LOOSE_ISIN:
                key = dto.getIsin() != null ? dto.getIsin().toUpperCase() : null;
                break;
            case STRICT_BOTH:
                if (dto.getSymbol() != null && dto.getIsin() != null) {
                    key = dto.getSymbol() + ":" + dto.getIsin();
                }
                break;
        }

        return key != null ? securityMap.get(key) : null;
    }

    /**
     * Apply selective field updates
     */
    private boolean applyUpdates(
            SecurityDocument existing,
            SecurityUpdateDto dto,
            Set<String> fieldsToUpdate,
            Map<String, Object> changes) {
        boolean hasChanges = false;

        if (existing.getMetadata() == null) {
            existing.setMetadata(new SecurityDocument.SecurityMetadata());
        }

        if (fieldsToUpdate.contains("companyName") && StringUtils.isNotBlank(dto.getCompanyName())) {
            existing.getMetadata().setCompanyName(dto.getCompanyName());
            changes.put("companyName", dto.getCompanyName());
            hasChanges = true;
        }

        if (fieldsToUpdate.contains("sector") && StringUtils.isNotBlank(dto.getSector())) {
            existing.getMetadata().setSector(dto.getCompanyName());
            changes.put("sector", dto.getSector());
            hasChanges = true;
        }

        if (fieldsToUpdate.contains("industry") && StringUtils.isNotBlank(dto.getIndustry())) {
            existing.getMetadata().setIndustry(dto.getIndustry());
            changes.put("industry", dto.getIndustry());
            hasChanges = true;
        }

        if (fieldsToUpdate.contains("marketCapValue") && dto.getMarketCapValue() != null) {
            existing.getMetadata().setMarketCapValue(dto.getMarketCapValue());
            changes.put("marketCapValue", dto.getMarketCapValue());
            hasChanges = true;
        }

        if (fieldsToUpdate.contains("marketCapType") && StringUtils.isNotBlank(dto.getMarketCapType())) {
            existing.getMetadata().setMarketCapType(dto.getMarketCapType());
            changes.put("marketCapType", dto.getMarketCapType());
            hasChanges = true;
        }

        if (fieldsToUpdate.contains("status") && StringUtils.isNotBlank(dto.getStatus())) {
            existing.getMetadata().setStatus(dto.getStatus());
            changes.put("status", dto.getStatus());
            hasChanges = true;
        }

        if (fieldsToUpdate.contains("group") && StringUtils.isNotBlank(dto.getGroup())) {
            existing.getMetadata().setGroup(dto.getGroup());
            changes.put("group", dto.getGroup());
            hasChanges = true;
        }

        return hasChanges;
    }

    /**
     * Parse file based on extension
     */
    private List<SecurityUpdateDto> parseFile(MultipartFile file) throws IOException, CsvException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name is null");
        }

        if (filename.endsWith(".csv")) {
            return parseCsv(file);
        } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return parseExcel(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format. Only CSV and Excel files are supported.");
        }
    }

    /**
     * Parse CSV file
     */
    private List<SecurityUpdateDto> parseCsv(MultipartFile file) throws IOException, CsvException {
        List<SecurityUpdateDto> results = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            List<String[]> rows = reader.readAll();
            if (rows.isEmpty()) {
                return results;
            }

            // First row is header
            String[] headers = rows.get(0);
            Map<String, Integer> columnMap = buildColumnMap(headers);

            // Process data rows
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                SecurityUpdateDto dto = mapRowToDto(row, columnMap, i + 1);
                results.add(dto);
            }
        }

        return results;
    }

    /**
     * Parse Excel file
     */
    private List<SecurityUpdateDto> parseExcel(MultipartFile file) throws IOException {
        List<SecurityUpdateDto> results = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // First row is header
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return results;
            }

            Map<String, Integer> columnMap = buildColumnMapFromExcel(headerRow);

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    SecurityUpdateDto dto = mapExcelRowToDto(row, columnMap, i + 1);
                    results.add(dto);
                }
            }
        }

        return results;
    }

    /**
     * Build column name to index map from CSV headers
     */
    private Map<String, Integer> buildColumnMap(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            map.put(headers[i].trim(), i);
        }
        return map;
    }

    /**
     * Build column name to index map from Excel headers
     */
    private Map<String, Integer> buildColumnMapFromExcel(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            map.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
        }
        return map;
    }

    /**
     * Map CSV row to DTO
     */
    private SecurityUpdateDto mapRowToDto(String[] row, Map<String, Integer> columnMap, int rowNumber) {
        SecurityUpdateDto dto = new SecurityUpdateDto();
        dto.setRowNumber(rowNumber);

        // Matching fields
        dto.setSymbol(getColumnValue(row, columnMap, "Security Id"));
        dto.setIsin(getColumnValue(row, columnMap, "ISIN No"));

        // Company name resolution: Issuer Name > Security Name
        String issuerName = getColumnValue(row, columnMap, "Issuer Name");
        String securityName = getColumnValue(row, columnMap, "Security Name");
        dto.setIssuerName(issuerName);
        dto.setSecurityName(securityName);
        dto.setCompanyName(StringUtils.isNotBlank(issuerName) ? issuerName : securityName);

        // Other fields
        dto.setSecurityCode(getColumnValue(row, columnMap, "Security Code"));
        dto.setSector(getColumnValue(row, columnMap, "Sector Name"));
        dto.setIndustry(getColumnValue(row, columnMap, "Industry"));
        dto.setStatus(getColumnValue(row, columnMap, "Status"));
        dto.setGroup(getColumnValue(row, columnMap, "Group"));
        dto.setFaceValue(getColumnValue(row, columnMap, "Face Value"));

        return dto;
    }

    /**
     * Map Excel row to DTO
     */
    private SecurityUpdateDto mapExcelRowToDto(Row row, Map<String, Integer> columnMap, int rowNumber) {
        SecurityUpdateDto dto = new SecurityUpdateDto();
        dto.setRowNumber(rowNumber);

        // Matching fields
        dto.setSymbol(getCellValue(row, columnMap, "Security Id"));
        dto.setIsin(getCellValue(row, columnMap, "ISIN No"));

        // Company name resolution
        String issuerName = getCellValue(row, columnMap, "Issuer Name");
        String securityName = getCellValue(row, columnMap, "Security Name");
        dto.setIssuerName(issuerName);
        dto.setSecurityName(securityName);
        dto.setCompanyName(StringUtils.isNotBlank(issuerName) ? issuerName : securityName);

        // Other fields
        dto.setSecurityCode(getCellValue(row, columnMap, "Security Code"));
        dto.setSector(getCellValue(row, columnMap, "Sector Name"));
        dto.setIndustry(getCellValue(row, columnMap, "Industry"));
        dto.setStatus(getCellValue(row, columnMap, "Status"));
        dto.setGroup(getCellValue(row, columnMap, "Group"));
        dto.setFaceValue(getCellValue(row, columnMap, "Face Value"));

        return dto;
    }

    /**
     * Get column value from CSV row
     */
    private String getColumnValue(String[] row, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName);
        if (index == null || index >= row.length) {
            return null;
        }
        String value = row[index].trim();
        return value.isEmpty() ? null : value;
    }

    /**
     * Get cell value from Excel row
     */
    private String getCellValue(Row row, Map<String, Integer> columnMap, String columnName) {
        Integer index = columnMap.get(columnName);
        if (index == null) {
            return null;
        }

        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    /**
     * Build response from results
     */
    private BulkUpdateResponse buildResponse(List<UpdateResult> results, List<String> errors) {
        int total = results.size();
        int matched = (int) results.stream()
                .filter(r -> r.getStatus() != MatchStatus.NOT_FOUND && r.getStatus() != MatchStatus.ERROR).count();
        int updated = (int) results.stream().filter(r -> r.getStatus() == MatchStatus.UPDATED).count();
        int failed = (int) results.stream().filter(r -> r.getStatus() == MatchStatus.ERROR).count();

        return BulkUpdateResponse.builder()
                .totalRecords(total)
                .matchedRecords(matched)
                .updatedRecords(updated)
                .failedRecords(failed)
                .results(results)
                .errors(errors)
                .build();
    }
}
