package com.am.marketdata.scheduler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Service for retrieving ISIN (International Securities Identification Number) data
 * Used by schedulers to get the list of symbols to process
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ISINService {

    @Value("${scheduler.symbols.default:RELIANCE}")
    private String defaultSymbols;

    /**
     * Find all distinct ISINs for processing
     * In a real implementation, this would likely fetch from a database
     * 
     * @return List of ISIN symbols
     */
    public List<String> findDistinctIsins() {
        // In a real implementation, this would likely fetch from a database
        // For now, we'll return a default list from configuration
        String[] symbols = defaultSymbols.split(",");
        log.debug("Retrieved {} ISINs for processing", symbols.length);
        return List.of(symbols);
    }
}
