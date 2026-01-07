class Etf {
  final String symbol;
  final String name;
  final String? isin;
  final String? assetClass;
  final String? marketCapCategory;

  Etf({
    required this.symbol,
    required this.name,
    this.isin,
    this.assetClass,
    this.marketCapCategory,
  });

  factory Etf.fromJson(Map<String, dynamic> json) {
    return Etf(
      symbol: json['symbol'] ?? '',
      name: json['name'] ?? '',
      isin: json['isin'],
      assetClass: json['asset_class'],
      marketCapCategory: json['market_cap_category'],
    );
  }
}

class EtfHoldings {
  final String symbol;
  final String name;
  final int holdingsCount;
  final List<EtfHoldingItem> holdings;

  EtfHoldings({
    required this.symbol,
    required this.name,
    required this.holdingsCount,
    required this.holdings,
  });

  factory EtfHoldings.fromJson(Map<String, dynamic> json) {
    return EtfHoldings(
      symbol: json['symbol'] ?? '',
      name: json['name'] ?? '',
      holdingsCount: json['holdings_count'] ?? 0,
      holdings: (json['holdings'] as List<dynamic>?)
              ?.map((e) => EtfHoldingItem.fromJson(e))
              .toList() ??
          [],
    );
  }
}

class EtfHoldingItem {
  final String stockName;
  final String isinCode;
  final double percentage;
  final double? marketValue;
  final double? quantity;

  EtfHoldingItem({
    required this.stockName,
    required this.isinCode,
    required this.percentage,
    this.marketValue,
    this.quantity,
  });

  factory EtfHoldingItem.fromJson(Map<String, dynamic> json) {
    return EtfHoldingItem(
      stockName: json['stock_name'] ?? '',
      isinCode: json['isin_code'] ?? '',
      percentage: (json['percentage'] as num?)?.toDouble() ?? 0.0,
      marketValue: (json['market_value'] as num?)?.toDouble(),
      quantity: (json['quantity'] as num?)?.toDouble(),
    );
  }
}
