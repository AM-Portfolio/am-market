class AvailableIndices {
  final List<String> broadMarketIndices;
  final List<String> sectoralIndices;
  final List<String> thematicIndices;
  final List<String> strategyIndices;

  AvailableIndices({
    this.broadMarketIndices = const [],
    this.sectoralIndices = const [],
    this.thematicIndices = const [],
    this.strategyIndices = const [],
  });

  factory AvailableIndices.fromJson(Map<String, dynamic> json) {
    return AvailableIndices(
      broadMarketIndices: List<String>.from(json['broad'] ?? json['broadMarketIndices'] ?? []),
      sectoralIndices: List<String>.from(json['sector'] ?? json['sectoralIndices'] ?? []),
      thematicIndices: List<String>.from(json['thematic'] ?? json['thematicIndices'] ?? []),
      strategyIndices: List<String>.from(json['strategy'] ?? json['strategyIndices'] ?? []),
    );
  }

  // Legacy compatibility getters
  List<String> get broad => broadMarketIndices;
  List<String> get sector => sectoralIndices;
  List<String> get sectoral => sectoralIndices;
}
