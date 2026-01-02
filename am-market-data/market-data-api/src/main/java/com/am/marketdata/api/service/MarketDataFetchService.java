package com.am.marketdata.api.service;

import com.am.common.investment.model.stockindice.StockIndicesMarketData;
import com.am.marketdata.common.model.OHLCQuote;
import com.am.marketdata.common.model.TimeFrame;
import com.am.marketdata.api.model.HistoricalDataResponseV1;
import com.am.marketdata.api.dto.HistoricalDataRequest;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Cache service for market data
 * Handles caching of responses from MarketDataService and
 * InvestmentInstrumentService
 */
public interface MarketDataFetchService {

        /**
         * Get quotes from cache or service
         * 
         * @param tradingSymbols List of trading symbols
         * @param forceRefresh   Whether to force a refresh from the source
         * @return Map containing quote data for each symbol
         */
        Map<String, Map<String, Object>> getQuotes(Set<String> tradingSymbols, boolean forceRefresh);

        /**
         * Get quotes from cache or service with timeframe support
         * 
         * @param tradingSymbols List of trading symbols
         * @param isIndexSymbol  Whether the symbols are index symbols
         * @param timeFrame      The time frame for the quotes data
         * @param forceRefresh   Whether to force a refresh from the source
         * @return Map containing quotes data and metadata
         */
        Map<String, Object> getQuotes(Set<String> tradingSymbols, boolean isIndexSymbol, TimeFrame timeFrame,
                        boolean forceRefresh);

        /**
         * Get live prices from cache or service
         * 
         * @param symbols      Optional list of trading symbols (null or empty for all
         *                     available instruments)
         * @param forceRefresh Whether to force a refresh from the source
         * @return Map containing prices, count, timestamp and processing time
         */
        Map<String, Object> getLivePrices(Set<String> symbols, boolean indexSymbol, boolean forceRefresh);

        /**
         * Get historical data for multiple symbols from cache or service
         * 
         * @param symbols          List of trading symbols
         * @param fromDate         Start date
         * @param toDate           End date
         * @param interval         Data interval (minute, day, etc.)
         * @param instrumentType   Type of instrument (STOCK, OPTION, MUTUAL_FUND, etc.)
         * @param additionalParams Additional parameters specific to instrument type
         *                         including:
         *                         - filterType: Type of filtering (ALL, START_END,
         *                         CUSTOM)
         *                         - filterFrequency: When using CUSTOM filter, return
         *                         every Nth data point
         * @param forceRefresh     Whether to force a refresh from the source
         * @param fetchIndexStocks If true, fetch individual stocks that make up index
         *                         symbols.
         *                         If false, keep index symbols as-is
         * @return Historical data response with metadata for all symbols
         */
        HistoricalDataResponseV1 getHistoricalDataMultipleSymbols(Set<String> symbols,
                        Date fromDate, Date toDate,
                        TimeFrame interval, String instrumentType,
                        Map<String, Object> additionalParams, boolean forceRefresh, boolean fetchIndexStocks);

        /**
         * Get option chain data from cache or service
         * 
         * @param underlyingSymbol Symbol of the underlying instrument
         * @param expiryDate       Optional expiry date, if null will return the nearest
         *                         expiry
         * @param forceRefresh     Whether to force a refresh from the source
         * @return Option chain data with calls and puts
         */
        Map<String, Object> getOptionChain(String underlyingSymbol, Date expiryDate, boolean forceRefresh);

        /**
         * Get mutual fund details from cache or service
         * 
         * @param schemeCode   Mutual fund scheme code
         * @param forceRefresh Whether to force a refresh from the source
         * @return Mutual fund details
         */
        Map<String, Object> getMutualFundDetails(String schemeCode, boolean forceRefresh);

        /**
         * Get mutual fund NAV history from cache or service
         * 
         * @param schemeCode   Mutual fund scheme code
         * @param from         Start date
         * @param to           End date
         * @param forceRefresh Whether to force a refresh from the source
         * @return NAV history data
         */
        Map<String, Object> getMutualFundNavHistory(String schemeCode, Date from, Date to, boolean forceRefresh);

        /**
         * Get OHLC data from cache or service
         * 
         * @param symbols       Array of trading symbols
         * @param isIndexSymbol Whether the symbols are index symbols
         * @param timeFrame     The time frame for the OHLC data
         * @param forceRefresh  Whether to force a refresh from the source
         * @return Map of symbol to OHLC data with cache status
         */
        Map<String, OHLCQuote> getOHLC(Set<String> symbols, boolean isIndexSymbol, TimeFrame timeFrame,
                        boolean forceRefresh);

        /**
         * Get latest stock index data from cache or service
         * 
         * @param indexSymbol  Stock index symbol
         * @param forceRefresh Whether to force a refresh from the source
         * @return Stock index market data with cache status information
         */
        StockIndicesMarketData getStockIndexData(String indexSymbol, boolean forceRefresh);

        /**
         * Get latest stock indices data from cache or service
         * 
         * @param indexSymbols List of stock index symbols
         * @param forceRefresh Whether to force a refresh from the source
         * @return List of stock indices market data with cache status information
         */
        Set<StockIndicesMarketData> getStockIndicesData(Set<String> indexSymbols, boolean forceRefresh);

        /**
         * Process historical data request directly from the controller
         * 
         * @param request The HistoricalDataRequest containing all parameters
         * @return Response map with historical data and metadata
         * @throws Exception If there's an error processing the request
         */
        HistoricalDataResponseV1 processHistoricalDataRequest(
                        HistoricalDataRequest request)
                        throws Exception;

        /**
         * Get historical charts data (1Y Daily or 5Y Monthly)
         * 
         * @param symbol Symbol to fetch data for
         * @param range  Range (1Y or 5Y)
         * @return Historical data map
         */
        Map<String, Object> getHistoricalChartsData(String symbol, String range);
}
