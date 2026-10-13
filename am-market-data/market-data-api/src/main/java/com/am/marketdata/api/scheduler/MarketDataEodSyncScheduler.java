package com.am.marketdata.api.scheduler;

import com.am.marketdata.api.service.StockIndicesService;
import com.am.marketdata.common.log.AppLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketDataEodSyncScheduler {

    private final AppLogger log = AppLogger.getLogger();
    private final StockIndicesService stockIndicesService;
    private final com.am.marketdata.scraper.config.NSEIndicesConfig nseIndicesConfig;
    private final com.am.marketdata.service.MarketDataService marketDataService;
    private final java.util.Optional<com.am.marketdata.service.SymbolOrchestratorService> symbolOrchestratorService;

    /**
     * Runs Monday to Friday at 3:40 PM IST (15:40) to sync final EOD prices 
     * from Redis back to MongoDB for permanent storage.
     * 
     * Cron expression: "0 40 15 * * MON-FRI"
     * TimeZone: "Asia/Kolkata"
     */
    @Scheduled(cron = "0 40 15 * * MON-FRI", zone = "Asia/Kolkata")
    @com.am.scheduler.annotation.TrackedAndLockedScheduler(name = "marketEodSyncJob", lockAtMostFor = "15m", lockAtLeastFor = "1m")
    public void syncEodIndexPrices() {
        String methodName = "syncEodIndexPrices";
        log.info(methodName, "Starting EOD Index Price Sync Scheduler to persist Redis prices to MongoDB");

        try {
            // Dynamically load all configured index names from application yml/properties
            List<String> allIndices = new java.util.ArrayList<>();
            if (nseIndicesConfig.getBroadMarketIndices() != null) {
                allIndices.addAll(nseIndicesConfig.getBroadMarketIndices());
            }
            if (nseIndicesConfig.getSectorIndices() != null) {
                allIndices.addAll(nseIndicesConfig.getSectorIndices());
            }

            // Also ensure both plural and singular aliases of Nifty Fin Services are synced
            if (allIndices.contains("NIFTY FIN SERVICES") && !allIndices.contains("NIFTY FIN SERVICE")) {
                allIndices.add("NIFTY FIN SERVICE");
            }

            if (allIndices.isEmpty()) {
                log.warn(methodName, "No indices found in configuration. Falling back to default list.");
                allIndices = Arrays.asList(
                    "NIFTY 50", "NIFTY BANK", "NIFTY FIN SERVICE", "NIFTY IT", 
                    "NIFTY 100", "NIFTY 200", "NIFTY 500", "NIFTY MIDCAP 100", 
                    "NIFTY SMLCAP 100"
                );
            }

            // 1. Force a refresh to fetch the official exchange-adjusted post-market closing prices
            // for the index headers and update MongoDB.
            List<com.am.common.investment.model.stockindice.StockIndicesMarketData> indexResults =
                    stockIndicesService.getLatestIndicesData(allIndices, true);
            log.info(methodName, "Successfully completed EOD Index Price Sync for " + allIndices.size() + " indices.");

            // 2. Collect all unique constituent symbols across the synced indices
            java.util.Set<String> uniqueSymbols = new java.util.HashSet<>();
            if (indexResults != null) {
                for (com.am.common.investment.model.stockindice.StockIndicesMarketData idx : indexResults) {
                    if (idx.getData() != null) {
                        for (com.am.common.investment.model.stockindice.StockData stock : idx.getData()) {
                            if (stock.getSymbol() != null && !stock.getSymbol().trim().isEmpty()) {
                                uniqueSymbols.add(stock.getSymbol().trim().toUpperCase());
                            }
                        }
                    }
                }
            }

            // 3. Include all active user portfolio holdings so small-caps are pre-cached
            symbolOrchestratorService.ifPresent(orchestrator -> {
                try {
                    java.util.Set<String> portfolioSymbols = orchestrator.findDistinctSymbols();
                    if (portfolioSymbols != null) {
                        for (String sym : portfolioSymbols) {
                            if (sym != null && !sym.isBlank() && !sym.startsWith("GLOBAL_")) {
                                String clean = sym.contains(":") ? sym.substring(sym.indexOf(":") + 1) : sym;
                                uniqueSymbols.add(clean.trim().toUpperCase());
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn(methodName, "Failed to append portfolio symbols to EOD sync: " + ex.getMessage());
                }
            });

            // 4. Batch fetch and cache the official closing quotes in ONE single Upstox pass.
            // This hydrates Redis with 7-day TTL and saves to MongoDB.
            if (!uniqueSymbols.isEmpty()) {
                log.info(methodName, "Syncing EOD constituent quotes for " + uniqueSymbols.size() + " unique stocks...");
                marketDataService.getOHLC(
                        new java.util.ArrayList<>(uniqueSymbols),
                        com.am.marketdata.common.model.TimeFrame.DAY,
                        true,
                        null
                );
                log.info(methodName, "Successfully synced and cached EOD constituent quotes for " + uniqueSymbols.size() + " stocks.");
            }
        } catch (Exception e) {
            log.error(methodName, "Error during EOD Index Price Sync", e);
        }
    }
}
