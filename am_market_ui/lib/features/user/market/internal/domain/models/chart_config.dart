import 'package:equatable/equatable.dart';

class ChartConfig extends Equatable {
  const ChartConfig({
    required this.symbol,
    this.theme = 'dark',
    this.interval = 'D',
    this.locale = 'en',
  });
  final String symbol;
  final String theme;
  final String interval;
  final String locale;

  ChartConfig copyWith({
    String? symbol,
    String? theme,
    String? interval,
    String? locale,
  }) => ChartConfig(
    symbol: symbol ?? this.symbol,
    theme: theme ?? this.theme,
    interval: interval ?? this.interval,
    locale: locale ?? this.locale,
  );

  @override
  List<Object?> get props => [symbol, theme, interval, locale];
}
