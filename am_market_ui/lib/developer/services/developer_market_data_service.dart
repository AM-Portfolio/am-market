import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

class DeveloperMarketDataService {
  static const String baseUrl = 'http://localhost:8092/v1/market-data';
  final Dio _dio = Dio();

  Future<Map<String, dynamic>> getQuotes(String symbols, {bool refresh = false}) async {
    try {
      final response = await _dio.get(
        '$baseUrl/quotes',
        queryParameters: {
          'symbols': symbols,
          'refresh': refresh,
        },
      );
      return response.data as Map<String, dynamic>;
    } catch (e) {
      debugPrint('Error fetching quotes: $e');
      return {'error': e.toString()};
    }
  }

  Future<Map<String, dynamic>> getOHLC(String symbols, {bool refresh = false}) async {
    try {
      final response = await _dio.post(
        '$baseUrl/ohlc',
        data: {
          'symbols': symbols,
          'timeFrame': '1D',
          'forceRefresh': refresh,
          'isIndexSymbol': false,
        },
      );
      return response.data as Map<String, dynamic>;
    } catch (e) {
      debugPrint('Error fetching OHLC: $e');
      return {'error': e.toString()};
    }
  }

  Future<Map<String, dynamic>> getHistorical(String symbols, {bool refresh = false}) async {
    try {
      final response = await _dio.post(
        '$baseUrl/historical-data',
        data: {
          'symbols': symbols,
          'fromDate': DateTime.now().subtract(const Duration(days: 7)).toIso8601String().split('T')[0],
          'toDate': DateTime.now().toIso8601String().split('T')[0],
          'interval': '1D',
          'forceRefresh': refresh,
        },
      );
      return response.data as Map<String, dynamic>;
    } catch (e) {
      debugPrint('Error fetching Historical: $e');
      return {'error': e.toString()};
    }
  }
}
