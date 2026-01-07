import 'package:am_auth_ui/core/services/secure_storage_service.dart';

import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:get_it/get_it.dart';

import 'package:am_design_system/am_design_system.dart';

class MarketAnalysisService {
  // Use the new public base URL mapped via Traefik
  // Updated to match Traefik routing: /api/market/analysis
  final String baseUrl = 'https://am.munish.org/api/market/analysis';

  final _storage = GetIt.I<SecureStorageService>();

  Future<Map<String, String>> _getHeaders() async {
    final token = await _storage.getAccessToken();
    // Use print since CommonLogger might not be imported here, but actually it is better to import it
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  Future<Map<String, dynamic>> analyzeSymbol(String symbol, {String timeframe = "1D"}) async {
    try {
      final headers = await _getHeaders();
      
      final payload = {
        "symbol": symbol,
        "timeframe": timeframe,
        "exchange": "NSE",
        "indicators": [
          { "kind": "sma", "length": 50 },
          { "kind": "rsi", "length": 14 },
          { "kind": "ema", "length": 20 }
        ]
      };

      final response = await http.post(
        Uri.parse('$baseUrl/v1/analyze'), 
        headers: headers,
        body: jsonEncode(payload),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body);
      } else {
        throw Exception('Analysis failed: ${response.statusCode} ${response.body}');
      }
    } catch (e) {
      print("Analysis Error: $e");
      rethrow;
    }
  }
}
