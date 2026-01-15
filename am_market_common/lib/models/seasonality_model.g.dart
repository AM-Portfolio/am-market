// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'seasonality_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

SeasonalityResponse _$SeasonalityResponseFromJson(Map<String, dynamic> json) =>
    SeasonalityResponse(
      symbol: json['symbol'] as String,
      dayOfWeekReturns:
          (json['dayOfWeekReturns'] as Map<String, dynamic>?)?.map(
            (k, e) => MapEntry(k, (e as num).toDouble()),
          ) ??
          {},
      monthlyReturns:
          (json['monthlyReturns'] as Map<String, dynamic>?)?.map(
            (k, e) => MapEntry(k, (e as num).toDouble()),
          ) ??
          {},
    );

Map<String, dynamic> _$SeasonalityResponseToJson(
  SeasonalityResponse instance,
) => <String, dynamic>{
  'symbol': instance.symbol,
  'dayOfWeekReturns': instance.dayOfWeekReturns,
  'monthlyReturns': instance.monthlyReturns,
};
