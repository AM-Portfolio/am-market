package com.am.marketdata.scripts;

import com.am.marketdata.service.model.security.SecurityDocument;
import com.mongodb.bulk.BulkWriteResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Migration script to populate companyName field in SecurityDocument
 * 
 * Data Source: am-market-data/data/Equity.csv
 * Column Mappings:
 *   - ISIN: column index 7 ("ISIN No")
 *   - Company Name: column index 3 ("Security Name") with fallback to index 1 ("Issuer Name")
 * 
 * Usage:
 *   1. Dry Run: java -jar ... --dry-run=true
 *   2. Execute: java -jar ... --dry-run=false
 */
@Slf4j
@Component
public class SecurityMigrationScript {

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String CSV_FILE_PATH = "data/Equity.csv";
    private static final int CSV_COLUMN_ISSUER_NAME = 1;     // "Issuer Name"
    private static final int CSV_COLUMN_SECURITY_ID = 2;     // "Security Id" (Symbol)
    private static final int CSV_COLUMN_SECURITY_NAME = 3;   // "Security Name"
    private static final int CSV_COLUMN_ISIN = 7;            // "ISIN No"

    /**
     * Main migration method
     * 
     * @param csvFilePath Path to Equity.csv file
     * @param dryRun If true, only logs changes without applying them
     */
    public MigrationResult migrateCompanyNames(String csvFilePath, boolean dryRun) {
        log.info("Starting company name migration. DryRun: {}", dryRun);
        
        // 1. Load ISIN → Company Name mapping from CSV
        Map<String, CompanyData> isinToCompanyData = loadCsvMapping(csvFilePath);
        log.info("Loaded {} ISIN mappings from CSV", isinToCompanyData.size());

        // 2. Fetch all securities WITHOUT company name from MongoDB
        Query query = Query.query(
            Criteria.where("metadata.company_name").exists(false)
        );
        List<SecurityDocument> securities = mongoTemplate.find(query, SecurityDocument.class);
        log.info("Found {} securities without company name in database", securities.size());

        // 3. Prepare bulk updates with STRICT validation
        BulkOperations bulkOps = mongoTemplate.bulkOps(
            BulkOperations.BulkMode.UNORDERED, 
            SecurityDocument.class
        );

        int matchCount = 0;
        int unmatchedCount = 0;
        int validationFailedCount = 0;
        StringBuilder unmatchedLog = new StringBuilder("Unmatched ISINs:\n");
        StringBuilder validationLog = new StringBuilder("Validation Failures:\n");

        for (SecurityDocument sec : securities) {
            if (sec.getKey() == null || sec.getKey().getIsin() == null || sec.getKey().getSymbol() == null) {
                log.warn("Security {} has no ISIN or SYMBOL, skipping", sec.getId());
                unmatchedCount++;
                continue;
            }

            String dbIsin = sec.getKey().getIsin();
            String dbSymbol = sec.getKey().getSymbol();
            
            CompanyData csvData = isinToCompanyData.get(dbIsin);

            if (csvData != null) {
                // CRITICAL VALIDATION: Match both SYMBOL and ISIN
                if (!csvData.getSymbol().equalsIgnoreCase(dbSymbol)) {
                    validationFailedCount++;
                    validationLog.append(String.format(
                        "  - MISMATCH: DB Symbol=%s, CSV Symbol=%s, ISIN=%s\n", 
                        dbSymbol, csvData.getSymbol(), dbIsin
                    ));
                    log.warn("Symbol mismatch for ISIN {}: DB={}, CSV={}", 
                        dbIsin, dbSymbol, csvData.getSymbol());
                    continue; // Skip this record - data inconsistency
                }
                
                // Validation passed - safe to update
                String companyName = csvData.getCompanyName();
                if (companyName != null && !companyName.trim().isEmpty()) {
                    Update update = new Update()
                        .set("metadata.company_name", companyName);
                    
                    bulkOps.updateOne(
                        Query.query(Criteria.where("_id").is(sec.getId())), 
                        update
                    );
                    matchCount++;
                    log.debug("Validated and matched: Symbol={}, ISIN={}, Company={}", 
                        dbSymbol, dbIsin, companyName);
                }
            } else {
                // ISIN not found in CSV (expected since CSV has only 6K vs 40K in DB)
                unmatchedCount++;
                unmatchedLog.append(String.format("  - Symbol: %s, ISIN: %s\n", dbSymbol, dbIsin));
            }
        }

        // 4. Execute or log results
        MigrationResult result = new MigrationResult();
        result.setTotalSecurities(securities.size());
        result.setMatchedCount(matchCount);
        result.setUnmatchedCount(unmatchedCount);
        result.setValidationFailedCount(validationFailedCount);

        if (!dryRun && matchCount > 0) {
            BulkWriteResult bulkResult = bulkOps.execute();
            result.setModifiedCount(bulkResult.getModifiedCount());
            log.info("Migration complete: {} documents updated", bulkResult.getModifiedCount());
        } else if (dryRun) {
            log.info("Dry run: {} documents would be updated", matchCount);
            result.setModifiedCount(0);
        } else {
            log.warn("No documents to update");
            result.setModifiedCount(0);
        }

        log.info("Migration Summary:");
        log.info("  Total securities: {}", result.getTotalSecurities());
        log.info("  Matched & Updated: {}", result.getMatchedCount());
        log.info("  Unmatched (Not in CSV): {}", result.getUnmatchedCount());
        log.info("  Validation Failed (Symbol Mismatch): {}", result.getValidationFailedCount());
        log.info("  Modified: {}", result.getModified Count());

        if (validationFailedCount > 0) {
            log.error(validationLog.toString());
        }
        
        if (unmatchedCount > 0 && unmatchedCount < 50) {
            log.warn(unmatchedLog.toString());
        }

        return result;
    }

    /**
     * Load ISIN to Company Data mapping from CSV file
     * 
     * @param filePath Path to Equity.csv
     * @return Map of ISIN -> CompanyData (symbol + company name)
     */
    private Map<String, CompanyData> loadCsvMapping(String filePath) {
        Map<String, CompanyData> mapping = new HashMap<>();
        Map<String, String> mapping = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; // Skip header row
                    continue;
                }

                // Handle quoted CSV fields
                String[] columns = parseCsvLine(line);

                if (columns.length > CSV_COLUMN_ISIN) {
                    String isin = columns[CSV_COLUMN_ISIN].trim();
                    String symbol = columns[CSV_COLUMN_SECURITY_ID].trim();
                    String companyName = columns[CSV_COLUMN_SECURITY_NAME].trim();

                    // Fallback to Issuer Name if Security Name is empty
                    if (companyName.isEmpty() && columns.length > CSV_COLUMN_ISSUER_NAME) {
                        companyName = columns[CSV_COLUMN_ISSUER_NAME].trim();
                    }

                    // Only add valid ISIN entries (skip "NA", blanks, etc.)
                    if (!isin.isEmpty() && 
                        !isin.equalsIgnoreCase("NA") && 
                        !isin.contains("INF189") &&  // Skip mutual funds
                        !symbol.isEmpty() &&
                        !companyName.isEmpty()) {
                        mapping.put(isin, new CompanyData(symbol, companyName));
                    }
                }
            }

            log.info("Loaded {} ISIN to Company Name mappings from {}", mapping.size(), filePath);
        } catch (IOException e) {
            log.error("Error reading CSV file: {}", filePath, e);
            throw new RuntimeException("Failed to load CSV mapping", e);
        }

        return mapping;
    }

    /**
     * Parse CSV line handling quoted fields (e.g., "Murpher, Wilson & Co.")
     */
    private String[] parseCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }

    /**
     * Helper class to store company data from CSV
     */
    private static class CompanyData {
        private final String symbol;
        private final String companyName;

        public CompanyData(String symbol, String companyName) {
            this.symbol = symbol;
            this.companyName = companyName;
        }

        public String getSymbol() { return symbol; }
        public String getCompanyName() { return companyName; }
    }

    /**
     * Result object for migration statistics
     */
    public static class MigrationResult {
        private int totalSecurities;
        private int matchedCount;
        private int unmatchedCount;
        private int validationFailedCount;
        private int modifiedCount;

        // Getters and setters
        public int getTotalSecurities() { return totalSecurities; }
        public void setTotalSecurities(int totalSecurities) { this.totalSecurities = totalSecurities; }

        public int getMatchedCount() { return matchedCount; }
        public void setMatchedCount(int matchedCount) { this.matchedCount = matchedCount; }

        public int getUnmatchedCount() { return unmatchedCount; }
        public void setUnmatchedCount(int unmatchedCount) { this.unmatchedCount = unmatchedCount; }

        public int getValidationFailedCount() { return validationFailedCount; }
        public void setValidationFailedCount(int validationFailedCount) { this.validationFailedCount = validationFailedCount; }

        public int getModifiedCount() { return modifiedCount; }
        public void setModifiedCount(int modifiedCount) { this.modifiedCount = modifiedCount; }
    }

    /**
     * Main entry point for command-line execution
     */
    public static void main(String[] args) {
        boolean dryRun = true;
        String csvFile = CSV_FILE_PATH;

        // Parse command line arguments
        for (String arg : args) {
            if (arg.startsWith("--file=")) {
                csvFile = arg.substring(7);
            } else if (arg.startsWith("--dry-run=")) {
                dryRun = Boolean.parseBoolean(arg.substring(10));
            }
        }

        log.info("Executing migration with file: {}, dryRun: {}", csvFile, dryRun);

        // Note: This would typically be run via Spring Boot CommandLineRunner
        // or using ApplicationContext in a proper application
        // For now, this serves as documentation of usage
        System.out.println("To run this script, use Spring Boot's CommandLineRunner");
        System.out.println("Example: @SpringBootApplication with implements CommandLineRunner");
    }
}
