class IndicesHistoricalPerformanceResponse {
  final int startYear;
  final int endYear;
  final List<MonthlyIndicesPerformance> monthlyPerformance;

  IndicesHistoricalPerformanceResponse({
    required this.startYear,
    required this.endYear,
    required this.monthlyPerformance,
  });

  factory IndicesHistoricalPerformanceResponse.fromJson(Map<String, dynamic> json) {
    return IndicesHistoricalPerformanceResponse(
      startYear: json['startYear'] ?? 0,
      endYear: json['endYear'] ?? 0,
      monthlyPerformance: (json['monthlyPerformance'] as List<dynamic>?)
              ?.map((e) => MonthlyIndicesPerformance.fromJson(e))
              .toList() ??
          [],
    );
  }
}

class MonthlyIndicesPerformance {
  final int year;
  final int month;
  final String monthName;
  final IndexPerformance? topPerformer;
  final IndexPerformance? worstPerformer;
  final List<IndexPerformance> allIndices;

  MonthlyIndicesPerformance({
    required this.year,
    required this.month,
    required this.monthName,
    this.topPerformer,
    this.worstPerformer,
    required this.allIndices,
  });

  factory MonthlyIndicesPerformance.fromJson(Map<String, dynamic> json) {
    return MonthlyIndicesPerformance(
      year: json['year'] ?? 0,
      month: json['month'] ?? 0,
      monthName: json['monthName'] ?? '',
      topPerformer: json['topPerformer'] != null
          ? IndexPerformance.fromJson(json['topPerformer'])
          : null,
      worstPerformer: json['worstPerformer'] != null
          ? IndexPerformance.fromJson(json['worstPerformer'])
          : null,
      allIndices: (json['allIndices'] as List<dynamic>?)
              ?.map((e) => IndexPerformance.fromJson(e))
              .toList() ??
          [],
    );
  }
}

class IndexPerformance {
  final String symbol;
  final double returnPercentage;

  IndexPerformance({
    required this.symbol,
    required this.returnPercentage,
  });

  factory IndexPerformance.fromJson(Map<String, dynamic> json) {
    return IndexPerformance(
      symbol: json['symbol'] ?? '',
      returnPercentage: (json['returnPercentage'] as num?)?.toDouble() ?? 0.0,
    );
  }
}
