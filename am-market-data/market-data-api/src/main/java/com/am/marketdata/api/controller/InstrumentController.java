package com.am.marketdata.api.controller;

import com.am.marketdata.service.dto.InstrumentSearchCriteria;
import com.am.marketdata.service.provider.InstrumentDataProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/v1/instruments")
@RequiredArgsConstructor
@Tag(name = "Instrument Management", description = "APIs for managing and searching financial instruments")
public class InstrumentController {

    private final List<InstrumentDataProvider> dataProviders;

    // Default path as per user context
    private static final String DEFAULT_FILE_PATH = "data/upstock/instrument/NSE.json";

    private InstrumentDataProvider getProvider(String providerName) {
        String name = (providerName == null || providerName.isEmpty()) ? "UPSTOX" : providerName.toUpperCase();
        return dataProviders.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Provider not supported: " + name));
    }

    @Operation(summary = "Update Instruments from File", description = "Triggers an update of instruments from the local JSON file (NSE.json). Uses streaming to handle large files.")
    @PostMapping("/update")
    public ResponseEntity<String> updateInstruments(
            @RequestParam(required = false) String filePath,
            @RequestParam(required = false, defaultValue = "UPSTOX") String provider) {
        String path = (filePath != null && !filePath.isEmpty()) ? filePath : DEFAULT_FILE_PATH;
        try {
            getProvider(provider).updateInstruments(path);
            return ResponseEntity.ok("Instrument update triggered successfully for " + provider + " from: " + path);
        } catch (Exception e) {
            log.error("Failed to update instruments from file: {}", path, e);
            return ResponseEntity.internalServerError().body("Failed to update instruments: " + e.getMessage());
        }
    }

    @Operation(summary = "Search Instruments", description = "Search for instruments using criteria: list of symbols ('gym balls'), exchanges, and instrument types. Supports semantic text search combined with filters.")
    @PostMapping("/search")
    public ResponseEntity<List<?>> searchInstruments(
            @RequestBody InstrumentSearchCriteria criteria) {
        log.info("Searching instruments with criteria: {}", criteria);
        try {
            List<?> results = getProvider(criteria.getProvider()).searchInstruments(criteria);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
