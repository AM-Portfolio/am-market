package com.am.marketdata.provider.zerodha;

import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.common.mapper.OHLCMapper;
import com.am.marketdata.common.mapper.HistoryDataMapper;
import com.marketdata.common.MarketDataProvider;
import com.am.common.investment.model.historical.HistoricalData;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.models.LTPQuote;
import com.zerodhatech.models.Quote;
import com.am.marketdata.provider.zerodha.ZerodhaApiException;
import com.zerodhatech.ticker.OnTicks;

import com.am.marketdata.common.log.AppLogger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Zerodha implementation of the MarketDataProvider interface
 */
@Service("zerodhaMarketDataProvider")
public class ZerodhaMarketDataProvider implements MarketDataProvider {

    private final AppLogger log = AppLogger.getLogger();

    private final ZerodhaApiService zerodhaApiService;
    private final OHLCMapper ohlcMapper;
    private final HistoryDataMapper historyDataMapper;

    public ZerodhaMarketDataProvider(ZerodhaApiService zerodhaApiService, OHLCMapper ohlcMapper,
            HistoryDataMapper historyDataMapper) {
        this.zerodhaApiService = zerodhaApiService;
        this.ohlcMapper = ohlcMapper;
        this.historyDataMapper = historyDataMapper;
        log.info("ZerodhaMarketDataProvider", "Initialized Zerodha market data provider");
    }

    @PostConstruct
    @Override
    public void initialize() {
        log.info("initialize", "Initializing Zerodha market data provider");
        zerodhaApiService.initialize();
    }

    @PreDestroy
    @Override
    public void cleanup() {
        log.info("cleanup", "Cleaning up Zerodha market data provider");
        zerodhaApiService.cleanup();
    }

    @Override
    public void setAccessToken(String accessToken) {
        zerodhaApiService.setAccessToken(accessToken);
    }

    @Override
    public String getLoginUrl() {
        return zerodhaApiService.getLoginUrl();
    }

    @Override
    public Object generateSession(String requestToken) {
        return zerodhaApiService.generateSession(requestToken);
    }

    @Override
    public Map<String, Object> getQuotes(String[] symbols) {
        try {
            Map<String, Quote> quotes = zerodhaApiService.getQuotes(symbols);
            return new HashMap<>(quotes);
        } catch (Exception e) {
            log.error("getQuotes", "Error getting quotes from Zerodha: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, OHLCQuote> getOHLC(List<String> symbols, TimeFrame timeFrame) {
        try {
            // Note: Zerodha getOHLC typically returns the day's OHLC.
            // Validating if we need to support other timeframes via historical data here.
            // For now, passing symbols directly.
            Map<String, com.zerodhatech.models.OHLCQuote> ohlc = zerodhaApiService
                    .getOHLC(symbols.toArray(new String[0]));
            return ohlcMapper.toServiceOHLCQuoteMap(ohlc);
        } catch (Exception e) {
            log.error("getOHLC", "Error getting OHLC from Zerodha: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, LTPQuote> getLTP(String[] symbols) {
        try {
            Map<String, LTPQuote> ltp = zerodhaApiService.getLTP(symbols);
            return new HashMap<>(ltp);
        } catch (Exception e) {
            log.error("getLTP", "Error getting LTP from Zerodha: " + e.getMessage(), e);
            return new HashMap<>();
        }
    }

    @Override
    public HistoricalData getHistoricalData(String symbol, Date from, Date to, TimeFrame interval,
            boolean continuous, Map<String, Object> additionalParams) {
        boolean oi = additionalParams != null && additionalParams.containsKey("oi")
                ? (Boolean) additionalParams.get("oi")
                : false;
        com.zerodhatech.models.HistoricalData zerodhaData = zerodhaApiService.getHistoricalData(symbol, from, to,
                interval, continuous, oi);

        if (zerodhaData != null) {
            HistoricalData commonData = historyDataMapper.toCommonHistoricalData(zerodhaData);
            if (commonData != null) {
                commonData.setTradingSymbol(symbol);
                return commonData;
            }
        }
        return new HistoricalData();
    }

    @Override
    public Object initializeTicker(List<String> instrumentIds, Object tickListener) {
        // Convert string instrument IDs to longs for Zerodha
        List<Long> tokens = instrumentIds.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        return zerodhaApiService.initializeTicker(tokens, (OnTicks) tickListener);
    }

    @Override
    public boolean isTickerConnected() {
        return zerodhaApiService.isTickerConnected();
    }

    @Override
    public List<Instrument> getAllInstruments() {
        try {
            List<Instrument> instruments = zerodhaApiService.getAllInstruments();
            return new ArrayList<>(instruments);
        } catch (Exception e) {
            log.error("getAllInstruments", "Error getting all instruments from Zerodha: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Object> getSymbolsForExchange(String exchange) {
        try {
            List<Instrument> instruments = zerodhaApiService.getInstrumentsForExchange(exchange);
            return new ArrayList<>(instruments);
        } catch (Exception e) {
            log.error("getSymbolsForExchange", "Error getting instruments for exchange from Zerodha: " + e.getMessage(),
                    e);
            return new ArrayList<>();
        }
    }

    @Override
    public <T> CompletableFuture<T> executeAsync(ProviderOperation<T> operation) {
        return zerodhaApiService.executeAsync(() -> {
            try {
                return operation.execute();
            } catch (Exception e) {
                throw new ZerodhaApiException("Error executing async operation", e);
            }
        });
    }

    @Override
    public boolean logout() {
        return zerodhaApiService.logout();
    }

    @Override
    public String getProviderName() {
        return "zerodha";
    }
}
