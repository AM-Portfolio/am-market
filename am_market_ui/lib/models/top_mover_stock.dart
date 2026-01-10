import 'package:flutter/material.dart';

/// Model for top mover stock data
class TopMoverStock {
  final String symbol;
  final String companyName;
  final double lastPrice;
  final double change;
  final double changePercent;
  final int volume;

  TopMoverStock({
    required this.symbol,
    required this.companyName,
    required this.lastPrice,
    required this.change,
    required this.changePercent,
    required this.volume,
  });

  factory TopMoverStock.fromJson(Map<String, dynamic> json) {
    return TopMoverStock(
      symbol: json['symbol'] as String? ?? '',
      companyName: json['companyName'] as String? ?? json['symbol'] as String? ?? '',
      lastPrice: (json['lastPrice'] as num?)?.toDouble() ?? 0.0,
      change: (json['change'] as num?)?.toDouble() ?? 0.0,
      changePercent: (json['changePercent'] as num?)?.toDouble() ?? (json['pChange'] as num?)?.toDouble() ?? 0.0,
      volume: (json['volume'] as num?)?.toInt() ?? 0,
    );
  }
}
