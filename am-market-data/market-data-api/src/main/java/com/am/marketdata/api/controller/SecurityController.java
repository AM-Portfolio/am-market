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

    @GetMapping("/search")
    @Operation(summary = "Search securities by symbol or ISIN")
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
}
