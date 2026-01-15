package com.am.marketdata.provider.upstox.converter;

import com.am.marketdata.common.model.OHLCQuote;
import com.upstox.feeder.MarketUpdateV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Converter to transform Upstox V3 messages to organization-level common model
 * (OHLCQuote).
 * Extracts LTP and OHLC from MarketUpdateV3.Feed objects.
 * 
 * IMPORTANT: Service layer should ONLY receive Map<String, OHLCQuote>, never
 * provider-specific models.
 */
@Slf4j
@Component
public class UpstoxV3FeedConverter {

    /**
     * Convert Upstox V3 message to common OHLCQuote map (organization-level model)
     * 
     * @param updateV3 Upstox V3 message
     * @return Map of instrumentKey -> OHLCQuote (common model)
     */
    public Map<String, OHLCQuote> convert(MarketUpdateV3 updateV3) {
        Map<String, OHLCQuote> results = new HashMap<>();

        if (updateV3 == null) {
            return results;
        }

        try {
            Map<String, MarketUpdateV3.Feed> feeds = updateV3.getFeeds();

            if (feeds == null || feeds.isEmpty()) {
                log.debug("No feeds in V3 message (might be market_info or heartbeat)");
                return results;
            }

            log.debug("Processing {} feeds from MarketUpdateV3", feeds.size());

            // Extract data from each feed
            feeds.forEach((instrumentKey, feed) -> {
                try {
                    OHLCQuote quote = extractQuote(feed);
                    if (quote != null && quote.getLastPrice() > 0) {
                        results.put(instrumentKey, quote);
                        log.debug("Extracted quote for {}: LTP={}", instrumentKey, quote.getLastPrice());
                    }
                } catch (Exception e) {
                    log.error("Error processing feed for key {}: {}", instrumentKey, e.getMessage());
                }
            });

        } catch (Exception e) {
            log.error("Error converting V3 to common OHLCQuote", e);
        }

        return results;
    }

    /**
     * Extract OHLCQuote from MarketUpdateV3.Feed
     * Structure: Feed -> fullFeed -> marketFF -> ltpc -> ltp
     */
    private OHLCQuote extractQuote(MarketUpdateV3.Feed feed) {
        if (feed == null) {
            return null;
        }

        OHLCQuote quote = new OHLCQuote();

        try {
            // Navigate: Feed -> fullFeed -> marketFF -> ltpc
            MarketUpdateV3.FullFeed fullFeed = feed.getFullFeed();
            if (fullFeed != null) {
                MarketUpdateV3.MarketFullFeed marketFF = fullFeed.getMarketFF();
                if (marketFF != null) {
                    MarketUpdateV3.LTPC ltpc = marketFF.getLtpc();
                    if (ltpc != null) {
                        // Extract LTP (Last Traded Price)
                        double ltp = ltpc.getLtp();
                        if (ltp > 0) {
                            quote.setLastPrice(ltp);
                        }

                        // Extract previous close
                        double cp = ltpc.getCp();
                        if (cp > 0) {
                            quote.setPreviousClose(cp);
                        }
                    }

                    // Extract OHLC if available
                    MarketUpdateV3.MarketOHLC marketOHLC = marketFF.getMarketOHLC();
                    if (marketOHLC != null && marketOHLC.getOhlc() != null && !marketOHLC.getOhlc().isEmpty()) {
                        MarketUpdateV3.OHLC ohlc = marketOHLC.getOhlc().get(0); // First interval (usually 1d)

                        OHLCQuote.OHLC ohlcData = OHLCQuote.OHLC.builder()
                                .open(ohlc.getOpen())
                                .high(ohlc.getHigh())
                                .low(ohlc.getLow())
                                .close(ohlc.getClose())
                                .build();

                        quote.setOhlc(ohlcData);
                    }
                }
            }

        } catch (Exception e) {
            log.debug("Error extracting quote fields: {}", e.getMessage());
        }

        return quote;
    }
}
