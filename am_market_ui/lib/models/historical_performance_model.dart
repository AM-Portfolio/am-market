class HistoricalPerformanceResponse {
  final String symbol;
  final int startYear;
  final int endYear;
  final double? overallReturn;
  final List<YearlyPerformance> yearlyPerformance;

  HistoricalPerformanceResponse({
    required this.symbol,
    required this.startYear,
    required this.endYear,
    this.overallReturn,
    required this.yearlyPerformance,
  });

  factory HistoricalPerformanceResponse.fromJson(Map<String, dynamic> json) {
    return HistoricalPerformanceResponse(
      symbol: json['symbol'] ?? '',
      startYear: json['startYear'] ?? 0,
      endYear: json['endYear'] ?? 0,
      overallReturn: (json['overallReturn'] as num?)?.toDouble(),
      yearlyPerformance: (json['yearlyPerformance'] as List<dynamic>?)
              ?.map((e) => YearlyPerformance.fromJson(e))
              .toList() ??
          [],
    );
  }
}

class YearlyPerformance {
  final int year;
  final double? yearlyReturn;
  final Map<String, double> monthlyReturns;
  final Map<String, Map<int, double>>? dailyReturns;

  YearlyPerformance({
    required this.year,
    this.yearlyReturn,
    required this.monthlyReturns,
    this.dailyReturns,
  });

  factory YearlyPerformance.fromJson(Map<String, dynamic> json) {
    // Parse monthly returns
    final Map<String, double> months = {};
    if (json['monthlyReturns'] != null) {
      (json['monthlyReturns'] as Map<String, dynamic>).forEach((key, value) {
        months[key] = (value as num).toDouble();
      });
    }

    // Parse daily returns if needed (optional)
    Map<String, Map<int, double>>? daily;
    if (json['dailyReturns'] != null) {
      daily = {};
      (json['dailyReturns'] as Map<String, dynamic>).forEach((month, daysMap) {
        final Map<int, double> days = {};
        (daysMap as Map<String, dynamic>).forEach((dayStr, val) {
           days[int.parse(dayStr)] = (val as num).toDouble();
        });
        daily![month] = days;
      });
    }

    return YearlyPerformance(
      year: json['year'] ?? 0,
      yearlyReturn: (json['yearlyReturn'] as num?)?.toDouble(),
      monthlyReturns: months,
      dailyReturns: daily,
    );
  }
}
