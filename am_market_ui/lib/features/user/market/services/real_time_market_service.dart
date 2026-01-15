
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:am_market_common/core/constants/market_endpoints.dart';
import 'package:am_market_common/models/market_data_update.dart';

class RealTimeMarketService {
  WebSocketChannel? _channel;
  
  // Convert http://host:port to ws://host:port
  String get _wsUrl {
    final baseUrl = MarketEndpoints.baseUrl;
    final wsBase = baseUrl.startsWith('https') 
        ? baseUrl.replaceFirst('https', 'wss') 
        : baseUrl.replaceFirst('http', 'ws');
    return '$wsBase/ws/market-data-stream';
  }

  Stream<MarketDataUpdate>? get stream => _channel?.stream.map((event) {
        try {
          return MarketDataUpdate.fromJson(jsonDecode(event));
        } catch (e) {
          // Return empty or throw? For resilience, maybe log and ignore
          print("Error parsing market update: $e");
          throw e; 
        }
      });

  void connect() {
    if (_channel != null) return;
    try {
      print("Connecting to WebSocket: $_wsUrl");
      _channel = WebSocketChannel.connect(Uri.parse(_wsUrl));
    } catch (e) {
      print('WebSocket Connection Error: $e');
    }
  }

  void disconnect() {
    _channel?.sink.close();
    _channel = null;
  }
}
