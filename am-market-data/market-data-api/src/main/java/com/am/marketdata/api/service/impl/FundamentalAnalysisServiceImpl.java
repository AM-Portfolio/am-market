package com.am.marketdata.api.service.impl;

import com.am.marketdata.api.model.FundamentalAnalysisResponse;
import com.am.marketdata.api.model.FundamentalRatiosResponse;
import com.am.marketdata.api.service.FundamentalAnalysisService;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.model.fundamental.*;
import com.am.marketdata.service.fundamental.FundamentalCalculationEngine;
import com.am.marketdata.service.fundamental.FundamentalQueryService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.am.marketdata.service.model.security.SecurityDocument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundamentalAnalysisServiceImpl implements FundamentalAnalysisService {

    private final FundamentalQueryService fundamentalQueryService;
    private final MarketDataFetchService marketDataFetchService;
    private final FundamentalCalculationEngine calculationEngine;
    private final FlowLogger flowLogger;

    @Override
    public FundamentalAnalysisResponse getFundamentals(String symbol) {
        return getFundamentals(symbol, "NSE");
    }

    @Override
    public FundamentalAnalysisResponse getFundamentals(String symbol, String exchange) {
        String rawSymbol = symbol.trim();
        String targetExchange = (exchange != null && !exchange.isBlank()) ? exchange.trim().toUpperCase() : "NSE";
        String resolvedIsin = fundamentalQueryService.resolveIsin(rawSymbol);

        try (FlowSpan span = flowLogger.start("market.fundamentals.fetch", "symbol", rawSymbol, "isin",
                resolvedIsin != null ? resolvedIsin : "UNRESOLVED", "exchange", targetExchange)) {
            
            if (resolvedIsin == null) {
                log.warn("Unable to resolve trading symbol={} to ISIN", rawSymbol);
                flowLogger.complete(span, "status", "NOT_FOUND");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to resolve symbol to ISIN");
            }

            Optional<FundamentalData> dataOpt = fundamentalQueryService.getFundamentalsByIsin(resolvedIsin);
            if (dataOpt.isEmpty()) {
                flowLogger.complete(span, "status", "NOT_FOUND");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fundamental data not found");
            }

            FundamentalData data = dataOpt.get();

            // Enrich with live price data using quotes flow for the requested exchange
            Double[] prices = fetchLivePrice(data.getSymbol() != null ? data.getSymbol() : rawSymbol.toUpperCase(), targetExchange);
            Double livePrice = prices[0];
            Double dayHigh = prices[1];
            Double dayLow = prices[2];
            Double dayChange = prices[3];
            Double dayChangePercent = prices[4];

            // Construct Company Overview Section
            CompanyProfile profile = data.getCompanyProfile();
            FundamentalAnalysisResponse.CompanyOverviewSection companySection = FundamentalAnalysisResponse.CompanyOverviewSection
                    .builder()
                    .isin(data.getIsin())
                    .symbol(data.getSymbol() != null ? data.getSymbol() : rawSymbol.toUpperCase())
                    .companyName(data.getCompanyName())
                    .description(profile != null ? profile.getDescription() : null)
                    .sector(profile != null ? profile.getSector() : null)
                    .sectorMarketCapInr(profile != null ? profile.getSectorMarketCapInr() : null)
                    .sectorMarketCapUsd(profile != null ? profile.getSectorMarketCapUsd() : null)
                    .currentPrice(livePrice)
                    .dayHigh(dayHigh)
                    .dayLow(dayLow)
                    .dayChange(dayChange)
                    .dayChangePercent(dayChangePercent)
                    .exchange(targetExchange)
                    .stalePrice(livePrice == null || livePrice <= 0)
                    .build();

            // Construct Profitability Section
            KeyRatios ratios = data.getKeyRatios();
            KeyRatios dynamicRatios = ratios;
            if (ratios != null) {
                dynamicRatios = KeyRatios.builder()
                        .sectorPe(ratios.getSectorPe())
                        .sectorPb(ratios.getSectorPb())
                        .roa(ratios.getRoa())
                        .sectorRoa(ratios.getSectorRoa())
                        .roe(ratios.getRoe())
                        .sectorRoce(ratios.getSectorRoce())
                        .roce(ratios.getRoce())
                        .pe(ratios.getPe())
                        .pb(ratios.getPb())
                        .build();
            }

            FundamentalAnalysisResponse.ProfitabilitySection profitabilitySection = null;
            if (dynamicRatios != null) {
                profitabilitySection = FundamentalAnalysisResponse.ProfitabilitySection
                        .builder()
                        .roa(dynamicRatios.getRoa())
                        .sectorRoa(dynamicRatios.getSectorRoa())
                        .roe(dynamicRatios.getRoe())
                        .sectorRoe(dynamicRatios.getSectorRoe())
                        .roce(dynamicRatios.getRoce())
                        .sectorRoce(dynamicRatios.getSectorRoce())
                        .build();
            }

            // Construct Financials Section with on-demand hydration if missing.
            List<IncomeStatementEntry> income = data.getIncomeStatements();
            if (income == null || income.isEmpty()) {
                income = fundamentalQueryService.hydrateIncomeStatements(data.getIsin());
            }
            List<IncomeStatementEntry> quarterlyIncome = data.getQuarterlyIncomeStatements();
            if (isQuarterlyDataInvalidOrMirrored(quarterlyIncome, income)) {
                quarterlyIncome = fundamentalQueryService.hydrateQuarterlyIncomeStatements(data.getIsin());
            }
            List<BalanceSheetEntry> balance = data.getBalanceSheets();
            if (balance == null || balance.isEmpty()) {
                balance = fundamentalQueryService.hydrateBalanceSheets(data.getIsin());
            }
            List<CashFlowEntry> cashFlow = data.getCashFlows();
            if (cashFlow == null || cashFlow.isEmpty()) {
                cashFlow = fundamentalQueryService.hydrateCashFlows(data.getIsin());
            }

            FundamentalAnalysisResponse.FinancialsSection financialsSection = FundamentalAnalysisResponse.FinancialsSection
                    .builder()
                    .incomeStatement(income != null ? income : Collections.emptyList())
                    .quarterlyIncomeStatement(quarterlyIncome != null ? quarterlyIncome : Collections.emptyList())
                    .balanceSheet(balance != null ? balance : Collections.emptyList())
                    .cashFlow(cashFlow != null ? cashFlow : Collections.emptyList())
                    .build();

            // Resolve shareholding, corporate actions, and peers with on-demand hydration
            List<ShareholdingQuarterEntry> shareholdings = data.getShareholdings();
            if (shareholdings == null || shareholdings.isEmpty()) {
                shareholdings = fundamentalQueryService.hydrateShareholding(data.getIsin());
            }

            List<CorporateActionEntry> corporateActions = data.getCorporateActions();
            if (corporateActions == null || corporateActions.isEmpty()) {
                corporateActions = fundamentalQueryService.hydrateCorporateActions(data.getIsin());
            }

            List<CompetitorPeer> peers = data.getPeers();
            if (peers == null || peers.isEmpty()) {
                peers = fundamentalQueryService.hydratePeers(data.getIsin());
            }

            // Build Final Unified Response
            FundamentalAnalysisResponse response = FundamentalAnalysisResponse.builder()
                    .company(companySection)
                    .valuation(dynamicRatios)
                    .profitability(profitabilitySection)
                    .financials(financialsSection)
                    .shareholding(shareholdings != null ? shareholdings : Collections.emptyList())
                    .corporateActions(corporateActions != null ? corporateActions : Collections.emptyList())
                    .peers(enrichPeers(peers, targetExchange))
                    .analytics(computeAnalyticsIfAbsent(data))
                    .build();

            flowLogger.complete(span);
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving fundamentals for stock={}", rawSymbol, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @Override
    public FundamentalAnalysisResponse.CompanyOverviewSection getCompanyProfile(String symbol) {
        return getCompanyProfile(symbol, "NSE");
    }

    @Override
    public FundamentalAnalysisResponse.CompanyOverviewSection getCompanyProfile(String symbol, String exchange) {
        String targetExchange = (exchange != null && !exchange.isBlank()) ? exchange.trim().toUpperCase() : "NSE";
        return processGranularRequest(symbol, "profile", data -> {
            CompanyProfile profile = data.getCompanyProfile();
            Double[] prices = fetchLivePrice(data.getSymbol() != null ? data.getSymbol() : symbol.trim().toUpperCase(), targetExchange);
            return FundamentalAnalysisResponse.CompanyOverviewSection.builder()
                    .isin(data.getIsin())
                    .symbol(data.getSymbol() != null ? data.getSymbol() : symbol.trim().toUpperCase())
                    .companyName(data.getCompanyName())
                    .description(profile != null ? profile.getDescription() : null)
                    .sector(profile != null ? profile.getSector() : null)
                    .sectorMarketCapInr(profile != null ? profile.getSectorMarketCapInr() : null)
                    .sectorMarketCapUsd(profile != null ? profile.getSectorMarketCapUsd() : null)
                    .currentPrice(prices[0])
                    .dayHigh(prices[1])
                    .dayLow(prices[2])
                    .dayChange(prices[3])
                    .dayChangePercent(prices[4])
                    .exchange(targetExchange)
                    .stalePrice(prices[0] == null || prices[0] <= 0)
                    .build();
        });
    }

    @Override
    public FundamentalRatiosResponse getRatios(String symbol) {
        return getRatios(symbol, "NSE");
    }

    @Override
    public FundamentalRatiosResponse getRatios(String symbol, String exchange) {
        String targetExchange = (exchange != null && !exchange.isBlank()) ? exchange.trim().toUpperCase() : "NSE";
        return processGranularRequest(symbol, "ratios", data -> {
            KeyRatios ratios = data.getKeyRatios();
            KeyRatios dynamicRatios = ratios;
            Double[] prices = fetchLivePrice(data.getSymbol() != null ? data.getSymbol() : symbol.trim().toUpperCase(), targetExchange);
            if (ratios != null && prices[0] != null && prices[0] > 0) {
                dynamicRatios = KeyRatios.builder()
                        .sectorPe(ratios.getSectorPe())
                        .sectorPb(ratios.getSectorPb())
                        .roa(ratios.getRoa())
                        .sectorRoa(ratios.getSectorRoa())
                        .roe(ratios.getRoe())
                        .sectorRoe(ratios.getSectorRoe())
                        .roce(ratios.getRoce())
                        .sectorRoce(ratios.getSectorRoce())
                        .pe(ratios.getPe())
                        .pb(ratios.getPb())
                        .build();
            }

            FundamentalAnalysisResponse.ProfitabilitySection profitability = dynamicRatios != null
                    ? FundamentalAnalysisResponse.ProfitabilitySection.builder()
                    .roa(dynamicRatios.getRoa()).sectorRoa(dynamicRatios.getSectorRoa())
                    .roe(dynamicRatios.getRoe()).sectorRoe(dynamicRatios.getSectorRoe())
                    .roce(dynamicRatios.getRoce()).sectorRoce(dynamicRatios.getSectorRoce())
                    .build()
                    : null;
            return FundamentalRatiosResponse.builder()
                    .valuation(dynamicRatios)
                    .profitability(profitability)
                    .build();
        });
    }

    @Override
    public FundamentalAnalysisResponse.FinancialsSection getFinancials(String symbol) {
        return processGranularRequest(symbol, "financials", data -> {
            // Retrieve Annual (yearly) income statements
            List<IncomeStatementEntry> income = data.getIncomeStatements();
            if (income == null || income.isEmpty()) {
                income = fundamentalQueryService.hydrateIncomeStatements(data.getIsin());
            }
            // Retrieve Quarterly income statements (cached in Mongo or hydrated via single-flight upstream call)
            List<IncomeStatementEntry> quarterlyIncome = data.getQuarterlyIncomeStatements();
            if (isQuarterlyDataInvalidOrMirrored(quarterlyIncome, income)) {
                quarterlyIncome = fundamentalQueryService.hydrateQuarterlyIncomeStatements(data.getIsin());
            }
            List<BalanceSheetEntry> balance = data.getBalanceSheets();
            if (balance == null || balance.isEmpty()) {
                balance = fundamentalQueryService.hydrateBalanceSheets(data.getIsin());
            }
            List<CashFlowEntry> cashFlow = data.getCashFlows();
            if (cashFlow == null || cashFlow.isEmpty()) {
                cashFlow = fundamentalQueryService.hydrateCashFlows(data.getIsin());
            }
            return FundamentalAnalysisResponse.FinancialsSection.builder()
                    .incomeStatement(income != null ? income : Collections.emptyList())
                    .quarterlyIncomeStatement(quarterlyIncome != null ? quarterlyIncome : Collections.emptyList())
                    .balanceSheet(balance != null ? balance : Collections.emptyList())
                    .cashFlow(cashFlow != null ? cashFlow : Collections.emptyList())
                    .build();
        });
    }

    @Override
    public List<ShareholdingQuarterEntry> getShareholding(String symbol) {
        return processGranularRequest(symbol, "shareholding", data -> {
            List<ShareholdingQuarterEntry> list = data.getShareholdings();
            if (list == null || list.isEmpty()) {
                list = fundamentalQueryService.hydrateShareholding(data.getIsin());
            }
            return list != null ? list : Collections.emptyList();
        });
    }

    @Override
    public List<CorporateActionEntry> getCorporateActions(String symbol) {
        return processGranularRequest(symbol, "corporateActions", data -> {
            List<CorporateActionEntry> list = data.getCorporateActions();
            if (list == null || list.isEmpty()) {
                list = fundamentalQueryService.hydrateCorporateActions(data.getIsin());
            }
            return list != null ? list : Collections.emptyList();
        });
    }

    @Override
    public List<CompetitorPeer> getPeers(String symbol) {
        return processGranularRequest(symbol, "peers", data -> {
            List<CompetitorPeer> list = data.getPeers();
            if (list == null || list.isEmpty()) {
                list = fundamentalQueryService.hydratePeers(data.getIsin());
            }
            return enrichPeers(list);
        });
    }

    @Override
    public FundamentalAnalytics getAnalytics(String symbol) {
        return processGranularRequest(symbol, "analytics", this::computeAnalyticsIfAbsent);
    }

    private <T> T processGranularRequest(String symbol, String spanOp, java.util.function.Function<FundamentalData, T> extractor) {
        String rawSymbol = symbol.trim();
        String resolvedIsin = fundamentalQueryService.resolveIsin(rawSymbol);

        try (FlowSpan span = flowLogger.start("market.fundamentals." + spanOp, "symbol", rawSymbol, "isin",
                resolvedIsin != null ? resolvedIsin : "UNRESOLVED")) {
            if (resolvedIsin == null) {
                log.warn("Unable to resolve trading symbol={} to ISIN for operation={}", rawSymbol, spanOp);
                flowLogger.complete(span, "status", "NOT_FOUND");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to resolve symbol to ISIN");
            }

            Optional<FundamentalData> dataOpt = fundamentalQueryService.getFundamentalsByIsin(resolvedIsin);
            if (dataOpt.isEmpty()) {
                flowLogger.complete(span, "status", "NOT_FOUND");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fundamental data not found");
            }
            T result = extractor.apply(dataOpt.get());
            flowLogger.complete(span);
            return result;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving {} for stock={}", spanOp, rawSymbol, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private FundamentalAnalytics computeAnalyticsIfAbsent(FundamentalData data) {
        if (data == null) return FundamentalAnalytics.builder().build();
        FundamentalAnalytics existing = data.getAnalytics();
        if (existing != null && (existing.getNetProfitMarginPercent() != null || existing.getCurrentRatio() != null)) {
            return existing;
        }

        FundamentalAnalytics.FundamentalAnalyticsBuilder builder = FundamentalAnalytics.builder();

        // 1. Compute margins from latest income statement
        if (data.getIncomeStatements() != null && !data.getIncomeStatements().isEmpty()) {
            IncomeStatementEntry latestInc = data.getIncomeStatements().get(0);
            Double totalRev = latestInc.getTotalRevenue();
            Double pat = latestInc.getProfitAfterTax();
            Double opProfit = latestInc.getOperatingProfit();
            Double expenses = latestInc.getTotalExpenses();

            if (totalRev != null && totalRev > 0) {
                if (pat != null) {
                    builder.netProfitMarginPercent(Math.round((pat / totalRev) * 10000.0) / 100.0);
                }
                if (opProfit != null) {
                    builder.operatingMarginPercent(Math.round((opProfit / totalRev) * 10000.0) / 100.0);
                } else if (expenses != null) {
                    builder.operatingMarginPercent(Math.round(((totalRev - expenses) / totalRev) * 10000.0) / 100.0);
                }
            }
        }

        // 2. Compute Current Ratio from latest balance sheet
        if (data.getBalanceSheets() != null && !data.getBalanceSheets().isEmpty()) {
            BalanceSheetEntry latestBal = data.getBalanceSheets().get(0);
            Double currentAssets = latestBal.getCurrentAssets();
            Double currentLiab = latestBal.getCurrentLiabilities();
            if (currentAssets != null && currentLiab != null && currentLiab > 0) {
                builder.currentRatio(Math.round((currentAssets / currentLiab) * 100.0) / 100.0);
            }
        }

        // 3. Compute CFO / PAT from latest cash flow & income statement
        if (data.getCashFlows() != null && !data.getCashFlows().isEmpty() &&
                data.getIncomeStatements() != null && !data.getIncomeStatements().isEmpty()) {
            CashFlowEntry latestCf = data.getCashFlows().get(0);
            IncomeStatementEntry latestInc = data.getIncomeStatements().get(0);
            Double cfo = latestCf.getOperatingCashFlow();
            Double pat = latestInc.getProfitAfterTax();
            if (cfo != null && pat != null && pat != 0) {
                builder.cfoPat(Math.round((cfo / pat) * 100.0) / 100.0);
            }
        }

        return builder.build();
    }

    private Double[] extractPricesFromQuote(Object quoteObj) {
        Double[] prices = new Double[5];
        if (quoteObj instanceof OHLCQuote quote) {
            double lastPrice = quote.getLastPrice();
            double high = quote.getOhlc() != null ? quote.getOhlc().getHigh() : 0.0;
            double low = quote.getOhlc() != null ? quote.getOhlc().getLow() : 0.0;
            double prevClose = quote.getPreviousClose();

            if (lastPrice > 0) {
                prices[0] = lastPrice;
                prices[1] = high > 0 ? high : null;
                prices[2] = low > 0 ? low : null;
                if (prevClose > 0) {
                    prices[3] = Math.round((lastPrice - prevClose) * 100.0) / 100.0;
                    prices[4] = Math.round(((lastPrice - prevClose) / prevClose) * 10000.0) / 100.0;
                }
            }
        } else if (quoteObj instanceof Map<?, ?> quoteMap) {
            double lastPrice = quoteMap.get("lastPrice") instanceof Number n ? n.doubleValue() : 0.0;
            double prevClose = quoteMap.get("previousClose") instanceof Number n ? n.doubleValue() : 0.0;
            double high = 0.0;
            double low = 0.0;
            if (quoteMap.get("ohlc") instanceof Map<?, ?> ohlcMap) {
                high = ohlcMap.get("high") instanceof Number n ? n.doubleValue() : 0.0;
                low = ohlcMap.get("low") instanceof Number n ? n.doubleValue() : 0.0;
            }

            if (lastPrice > 0) {
                prices[0] = lastPrice;
                prices[1] = high > 0 ? high : null;
                prices[2] = low > 0 ? low : null;
                if (prevClose > 0) {
                    prices[3] = Math.round((lastPrice - prevClose) * 100.0) / 100.0;
                    prices[4] = Math.round(((lastPrice - prevClose) / prevClose) * 10000.0) / 100.0;
                }
            }
        }
        return prices;
    }

    private Double[] fetchLivePrice(String symbol) {
        return fetchLivePrice(symbol, "NSE");
    }

    private Double[] fetchLivePrice(String symbol, String exchange) {
        Double[] prices = new Double[5];
        String targetExchange = (exchange != null && !exchange.isBlank()) ? exchange.trim().toUpperCase() : "NSE";
        String qualifiedSymbol = targetExchange + ":" + symbol;
        log.info("[EXCHANGE_DIAG] fetchLivePrice called: symbol={} targetExchange={} qualifiedSymbol={}",
                symbol, targetExchange, qualifiedSymbol);
        try {
            // First attempt with target exchange
            Map<String, Object> quotesMap = marketDataFetchService.getQuotes(Set.of(qualifiedSymbol), false, TimeFrame.DAY, false);
            if (quotesMap != null && quotesMap.get("quotes") instanceof Map<?, ?> map) {
                log.info("[EXCHANGE_DIAG] quotes map keys returned for qualifiedSymbol={}: {}",
                        qualifiedSymbol, map.keySet());
                Object quoteObj = map.get(qualifiedSymbol);
                String hitKey = qualifiedSymbol;
                if (quoteObj == null) {
                    quoteObj = map.get(symbol);
                    hitKey = symbol;
                }
                if (quoteObj == null && !map.isEmpty()) {
                    Map.Entry<?, ?> first = ((Map<?, ?>) map).entrySet().iterator().next();
                    quoteObj = first.getValue();
                    hitKey = String.valueOf(first.getKey());
                    log.warn("[EXCHANGE_DIAG] Exact key miss for qualifiedSymbol={}; falling back to first map entry key={}",
                            qualifiedSymbol, hitKey);
                }
                Double[] extracted = extractPricesFromQuote(quoteObj);
                log.info("[EXCHANGE_DIAG] Price extracted via key={}: lastPrice={} high={} low={} change={} changePct={}",
                        hitKey, extracted[0], extracted[1], extracted[2], extracted[3], extracted[4]);
                if (extracted[0] != null && extracted[0] > 0) {
                    return extracted;
                }
            } else {
                log.warn("[EXCHANGE_DIAG] getQuotes returned null or missing 'quotes' key for qualifiedSymbol={}",
                        qualifiedSymbol);
            }

            // Fallback for illiquid / zero-trade stocks on target exchange: try base symbol / alternate exchange
            if (!"NSE".equalsIgnoreCase(targetExchange)) {
                log.info("[EXCHANGE_DIAG] fetchLivePrice: Empty/zero price on exchange={}, falling back to NSE for symbol={}",
                        targetExchange, symbol);
                Map<String, Object> fallbackMap = marketDataFetchService.getQuotes(Set.of("NSE:" + symbol), false, TimeFrame.DAY, false);
                if (fallbackMap != null && fallbackMap.get("quotes") instanceof Map<?, ?> map) {
                    log.info("[EXCHANGE_DIAG] NSE fallback quotes map keys: {}", map.keySet());
                    Object fallbackObj = map.get("NSE:" + symbol);
                    if (fallbackObj == null) fallbackObj = map.get(symbol);
                    if (fallbackObj != null) {
                        Double[] fallbackPrices = extractPricesFromQuote(fallbackObj);
                        log.info("[EXCHANGE_DIAG] NSE fallback price for symbol={}: lastPrice={}", symbol, fallbackPrices[0]);
                        return fallbackPrices;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Live price lookup failed for symbol={} exchange={}: {}", symbol, targetExchange, e.getMessage());
        }
        log.warn("[EXCHANGE_DIAG] fetchLivePrice: no price found for symbol={} exchange={}, returning empty array",
                symbol, targetExchange);
        return prices;
    }

    private Map<String, Double[]> fetchLivePricesBulk(Set<String> symbols) {
        return fetchLivePricesBulk(symbols, "NSE");
    }

    private Map<String, Double[]> fetchLivePricesBulk(Set<String> symbols, String exchange) {
        Map<String, Double[]> result = new HashMap<>();
        if (symbols == null || symbols.isEmpty()) {
            return result;
        }
        String targetExchange = (exchange != null && !exchange.isBlank()) ? exchange.trim().toUpperCase() : "NSE";
        Set<String> qualifiedSymbols = symbols.stream()
                .map(s -> s.contains(":") ? s : targetExchange + ":" + s)
                .collect(Collectors.toSet());
        try {
            Map<String, Object> quotesMap = marketDataFetchService.getQuotes(qualifiedSymbols, false, TimeFrame.DAY, false);
            if (quotesMap != null && quotesMap.get("quotes") instanceof Map<?, ?> map) {
                for (String sym : symbols) {
                    String qual = targetExchange + ":" + sym;
                    Object quoteObj = map.get(qual);
                    if (quoteObj == null) {
                        quoteObj = map.get(sym);
                    }
                    if (quoteObj == null && !"NSE".equalsIgnoreCase(targetExchange)) {
                        quoteObj = map.get("NSE:" + sym);
                    }
                    if (quoteObj != null) {
                        Double[] prices = extractPricesFromQuote(quoteObj);
                        if (prices[0] != null) {
                            result.put(sym, prices);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Bulk live price lookup failed for exchange={}: {}", targetExchange, e.getMessage());
        }
        return result;
    }

    private List<CompetitorPeer> enrichPeers(List<CompetitorPeer> rawPeers) {
        return enrichPeers(rawPeers, "NSE");
    }

    private List<CompetitorPeer> enrichPeers(List<CompetitorPeer> rawPeers, String exchange) {
        if (rawPeers == null || rawPeers.isEmpty()) {
            return Collections.emptyList();
        }

        // Pass 1: Resolve ISINs and symbols
        Set<String> symbolsToFetch = new HashSet<>();
        List<CompetitorPeer.CompetitorPeerBuilder> builders = new ArrayList<>();
        List<String> resolvedSymbols = new ArrayList<>();

        for (CompetitorPeer peer : rawPeers) {
            String isin = peer.getIsin();
            if (isin == null && peer.getInstrumentKey() != null) {
                String key = peer.getInstrumentKey();
                if (key.contains("|")) {
                    isin = key.substring(key.indexOf("|") + 1);
                }
            }

            // Gracefully resolve clean company name and description
            String rawName = peer.getCompanyName();
            String description = peer.getDescription();
            String cleanName = rawName;

            // If stored companyName is actually a paragraph description (from old database records), fix it gracefully at runtime
            if (rawName != null && rawName.length() > 60) {
                if (description == null) {
                    description = rawName;
                }
                int isIndex = rawName.indexOf(" is ");
                if (isIndex > 0 && isIndex < 50) {
                    cleanName = rawName.substring(0, isIndex).trim();
                } else {
                    int firstDot = rawName.indexOf('.');
                    if (firstDot > 0 && firstDot < 80) {
                        cleanName = rawName.substring(0, firstDot).trim();
                    } else {
                        cleanName = rawName.length() > 50 ? rawName.substring(0, 50).trim() : rawName.trim();
                    }
                }
            }

            CompetitorPeer.CompetitorPeerBuilder builder = CompetitorPeer.builder()
                    .instrumentKey(peer.getInstrumentKey())
                    .isin(isin)
                    .companyName(cleanName)
                    .description(description)
                    .sectorMarketCapInr(peer.getSectorMarketCapInr())
                    .sectorMarketCapUsd(peer.getSectorMarketCapUsd())
                    .sector(peer.getSector());

            String resolvedSymbol = null;
            if (isin != null) {
                Optional<SecurityDocument> secOpt = fundamentalQueryService.getSecurityByIsin(isin);
                if (secOpt.isPresent() && secOpt.get().getKey() != null) {
                    resolvedSymbol = secOpt.get().getKey().getSymbol();
                }
            }

            if (isin != null) {
                Optional<FundamentalData> peerDataOpt = fundamentalQueryService.getExistingFundamentalsByIsin(isin);
                if (peerDataOpt.isPresent()) {
                    FundamentalData peerData = peerDataOpt.get();
                    if (resolvedSymbol == null) {
                        resolvedSymbol = peerData.getSymbol();
                    }

                    KeyRatios keyRatios = peerData.getKeyRatios();
                    if (keyRatios != null) {
                        builder.pe(keyRatios.getPe());
                        builder.pb(keyRatios.getPb());
                        builder.evEbitda(keyRatios.getEvEbitda());
                        builder.roe(keyRatios.getRoe());
                        builder.roce(keyRatios.getRoce());
                        builder.roa(keyRatios.getRoa());
                        builder.quickRatio(keyRatios.getQuickRatio());
                        builder.nim(keyRatios.getNim());
                        builder.netNpa(keyRatios.getNetNpa());
                        builder.casa(keyRatios.getCasa());
                        builder.dynamicRatios(keyRatios.getDynamicRatios());
                    }
                }
            }

            builder.symbol(resolvedSymbol);
            if (resolvedSymbol != null && !resolvedSymbol.trim().isEmpty()) {
                symbolsToFetch.add(resolvedSymbol);
            }
            builders.add(builder);
            resolvedSymbols.add(resolvedSymbol);
        }

        // Pass 2: Bulk fetch live prices in ONE call
        Map<String, Double[]> pricesMap = fetchLivePricesBulk(symbolsToFetch, exchange);

        // Pass 3: Attach prices and build
        List<CompetitorPeer> enrichedList = new ArrayList<>();
        for (int i = 0; i < builders.size(); i++) {
            CompetitorPeer.CompetitorPeerBuilder builder = builders.get(i);
            String symbol = resolvedSymbols.get(i);
            if (symbol != null && pricesMap.containsKey(symbol)) {
                Double[] prices = pricesMap.get(symbol);
                builder.currentPrice(prices[0]);
                builder.dayChange(prices[3]);
                builder.dayChangePercent(prices[4]);
            }
            enrichedList.add(builder.build());
        }
        return enrichedList;
    }

    /**
     * Checks if quarterly income statements are missing, empty, or mistakenly mirroring annual figures.
     */
    private boolean isQuarterlyDataInvalidOrMirrored(List<IncomeStatementEntry> quarterlyIncome, List<IncomeStatementEntry> annualIncome) {
        if (quarterlyIncome == null || quarterlyIncome.isEmpty()) {
            return true;
        }
        // If quarterly size matches annual size and top revenue is identical to annual revenue, it's corrupted/mirrored data
        if (annualIncome != null && !annualIncome.isEmpty() && quarterlyIncome.size() == annualIncome.size()) {
            Double qRev = quarterlyIncome.get(0).getRevenue();
            Double aRev = annualIncome.get(0).getRevenue();
            String qPeriod = quarterlyIncome.get(0).getPeriod();
            String aPeriod = annualIncome.get(0).getPeriod();
            if (qRev != null && qRev.equals(aRev) && qPeriod != null && qPeriod.equalsIgnoreCase(aPeriod)) {
                log.warn("Detected quarterly income statements mirroring annual data (period={}, revenue={}); forcing on-demand hydration", qPeriod, qRev);
                return true;
            }
        }
        // Check if periods don't look quarterly (e.g. all Mar 2026, Mar 2025, Mar 2024...)
        if (quarterlyIncome.size() > 1) {
            String p0 = quarterlyIncome.get(0).getPeriod();
            String p1 = quarterlyIncome.get(1).getPeriod();
            if (p0 != null && p1 != null) {
                String[] parts0 = p0.trim().split("\\s+");
                String[] parts1 = p1.trim().split("\\s+");
                if (parts0.length == 2 && parts1.length == 2 && parts0[0].equalsIgnoreCase(parts1[0]) && !parts0[1].equalsIgnoreCase(parts1[1])) {
                    log.warn("Detected quarterly income statements having multi-year intervals ({}, {}); forcing on-demand hydration", p0, p1);
                    return true;
                }
            }
        }
        return false;
    }
}
