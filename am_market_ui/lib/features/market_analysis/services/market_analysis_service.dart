import 'package:am_auth_ui/core/services/secure_storage_service.dart';
import 'package:am_market_common/models/indices_performance_model.dart';

import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:get_it/get_it.dart';

import 'package:am_design_system/am_design_system.dart';

class MarketAnalysisService {
  // Matches backend AnalysisController
  final String baseUrl = 'http://localhost:8092/api/v1/analysis';

  final _storage = GetIt.I<SecureStorageService>();

  Future<Map<String, String>> _getHeaders() async {
    final token = await _storage.getAccessToken();
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Future<Map<String, dynamic>> getSeasonality(String symbol, {String timeframe = "DAY"}) async {
    return _get('/seasonality', {'symbol': symbol, 'timeframe': timeframe});
  }

  Future<Map<String, dynamic>> getTechnicalAnalysis(String symbol, {String timeframe = "DAY"}) async {
    return _get('/technical', {'symbol': symbol, 'timeframe': timeframe});
  }

  Future<Map<String, dynamic>> getSeasonalityBatch(List<String> symbols, {String timeframe = "DAY"}) async {
    return _get('/seasonality/batch', {'symbols': symbols.join(','), 'timeframe': timeframe});
  }

  Future<Map<String, dynamic>> getTechnicalBatch(List<String> symbols, {String timeframe = "DAY"}) async {
    return _get('/technical/batch', {'symbols': symbols.join(','), 'timeframe': timeframe});
  }

  Future<Map<String, dynamic>> getCalendarHeatmap(String symbol, {int? year}) async {
    final params = {'symbol': symbol};
    if (year != null) params['year'] = year.toString();
    return _get('/heatmap/calendar', params);
  }

  Future<IndicesHistoricalPerformanceResponse> getIndicesHistoricalPerformance({int years = 10}) async {
    final response = await _get('/indices/historical-performance', {'years': years.toString()});
    return IndicesHistoricalPerformanceResponse.fromJson(response);
  }

  Future<Map<String, dynamic>> _get(String path, Map<String, String> queryParams) async {
    try {
      final headers = await _getHeaders();
      final uri = Uri.parse('$baseUrl$path').replace(queryParameters: queryParams);
      
      final response = await http.get(uri, headers: headers);

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        throw Exception('Request failed: ${response.statusCode} ${response.body}');
      }
    } catch (e) {
      print("Analysis Error ($path): $e");
      rethrow;
    }
  }
}
