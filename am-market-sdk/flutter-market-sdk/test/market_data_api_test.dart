//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

import 'package:am_market_client/api.dart';
import 'package:test/test.dart';


/// tests for MarketDataApi
void main() {
  // final instance = MarketDataApi();

  group('tests for MarketDataApi', () {
    // Generate session from request token
    //
    // Creates a new authenticated session using the request token obtained from broker login
    //
    //Future<Object> generateSession({ String requestToken, String requestToken2, String code, String status }) async
    test('test generateSession', () async {
      // TODO
    });

    // Get historical market data
    //
    // Retrieves historical price and volume data for one or more instruments with filtering options
    //
    //Future<HistoricalDataResponseV1> getHistoricalData(HistoricalDataRequest historicalDataRequest) async
    test('test getHistoricalData', () async {
      // TODO
    });

    // Get live LTP with change calculation
    //
    // Retrieves current LTP and calculates change based on historical closing price for the specified timeframe
    //
    //Future<Map<String, Object>> getLiveLTP(String symbols, { String timeframe, bool isIndexSymbol, bool refresh }) async
    test('test getLiveLTP', () async {
      // TODO
    });

    // Get live market prices
    //
    // Retrieves real-time market prices for specified symbols or all available symbols
    //
    //Future<Map<String, Object>> getLivePrices({ String symbols, bool isIndexSymbol, bool refresh }) async
    test('test getLivePrices', () async {
      // TODO
    });

    // Get login URL for broker authentication
    //
    // Returns a URL that can be used to authenticate with the broker's login page
    //
    //Future<Map<String, String>> getLoginUrl({ String provider }) async
    test('test getLoginUrl', () async {
      // TODO
    });

    // Get mutual fund details
    //
    // Retrieves detailed information about a mutual fund including NAV, returns, and other metrics
    //
    //Future<Map<String, Object>> getMutualFundDetails(String schemeCode, { bool refresh }) async
    test('test getMutualFundDetails', () async {
      // TODO
    });

    // Get mutual fund NAV history
    //
    // Retrieves historical Net Asset Value (NAV) data for a mutual fund over a specified date range
    //
    //Future<Map<String, Object>> getMutualFundNavHistory(String schemeCode, String from, String to, { bool refresh }) async
    test('test getMutualFundNavHistory', () async {
      // TODO
    });

    // Get OHLC data for multiple symbols
    //
    // Retrieves Open-High-Low-Close data for multiple symbols with support for different timeframes
    //
    //Future<Object> getOHLC(OHLCRequest oHLCRequest) async
    test('test getOHLC', () async {
      // TODO
    });

    // Get option chain data
    //
    // Retrieves option chain data including calls and puts for a given underlying instrument
    //
    //Future<Map<String, Object>> getOptionChain(String symbol, { String expiryDate, bool refresh }) async
    test('test getOptionChain', () async {
      // TODO
    });

    // Get quotes for multiple symbols
    //
    // Retrieves latest quotes for multiple symbols with support for different timeframes
    //
    //Future<Map<String, Object>> getQuotes(String symbols, { String timeFrame, bool refresh }) async
    test('test getQuotes', () async {
      // TODO
    });

    // Get quotes for multiple symbols (POST)
    //
    // Retrieves latest quotes for multiple symbols with support for different timeframes using POST request
    //
    //Future<Map<String, Object>> getQuotesPost(QuotesRequest quotesRequest) async
    test('test getQuotesPost', () async {
      // TODO
    });

    // Get symbols for a specific exchange
    //
    // Retrieves all available trading symbols for a specific exchange
    //
    //Future<List<Object>> getSymbolsForExchange(String exchange) async
    test('test getSymbolsForExchange', () async {
      // TODO
    });

    // Logout and invalidate session
    //
    // Invalidates the current broker session and clears authentication tokens
    //
    //Future<Map<String, Object>> logout() async
    test('test logout', () async {
      // TODO
    });

  });
}
