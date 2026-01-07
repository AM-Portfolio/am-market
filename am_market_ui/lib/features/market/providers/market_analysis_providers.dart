import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../internal/domain/models/chart_config.dart';

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
