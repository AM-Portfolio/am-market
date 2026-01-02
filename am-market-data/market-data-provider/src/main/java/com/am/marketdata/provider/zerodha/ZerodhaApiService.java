package com.am.marketdata.provider.zerodha;

import com.am.marketdata.common.model.TimeFrame;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.*;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnDisconnect;
import com.zerodhatech.ticker.OnTicks;
import com.zerodhatech.ticker.OnError;

import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for interacting with Zerodha Kite Connect API
 * Implements resilient patterns including retry, circuit breaker, and metrics
 */
import com.am.marketdata.common.log.AppLogger;
import org.springframework.stereotype.Service;

@Service
public class ZerodhaApiService {

    private final AppLogger log = AppLogger.getLogger();

    private KiteConnect kiteConnect;
    private KiteTicker tickerProvider;
    private final com.am.common.investment.service.instrument.InstrumentService instrumentService;
    private final MeterRegistry meterRegistry;
    private final ThreadPoolExecutor threadPoolExecutor;

    @Value("${market-data.zerodha.api.key}")
    private String apiKey;

    @Value("${market-data.zerodha.api.secret}")
    private String apiSecret;

    @Value("${market-data.zerodha.api.max.retries:3}")
    private int maxRetries;

    @Value("${market-data.zerodha.api.retry.delay.ms:1000}")
    private int retryDelayMs;

    @Value("${market-data.zerodha.ticker.reconnect.max.retries:10}")
    private int tickerMaxRetries;

    @Value("${market-data.zerodha.ticker.reconnect.interval:30}")
    private int tickerReconnectInterval;

    @Value("${market-data.zerodha.api.access.token}")
    private String accessToken;

    @Value("${market-data.zerodha.api.refresh.token:}")
    private String refreshToken;

    public ZerodhaApiService(com.am.common.investment.service.instrument.InstrumentService instrumentService,
            MeterRegistry meterRegistry, ThreadPoolExecutor threadPoolExecutor) {
        this.instrumentService = instrumentService;
        this.meterRegistry = meterRegistry;
        this.threadPoolExecutor = threadPoolExecutor;
        initialize();
        log.info("ZerodhaApiService", "Initializing Zerodha API service");
    }

    /**
     * Convert trading symbols to instrument token IDs
     * 
     * @param symbols Array of trading symbols
     * @return Array of instrument token IDs as strings
     */
    private String[] convertSymbolsToInstrumentIds(String[] symbols) {
        List<com.am.common.investment.model.equity.Instrument> instruments = instrumentService
                .getInstrumentByTradingsymbols(Arrays.asList(symbols));
        List<Long> instrumentIds = instruments.stream()
                .map(com.am.common.investment.model.equity.Instrument::getInstrumentToken)
                .collect(Collectors.toList());
        return instrumentIds.stream().map(Object::toString).toArray(String[]::new);
    }

    /**
     * Convert a map with instrument IDs as keys to a map with symbols as keys
     * 
     * @param <T>           Type of the value in the map
     * @param instrumentMap Map with instrument IDs as keys
     * @return Map with symbols as keys and the original values
     */
    private <T> Map<String, T> convertInstrumentMaptoSymbolMap(Map<String, T> instrumentMap) {
        if (instrumentMap == null || instrumentMap.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, T> symbolMap = new HashMap<>();

        try {
            // Get all instruments by their IDs
            List<String> instrumentIds = new ArrayList<>(instrumentMap.keySet());
            List<com.am.common.investment.model.equity.Instrument> instruments = instrumentService
                    .getInstrumentByInstrumentTokens(
                            instrumentIds.stream()
                                    .map(Long::parseLong)
                                    .collect(Collectors.toList()));

            // Create mapping from instrument ID to trading symbol
            Map<String, String> idToSymbolMap = instruments.stream()
                    .collect(Collectors.toMap(
                            instrument -> instrument.getInstrumentToken().toString(),
                            com.am.common.investment.model.equity.Instrument::getTradingSymbol,
                            (existing, replacement) -> existing)); // Keep first in case of duplicates

            // Convert the original map using the ID to symbol mapping
            for (Map.Entry<String, T> entry : instrumentMap.entrySet()) {
                String instrumentId = entry.getKey();
                String symbol = idToSymbolMap.get(instrumentId);
                if (symbol != null) {
                    symbolMap.put(symbol, entry.getValue());
                } else {
                    log.warn("convertInstrumentMaptoSymbolMap", "No symbol found for instrument ID: {}", instrumentId);
                    // Fallback to using the instrument ID as the key
                    symbolMap.put(instrumentId, entry.getValue());
                }
            }

            return symbolMap;
        } catch (Exception e) {
            log.error("convertInstrumentMaptoSymbolMap",
                    "Error converting instrument map to symbol map: " + e.getMessage(), e);
            return instrumentMap; // Return original map on error
        }
    }

    /**
     * Handle HistoricalData conversion - this is not a map so needs special
     * handling
     * 
     * @param historicalData The historical data to process
     * @return The same historical data (symbol conversion happens at the instrument
     *         level)
     */
    private HistoricalData convertInstrumentMaptoSymbolMap(HistoricalData historicalData) {
        // HistoricalData is not a map, so we can't convert keys
        // Just return the original data - the symbol conversion is handled at the API
        // call level
        return historicalData;
    }

    @PostConstruct
    public void initialize() {
        log.info("initialize", "Initializing Zerodha API service with API key: " + apiKey);

        // Re-initialize KiteConnect with injected properties
        this.kiteConnect = new KiteConnect(apiKey, true);

        // Set access token if available
        if (accessToken != null && !accessToken.isEmpty()) {
            log.info("initialize",
                    "Setting access token: " + accessToken.substring(0, Math.min(5, accessToken.length())) + "...");
            this.kiteConnect.setAccessToken(accessToken);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (tickerProvider != null && tickerProvider.isConnectionOpen()) {
            log.info("cleanup", "Disconnecting Zerodha ticker");
            tickerProvider.disconnect();
        }
        log.info("cleanup", "Cleaned up Zerodha API service resources");
    }

    /**
     * Sets the access token for API authentication
     * 
     * @param accessToken The access token from Zerodha
     */
    public void setAccessToken(String accessToken) {
        kiteConnect.setAccessToken(accessToken);
        this.accessToken = accessToken;
        log.info("setAccessToken", "Set Zerodha access token: " + maskToken(accessToken));
    }

    /**
     * Generate session URL for user login
     * 
     * @return Login URL for Zerodha authentication
     */
    public String getLoginUrl() {
        return kiteConnect.getLoginURL();
    }

    /**
     * Generate access token from request token
     * 
     * @param requestToken Request token received after login
     * @return User object containing access token
     */
    // @Retry(name = "marketDataZerodhaApi")
    public User generateSession(String requestToken) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            // Check if KiteConnect is properly initialized
            if (kiteConnect == null) {
                log.info("generateSession", "KiteConnect is null, initializing it now");
                initialize();
            }

            // If refresh token is not available or refresh failed, generate a new session
            // Validate parameters
            if (requestToken == null || requestToken.isEmpty()) {
                throw new IllegalArgumentException("Request token cannot be null or empty");
            }

            log.info("generateSession", "Generating Zerodha session with request token: " + requestToken);
            log.info("generateSession", String.format("Using API key: %s, API secret length: %d", apiKey,
                    apiSecret != null ? apiSecret.length() : 0));

            // Validate API key and secret
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("API key is not configured");
            }

            if (apiSecret == null || apiSecret.isEmpty()) {
                throw new IllegalStateException("API secret is not configured");
            }

            // Debug the KiteConnect instance
            log.debug("generateSession", "KiteConnect instance: {}", kiteConnect);

            // Generate session with detailed logging
            log.info("generateSession",
                    "Calling kiteConnect.generateSession with requestToken length: {}, apiSecret length: {}",
                    requestToken.length(), apiSecret.length());

            User user = kiteConnect.generateSession(requestToken, apiSecret);

            // Record metrics
            sample.stop(meterRegistry.timer("market-data.zerodha.api.session.time"));
            meterRegistry.counter("market-data.zerodha.api.session.success").increment();

            // Log success
            if (user != null && user.accessToken != null) {
                log.info("generateSession", "Successfully generated Zerodha session, access token: {}",
                        user.accessToken.substring(0, Math.min(5, user.accessToken.length())) + "...");
                setAccessToken(user.accessToken);

                // Store refresh token if available
                if (user.refreshToken != null && !user.refreshToken.isEmpty()) {
                    this.refreshToken = user.refreshToken;
                    log.info("generateSession", "Stored refresh token for future use");
                }
            } else {
                log.warn("generateSession", "Generated session but user or access token is null");
            }

            return user;
        } catch (KiteException | IOException e) {
            // Record error metrics
            meterRegistry.counter("market-data.zerodha.api.session.error", "error_type", getErrorType(e)).increment();

            // Enhanced error logging
            log.error("generateSession", "Failed to generate Zerodha session: " + e.getMessage(), e);
            log.error("generateSession", String.format("Error details - Request token: %s, API key: %s",
                    requestToken != null ? requestToken.substring(0, Math.min(5, requestToken.length())) + "..."
                            : "null",
                    apiKey != null ? apiKey.substring(0, Math.min(5, apiKey.length())) + "..." : "null"),
                    (Throwable) null);

            throw new ZerodhaApiException("Failed to generate session: " + e.getMessage(), e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            meterRegistry.counter("market-data.zerodha.api.session.error", "error_type", "unexpected").increment();
            log.error("generateSession", "Unexpected error generating Zerodha session: " + e.getMessage(), e);
            throw new ZerodhaApiException("Unexpected error generating session: " + e.getMessage(), e);
        }
    }

    /**
     * Get user profile information
     * 
     * @return Profile object with user details
     */
    // @Retry(name = "marketDataZerodhaApi")
    public Profile getProfile() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Profile profile = kiteConnect.getProfile();
            sample.stop(meterRegistry.timer("market-data.zerodha.api.profile.time"));
            meterRegistry.counter("market-data.zerodha.api.profile.success").increment();
            return profile;
        } catch (KiteException | IOException e) {
            meterRegistry.counter("market-data.zerodha.api.profile.error", "error_type", getErrorType(e)).increment();
            log.error("getProfile", "Failed to get profile: " + e.getMessage(), e);
            throw new ZerodhaApiException("Failed to get profile", e);
        }
    }

    /**
     * Get quotes for multiple instruments
     * 
     * @param instruments Array of instruments in format [exchange:tradingsymbol]
     *                    (e.g., ["NSE:INFY", "BSE:SBIN"])
     * @return Map of instrument to Quote object
     */
    // @Retry(name = "marketDataZerodhaApi")
    public Map<String, Quote> getQuotes(String[] symbols) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Prefix all symbols with NSE: if not already prefixed
            String[] prefixedSymbols = prefixSymbolsWithNSE(symbols);

            Map<String, Quote> quotes = kiteConnect.getQuote(prefixedSymbols);
            sample.stop(meterRegistry.timer("market-data.zerodha.api.quotes.time"));
            meterRegistry.counter("market-data.zerodha.api.quotes.success").increment();
            return convertInstrumentMaptoSymbolMap(quotes);
        } catch (KiteException | IOException e) {
            meterRegistry.counter("market-data.zerodha.api.quotes.error", "error_type", getErrorType(e)).increment();
            log.error("getQuotes",
                    "Failed to get quotes for instruments " + Arrays.toString(symbols) + ": " + e.getMessage(), e);
            throw new ZerodhaApiException("Failed to get quotes", e);
        }
    }

    /**
     * Get OHLC and last price for multiple instruments
     * 
     * @param instruments Array of instruments in format [exchange:tradingsymbol]
     *                    (e.g., ["NSE:INFY", "BSE:SBIN"])
     * @return Map of instrument to OHLC object
     */
    // @Retry(name = "marketDataZerodhaApi")
    public Map<String, OHLCQuote> getOHLC(String[] symbols) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Prefix all symbols with NSE: if not already prefixed
            String[] prefixedSymbols = prefixSymbolsWithNSE(symbols);

            Map<String, OHLCQuote> ohlc = kiteConnect.getOHLC(prefixedSymbols);
            sample.stop(meterRegistry.timer("market-data.zerodha.api.ohlc.time"));
            meterRegistry.counter("market-data.zerodha.api.ohlc.success").increment();
            return ohlc;
        } catch (KiteException | IOException e) {
            meterRegistry.counter("market-data.zerodha.api.ohlc.error", "error_type", getErrorType(e)).increment();
            log.error("getOHLC",
                    "Failed to get OHLC for instruments " + Arrays.toString(symbols) + ": " + e.getMessage(), e);
            throw new ZerodhaApiException("Failed to get OHLC", e);
        }
    }

    /**
     * Get last price for multiple instruments
     * 
     * @param instruments Array of instruments in format [exchange:tradingsymbol]
     *                    (e.g., ["NSE:INFY", "BSE:SBIN"])
     * @return Map of instrument to LTP object
     */
    public Map<String, LTPQuote> getLTP(String[] symbols) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Prefix all symbols with NSE: if not already prefixed
            String[] prefixedSymbols = prefixSymbolsWithNSE(symbols);

            Map<String, LTPQuote> ltp = kiteConnect.getLTP(prefixedSymbols);
            sample.stop(meterRegistry.timer("market-data.zerodha.api.ltp.time"));
            meterRegistry.counter("market-data.zerodha.api.ltp.success").increment();
            return ltp;
        } catch (KiteException | IOException e) {
            meterRegistry.counter("market-data.zerodha.api.ltp.error", "error_type", getErrorType(e)).increment();
            log.error("getLTP", "Failed to get LTP for instruments " + Arrays.toString(symbols) + ": " + e.getMessage(),
                    e);
            throw new ZerodhaApiException("Failed to get LTP", e);
        }
    }

    /**
     * Get historical data for an instrument
     * 
     * @param instrumentToken Instrument token
     * @param from            From date
     * @param to              To date
     * @param interval        Interval (minute, day, etc.)
     * @param continuous      Continuous flag for F&O contracts
     * @param oi              Include open interest
     * @return Historical data object
     */
    // @Retry(name = "marketDataZerodhaApi")
    public HistoricalData getHistoricalData(String symbol, Date from, Date to, TimeFrame interval, boolean continuous,
            boolean oi) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            // Convert interval to TimeFrame for proper mapping
            String zerodhaInterval = interval.getZerodhaValue();
            String[] instrumentIdsArray = convertSymbolsToInstrumentIds(new String[] { symbol });
            HistoricalData historicalData = kiteConnect.getHistoricalData(from, to, instrumentIdsArray[0],
                    zerodhaInterval, continuous, oi);
            sample.stop(meterRegistry.timer("market-data.zerodha.api.historical.time"));
            meterRegistry.counter("market-data.zerodha.api.historical.success").increment();
            return convertInstrumentMaptoSymbolMap(historicalData);
        } catch (KiteException | IOException e) {
            meterRegistry.counter("market-data.zerodha.api.historical.error", "error_type", getErrorType(e))
                    .increment();
            log.error("getHistoricalData",
                    "Failed to get historical data for instrument " + symbol + ": " + e.getMessage(), e);
            throw new ZerodhaApiException("Failed to get historical data", e);
        }
    }

    /**
     * Get all available instruments
     * 
     * @return List of instruments
     */
    @Retry(name = "marketDataZerodhaApi")
    public List<Instrument> getAllInstruments() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<Instrument> instruments = kiteConnect.getInstruments();
            sample.stop(meterRegistry.timer("market-data.zerodha.api.instruments.time"));
            meterRegistry.counter("market-data.zerodha.api.instruments.success").increment();
            return instruments;
        } catch (KiteException | IOException e) {
            meterRegistry.counter("market-data.zerodha.api.instruments.error", "error_type", getErrorType(e))
                    .increment();
            log.error("getAllInstruments", "Failed to get all instruments: " + e.getMessage(), e);
            throw new ZerodhaApiException("Failed to get instruments", e);
        }
    }

    /**
     * Get instruments for a specific exchange
     * 
     * @param exchange Exchange name (NSE, BSE, etc.)
     * @return List of instruments for the exchange
     */
    @Retry(name = "marketDataZerodhaApi")
    public List<Instrument> getInstrumentsForExchange(String exchange) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            List<Instrument> instruments = kiteConnect.getInstruments(exchange);
            sample.stop(meterRegistry.timer("market-data.zerodha.api.instruments.exchange.time"));
            meterRegistry.counter("market-data.zerodha.api.instruments.exchange.success").increment();
            return instruments;
        } catch (KiteException | IOException e) {
            meterRegistry.counter("market-data.zerodha.api.instruments.exchange.error", "error_type", getErrorType(e))
                    .increment();
            log.error("getInstrumentsForExchange",
                    "Failed to get instruments for exchange " + exchange + ": " + e.getMessage(), e);
            throw new ZerodhaApiException("Failed to get instruments for exchange", e);
        }
    }

    /**
     * Initialize and connect ticker for real-time data
     * 
     * @param tokens         List of instrument tokens to subscribe
     * @param onTickListener Callback for tick data
     * @return Connected KiteTicker instance
     */
    public KiteTicker initializeTicker(List<Long> tokens, OnTicks onTickListener) {
        if (tickerProvider != null && tickerProvider.isConnectionOpen()) {
            log.info("initializeTicker", "Ticker already connected, disconnecting first");
            tickerProvider.disconnect();
        }

        log.info("initializeTicker", "Initializing Zerodha ticker with " + tokens.size() + " tokens");
        tickerProvider = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());

        // Configure ticker
        tickerProvider.setTryReconnection(true);
        // tickerProvider.setMaximumRetries(tickerMaxRetries);
        // tickerProvider.setMaximumRetryInterval(tickerReconnectInterval);

        // Set listeners
        tickerProvider.setOnConnectedListener(new OnConnect() {
            @Override
            public void onConnected() {
                log.info("onConnected", "Ticker connected successfully");
                meterRegistry.counter("zerodha.ticker.connect").increment();
                // Convert List<Long> to ArrayList<Long> for API compatibility
                ArrayList<Long> tokenList = new ArrayList<>(tokens);
                tickerProvider.subscribe(tokenList);
                tickerProvider.setMode(tokenList, KiteTicker.modeFull);
            }
        });

        tickerProvider.setOnDisconnectedListener(new OnDisconnect() {
            @Override
            public void onDisconnected() {
                log.warn("onDisconnected", "Ticker disconnected");
                meterRegistry.counter("zerodha.ticker.disconnect").increment();
            }
        });

        tickerProvider.setOnErrorListener(new OnError() {
            @Override
            public void onError(Exception exception) {
                log.error("initializeTicker", "Ticker error: " + exception.getMessage(), exception);
                meterRegistry.counter("zerodha.ticker.error", "type", "exception").increment();
            }

            @Override
            public void onError(KiteException kiteException) {
                log.error("initializeTicker", "Ticker KiteException: " + kiteException.getMessage(), kiteException);
                meterRegistry.counter("zerodha.ticker.error", "type", "kite_exception").increment();
            }

            @Override
            public void onError(String error) {
                log.error("initializeTicker", "Ticker error: " + error, (Throwable) null);
                meterRegistry.counter("zerodha.ticker.error", "type", "string").increment();
            }
        });

        // Set tick listener
        tickerProvider.setOnTickerArrivalListener(onTickListener);

        // Connect
        tickerProvider.connect();
        log.info("initializeTicker", "Ticker connection initiated");

        return tickerProvider;
    }

    /**
     * Disconnect the ticker
     */
    public void disconnectTicker() {
        if (tickerProvider != null && tickerProvider.isConnectionOpen()) {
            log.info("disconnectTicker", "Disconnecting ticker");
            tickerProvider.disconnect();
            meterRegistry.counter("zerodha.ticker.manual_disconnect").increment();
        } else {
            log.info("disconnectTicker", "Ticker not connected, nothing to disconnect");
        }
    }

    /**
     * Check if ticker is connected
     * 
     * @return true if connected, false otherwise
     */
    public boolean isTickerConnected() {
        return tickerProvider != null && tickerProvider.isConnectionOpen();
    }

    /**
     * Logout and invalidate session
     * 
     * @return true if logout successful
     */
    public boolean logout() {
        try {
            kiteConnect.logout();
            meterRegistry.counter("market-data.zerodha.api.logout.success").increment();
            log.info("logout", "Logged out of Zerodha API");
            return true;
        } catch (KiteException | IOException e) {
            meterRegistry.counter("market-data.zerodha.api.logout.error").increment();
            log.error("logout", "Failed to logout: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Retry a function with exponential backoff
     * 
     * @param operation Function to retry
     * @param <T>       Return type
     * @return Result of the operation
     */
    private <T> T retryWithBackoff(ZerodhaOperation<T> operation) {
        Exception lastException = null;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                long delay = retryDelayMs * (long) Math.pow(2, attempt);
                log.warn("retryWithBackoff", String.format("Attempt %d failed, retrying after %dms: %s", attempt + 1,
                        delay, e.getMessage()));
                meterRegistry.counter("market-data.zerodha.api.retry").increment();
                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ZerodhaApiException("Retry interrupted", ie);
                }
            }
        }
        throw new ZerodhaApiException("All retry attempts failed", lastException);
    }

    /**
     * Async version of API calls using CompletableFuture
     * 
     * @param operation Operation to execute asynchronously
     * @param <T>       Return type
     * @return CompletableFuture with the result
     */
    public <T> CompletableFuture<T> executeAsync(ZerodhaOperation<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return retryWithBackoff(operation);
            } catch (Exception e) {
                log.error("executeAsync", "Async operation failed: " + e.getMessage(), e);
                throw new ZerodhaApiException("Async operation failed", e);
            }
        }, threadPoolExecutor);
    }

    /**
     * Prefix all symbols with NSE: if not already prefixed
     * 
     * @param symbols Array of symbols to prefix
     * @return Array of symbols with NSE: prefix
     */
    private String[] prefixSymbolsWithNSE(String[] symbols) {
        if (symbols == null || symbols.length == 0) {
            return new String[0];
        }

        return Arrays.stream(symbols)
                .map(symbol -> {
                    // Only add prefix if it doesn't already have one
                    if (symbol != null && !symbol.contains(":")) {
                        return "NSE:" + symbol;
                    }
                    return symbol;
                })
                .toArray(String[]::new);
    }

    private String getErrorType(Throwable e) {
        if (e instanceof KiteException) {
            KiteException ke = (KiteException) e;
            try {
                // Try to get the HTTP status code from the exception message or use reflection
                String message = ke.getMessage();
                if (message != null && message.contains("403"))
                    return "unauthorized";
                if (message != null && message.contains("401"))
                    return "unauthorized";
                if (message != null && message.contains("400"))
                    return "client_error";
                if (message != null && message.contains("404"))
                    return "client_error";
                if (message != null && message.contains("500"))
                    return "server_error";
                return "kite_error";
            } catch (Exception ex) {
                return "kite_error";
            }
        } else if (e instanceof IOException) {
            return "network_error";
        } else {
            return "unexpected_error";
        }
    }

    /**
     * Mask API key for logging
     * 
     * @param key API key
     * @return Masked key
     */
    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) {
            return "*****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    /**
     * Mask token for logging
     * 
     * @param token Token
     * @return Masked token
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "*****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }

    /**
     * Functional interface for operations that can throw exceptions
     * 
     * @param <T> Return type
     */
    @FunctionalInterface
    public interface ZerodhaOperation<T> {
        T execute() throws Exception;
    }
}
