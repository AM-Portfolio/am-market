package com.am.marketdata.service.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParserApiClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${parser.api.url:http://localhost:8000}")
    private String parserApiUrl;

    /**
     * Fetch all ETF symbols from the parser service.
     * Calls GET /v1/search?query=*&limit=2000
     */
    public List<String> getAllEtfSymbols() {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(parserApiUrl)
                    .path("/v1/search")
                    .queryParam("query", "*")
                    .queryParam("limit", 2000)
                    .toUriString();

            log.debug("Calling Parser API: {}", url);
            ParserSearchResponse response = restTemplate.getForObject(url, ParserSearchResponse.class);

            if (response != null && response.getEtfs() != null) {
                log.info("Retrieved {} ETFs from parser", response.getTotalFound());
                return response.getEtfs().stream()
                        .map(EtfSummary::getSymbol)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to fetch ETF symbols from parser: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Fetch holdings (constituents) for a specific ETF symbol.
     * Calls GET /v1/holdings/{symbol}
     */
    public List<String> getEtfConstituents(String symbol) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(parserApiUrl)
                    .path("/v1/holdings/{symbol}")
                    .buildAndExpand(symbol)
                    .toUriString();

            log.debug("Calling Parser API for holdings: {}", url);
            // using Map return type for simplicity as structure is complex
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("holdings")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> holdings = (List<Map<String, Object>>) response.get("holdings");
                if (holdings != null) {
                    log.info("Retrieved {} holdings for {}", holdings.size(), symbol);
                    return holdings.stream()
                            .map(h -> (String) h.get("stock_name"))
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch constituents for {}: {}", symbol, e.getMessage());
        }
        return Collections.emptyList();
    }

    // DTOs
    @Data
    public static class ParserSearchResponse {
        private String query;
        private int totalFound;
        private List<EtfSummary> etfs;
    }

    @Data
    public static class EtfSummary {
        private String symbol;
        private String name;
        private String isin;
    }
}
