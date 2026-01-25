package com.am.marketdata.api.controller;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.marketdata.api.service.StockIndicesService;
import com.am.marketdata.service.SecurityService;
import com.am.marketdata.service.dto.SecuritySearchRequest;
import com.am.marketdata.service.model.security.SecurityDocument;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/securities")
@RequiredArgsConstructor
@Tag(name = "Security Explorer", description = "endpoints for exploring security details")
public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);

    private final SecurityService securityService;
    private final StockIndicesService stockIndicesService;
    private final com.am.marketdata.service.SecurityBulkUpdateService securityBulkUpdateService;

    @GetMapping("/search")
    @Operation(summary = "Fuzzy search securities by symbol, ISIN, or company name", description = "Search across symbol, ISIN, and company name using case-insensitive fuzzy matching. "
            +
            "Returns all securities that partially match the query in any of these fields.")
    public ResponseEntity<List<SecurityDocument>> search(@RequestParam String query) {
        return ResponseEntity.ok(securityService.search(query));
    }

    @PostMapping("/search")
    @Operation(summary = "Advanced search securities with filters")
    public ResponseEntity<List<SecurityDocument>> searchAdvanced(@RequestBody SecuritySearchRequest request) {
        // Handle Index-based retrieval
        if (request.getIndex() != null && !request.getIndex().isEmpty()) {
            try {
                StockIndicesMarketData indexData = stockIndicesService.getLatestIndexData(request.getIndex());
                if (indexData != null && indexData.getData() != null) {
                    List<String> symbols = new java.util.ArrayList<>();
                    indexData.getData().forEach(s -> {
                        if (s.getSymbol() != null)
                            symbols.add(s.getSymbol());
                    });

                    if (request.getSymbols() == null) {
                        request.setSymbols(symbols);
                    } else {
                        request.getSymbols().addAll(symbols);
                    }
                } else {
                    log.warn("Index not found or empty: {}", request.getIndex());
                    // If index is specified but not found, and no other filters, return empty
                    if (request.getQuery() == null && request.getSector() == null && request.getIndustry() == null) {
                        return ResponseEntity.ok(List.of());
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching index data for {}", request.getIndex(), e);
            }
        }

        return ResponseEntity.ok(securityService.search(request));
    }

    @PostMapping(value = "/bulk-update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk update securities from CSV or Excel file", description = "Upload a CSV or Excel file to update security metadata. "
            +
            "Supports selective field updates and matching strategies.")
    public ResponseEntity<com.am.marketdata.common.dto.BulkUpdateResponse> bulkUpdate(
            @RequestPart("file") @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "CSV or Excel file containing security data", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(required = false) java.util.Set<String> fieldsToUpdate,
            @RequestParam(defaultValue = "STRICT_SYMBOL") com.am.marketdata.common.dto.BulkUpdateRequest.MatchingStrategy matchingStrategy,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        com.am.marketdata.common.dto.BulkUpdateResponse response = securityBulkUpdateService.processBulkUpdate(file,
                fieldsToUpdate, matchingStrategy, dryRun);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/batch-search")
    @Operation(summary = "Batch search securities", description = "Search for multiple securities at once. Useful for enriching ETF holdings data with ISINs.")
    public ResponseEntity<com.am.marketdata.common.dto.BatchSearchResponse> batchSearch(
            @RequestBody com.am.marketdata.common.dto.BatchSearchRequest request) {
        com.am.marketdata.common.dto.BatchSearchResponse response = securityService.batchSearch(request);
        return ResponseEntity.ok(response);
    }
}
