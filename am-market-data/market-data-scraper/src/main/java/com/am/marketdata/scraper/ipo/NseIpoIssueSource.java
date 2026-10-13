package com.am.marketdata.scraper.ipo;

import com.am.marketdata.common.ipo.IpoIssue;
import com.am.marketdata.common.ipo.IpoIssueSource;
import com.am.marketdata.common.ipo.IpoSubscription;
import com.am.marketdata.scraper.client.NSEApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market-data.ipo.source", havingValue = "nse", matchIfMissing = true)
public class NseIpoIssueSource implements IpoIssueSource {

    private final NSEApiClient nseApiClient;

    @Override
    public String sourceId() {
        return "NSE";
    }

    @Override
    public List<IpoIssue> fetchPast() {
        JsonNode root = nseApiClient.getIpoPastIssues();
        return NseIpoMapper.mapPast(root);
    }

    @Override
    public List<IpoIssue> fetchCurrent() {
        JsonNode root = nseApiClient.getIpoCurrentIssues();
        return NseIpoMapper.mapCurrent(root);
    }

    @Override
    public List<IpoIssue> fetchUpcoming() {
        JsonNode root = nseApiClient.getIpoUpcomingIssues();
        return NseIpoMapper.mapUpcoming(root);
    }

    @Override
    public Optional<IpoSubscription> fetchSubscription(String symbol, String series) {
        try {
            JsonNode root = nseApiClient.getIpoBidDetails(symbol, series == null ? "EQ" : series);
            return NseIpoMapper.mapSubscription(symbol, series == null ? "EQ" : series, root);
        } catch (Exception e) {
            log.warn("IPO bid details failed for {} {}: {}", symbol, series, e.getMessage());
            return Optional.empty();
        }
    }
}
