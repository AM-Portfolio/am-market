import 'package:dio/dio.dart';
import 'package:am_common/core/utils/logger.dart';

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
        '$baseUrl/v1/market-analytics/movers',
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
      CommonLogger.debug(
        'Fetching historical data for ${symbols.join(", ")} with range $range',
        tag: 'MarketAnalyticsService.getHistoricalData',
      );

      final Map<String, List<Map<String, dynamic>>> result = {};

      // Fetch data for each symbol
      for (final symbol in symbols) {
        try {
          final response = await _dio.get(
            '$baseUrl/v1/market-analytics/historical-charts/$symbol',
            queryParameters: {
              'range': range,
            },
          );

          if (response.statusCode == 200 && response.data != null) {
            final data = response.data;
            
            // Extract the data array from response
            if (data is Map && data.containsKey('data')) {
              result[symbol] = List<Map<String, dynamic>>.from(data['data'] as List);
            } else if (data is List) {
              result[symbol] = List<Map<String, dynamic>>.from(data);
            }
          }
        } catch (e) {
          CommonLogger.error(
            'Error fetching historical data for $symbol',
            tag: 'MarketAnalyticsService.getHistoricalData',
            error: e,
          );
          // Continue with other symbols
          result[symbol] = [];
        }
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
