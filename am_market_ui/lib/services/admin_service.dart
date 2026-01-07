import 'package:am_auth_ui/core/services/secure_storage_service.dart';

import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:am_design_system/am_design_system.dart';
import 'package:get_it/get_it.dart';
import '../models/ingestion_log.dart';

class AdminService {
  // Use localhost for now, assume proxy or direct access
  static const String baseUrl = 'https://am.munish.org/api/market';
  final _storage = GetIt.I<SecureStorageService>();

  Future<Map<String, String>> _getHeaders() async {
    final token = await _storage.getAccessToken();
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Future<List<IngestionLog>> getLogs({int page = 0, int size = 20, DateTime? startDate, DateTime? endDate}) async {
    String query = 'page=$page&size=$size';
    if (startDate != null) {
      query += '&startDate=${startDate.toIso8601String().split('T')[0]}';
    }
    if (endDate != null) {
      query += '&endDate=${endDate.toIso8601String().split('T')[0]}';
    }
    
    final headers = await _getHeaders();
    final response = await http.get(Uri.parse('$baseUrl/v1/admin/logs?$query'), headers: headers);
    
    if (response.statusCode == 200) {
      final List<dynamic> body = jsonDecode(response.body);
      return body.map((e) => IngestionLog.fromJson(e)).toList();
    } else {
      throw Exception('Failed to load logs');
    }
  }

  Future<IngestionLog> getJobDetails(String jobId) async {
    final headers = await _getHeaders();
    final response = await http.get(Uri.parse('$baseUrl/v1/admin/logs/$jobId'), headers: headers);
    if (response.statusCode == 200) {
      return IngestionLog.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to load job details');
    }
  }

  Future<void> triggerHistoricalSync({
    String? symbol, 
    bool forceRefresh = true,
    bool fetchIndexStocks = false,
  }) async {
    final uri = Uri.parse('$baseUrl/v1/admin/sync/historical').replace(
      queryParameters: {
        if (symbol != null && symbol.isNotEmpty) 'symbol': symbol,
        'forceRefresh': forceRefresh.toString(),
        'fetchIndexStocks': fetchIndexStocks.toString(),
      }
    );
    final headers = await _getHeaders();
    final response = await http.post(uri, headers: headers);
    if (response.statusCode != 200) {
      throw Exception('Failed to trigger sync: ${response.body}');
    }
  }

  Future<void> startIngestion(String provider, {List<String>? symbols}) async {
    String url = '$baseUrl/v1/admin/ingestion/start?provider=$provider';
    if (symbols != null && symbols.isNotEmpty) {
      url += '&symbols=${symbols.join(",")}';
    }
    final headers = await _getHeaders();
    final response = await http.post(Uri.parse(url), headers: headers);
    if (response.statusCode != 200) {
      throw Exception('Failed to start ingestion: ${response.body}');
    }
  }

  Future<void> stopIngestion(String provider) async {
    final headers = await _getHeaders();
    final response = await http.post(Uri.parse('$baseUrl/v1/admin/ingestion/stop?provider=$provider'), headers: headers);
    if (response.statusCode != 200) {
      throw Exception('Failed to stop ingestion: ${response.body}');
    }
  }
}
