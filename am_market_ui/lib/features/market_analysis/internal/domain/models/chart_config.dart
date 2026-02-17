class ChartConfig {
  final String symbol;
  final String interval;
  final String chartType;
  final String theme;
  final String locale;

  const ChartConfig({
    required this.symbol,
    this.interval = '1D',
    this.chartType = 'CANDLE',
    this.theme = 'dark',
    this.locale = 'en',
  });
}
