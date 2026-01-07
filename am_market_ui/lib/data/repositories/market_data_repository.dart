import '../../models/market_data.dart';
import '../../models/available_indices.dart';

/// Abstract repository interface for market data operations
/// 
/// This provides an abstraction layer between the UI and the SDK,
/// allowing for:
/// - Model mapping (SDK models → App models)
/// - Easier testing with mocks
/// - Flexibility to change data sources
abstract class MarketDataRepository {
  /// Fetch available indices from the market
  Future<AvailableIndices> getAvailableIndices();

  /// Fetch stock indices data for a specific index symbol
  /// 
  /// [indexSymbol] - The symbol of the index (e.g., "NIFTY 50", "SENSEX")
  /// [fetchConstituents] - Whether to include constituent stock data
  Future<StockIndicesMarketData> getIndexData(
    String indexSymbol, {
    bool fetchConstituents = true,
  });

  /// Fetch historical OHLC data for symbols
  /// 
  /// [symbols] - List of trading symbols
  /// [fromDate] - Start date for historical data
  /// [toDate] - End date for historical data
  /// [interval] - Data interval (e.g., "1d", "1h")
  Future<Map<String, dynamic>> getHistoricalData({
    required List<String> symbols,
    required String fromDate,
    required String toDate,
    String interval = '1d',
  });

  /// Calculate brokerage fees for a trade
  Future<Map<String, dynamic>> calculateBrokerage({
    required String tradingSymbol,
    required int quantity,
    required double buyPrice,
    required double sellPrice,
    required String exchange,
    required String tradeType,
  });

  /// Search for securities/instruments
  Future<List<Map<String, dynamic>>> searchSecurities({
    String? query,
    List<String>? symbols,
    String? sector,
    String? industry,
  });
}
