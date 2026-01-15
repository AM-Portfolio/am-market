import 'dart:async';
import 'package:am_market_common/models/market_data_update.dart';

class RealTimeMarketService {
  final _controller = StreamController<MarketDataUpdate>.broadcast();

  Stream<MarketDataUpdate> get stream => _controller.stream;

  void connect() {
    // TODO: Implement actual connection logic
  }

  void disconnect() {
    // TODO: Implement actual disconnection logic
  }

  void dispose() {
    _controller.close();
  }
}
