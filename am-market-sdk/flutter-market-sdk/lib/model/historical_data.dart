// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class HistoricalData {
  /// Returns a new [HistoricalData] instance.
  HistoricalData({
    this.tradingSymbol,
    this.isin,
    this.fromDate,
    this.toDate,
    this.interval,
    this.dataPoints = const [],
    this.dataPointCount,
    this.exchange,
    this.currency,
    this.retrievalTime,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? tradingSymbol;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? isin;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? fromDate;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? toDate;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? interval;

  List<OHLCVTPoint> dataPoints;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? dataPointCount;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? exchange;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? currency;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? retrievalTime;

  @override
  bool operator ==(Object other) => identical(this, other) || other is HistoricalData &&
    other.tradingSymbol == tradingSymbol &&
    other.isin == isin &&
    other.fromDate == fromDate &&
    other.toDate == toDate &&
    other.interval == interval &&
    _deepEquality.equals(other.dataPoints, dataPoints) &&
    other.dataPointCount == dataPointCount &&
    other.exchange == exchange &&
    other.currency == currency &&
    other.retrievalTime == retrievalTime;

  @override
  int get hashCode =>
    (tradingSymbol == null ? 0 : tradingSymbol!.hashCode) +
    (isin == null ? 0 : isin!.hashCode) +
    (fromDate == null ? 0 : fromDate!.hashCode) +
    (toDate == null ? 0 : toDate!.hashCode) +
    (interval == null ? 0 : interval!.hashCode) +
    (dataPoints.hashCode) +
    (dataPointCount == null ? 0 : dataPointCount!.hashCode) +
    (exchange == null ? 0 : exchange!.hashCode) +
    (currency == null ? 0 : currency!.hashCode) +
    (retrievalTime == null ? 0 : retrievalTime!.hashCode);

  @override
  String toString() => 'HistoricalData[tradingSymbol=$tradingSymbol, isin=$isin, fromDate=$fromDate, toDate=$toDate, interval=$interval, dataPoints=$dataPoints, dataPointCount=$dataPointCount, exchange=$exchange, currency=$currency, retrievalTime=$retrievalTime]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.tradingSymbol != null) {
      json[r'tradingSymbol'] = this.tradingSymbol;
    } else {
      json[r'tradingSymbol'] = null;
    }
    if (this.isin != null) {
      json[r'isin'] = this.isin;
    } else {
      json[r'isin'] = null;
    }
    if (this.fromDate != null) {
      json[r'fromDate'] = this.fromDate!.toUtc().toIso8601String();
    } else {
      json[r'fromDate'] = null;
    }
    if (this.toDate != null) {
      json[r'toDate'] = this.toDate!.toUtc().toIso8601String();
    } else {
      json[r'toDate'] = null;
    }
    if (this.interval != null) {
      json[r'interval'] = this.interval;
    } else {
      json[r'interval'] = null;
    }
      json[r'dataPoints'] = this.dataPoints;
    if (this.dataPointCount != null) {
      json[r'dataPointCount'] = this.dataPointCount;
    } else {
      json[r'dataPointCount'] = null;
    }
    if (this.exchange != null) {
      json[r'exchange'] = this.exchange;
    } else {
      json[r'exchange'] = null;
    }
    if (this.currency != null) {
      json[r'currency'] = this.currency;
    } else {
      json[r'currency'] = null;
    }
    if (this.retrievalTime != null) {
      json[r'retrievalTime'] = this.retrievalTime!.toUtc().toIso8601String();
    } else {
      json[r'retrievalTime'] = null;
    }
    return json;
  }

  /// Returns a new [HistoricalData] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static HistoricalData? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "HistoricalData[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "HistoricalData[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return HistoricalData(
        tradingSymbol: mapValueOfType<String>(json, r'tradingSymbol'),
        isin: mapValueOfType<String>(json, r'isin'),
        fromDate: mapDateTime(json, r'fromDate', r''),
        toDate: mapDateTime(json, r'toDate', r''),
        interval: mapValueOfType<String>(json, r'interval'),
        dataPoints: OHLCVTPoint.listFromJson(json[r'dataPoints']),
        dataPointCount: mapValueOfType<int>(json, r'dataPointCount'),
        exchange: mapValueOfType<String>(json, r'exchange'),
        currency: mapValueOfType<String>(json, r'currency'),
        retrievalTime: mapDateTime(json, r'retrievalTime', r''),
      );
    }
    return null;
  }

  static List<HistoricalData> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <HistoricalData>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = HistoricalData.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, HistoricalData> mapFromJson(dynamic json) {
    final map = <String, HistoricalData>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = HistoricalData.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of HistoricalData-objects as value to a dart map
  static Map<String, List<HistoricalData>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<HistoricalData>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = HistoricalData.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

