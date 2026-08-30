package com.am.marketdata.api.controller;

import com.am.marketdata.api.model.FundamentalAnalysisResponse;
import com.am.marketdata.api.model.FundamentalRatiosResponse;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.model.fundamental.*;
import com.am.marketdata.service.fundamental.FundamentalCalculationEngine;
import com.am.marketdata.service.fundamental.FundamentalQueryService;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST Controller for Stock Fundamental Analysis.
 * Adheres to `/v1/**` routing and unified JSON response conventions.
 */
@Slf4j
@RestController
@RequestMapping("/v1/fundamentals")
@Tag(name = "Fundamental Analysis", description = "APIs for company profile, financial statements, valuation, ratios, and peers")
@RequiredArgsConstructor
public class FundamentalAnalysisController {

        private final FundamentalQueryService fundamentalQueryService;
        private final MarketDataFetchService marketDataFetchService;
        private final FundamentalCalculationEngine calculationEngine;
        private final FlowLogger flowLogger;

        /**
         * Retrieves unified fundamental analysis data for a stock by trading symbol.
         *
         * @param symbol Trading symbol (e.g. "TCS", "ITC", "INFY", "RELIANCE").
         * @return 200 OK with {@link FundamentalAnalysisResponse}, or 404 if not found.
         */
        @GetMapping(value = "/{symbol}", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getFundamentalAnalysis", summary = "Get unified fundamental analysis for a stock", description = "Retrieves complete fundamental profile, key ratios, P&L statements, balance sheets, cash flows, shareholding pattern, corporate actions, competitor peers, and health analytics by stock symbol (e.g., TCS, INFY, ITC).")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Fundamental data retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalAnalysisResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(name = "Bad Request Example", value = "{\"error\": \"INVALID_PARAMETER\", \"message\": \"Stock symbol parameter is required\"}"))),
                        @ApiResponse(responseCode = "404", description = "Fundamental data not found for given stock symbol", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(name = "Not Found Example", value = "{\"error\": \"NOT_FOUND\", \"message\": \"No fundamental data available for stock: UNKNOWN\"}"))),
                        @ApiResponse(responseCode = "500", description = "Internal server error occurred while retrieving fundamental data", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = @ExampleObject(name = "Server Error Example", value = "{\"error\": \"INTERNAL_ERROR\", \"message\": \"Failed to retrieve fundamental analysis\"}")))
        })
        public ResponseEntity<FundamentalAnalysisResponse> getFundamentals(
                        @Parameter(name = "symbol", description = "NSE / BSE Stock Trading Symbol (case-insensitive, e.g., 'TCS', 'tcs', 'INFY')", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }

                String rawSymbol = symbol.trim();
                String resolvedIsin = fundamentalQueryService.resolveIsin(rawSymbol);

                try (FlowSpan span = flowLogger.start("market.fundamentals.fetch", "symbol", rawSymbol, "isin",
                                resolvedIsin != null ? resolvedIsin : "UNRESOLVED")) {
                        try {
                                if (resolvedIsin == null) {
                                        log.warn("Unable to resolve trading symbol={} to ISIN", rawSymbol);
                                        flowLogger.complete(span, "status", "NOT_FOUND");
                                        return ResponseEntity.notFound().build();
                                }

                                Optional<FundamentalData> dataOpt = fundamentalQueryService
                                                .getFundamentalsByIsin(resolvedIsin);
                                if (dataOpt.isEmpty()) {
                                        flowLogger.complete(span, "status", "NOT_FOUND");
                                        return ResponseEntity.notFound().build();
                                }

                                FundamentalData data = dataOpt.get();

                                // Enrich with live price data using quotes flow
                                Double[] prices = fetchLivePrice(
                                                data.getSymbol() != null ? data.getSymbol() : rawSymbol.toUpperCase());
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
                                                .symbol(data.getSymbol() != null ? data.getSymbol()
                                                                : rawSymbol.toUpperCase())
                                                .companyName(data.getCompanyName())
                                                .description(profile != null ? profile.getDescription() : null)
                                                .sector(profile != null ? profile.getSector() : null)
                                                .sectorMarketCapInr(profile != null ? profile.getSectorMarketCapInr()
                                                                : null)
                                                .sectorMarketCapUsd(profile != null ? profile.getSectorMarketCapUsd()
                                                                : null)
                                                .currentPrice(livePrice)
                                                .dayHigh(dayHigh)
                                                .dayLow(dayLow)
                                                .dayChange(dayChange)
                                                .dayChangePercent(dayChangePercent)
                                                .build();

                                // Construct Profitability Section
                                KeyRatios ratios = data.getKeyRatios();
                                FundamentalAnalysisResponse.ProfitabilitySection profitabilitySection = null;
                                if (ratios != null) {
                                        profitabilitySection = FundamentalAnalysisResponse.ProfitabilitySection
                                                        .builder()
                                                        .roa(ratios.getRoa())
                                                        .sectorRoa(ratios.getSectorRoa())
                                                        .roe(ratios.getRoe())
                                                        .sectorRoe(ratios.getSectorRoe())
                                                        .roce(ratios.getRoce())
                                                        .sectorRoce(ratios.getSectorRoce())
                                                        .build();
                                }

                                // Construct Financials Section
                                FundamentalAnalysisResponse.FinancialsSection financialsSection = FundamentalAnalysisResponse.FinancialsSection
                                                .builder()
                                                .incomeStatement(data.getIncomeStatements() != null
                                                                ? data.getIncomeStatements()
                                                                : Collections.emptyList())
                                                .balanceSheet(data.getBalanceSheets() != null ? data.getBalanceSheets()
                                                                : Collections.emptyList())
                                                .cashFlow(data.getCashFlows() != null ? data.getCashFlows()
                                                                : Collections.emptyList())
                                                .build();

                                // Build Final Unified Response
                                FundamentalAnalysisResponse response = FundamentalAnalysisResponse.builder()
                                                .company(companySection)
                                                .valuation(ratios)
                                                .profitability(profitabilitySection)
                                                .financials(financialsSection)
                                                .shareholding(data.getShareholdings() != null ? data.getShareholdings()
                                                                : Collections.emptyList())
                                                .corporateActions(data.getCorporateActions() != null
                                                                ? data.getCorporateActions()
                                                                : Collections.emptyList())
                                                .peers(data.getPeers() != null ? data.getPeers()
                                                                : Collections.emptyList())
                                                .analytics(data.getAnalytics())
                                                .build();

                                flowLogger.complete(span);
                                return ResponseEntity.ok(response);
                        } catch (Exception e) {
                                log.error("Error retrieving fundamentals for stock={}", rawSymbol, e);
                                flowLogger.fail(span, e);
                                return ResponseEntity.internalServerError().build();
                        }
                }
        }

        @GetMapping(value = "/{symbol}/profile", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getCompanyProfile", summary = "Get company profile", description = "Retrieves company overview, sector, market cap, and live price by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Company profile retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalAnalysisResponse.CompanyOverviewSection.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Company profile not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<FundamentalAnalysisResponse.CompanyOverviewSection> getCompanyProfile(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                return processGranularRequest(symbol, "profile", data -> {
                        CompanyProfile profile = data.getCompanyProfile();
                        Double[] prices = fetchLivePrice(
                                        data.getSymbol() != null ? data.getSymbol() : symbol.trim().toUpperCase());
                        return FundamentalAnalysisResponse.CompanyOverviewSection.builder()
                                        .isin(data.getIsin())
                                        .symbol(data.getSymbol() != null ? data.getSymbol()
                                                        : symbol.trim().toUpperCase())
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
                                        .build();
                });
        }

        @GetMapping(value = "/{symbol}/ratios", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getFinancialRatios", summary = "Get valuation and profitability ratios", description = "Retrieves PE, PB, ROE, ROA, ROCE, EV/EBITDA compared with sector benchmarks by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Key ratios retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalRatiosResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Ratios not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<FundamentalRatiosResponse> getRatios(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                return processGranularRequest(symbol, "ratios", data -> {
                        KeyRatios ratios = data.getKeyRatios();
                        FundamentalAnalysisResponse.ProfitabilitySection profitability = ratios != null
                                        ? FundamentalAnalysisResponse.ProfitabilitySection.builder()
                                                        .roa(ratios.getRoa()).sectorRoa(ratios.getSectorRoa())
                                                        .roe(ratios.getRoe()).sectorRoe(ratios.getSectorRoe())
                                                        .roce(ratios.getRoce()).sectorRoce(ratios.getSectorRoce())
                                                        .build()
                                        : null;
                        return FundamentalRatiosResponse.builder()
                                        .valuation(ratios)
                                        .profitability(profitability)
                                        .build();
                });
        }

        @GetMapping(value = "/{symbol}/financials", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getFinancialStatements", summary = "Get financial statements", description = "Retrieves multi-year income statements, balance sheets, and cash flow statements by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Financial statements retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalAnalysisResponse.FinancialsSection.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Financials not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<FundamentalAnalysisResponse.FinancialsSection> getFinancials(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                return processGranularRequest(symbol, "financials",
                                data -> FundamentalAnalysisResponse.FinancialsSection.builder()
                                                .incomeStatement(data.getIncomeStatements() != null
                                                                ? data.getIncomeStatements()
                                                                : Collections.emptyList())
                                                .balanceSheet(data.getBalanceSheets() != null ? data.getBalanceSheets()
                                                                : Collections.emptyList())
                                                .cashFlow(data.getCashFlows() != null ? data.getCashFlows()
                                                                : Collections.emptyList())
                                                .build());
        }

        @GetMapping(value = "/{symbol}/shareholding", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getShareholdingPattern", summary = "Get shareholding patterns", description = "Retrieves historical quarterly ownership trends (Promoters, FII, DII, Mutual Funds, Retail) by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Shareholding pattern retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ShareholdingQuarterEntry.class)))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Shareholding pattern not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<ShareholdingQuarterEntry>> getShareholding(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                return processGranularRequest(symbol, "shareholding",
                                data -> data.getShareholdings() != null ? data.getShareholdings()
                                                : Collections.emptyList());
        }

        @GetMapping(value = "/{symbol}/corporate-actions", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getCorporateActions", summary = "Get corporate actions", description = "Retrieves dividends, stock splits, bonuses, and rights announcements by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Corporate actions retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CorporateActionEntry.class)))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Corporate actions not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<CorporateActionEntry>> getCorporateActions(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                return processGranularRequest(symbol, "corporateActions",
                                data -> data.getCorporateActions() != null ? data.getCorporateActions()
                                                : Collections.emptyList());
        }

        @GetMapping(value = "/{symbol}/peers", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getCompetitorPeers", summary = "Get competitor peers", description = "Retrieves industry peer comparisons and competitor valuations by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Competitor peers retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CompetitorPeer.class)))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Peers not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<List<CompetitorPeer>> getPeers(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                return processGranularRequest(symbol, "peers",
                                data -> data.getPeers() != null ? data.getPeers() : Collections.emptyList());
        }

        @GetMapping(value = "/{symbol}/analytics", produces = MediaType.APPLICATION_JSON_VALUE)
        @Operation(operationId = "getHealthAnalytics", summary = "Get derived health analytics", description = "Retrieves calculated financial health analytics (Current Ratio, CFO/PAT, CAGRs) by trading symbol")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Health analytics retrieved successfully", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = FundamentalAnalytics.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid stock symbol parameter"),
                        @ApiResponse(responseCode = "404", description = "Analytics not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<FundamentalAnalytics> getAnalytics(
                        @Parameter(name = "symbol", description = "Stock trading symbol", required = true, example = "TCS") @PathVariable("symbol") String symbol) {
                return processGranularRequest(symbol, "analytics",
                                data -> data.getAnalytics() != null ? data.getAnalytics()
                                                : FundamentalAnalytics.builder().build());
        }

        // --- Helper Methods ---

        private <T> ResponseEntity<T> processGranularRequest(String symbol, String spanOp,
                        java.util.function.Function<FundamentalData, T> extractor) {
                if (symbol == null || symbol.trim().isEmpty()) {
                        return ResponseEntity.badRequest().build();
                }
                String rawSymbol = symbol.trim();
                String resolvedIsin = fundamentalQueryService.resolveIsin(rawSymbol);

                try (FlowSpan span = flowLogger.start("market.fundamentals." + spanOp, "symbol", rawSymbol, "isin",
                                resolvedIsin != null ? resolvedIsin : "UNRESOLVED")) {
                        if (resolvedIsin == null) {
                                log.warn("Unable to resolve trading symbol={} to ISIN for operation={}", rawSymbol,
                                                spanOp);
                                flowLogger.complete(span, "status", "NOT_FOUND");
                                return ResponseEntity.notFound().build();
                        }

                        Optional<FundamentalData> dataOpt = fundamentalQueryService.getFundamentalsByIsin(resolvedIsin);
                        if (dataOpt.isEmpty()) {
                                flowLogger.complete(span, "status", "NOT_FOUND");
                                return ResponseEntity.notFound().build();
                        }
                        T result = extractor.apply(dataOpt.get());
                        flowLogger.complete(span);
                        return ResponseEntity.ok(result);
                } catch (Exception e) {
                        log.error("Error retrieving {} for stock={}", spanOp, rawSymbol, e);
                        return ResponseEntity.internalServerError().build();
                }
        }

        private Double[] fetchLivePrice(String symbol) {
                Double[] prices = new Double[5]; // price, high, low, change, changePercent
                try {
                        Map<String, Object> quotesMap = marketDataFetchService.getQuotes(Set.of(symbol), false,
                                        TimeFrame.DAY, false);
                        if (quotesMap != null && quotesMap.get("quotes") instanceof Map<?, ?> map) {
                                Object quoteObj = map.get(symbol);
                                if (quoteObj == null) {
                                        quoteObj = map.get("NSE:" + symbol);
                                }
                                if (quoteObj == null && !map.isEmpty()) {
                                        quoteObj = map.values().iterator().next();
                                }

                                double lastPrice = 0.0;
                                double high = 0.0;
                                double low = 0.0;
                                double prevClose = 0.0;

                                if (quoteObj instanceof OHLCQuote quote) {
                                        lastPrice = quote.getLastPrice();
                                        if (quote.getOhlc() != null) {
                                                high = quote.getOhlc().getHigh();
                                                low = quote.getOhlc().getLow();
                                        }
                                        prevClose = quote.getPreviousClose();
                                } else if (quoteObj instanceof Map<?, ?> quoteMap) {
                                        lastPrice = quoteMap.get("lastPrice") instanceof Number n ? n.doubleValue()
                                                        : 0.0;
                                        prevClose = quoteMap.get("previousClose") instanceof Number n ? n.doubleValue()
                                                        : 0.0;
                                        if (quoteMap.get("ohlc") instanceof Map<?, ?> ohlcMap) {
                                                high = ohlcMap.get("high") instanceof Number n ? n.doubleValue() : 0.0;
                                                low = ohlcMap.get("low") instanceof Number n ? n.doubleValue() : 0.0;
                                        }
                                }

                                if (lastPrice > 0) {
                                        prices[0] = lastPrice;
                                        prices[1] = high > 0 ? high : null;
                                        prices[2] = low > 0 ? low : null;
                                        if (prevClose > 0) {
                                                prices[3] = Math.round((lastPrice - prevClose) * 100.0) / 100.0;
                                                prices[4] = Math.round(((lastPrice - prevClose) / prevClose) * 10000.0)
                                                                / 100.0;
                                        }
                                }
                        }
                } catch (Exception e) {
                        log.debug("Live price lookup failed for symbol={}: {}", symbol, e.getMessage());
                }
                return prices;
        }
}
