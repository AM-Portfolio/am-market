// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class HistoricalDataMetadata {
  /// Returns a new [HistoricalDataMetadata] instance.
  HistoricalDataMetadata({
    this.fromDate,
    this.toDate,
    this.interval,
    this.intervalEnum,
    this.totalSymbols,
    this.successfulSymbols,
    this.totalDataPoints,
    this.filteredDataPoints,
    this.filtered,
    this.filterType,
    this.filterFrequency,
    this.processingTimeMs,
    this.source_,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? fromDate;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? toDate;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? interval;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? intervalEnum;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalSymbols;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? successfulSymbols;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalDataPoints;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? filteredDataPoints;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? filtered;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? filterType;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? filterFrequency;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? processingTimeMs;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? source_;

  @override
  bool operator ==(Object other) => identical(this, other) || other is HistoricalDataMetadata &&
    other.fromDate == fromDate &&
    other.toDate == toDate &&
    other.interval == interval &&
    other.intervalEnum == intervalEnum &&
    other.totalSymbols == totalSymbols &&
    other.successfulSymbols == successfulSymbols &&
    other.totalDataPoints == totalDataPoints &&
    other.filteredDataPoints == filteredDataPoints &&
    other.filtered == filtered &&
    other.filterType == filterType &&
    other.filterFrequency == filterFrequency &&
    other.processingTimeMs == processingTimeMs &&
    other.source_ == source_;

  @override
  int get hashCode =>
    (fromDate == null ? 0 : fromDate!.hashCode) +
    (toDate == null ? 0 : toDate!.hashCode) +
    (interval == null ? 0 : interval!.hashCode) +
    (intervalEnum == null ? 0 : intervalEnum!.hashCode) +
    (totalSymbols == null ? 0 : totalSymbols!.hashCode) +
    (successfulSymbols == null ? 0 : successfulSymbols!.hashCode) +
    (totalDataPoints == null ? 0 : totalDataPoints!.hashCode) +
    (filteredDataPoints == null ? 0 : filteredDataPoints!.hashCode) +
    (filtered == null ? 0 : filtered!.hashCode) +
    (filterType == null ? 0 : filterType!.hashCode) +
    (filterFrequency == null ? 0 : filterFrequency!.hashCode) +
    (processingTimeMs == null ? 0 : processingTimeMs!.hashCode) +
    (source_ == null ? 0 : source_!.hashCode);

  @override
  String toString() => 'HistoricalDataMetadata[fromDate=$fromDate, toDate=$toDate, interval=$interval, intervalEnum=$intervalEnum, totalSymbols=$totalSymbols, successfulSymbols=$successfulSymbols, totalDataPoints=$totalDataPoints, filteredDataPoints=$filteredDataPoints, filtered=$filtered, filterType=$filterType, filterFrequency=$filterFrequency, processingTimeMs=$processingTimeMs, source_=$source_]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.fromDate != null) {
      json[r'fromDate'] = this.fromDate;
    } else {
      json[r'fromDate'] = null;
    }
    if (this.toDate != null) {
      json[r'toDate'] = this.toDate;
    } else {
      json[r'toDate'] = null;
    }
    if (this.interval != null) {
      json[r'interval'] = this.interval;
    } else {
      json[r'interval'] = null;
    }
    if (this.intervalEnum != null) {
      json[r'intervalEnum'] = this.intervalEnum;
    } else {
      json[r'intervalEnum'] = null;
    }
    if (this.totalSymbols != null) {
      json[r'totalSymbols'] = this.totalSymbols;
    } else {
      json[r'totalSymbols'] = null;
    }
    if (this.successfulSymbols != null) {
      json[r'successfulSymbols'] = this.successfulSymbols;
    } else {
      json[r'successfulSymbols'] = null;
    }
    if (this.totalDataPoints != null) {
      json[r'totalDataPoints'] = this.totalDataPoints;
    } else {
      json[r'totalDataPoints'] = null;
    }
    if (this.filteredDataPoints != null) {
      json[r'filteredDataPoints'] = this.filteredDataPoints;
    } else {
      json[r'filteredDataPoints'] = null;
    }
    if (this.filtered != null) {
      json[r'filtered'] = this.filtered;
    } else {
      json[r'filtered'] = null;
    }
    if (this.filterType != null) {
      json[r'filterType'] = this.filterType;
    } else {
      json[r'filterType'] = null;
    }
    if (this.filterFrequency != null) {
      json[r'filterFrequency'] = this.filterFrequency;
    } else {
      json[r'filterFrequency'] = null;
    }
    if (this.processingTimeMs != null) {
      json[r'processingTimeMs'] = this.processingTimeMs;
    } else {
      json[r'processingTimeMs'] = null;
    }
    if (this.source_ != null) {
      json[r'source'] = this.source_;
    } else {
      json[r'source'] = null;
    }
    return json;
  }

  /// Returns a new [HistoricalDataMetadata] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static HistoricalDataMetadata? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "HistoricalDataMetadata[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "HistoricalDataMetadata[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return HistoricalDataMetadata(
        fromDate: mapValueOfType<String>(json, r'fromDate'),
        toDate: mapValueOfType<String>(json, r'toDate'),
        interval: mapValueOfType<String>(json, r'interval'),
        intervalEnum: mapValueOfType<String>(json, r'intervalEnum'),
        totalSymbols: mapValueOfType<int>(json, r'totalSymbols'),
        successfulSymbols: mapValueOfType<int>(json, r'successfulSymbols'),
        totalDataPoints: mapValueOfType<int>(json, r'totalDataPoints'),
        filteredDataPoints: mapValueOfType<int>(json, r'filteredDataPoints'),
        filtered: mapValueOfType<bool>(json, r'filtered'),
        filterType: mapValueOfType<String>(json, r'filterType'),
        filterFrequency: mapValueOfType<int>(json, r'filterFrequency'),
        processingTimeMs: mapValueOfType<int>(json, r'processingTimeMs'),
        source_: mapValueOfType<String>(json, r'source'),
      );
    }
    return null;
  }

  static List<HistoricalDataMetadata> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <HistoricalDataMetadata>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = HistoricalDataMetadata.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, HistoricalDataMetadata> mapFromJson(dynamic json) {
    final map = <String, HistoricalDataMetadata>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = HistoricalDataMetadata.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of HistoricalDataMetadata-objects as value to a dart map
  static Map<String, List<HistoricalDataMetadata>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<HistoricalDataMetadata>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = HistoricalDataMetadata.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

