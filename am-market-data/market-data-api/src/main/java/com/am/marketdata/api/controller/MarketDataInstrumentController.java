package com.am.marketdata.api.controller;

import com.am.marketdata.service.SecurityService;
import com.am.marketdata.service.model.security.SecurityDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/market-data/instruments")
@RequiredArgsConstructor
@Tag(name = "Market Data Instruments", description = "APIs for internal instrument resolution by ISIN")
public class MarketDataInstrumentController {

    private final SecurityService securityService;

    @GetMapping("/isin/{isin}")
    @Operation(summary = "Resolve trading symbol by ISIN")
    public ResponseEntity<Map<String, String>> resolveTickerByIsin(@PathVariable String isin) {
        log.info("Resolving ticker by ISIN: {}", isin);
        String upperIsin = isin.trim().toUpperCase();
        
        List<SecurityDocument> docs = securityService.findBySymbols(List.of(upperIsin));
        
        Map<String, String> response = new HashMap<>();
        if (docs != null && !docs.isEmpty()) {
            SecurityDocument doc = docs.get(0);
            if (doc.getKey() != null && doc.getKey().getSymbol() != null) {
                response.put("symbol", doc.getKey().getSymbol());
            }
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/isin")
    @Operation(summary = "Batch resolve trading symbols by ISINs")
    public ResponseEntity<Map<String, String>> resolveTickersByIsins(@RequestBody List<String> isins) {
        log.info("Resolving {} tickers by ISINs", isins.size());
        
        List<String> upperIsins = isins.stream()
                .filter(i -> i != null && !i.trim().isEmpty())
                .map(i -> i.trim().toUpperCase())
                .collect(Collectors.toList());
                
        List<SecurityDocument> docs = securityService.findBySymbols(upperIsins);
        
        Map<String, String> response = new HashMap<>();
        if (docs != null) {
            for (SecurityDocument doc : docs) {
                if (doc.getKey() != null && doc.getKey().getIsin() != null && doc.getKey().getSymbol() != null) {
                    response.put(doc.getKey().getIsin(), doc.getKey().getSymbol());
                }
            }
        }
        
        return ResponseEntity.ok(response);
    }
}
