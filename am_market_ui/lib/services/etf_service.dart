import 'package:am_auth_ui/core/services/secure_storage_service.dart';

import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:am_design_system/am_design_system.dart';
import 'package:get_it/get_it.dart';
import 'package:am_market_common/models/etf.dart';


class EtfService {
  // Use relative path which goes through Nginx proxy
  // Local development fallback could be configured if needed, but assuming Docker setup
  static const String baseUrl = 'https://am.munish.org/api/etf/v1';
  final _storage = GetIt.I<SecureStorageService>();

  Future<Map<String, String>> _getHeaders() async {
    final token = await _storage.getAccessToken();
    CommonLogger.debug("Token present: ${token != null}", tag: "EtfService");

    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Future<List<Etf>> searchEtfs(String query, {int limit = 50}) async {
    try {
      final uri = Uri.parse('$baseUrl/search').replace(queryParameters: {
        'query': query,
        'limit': limit.toString(),
      });
      
      final headers = await _getHeaders();
      final response = await http.get(uri, headers: headers);

      if (response.statusCode == 200) {
        final dynamic decoded = json.decode(response.body);
        
        // Handle both List and Map responses
        if (decoded is List) {
          return decoded.map((e) => Etf.fromJson(e)).toList();
        } else if (decoded is Map) {
          // If response is a Map, check for common data keys
          final data = decoded['data'] ?? decoded['etfs'] ?? decoded['results'];
          if (data is List) {
            return (data as List).map((e) => Etf.fromJson(e)).toList();
          }
        }
        CommonLogger.warning("Unexpected response structure: $decoded", tag: "EtfService.searchEtfs");

        return [];
      } else {
        CommonLogger.error("Failed search: ${response.statusCode}", tag: "EtfService.searchEtfs");

        return [];
      }
    } catch (e) {
      CommonLogger.error("Error searching ETFs", tag: "EtfService.searchEtfs", error: e);

      return [];
    }
  }

  Future<EtfHoldings?> getEtfHoldings(String symbol) async {
    try {
      final headers = await _getHeaders();
      final response = await http.get(Uri.parse('$baseUrl/holdings/$symbol'), headers: headers);

      if (response.statusCode == 200) {
        return EtfHoldings.fromJson(json.decode(response.body));
      } else if (response.statusCode == 404) {
        return null; 
      } else {
        CommonLogger.error("Failed to get holdings: ${response.statusCode}", tag: "EtfService.getEtfHoldings");

        return null;
      }
    } catch (e) {
      CommonLogger.error("Error getting holdings", tag: "EtfService.getEtfHoldings", error: e);

      return null;
    }
  }

  Future<bool> triggerFetchHoldings(String symbol) async {
    try {
       final headers = await _getHeaders();
       final response = await http.post(Uri.parse('$baseUrl/fetch-holdings/$symbol'), headers: headers);
       return response.statusCode == 200;
    } catch (e) {
       CommonLogger.error("Error triggering fetch", tag: "EtfService.triggerFetchHoldings", error: e);

       return false;
    }
  }
}
