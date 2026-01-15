import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:am_market_ui/features/market_analysis/internal/domain/models/chart_config.dart';
import 'package:am_market_ui/features/market_analysis/services/real_time_market_service.dart';
import 'package:am_market_common/models/market_data_update.dart';

final realTimeMarketServiceProvider = Provider<RealTimeMarketService>((ref) {
  final service = RealTimeMarketService();
  ref.onDispose(() => service.disconnect());
  service.connect();
  return service;
});

final marketDataStreamProvider = StreamProvider<MarketDataUpdate>((ref) {
  final service = ref.watch(realTimeMarketServiceProvider);
  return service.stream ?? const Stream.empty();
});


final marketAnalysisSymbolProvider =
    NotifierProvider<MarketAnalysisSymbolNotifier, String>(
      MarketAnalysisSymbolNotifier.new,
    );

class MarketAnalysisSymbolNotifier extends Notifier<String> {
  @override
  String build() => 'NASDAQ:AAPL';

  void updateSymbol(String symbol) {
    state = symbol;
  }
}

final marketAnalysisChartConfigProvider = Provider<ChartConfig>((ref) {
  final symbol = ref.watch(marketAnalysisSymbolProvider);
  return ChartConfig(symbol: symbol);
});
