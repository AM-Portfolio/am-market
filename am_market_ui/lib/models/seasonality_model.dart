import 'package:json_annotation/json_annotation.dart';

part 'seasonality_model.g.dart';

@JsonSerializable()
class SeasonalityResponse {
  final String symbol;
  @JsonKey(defaultValue: {})
  final Map<String, double> dayOfWeekReturns;
  @JsonKey(defaultValue: {})
  final Map<String, double> monthlyReturns;

  SeasonalityResponse({
    required this.symbol,
    this.dayOfWeekReturns = const {}, // Default to empty map
    this.monthlyReturns = const {}, // Default to empty map
  });

  factory SeasonalityResponse.fromJson(Map<String, dynamic> json) =>
      _$SeasonalityResponseFromJson(json);

  Map<String, dynamic> toJson() => _$SeasonalityResponseToJson(this);
}
