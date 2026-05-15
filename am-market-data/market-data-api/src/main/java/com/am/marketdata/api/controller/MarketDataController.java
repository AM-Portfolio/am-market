package com.am.marketdata.api.controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.am.marketdata.api.dto.HistoricalDataRequest;
import com.am.marketdata.api.model.OHLCRequest;
import com.am.marketdata.api.model.QuotesRequest;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.marketdata.api.service.MarketDataFetchService;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.service.MarketDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.extern.slf4j.Slf4j;
import com.am.observability.flow.FlowLogger;
import com.am.observability.flow.FlowSpan;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for market data operations
 * Provides endpoints for fetching various types of market data including
 * quotes, OHLC, historical data,
 * option chains, mutual fund details, and more
 */
@Slf4j
@RestController
@RequestMapping("/v1/market-data")
@Tag(name = "Market Data", description = "APIs for retrieving various types of market data including quotes, historical data, option chains, and more")
public class MarketDataController {

    private final FlowLogger flowLogger;
    private final MarketDataService marketDataService;
    private final MarketDataFetchService marketDataCacheService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public MarketDataController(FlowLogger flowLogger,
            MarketDataService marketDataService,
            MarketDataFetchService marketDataCacheService) {
        this.flowLogger = flowLogger;
        this.marketDataService = marketDataService;
        this.marketDataCacheService = marketDataCacheService;
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    /**
     * Get login URL for authentication
     * 
     * @return Login URL for broker authentication
     */
    @GetMapping(value = "/auth/login-url", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get login URL for broker authentication", description = "Returns a URL that can be used to authenticate with the broker's login page")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login URL generated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> getLoginUrl(
            @RequestParam(required = false) String provider) {
        log.info("Request for login URL provider={}", provider);
        try {
            Map<String, String> response = marketDataService.getLoginUrl(provider);
            log.info("Successfully generated login URL for provider={}", provider);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting login URL provider={}", provider, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate session from request token
     * 
     * @param requestToken    Request token from broker authentication
     * @param requestTokenAlt Alternative request token parameter name
     * @param status          Authentication status
     * @return Session information
     */
    @GetMapping(value = "/auth/session", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate session from request token", description = "Creates a new authenticated session using the request token obtained from broker login")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request token or authentication failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Object> generateSession(
            @RequestParam(value = "request_token", required = false) String requestToken,
            @RequestParam(value = "requestToken", required = false) String requestTokenAlt,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "status", required = false, defaultValue = "success") String status) {
        try {
            if (!"success".equalsIgnoreCase(status)) {
                log.warn("Authentication failed status={}", status);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Authentication failed");
                errorResponse.put("message", "Login was not successful. Status: " + status);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String token = requestToken != null ? requestToken : requestTokenAlt;
            if (code != null)
                token = code;

            if (token == null) {
                log.warn("Missing request token in session generation request");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing request token");
                errorResponse.put("message", "No request token provided");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Generating session tokenPresent={} status={}", true, status);
            Object session = marketDataService.generateSession(token);
            log.info("Successfully generated session");
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            log.error("Error generating session", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get quotes for symbols with timeframe support
     * 
     * @param symbols      Comma-separated list of symbols
     * @param timeFrameStr The timeframe for quotes (e.g., 5m, 15m, 1H, 1D)
     * @param forceRefresh Whether to force refresh from provider
     * @return Map of symbol to quote data with metadata
     */
    @GetMapping(value = "/quotes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get quotes for multiple symbols", description = "Retrieves latest quotes for multiple symbols with support for different timeframes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quotes retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getQuotes(
            @RequestParam("symbols") String symbols,
            @RequestParam(name = "timeFrame", defaultValue = "5m") String timeFrameStr,
            @RequestParam(name = "refresh", defaultValue = "false") boolean forceRefresh) {

        Set<String> symbolList = parseSymbols(symbols);
        try (FlowSpan span = flowLogger.start("market.quotes.fetch",
                "symbolsCount", symbolList.size(), "timeFrame", timeFrameStr, "forceRefresh", forceRefresh)) {
            try {
                TimeFrame timeFrame = TimeFrame.fromApiValue(timeFrameStr);
                Map<String, Object> quotesResponse = marketDataCacheService.getQuotes(symbolList, false, timeFrame,
                        forceRefresh);

                if (quotesResponse.containsKey("ERROR")) {
                    log.warn("Quotes fetch returned error branch symbolsCount={}", symbolList.size());
                    Map<String, Object> errorResponse = new HashMap<>();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> errorDetails = (Map<String, Object>) quotesResponse.get("ERROR");
                    errorResponse.put("error", errorDetails.get("error"));
                    errorResponse.put("message", errorDetails.get("message"));
                    flowLogger.fail(span, new Exception(String.valueOf(errorDetails.get("message"))));
                    return ResponseEntity.internalServerError().body(errorResponse);
                }

                flowLogger.complete(span, "resultCount", quotesResponse.size());
                return ResponseEntity.ok(quotesResponse);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid timeframe requested symbolsCount={} timeFrame={}", symbolList.size(), timeFrameStr);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "INVALID_PARAMETER");
                errorResponse.put("message", "Invalid timeFrame parameter: " + e.getMessage());
                flowLogger.warn(span, "Invalid timeframe: " + e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            } catch (Exception e) {
                log.error("Error processing quotes request symbolsCount={}", symbolList.size(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "INTERNAL_ERROR");
                errorResponse.put("message", e.getMessage());
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get quotes for symbols with timeframe support (POST version)
     * 
     * @param request The quotes request containing symbols and timeframe
     * @return Map of symbol to quote data with metadata
     */
    @PostMapping(value = "/quotes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get quotes for multiple symbols (POST)", description = "Retrieves latest quotes for multiple symbols with support for different timeframes using POST request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quotes retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getQuotesPost(@RequestBody QuotesRequest request) {
        Set<String> symbolList = parseSymbols(request.getSymbols());
        try (FlowSpan span = flowLogger.start("market.quotes.fetch.post",
                "symbolsCount", symbolList.size(), "timeFrame", request.getTimeFrame(), "forceRefresh",
                request.isForceRefresh())) {
            try {
                Map<String, Object> quotesResponse = marketDataCacheService.getQuotes(
                        symbolList, request.isIndexSymbol(), TimeFrame.fromApiValue(request.getTimeFrame()),
                        request.isForceRefresh());

                if (quotesResponse.containsKey("ERROR")) {
                    log.warn("Quotes fetch post returned error branch symbolsCount={}", symbolList.size());
                    Map<String, Object> errorResponse = new HashMap<>();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> errorDetails = (Map<String, Object>) quotesResponse.get("ERROR");
                    errorResponse.put("error", errorDetails.get("error"));
                    errorResponse.put("message", errorDetails.get("message"));
                    flowLogger.fail(span, new Exception(String.valueOf(errorDetails.get("message"))));
                    return ResponseEntity.internalServerError().body(errorResponse);
                }

                flowLogger.complete(span, "resultCount", quotesResponse.size());
                return ResponseEntity.ok(quotesResponse);
            } catch (Exception e) {
                log.error("Error processing quotes post request symbolsCount={}", symbolList.size(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "INTERNAL_ERROR");
                errorResponse.put("message", e.getMessage());
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get OHLC data for symbols
     * 
     * @param request Request body containing symbols and options
     * @return Map of symbol to OHLC data with cache status
     */
    @PostMapping(value = "/ohlc", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get OHLC data for multiple symbols", description = "Retrieves Open-High-Low-Close data for multiple symbols with support for different timeframes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OHLC data retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getOHLC(@RequestBody OHLCRequest request) {
        Set<String> symbolList = parseSymbols(request.getSymbols());
        try (FlowSpan span = flowLogger.start("market.ohlc.fetch",
                "symbolsCount", symbolList.size(), "timeFrame", request.getTimeFrame(), "indexSymbol",
                request.isIndexSymbol(), "forceRefresh", request.isForceRefresh())) {
            try {
                Map<String, OHLCQuote> response = marketDataCacheService.getOHLC(
                        symbolList, request.isIndexSymbol(), TimeFrame.fromApiValue(request.getTimeFrame()),
                        request.isForceRefresh());

                flowLogger.complete(span, "resultCount", response.size());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Error getting OHLC symbolsCount={}", symbolList.size(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to fetch OHLC data");
                errorResponse.put("message", e.getMessage());
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get historical data for one or more instruments
     * 
     * @param request Request body containing symbols, date range, and other
     *                parameters
     * @return Historical data with metadata
     */
    @PostMapping(value = "/historical-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get historical market data", description = "Retrieves historical price and volume data for one or more instruments with filtering options")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historical data retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<HistoricalDataResponseV1> getHistoricalData(@RequestBody HistoricalDataRequest request) {
        Set<String> symbolList = parseSymbols(request.getSymbols());
        try (FlowSpan span = flowLogger.start("market.historical.fetch",
                "symbolsCount", symbolList.size(), "interval", request.getInterval(), "forceRefresh",
                request.isForceRefresh())) {
            try {
                HistoricalDataResponseV1 response = marketDataCacheService.processHistoricalDataRequest(request);

                if (response.getError() != null) {
                    log.warn("Historical data fetch returned error branch symbolsCount={}", symbolList.size());
                    String errorType = response.getError();
                    if (errorType.contains("No valid symbols") || errorType.contains("Invalid date format")) {
                        flowLogger.warn(span, "Validation error: " + errorType);
                        return ResponseEntity.badRequest().body(response);
                    } else {
                        flowLogger.fail(span, new Exception(response.getError()));
                        return ResponseEntity.internalServerError().body(response);
                    }
                }

                flowLogger.complete(span, "resultCount", response.getData() != null ? response.getData().size() : 0);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Unexpected error in controller while getting historical data symbolsCount={}", symbolList.size(),
                        e);
                HistoricalDataResponseV1 errorResponse = HistoricalDataResponseV1.builder()
                        .error("Failed to fetch historical data")
                        .message(e.getMessage())
                        .build();
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get symbols for a specific exchange
     * 
     * @param exchange Exchange name
     * @return List of symbols for the exchange
     */
    @GetMapping(value = "/symbols/{exchange}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get symbols for a specific exchange", description = "Retrieves all available trading symbols for a specific exchange")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Symbols retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Object>> getSymbolsForExchange(
            @PathVariable String exchange) {
        try {
            List<Object> symbols = marketDataService.getSymbolsForExchange(exchange, null);
            return ResponseEntity.ok(symbols);
        } catch (Exception e) {
            log.error("getSymbolsForExchange", "Error getting symbols for exchange: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Logout and invalidate session
     * 
     * @return Success status
     */
    @PostMapping(value = "/auth/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Logout and invalidate session", description = "Invalidates the current broker session and clears authentication tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> logout() {
        try {
            Map<String, Object> response = marketDataService.logout(null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("logout", "Error logging out", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get option chain data for a given underlying instrument
     * 
     * @param underlyingSymbol Symbol of the underlying instrument
     * @param expiryDate       Optional expiry date (yyyy-MM-dd)
     * @param forceRefresh     Whether to force refresh from provider
     * @return Option chain data with calls and puts
     */
    @GetMapping(value = "/option-chain", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get option chain data", description = "Retrieves option chain data including calls and puts for a given underlying instrument")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Option chain data retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getOptionChain(
            @RequestParam("symbol") String underlyingSymbol,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(name = "refresh", defaultValue = "false") boolean forceRefresh) {
        try (FlowSpan span = flowLogger.start("market.optionchain.fetch",
                "underlying", underlyingSymbol, "expiryDate", expiryDate, "forceRefresh", forceRefresh)) {
            try {
                Date expiry = null;
                if (expiryDate != null && !expiryDate.isEmpty()) {
                    try {
                        expiry = dateFormat.parse(expiryDate);
                    } catch (ParseException e) {
                        log.warn("Invalid date format for option chain underlying={} expiryDate={}", underlyingSymbol,
                                expiryDate);
                        Map<String, Object> errorResponse = new HashMap<>();
                        errorResponse.put("error", "Invalid date format");
                        errorResponse.put("message", "Use yyyy-MM-dd format for expiry date");
                        flowLogger.warn(span, "Invalid date format: " + expiryDate);
                        return ResponseEntity.badRequest().body(errorResponse);
                    }
                }

                Map<String, Object> response = marketDataCacheService.getOptionChain(underlyingSymbol, expiry,
                        forceRefresh);

                if (response.containsKey("error")) {
                    log.warn("Option chain fetch returned error underlying={}", underlyingSymbol);
                    flowLogger.fail(span, new Exception(String.valueOf(response.get("error"))));
                    return ResponseEntity.internalServerError().body(response);
                }

                if (!response.containsKey("cached")) {
                    response.put("cached", !forceRefresh);
                }

                flowLogger.complete(span);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Unexpected error in controller while getting option chain underlying={}", underlyingSymbol, e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to fetch option chain");
                errorResponse.put("message", e.getMessage());
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get mutual fund details including NAV, returns, etc.
     * 
     * @param schemeCode   Mutual fund scheme code
     * @param forceRefresh Whether to force refresh from provider
     * @return Mutual fund details
     */
    @GetMapping(value = "/mutual-fund/{schemeCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get mutual fund details", description = "Retrieves detailed information about a mutual fund including NAV, returns, and other metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mutual fund details retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getMutualFundDetails(
            @PathVariable String schemeCode,
            @RequestParam(name = "refresh", defaultValue = "false") boolean forceRefresh) {
        try (FlowSpan span = flowLogger.start("market.mutualfund.fetch",
                "schemeCode", schemeCode, "forceRefresh", forceRefresh)) {
            try {
                Map<String, Object> response = marketDataCacheService.getMutualFundDetails(schemeCode, forceRefresh);

                if (response.containsKey("error")) {
                    log.warn("Mutual fund details fetch returned error schemeCode={}", schemeCode);
                    flowLogger.fail(span, new Exception(String.valueOf(response.get("error"))));
                    return ResponseEntity.internalServerError().body(response);
                }

                if (!response.containsKey("cached")) {
                    response.put("cached", !forceRefresh);
                }

                flowLogger.complete(span);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Unexpected error in controller while fetching mutual fund details schemeCode={}", schemeCode, e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to fetch mutual fund details");
                errorResponse.put("message", e.getMessage());
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get mutual fund NAV history
     * 
     * @param schemeCode   Mutual fund scheme code
     * @param from         Start date (yyyy-MM-dd)
     * @param to           End date (yyyy-MM-dd)
     * @param forceRefresh Whether to force refresh from provider
     * @return NAV history data
     */
    @GetMapping(value = "/mutual-fund/{schemeCode}/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get mutual fund NAV history", description = "Retrieves historical Net Asset Value (NAV) data for a mutual fund over a specified date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "NAV history retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date format or request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getMutualFundNavHistory(
            @PathVariable String schemeCode,
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam(name = "refresh", defaultValue = "false") boolean forceRefresh) {
        try (FlowSpan span = flowLogger.start("market.mutualfund.history.fetch",
                "schemeCode", schemeCode, "from", from, "to", to, "forceRefresh", forceRefresh)) {
            try {
                Date fromDate;
                Date toDate;
                try {
                    fromDate = dateFormat.parse(from);
                    toDate = dateFormat.parse(to);
                } catch (ParseException e) {
                    log.warn("Invalid date format for mutual fund history schemeCode={} from={} to={}", schemeCode, from,
                            to);
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid date format");
                    errorResponse.put("message", "Use yyyy-MM-dd format for dates");
                    flowLogger.warn(span, "Invalid date format: " + from + " - " + to);
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                Map<String, Object> response = marketDataCacheService.getMutualFundNavHistory(schemeCode, fromDate, toDate,
                        forceRefresh);

                if (response.containsKey("error")) {
                    log.warn("Mutual fund history fetch returned error schemeCode={}", schemeCode);
                    flowLogger.fail(span, new Exception(String.valueOf(response.get("error"))));
                    return ResponseEntity.internalServerError().body(response);
                }

                if (!response.containsKey("cached")) {
                    response.put("cached", !forceRefresh);
                }

                flowLogger.complete(span);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Unexpected error in controller while fetching mutual fund NAV history schemeCode={}", schemeCode,
                        e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to fetch mutual fund NAV history");
                errorResponse.put("message", e.getMessage());
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get live prices for all symbols or filtered by symbol IDs
     * 
     * @param symbols      Optional comma-separated list of trading symbols to
     *                     filter by
     * @param indexSymbol  Whether the symbols are index symbols
     * @param forceRefresh Whether to force refresh from provider
     * @return Map containing prices, count, timestamp and processing time
     */
    @GetMapping(value = "/live-prices", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get live market prices", description = "Retrieves real-time market prices for specified symbols or all available symbols")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Live prices retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getLivePrices(
            @RequestParam(name = "symbols", required = false) String symbols,
            @RequestParam(name = "isIndexSymbol", required = false) boolean indexSymbol,
            @RequestParam(name = "refresh", defaultValue = "false") boolean forceRefresh) {
        Set<String> symbolList = parseSymbols(symbols);
        try (FlowSpan span = flowLogger.start("market.liveprices.fetch",
                "symbolsCount", symbolList.size(), "isIndex", indexSymbol, "forceRefresh", forceRefresh)) {
            try {
                Map<String, Object> response = marketDataCacheService.getLivePrices(symbolList, indexSymbol, forceRefresh);

                if (response.containsKey("error")) {
                    log.warn("Live prices fetch returned error symbolsCount={}", symbolList.size());
                    flowLogger.fail(span, new Exception(String.valueOf(response.get("error"))));
                    return ResponseEntity.internalServerError().body(response);
                }

                if (!response.containsKey("cached")) {
                    response.put("cached", !forceRefresh);
                }

                flowLogger.complete(span, "resultCount",
                        response.containsKey("prices") ? ((List<?>) response.get("prices")).size() : 0);
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Unexpected error in controller while fetching live prices symbolsCount={}", symbolList.size(),
                        e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to fetch live prices");
                errorResponse.put("message", e.getMessage());
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Get live LTP with change calculation based on historical closing price
     * 
     * @param symbols     Comma-separated list of symbols
     * @param timeframe   Timeframe for historical comparison (1D, 1W, 1M, 1Y)
     * @param indexSymbol Whether symbols are indices
     * @return Map containing LTP, change, and changePercent for each symbol
     */
    @GetMapping(value = "/live-ltp", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get live LTP with change calculation", description = "Retrieves current LTP and calculates change based on historical closing price for the specified timeframe")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Live LTP with change retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getLiveLTP(
            @RequestParam(name = "symbols", required = true) String symbols,
            @RequestParam(name = "timeframe", defaultValue = "1D") String timeframe,
            @RequestParam(name = "isIndexSymbol", required = false, defaultValue = "true") boolean indexSymbol,
            @RequestParam(name = "refresh", defaultValue = "false") boolean forceRefresh) {

        Set<String> symbolList = parseSymbols(symbols);
        try (FlowSpan span = flowLogger.start("market.live.ltp.fetch",
                "symbolsCount", symbolList.size(), "timeframe", timeframe, "isIndex", indexSymbol, "forceRefresh",
                forceRefresh)) {
            try {
                if (symbolList.isEmpty()) {
                    log.warn("Live LTP request with no symbols");
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "No symbols provided");
                    flowLogger.warn(span, "No symbols provided");
                    return ResponseEntity.badRequest().body(errorResponse);
                }

                TimeFrame tf;
                try {
                    tf = TimeFrame.fromApiValue(timeframe);
                } catch (Exception e) {
                    log.warn("Invalid timeframe: {} defaulting to 1D", timeframe);
                    tf = TimeFrame.DAY;
                }

                Map<String, OHLCQuote> historicalData = marketDataCacheService.getOHLC(symbolList, indexSymbol, tf, false);
                Map<String, Object> livePrices = marketDataCacheService.getLivePrices(symbolList, indexSymbol,
                        forceRefresh);

                Map<String, Map<String, Object>> result = new HashMap<>();
                Object pricesObj = livePrices.get("prices");
                if (pricesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> pricesList = (List<Map<String, Object>>) pricesObj;

                    for (Map<String, Object> priceData : pricesList) {
                        String symbol = (String) priceData.get("symbol");
                        if (symbol == null)
                            continue;

                        Double currentPrice = ((Number) priceData.get("lastPrice")).doubleValue();
                        OHLCQuote historical = historicalData.get(symbol);
                        if (historical == null) {
                            historical = historicalData.get("NSE:" + symbol);
                        }

                        double previousClose = 0.0;
                        if (historical != null && historical.getOhlc() != null) {
                            previousClose = historical.getOhlc().getClose();
                        }

                        double change = currentPrice - previousClose;
                        double changePercent = previousClose != 0 ? (change / previousClose) * 100 : 0.0;

                        Map<String, Object> ltpData = new HashMap<>();
                        ltpData.put("symbol", symbol);
                        ltpData.put("lastPrice", currentPrice);
                        ltpData.put("previousClose", previousClose);
                        ltpData.put("change", change);
                        ltpData.put("changePercent", changePercent);
                        ltpData.put("timeframe", tf.getApiValue());

                        result.put(symbol, ltpData);
                    }
                }

                Map<String, Object> response = new HashMap<>();
                response.put("count", result.size());
                response.put("timeframe", tf.getApiValue());
                response.put("data", result);
                response.put("timestamp", new Date());

                flowLogger.complete(span, "resultCount", result.size());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Unexpected error while fetching live LTP symbolsCount={}", symbolList.size(), e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to fetch live LTP");
                errorResponse.put("message", e.getMessage());
                flowLogger.fail(span, e);
                return ResponseEntity.internalServerError().body(errorResponse);
            }
        }
    }

    /**
     * Utility method to convert comma-separated string to Set of symbols
     * 
     * @param symbols Comma-separated string of symbols
     * @return Set of trimmed symbols, or empty set if input is null/empty
     */
    private Set<String> parseSymbols(String symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return new HashSet<>();
        }

        return Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}

