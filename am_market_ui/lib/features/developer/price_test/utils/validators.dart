/// Utility class for detecting symbol types
class SymbolDetector {
  /// Determine if a symbol is an index based on common keywords
  static bool isIndexSymbol(String symbol) {
    final upperSymbol = symbol.toUpperCase();
    return upperSymbol.contains('NIFTY') || 
           upperSymbol.contains('SENSEX') ||
           upperSymbol.contains('INDEX');
  }

  /// Validate if a symbol string is not empty
  static bool isValidSymbol(String? symbol) {
    return symbol != null && symbol.trim().isNotEmpty;
  }

  /// Validate if date range is valid (from date before to date)
  static bool isValidDateRange(DateTime? from, DateTime? to) {
    if (from == null || to == null) return false;
    return from.isBefore(to) || from.isAtSameMomentAs(to);
  }
}
