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

import com.am.marketdata.common.log.AppLogger;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for market data operations
 * Provides endpoints for fetching various types of market data including
 * quotes, OHLC, historical data,
 * option chains, mutual fund details, and more
 */
@RestController
@RequestMapping("/v1/market-data")
@Tag(name = "Market Data", description = "APIs for retrieving various types of market data including quotes, historical data, option chains, and more")
public class MarketDataController {

    private final AppLogger log = AppLogger.getLogger(MarketDataController.class);
    private final MarketDataService marketDataService;
    private final MarketDataFetchService marketDataCacheService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public MarketDataController(MarketDataService marketDataService,
            MarketDataFetchService marketDataCacheService) {
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
        try {
            Map<String, String> response = marketDataService.getLoginUrl(provider);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getLoginUrl", "Error getting login URL", e);
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
            // Check status parameter - only proceed if it's "success" or not provided
            if (!"success".equalsIgnoreCase(status)) {
                log.error("generateSession", "Authentication failed with status: " + status);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Authentication failed");
                errorResponse.put("message", "Login was not successful. Status: " + status);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Use request_token if provided, otherwise fall back to requestToken
            String token = requestToken != null ? requestToken : requestTokenAlt;

            if (code != null) {
                token = code;
            }
            if (token == null) {
                log.error("generateSession",
                        "No request token provided in either request_token or requestToken parameters");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Missing request token");
                errorResponse.put("message", "No request token provided");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("generateSession", "Generating session with token: " + token);
            Object session = marketDataService.generateSession(token);
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            log.error("generateSession", "Error generating session: " + e.getMessage(), e);
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
        try {
            log.info("getQuotes",
                    String.format(
                            "Controller received request for quotes for symbols: %s, timeFrame: %s, forceRefresh: %s",
                            symbols, timeFrameStr, forceRefresh));

            // Parse symbols and timeframe
            Set<String> symbolList = parseSymbols(symbols);
            TimeFrame timeFrame = TimeFrame.fromApiValue(timeFrameStr);

            // Use cache service instead of direct service call
            Map<String, Object> quotesResponse = marketDataCacheService.getQuotes(symbolList, false, timeFrame,
                    forceRefresh);

            // Check if there was an error
            if (quotesResponse.containsKey("ERROR")) {
                Map<String, Object> errorResponse = new HashMap<>();
                @SuppressWarnings("unchecked")
                Map<String, Object> errorDetails = (Map<String, Object>) quotesResponse.get("ERROR");
                errorResponse.put("error", errorDetails.get("error"));
                errorResponse.put("message", errorDetails.get("message"));
                return ResponseEntity.internalServerError().body(errorResponse);
            }

            return ResponseEntity.ok(quotesResponse);
        } catch (IllegalArgumentException e) {
            // Handle invalid timeframe
            log.error("getQuotes", "Invalid timeFrame parameter: " + timeFrameStr, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "INVALID_PARAMETER");
            errorResponse.put("message", "Invalid timeFrame parameter: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            log.error("getQuotes", "Error processing quotes request: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "INTERNAL_ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
        try {
            log.info("getQuotesPost", String.format(
                    "Controller received POST request for quotes for symbols: %s, timeFrame: %s, forceRefresh: %s",
                    request.getSymbols(), request.getTimeFrame(), request.isForceRefresh()));

            // Parse symbols
            Set<String> symbolList = parseSymbols(request.getSymbols());

            // Use cache service instead of direct service call
            Map<String, Object> quotesResponse = marketDataCacheService.getQuotes(
                    symbolList, request.isIndexSymbol(), TimeFrame.fromApiValue(request.getTimeFrame()),
                    request.isForceRefresh());

            // Check if there was an error
            if (quotesResponse.containsKey("ERROR")) {
                Map<String, Object> errorResponse = new HashMap<>();
                @SuppressWarnings("unchecked")
                Map<String, Object> errorDetails = (Map<String, Object>) quotesResponse.get("ERROR");
                errorResponse.put("error", errorDetails.get("error"));
                errorResponse.put("message", errorDetails.get("message"));
                return ResponseEntity.internalServerError().body(errorResponse);
            }

            return ResponseEntity.ok(quotesResponse);
        } catch (Exception e) {
            log.error("getQuotesPost", "Error processing quotes request: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "INTERNAL_ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
        try {
            log.info("getOHLC",
                    String.format(
                            "Controller received POST request for OHLC data for symbols: %s, timeFrame: %s, forceRefresh: %s, indexSymbol: %s",
                            request.getSymbols(), request.getTimeFrame(), request.isForceRefresh(),
                            request.isIndexSymbol()));
            Set<String> symbolList = parseSymbols(request.getSymbols());

            // Use cache service instead of direct service call
            Map<String, OHLCQuote> response = marketDataCacheService.getOHLC(
                    symbolList, request.isIndexSymbol(), TimeFrame.fromApiValue(request.getTimeFrame()),
                    request.isForceRefresh());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getOHLC", "Error getting OHLC: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch OHLC data");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
        String methodName = "getHistoricalData";
        log.info(methodName, "Received historical data request for: " + request.getSymbols());

        try {
            // Delegate all processing to the service
            HistoricalDataResponseV1 response = marketDataCacheService.processHistoricalDataRequest(request);

            // Check if there was an error
            if (response.getError() != null) {
                // Determine if it's a client error or server error
                String errorType = response.getError();
                if (errorType.contains("No valid symbols") || errorType.contains("Invalid date format")) {
                    return ResponseEntity.badRequest().body(response);
                } else {
                    return ResponseEntity.internalServerError().body(response);
                }
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getHistoricalData",
                    "Unexpected error in controller while getting historical data: " + e.getMessage(), e);
            HistoricalDataResponseV1 errorResponse = HistoricalDataResponseV1.builder()
                    .error("Failed to fetch historical data")
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
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
        try {
            log.info("Controller received request for option chain for symbol: {} with expiry: {}, forceRefresh: {}",
                    underlyingSymbol, expiryDate, forceRefresh);

            Date expiry = null;
            if (expiryDate != null && !expiryDate.isEmpty()) {
                try {
                    expiry = dateFormat.parse(expiryDate);
                } catch (ParseException e) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Invalid date format");
                    errorResponse.put("message", "Use yyyy-MM-dd format for expiry date");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }

            // Use cache service instead of direct service call
            Map<String, Object> response = marketDataCacheService.getOptionChain(underlyingSymbol, expiry,
                    forceRefresh);

            // Check if there was an error
            if (response.containsKey("error")) {
                return ResponseEntity.internalServerError().body(response);
            }

            // Add cache status to response
            if (!response.containsKey("cached")) {
                response.put("cached", !forceRefresh);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getOptionChain", "Unexpected error in controller while getting option chain: " + e.getMessage(),
                    e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch option chain");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
        try {
            log.info("Controller received request for mutual fund details for scheme code: {}, forceRefresh: {}",
                    schemeCode, forceRefresh);

            // Use cache service instead of direct service call
            Map<String, Object> response = marketDataCacheService.getMutualFundDetails(schemeCode, forceRefresh);

            // Check if there was an error
            if (response.containsKey("error")) {
                return ResponseEntity.internalServerError().body(response);
            }

            // Add cache status to response
            if (!response.containsKey("cached")) {
                response.put("cached", !forceRefresh);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getMutualFundDetails",
                    "Unexpected error in controller while fetching mutual fund details: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch mutual fund details");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
        try {
            log.info(
                    "Controller received request for mutual fund NAV history for scheme code: {} from {} to {}, forceRefresh: {}",
                    schemeCode, from, to, forceRefresh);

            Date fromDate;
            Date toDate;
            try {
                fromDate = dateFormat.parse(from);
                toDate = dateFormat.parse(to);
            } catch (ParseException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid date format");
                errorResponse.put("message", "Use yyyy-MM-dd format for dates");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Use cache service instead of direct service call
            Map<String, Object> response = marketDataCacheService.getMutualFundNavHistory(schemeCode, fromDate, toDate,
                    forceRefresh);

            // Check if there was an error
            if (response.containsKey("error")) {
                return ResponseEntity.internalServerError().body(response);
            }

            // Add cache status to response
            if (!response.containsKey("cached")) {
                response.put("cached", !forceRefresh);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getMutualFundNavHistory",
                    "Unexpected error in controller while fetching mutual fund NAV history: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch mutual fund NAV history");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
        try {
            Set<String> symbolList = parseSymbols(symbols);

            // Use cache service instead of direct service call
            Map<String, Object> response = marketDataCacheService.getLivePrices(symbolList, indexSymbol, forceRefresh);

            // Check if there was an error
            if (response.containsKey("error")) {
                return ResponseEntity.internalServerError().body(response);
            }

            // Add cache status to response
            if (!response.containsKey("cached")) {
                response.put("cached", !forceRefresh);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("getLivePrices", "Unexpected error in controller while fetching live prices: " + e.getMessage(),
                    e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch live prices");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
        try {
            log.info("getLiveLTP", "Fetching live LTP for symbols: " + symbols + ", timeframe: " + timeframe
                    + ", forceRefresh: " + forceRefresh);

            Set<String> symbolList = parseSymbols(symbols);
            if (symbolList.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No symbols provided");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Parse timeframe
            TimeFrame tf;
            try {
                tf = TimeFrame.fromApiValue(timeframe);
            } catch (Exception e) {
                log.warn("getLiveLTP", "Invalid timeframe: " + timeframe + ", defaulting to 1D");
                tf = TimeFrame.DAY;
            }

            // Step 1: Fetch historical data (last closing price) via cache → DB → provider
            log.info("getLiveLTP", "Fetching historical OHLC data for " + symbolList.size()
                    + " symbols with timeframe: " + tf.getApiValue());
            Map<String, OHLCQuote> historicalData = marketDataCacheService.getOHLC(symbolList, indexSymbol, tf, false);

            // Step 2: Fetch current live prices
            log.info("getLiveLTP", "Fetching current live prices for " + symbolList.size() + " symbols");
            Map<String, Object> livePrices = marketDataCacheService.getLivePrices(symbolList, indexSymbol,
                    forceRefresh);

            // Step 3: Calculate change and percentage change
            Map<String, Map<String, Object>> result = new HashMap<>();

            // Extract prices from live prices response
            Object pricesObj = livePrices.get("prices");
            if (pricesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> pricesList = (List<Map<String, Object>>) pricesObj;

                for (Map<String, Object> priceData : pricesList) {
                    String symbol = (String) priceData.get("symbol");
                    if (symbol == null)
                        continue;

                    Double currentPrice = ((Number) priceData.get("lastPrice")).doubleValue();

                    // Get historical closing price
                    OHLCQuote historical = historicalData.get(symbol);
                    if (historical == null) {
                        // Try with NSE: prefix
                        historical = historicalData.get("NSE:" + symbol);
                    }

                    double previousClose = 0.0;
                    if (historical != null && historical.getOhlc() != null) {
                        previousClose = historical.getOhlc().getClose();
                    } else {
                        log.warn("getLiveLTP",
                                "No historical data found for symbol: " + symbol + ", using 0 as previous close");
                    }

                    // Calculate change and percentage
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

            log.info("getLiveLTP", "Successfully calculated LTP with change for " + result.size() + " symbols");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("getLiveLTP", "Unexpected error while fetching live LTP: " + e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch live LTP");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
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
