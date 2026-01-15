class ChartConfig {
  final String symbol;
  final String interval;
  final String chartType;

  const ChartConfig({
    required this.symbol,
    this.interval = '1D',
    this.chartType = 'CANDLE',
  });
}
