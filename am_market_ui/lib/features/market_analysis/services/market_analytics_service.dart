import 'package:dio/dio.dart';
import 'package:am_design_system/am_design_system.dart';

/// Service for market analytics API calls (top movers, historical data)
class MarketAnalyticsService {
  final Dio _dio;
  final String baseUrl;

  MarketAnalyticsService({
    required this.baseUrl,
    Dio? dio,
  }) : _dio = dio ?? Dio();

  /// Get top gainers/losers for a specific index
  /// 
  /// Endpoint: GET /v1/market-analytics/movers
  /// Params:
  ///   - type: 'gainers' or 'losers'
  ///   - limit: number of results (default 5)
  ///   - indexSymbol: index name (e.g., 'NIFTY 50')
  Future<List<Map<String, dynamic>>> getMovers({
    required String type, // 'gainers' or 'losers'
    required String indexSymbol,
    int limit = 5,
  }) async {
    try {
      CommonLogger.debug(
        'Fetching $type for $indexSymbol (limit: $limit)',
        tag: 'MarketAnalyticsService.getMovers',
      );

      final response = await _dio.get(
        '$baseUrl/api/v1/analysis/movers',
        queryParameters: {
          'type': type,
          'limit': limit,
          'indexSymbol': indexSymbol,
        },
      );

      if (response.statusCode == 200 && response.data != null) {
        return List<Map<String, dynamic>>.from(response.data as List);
      }

      return [];
    } catch (e) {
      CommonLogger.error(
        'Error fetching movers',
        tag: 'MarketAnalyticsService.getMovers',
        error: e,
      );
      rethrow;
    }
  }

  /// Get historical chart data for multiple symbols
  /// 
  /// Endpoint: GET /v1/market-analytics/historical-charts/{symbol}
  /// Params:
  ///   - range: '1D', '1W', '1M', '3M', '6M', '1Y', '5Y', '10Y'
  Future<Map<String, List<Map<String, dynamic>>>> getHistoricalData({
    required List<String> symbols,
    String range = '1Y', // Default 1 year
  }) async {
    try {
      if (symbols.isEmpty) {
        return {};
      }

      CommonLogger.debug(
        'Fetching historical data for ${symbols.join(", ")} with range $range',
        tag: 'MarketAnalyticsService.getHistoricalData',
      );

      final Map<String, List<Map<String, dynamic>>> result = {};

      // Make a single batch request
      try {
        final response = await _dio.get(
          '$baseUrl/api/v1/analysis/historical-charts',
          queryParameters: {
            'symbols': symbols.join(','),
            'range': range,
          },
        );

        if (response.statusCode == 200 && response.data != null) {
          final data = response.data;
          
          if (data is Map && data.containsKey('data')) {
            final Map<String, dynamic> symbolsData = data['data'];
            
            // Iterate through the response map where keys are symbols
            symbolsData.forEach((symbol, symbolData) {
              if (symbolData is Map && symbolData.containsKey('dataPoints')) {
                result[symbol] = List<Map<String, dynamic>>.from(symbolData['dataPoints'] as List);
              } else if (symbolData is Map && symbolData.containsKey('data')) {
                 // Fallback if structure is slightly different
                 result[symbol] = List<Map<String, dynamic>>.from(symbolData['data'] as List);
              }
            });
          }
        }
      } catch (e) {
        CommonLogger.error(
          'Error fetching historical data batch',
          tag: 'MarketAnalyticsService.getHistoricalData',
          error: e,
        );
      }

      return result;
    } catch (e) {
      CommonLogger.error(
        'Error fetching historical data',
        tag: 'MarketAnalyticsService.getHistoricalData',
        error: e,
      );
      rethrow;
    }
  }
}
