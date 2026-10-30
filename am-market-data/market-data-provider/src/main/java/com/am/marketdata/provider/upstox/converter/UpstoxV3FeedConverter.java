package com.am.marketdata.provider.upstox.converter;

import com.am.marketdata.common.model.OHLCQuote;
import com.upstox.feeder.MarketUpdateV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

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
     * Structure: Feed -> fullFeed -> marketFF/indexFF -> ltpc -> ltp
     */
    private OHLCQuote extractQuote(MarketUpdateV3.Feed feed) {
        if (feed == null) {
            return null;
        }

        OHLCQuote quote = new OHLCQuote();

        try {
            // Upstox uses marketFF for equities and indexFF for indices. Both
            // expose the same LTPC/OHLC shape, so normalize them into one
            // organization-level quote before the provider-specific layer ends.
            MarketUpdateV3.FullFeed fullFeed = feed.getFullFeed();
            if (fullFeed == null) {
                return null;
            }

            MarketUpdateV3.LTPC ltpc = null;
            MarketUpdateV3.MarketOHLC marketOHLC = null;
            if (fullFeed.getMarketFF() != null) {
                ltpc = fullFeed.getMarketFF().getLtpc();
                marketOHLC = fullFeed.getMarketFF().getMarketOHLC();
            } else if (fullFeed.getIndexFF() != null) {
                ltpc = fullFeed.getIndexFF().getLtpc();
                marketOHLC = fullFeed.getIndexFF().getMarketOHLC();
            }

            if (ltpc == null) {
                return null;
            }

            double ltp = ltpc.getLtp();
            if (ltp <= 0) {
                return null;
            }
            quote.setLastPrice(ltp);

            double cp = ltpc.getCp();
            if (cp > 0) {
                quote.setPreviousClose(cp);
            }
            if (ltpc.getLtt() > 0) {
                quote.setQuoteTimestamp(Instant.ofEpochMilli(ltpc.getLtt()));
            }

            // OHLC is optional in some LTPC/index updates. Keep the LTP quote
            // rather than dropping the entire update when OHLC is absent.
            if (marketOHLC != null && marketOHLC.getOhlc() != null && !marketOHLC.getOhlc().isEmpty()) {
                MarketUpdateV3.OHLC ohlc = marketOHLC.getOhlc().get(0);
                OHLCQuote.OHLC ohlcData = OHLCQuote.OHLC.builder()
                        .open(ohlc.getOpen())
                        .high(ohlc.getHigh())
                        .low(ohlc.getLow())
                        .close(ohlc.getClose())
                        .build();
                quote.setOhlc(ohlcData);
            }

        } catch (Exception e) {
            log.debug("Error extracting quote fields: {}", e.getMessage());
        }

        return quote;
    }
}
