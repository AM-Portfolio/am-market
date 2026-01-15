class MarketDataUpdate {
  final int timestamp;
  final Map<String, QuoteChange> quotes;

  MarketDataUpdate({required this.timestamp, required this.quotes});

  factory MarketDataUpdate.fromJson(Map<String, dynamic> json) {
    var quotesJson = (json['quotes'] as Map<String, dynamic>);
    Map<String, QuoteChange> quotes = {};
    quotesJson.forEach((key, value) {
      quotes[key] = QuoteChange.fromJson(value);
    });
    return MarketDataUpdate(
      timestamp: json['timestamp'],
      quotes: quotes,
    );
  }
}

class QuoteChange {
  final double? lastPrice;
  final double? change;
  final double? changePercent;
  final double? previousClose;

  QuoteChange({
    this.lastPrice,
    this.change,
    this.changePercent,
    this.previousClose,
  });

  factory QuoteChange.fromJson(Map<String, dynamic> json) {
    return QuoteChange(
      lastPrice: json['lastPrice']?.toDouble(),
      change: json['change']?.toDouble(),
      changePercent: json['changePercent']?.toDouble(),
      previousClose: json['previousClose']?.toDouble(),
    );
  }
}
