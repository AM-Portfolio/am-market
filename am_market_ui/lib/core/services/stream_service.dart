import 'package:am_design_system/am_design_system.dart';
import 'package:am_auth_ui/core/services/secure_storage_service.dart';

import 'dart:async';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';


class StreamService {
  WebSocketChannel? _channel;
  final StreamController<Map<String, dynamic>> _streamController = StreamController<Map<String, dynamic>>.broadcast();

  Stream<Map<String, dynamic>> get stream => _streamController.stream;

  // Connection status
  bool _isConnected = false;
  bool get isConnected => _isConnected;

  final String _wsUrl = 'wss://am.munish.org/api/market/ws/market-data-stream';

  void connect() {
    if (_isConnected) return;
    
    try {
      _channel = WebSocketChannel.connect(Uri.parse(_wsUrl));
      _isConnected = true;
      _isConnected = true;
      CommonLogger.info("WebSocket Connected", tag: "StreamService.connect");


      _channel!.stream.listen(
        (message) {
          try {
            final data = json.decode(message);
             if (data is Map<String, dynamic>) {
                 _streamController.add(data);
             }
          } catch (e) {
            CommonLogger.error("Error parsing WS message", tag: "StreamService.connect", error: e);

          }
        },
        onError: (error) {
          CommonLogger.error("WebSocket Error", tag: "StreamService.connect", error: error);

          _isConnected = false;
        },
        onDone: () {
          CommonLogger.info("WebSocket Closed", tag: "StreamService.connect");

          _isConnected = false;
        },
      );
    } catch (e) {
      CommonLogger.error("Error connecting to WebSocket", tag: "StreamService.connect", error: e);

      _isConnected = false;
    }
  }

  void disconnect() {
    if (_channel != null) {
      _channel!.sink.close();
      _isConnected = false;
    }
  }

  void dispose() {
    disconnect();
    _streamController.close();
  }
}
