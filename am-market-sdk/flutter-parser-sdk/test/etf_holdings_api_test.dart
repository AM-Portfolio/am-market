//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

import 'package:am_parser_client/api.dart';
import 'package:test/test.dart';


/// tests for ETFHoldingsApi
void main() {
  // final instance = ETFHoldingsApi();

  group('tests for ETFHoldingsApi', () {
    // Fetch All Etf Holdings
    //
    // Fetch holdings for all ETFs with ISINs from moneycontrol API Returns immediately with job ID, processes in background Smart caching: Only fetches if data is missing or stale
    //
    //Future<JobResponse> fetchAllEtfHoldingsV1FetchAllHoldingsPost({ String callbackUrl, String userId, int limit, bool forceRefresh }) async
    test('test fetchAllEtfHoldingsV1FetchAllHoldingsPost', () async {
      // TODO
    });

    // Fetch Holdings For Etf
    //
    // Fetch holdings for a specific ETF by symbol Returns immediately with job ID, processes in background
    //
    //Future<Object> fetchHoldingsForEtfV1FetchHoldingsSymbolPost(String symbol, { String callbackUrl, String userId }) async
    test('test fetchHoldingsForEtfV1FetchHoldingsSymbolPost', () async {
      // TODO
    });

    // Get Cache Statistics
    //
    // Get ETF holdings cache statistics
    //
    //Future<Object> getCacheStatisticsV1CacheStatsGet() async
    test('test getCacheStatisticsV1CacheStatsGet', () async {
      // TODO
    });

    // Get Etf Holdings
    //
    // Get stored holdings for a specific ETF
    //
    //Future<Object> getEtfHoldingsV1HoldingsSymbolGet(String symbol) async
    test('test getEtfHoldingsV1HoldingsSymbolGet', () async {
      // TODO
    });

    // Get Etf Stats
    //
    // Get ETF database statistics
    //
    //Future<Object> getEtfStatsV1StatsGet() async
    test('test getEtfStatsV1StatsGet', () async {
      // TODO
    });

    // Load Etfs From Json
    //
    // Load ETF data from JSON file Accepts etf_details.json and loads all ETFs into database
    //
    //Future<Object> loadEtfsFromJsonV1LoadFromJsonPost(MultipartFile file, { bool dryRun }) async
    test('test loadEtfsFromJsonV1LoadFromJsonPost', () async {
      // TODO
    });

    // Search Etfs
    //
    // Search ETFs by symbol, name, or ISIN
    //
    //Future<Object> searchEtfsV1SearchGet(String query, { int limit }) async
    test('test searchEtfsV1SearchGet', () async {
      // TODO
    });

  });
}
