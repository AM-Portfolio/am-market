// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class IndexMetadata {
  /// Returns a new [IndexMetadata] instance.
  IndexMetadata({
    this.indexName,
    this.open,
    this.high,
    this.low,
    this.previousClose,
    this.last,
    this.percChange,
    this.change,
    this.timeVal,
    this.yearHigh,
    this.yearLow,
    this.indicativeClose,
    this.totalTradedVolume,
    this.totalTradedValue,
    this.ffmcSum,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? indexName;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? open;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? high;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? low;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? previousClose;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? last;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? percChange;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? change;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? timeVal;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? yearHigh;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? yearLow;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? indicativeClose;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalTradedVolume;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? totalTradedValue;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? ffmcSum;

  @override
  bool operator ==(Object other) => identical(this, other) || other is IndexMetadata &&
    other.indexName == indexName &&
    other.open == open &&
    other.high == high &&
    other.low == low &&
    other.previousClose == previousClose &&
    other.last == last &&
    other.percChange == percChange &&
    other.change == change &&
    other.timeVal == timeVal &&
    other.yearHigh == yearHigh &&
    other.yearLow == yearLow &&
    other.indicativeClose == indicativeClose &&
    other.totalTradedVolume == totalTradedVolume &&
    other.totalTradedValue == totalTradedValue &&
    other.ffmcSum == ffmcSum;

  @override
  int get hashCode =>
    (indexName == null ? 0 : indexName!.hashCode) +
    (open == null ? 0 : open!.hashCode) +
    (high == null ? 0 : high!.hashCode) +
    (low == null ? 0 : low!.hashCode) +
    (previousClose == null ? 0 : previousClose!.hashCode) +
    (last == null ? 0 : last!.hashCode) +
    (percChange == null ? 0 : percChange!.hashCode) +
    (change == null ? 0 : change!.hashCode) +
    (timeVal == null ? 0 : timeVal!.hashCode) +
    (yearHigh == null ? 0 : yearHigh!.hashCode) +
    (yearLow == null ? 0 : yearLow!.hashCode) +
    (indicativeClose == null ? 0 : indicativeClose!.hashCode) +
    (totalTradedVolume == null ? 0 : totalTradedVolume!.hashCode) +
    (totalTradedValue == null ? 0 : totalTradedValue!.hashCode) +
    (ffmcSum == null ? 0 : ffmcSum!.hashCode);

  @override
  String toString() => 'IndexMetadata[indexName=$indexName, open=$open, high=$high, low=$low, previousClose=$previousClose, last=$last, percChange=$percChange, change=$change, timeVal=$timeVal, yearHigh=$yearHigh, yearLow=$yearLow, indicativeClose=$indicativeClose, totalTradedVolume=$totalTradedVolume, totalTradedValue=$totalTradedValue, ffmcSum=$ffmcSum]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.indexName != null) {
      json[r'indexName'] = this.indexName;
    } else {
      json[r'indexName'] = null;
    }
    if (this.open != null) {
      json[r'open'] = this.open;
    } else {
      json[r'open'] = null;
    }
    if (this.high != null) {
      json[r'high'] = this.high;
    } else {
      json[r'high'] = null;
    }
    if (this.low != null) {
      json[r'low'] = this.low;
    } else {
      json[r'low'] = null;
    }
    if (this.previousClose != null) {
      json[r'previousClose'] = this.previousClose;
    } else {
      json[r'previousClose'] = null;
    }
    if (this.last != null) {
      json[r'last'] = this.last;
    } else {
      json[r'last'] = null;
    }
    if (this.percChange != null) {
      json[r'percChange'] = this.percChange;
    } else {
      json[r'percChange'] = null;
    }
    if (this.change != null) {
      json[r'change'] = this.change;
    } else {
      json[r'change'] = null;
    }
    if (this.timeVal != null) {
      json[r'timeVal'] = this.timeVal;
    } else {
      json[r'timeVal'] = null;
    }
    if (this.yearHigh != null) {
      json[r'yearHigh'] = this.yearHigh;
    } else {
      json[r'yearHigh'] = null;
    }
    if (this.yearLow != null) {
      json[r'yearLow'] = this.yearLow;
    } else {
      json[r'yearLow'] = null;
    }
    if (this.indicativeClose != null) {
      json[r'indicativeClose'] = this.indicativeClose;
    } else {
      json[r'indicativeClose'] = null;
    }
    if (this.totalTradedVolume != null) {
      json[r'totalTradedVolume'] = this.totalTradedVolume;
    } else {
      json[r'totalTradedVolume'] = null;
    }
    if (this.totalTradedValue != null) {
      json[r'totalTradedValue'] = this.totalTradedValue;
    } else {
      json[r'totalTradedValue'] = null;
    }
    if (this.ffmcSum != null) {
      json[r'ffmc_sum'] = this.ffmcSum;
    } else {
      json[r'ffmc_sum'] = null;
    }
    return json;
  }

  /// Returns a new [IndexMetadata] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static IndexMetadata? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "IndexMetadata[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "IndexMetadata[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return IndexMetadata(
        indexName: mapValueOfType<String>(json, r'indexName'),
        open: mapValueOfType<double>(json, r'open'),
        high: mapValueOfType<double>(json, r'high'),
        low: mapValueOfType<double>(json, r'low'),
        previousClose: mapValueOfType<double>(json, r'previousClose'),
        last: mapValueOfType<double>(json, r'last'),
        percChange: mapValueOfType<double>(json, r'percChange'),
        change: mapValueOfType<double>(json, r'change'),
        timeVal: mapValueOfType<String>(json, r'timeVal'),
        yearHigh: mapValueOfType<double>(json, r'yearHigh'),
        yearLow: mapValueOfType<double>(json, r'yearLow'),
        indicativeClose: mapValueOfType<double>(json, r'indicativeClose'),
        totalTradedVolume: mapValueOfType<int>(json, r'totalTradedVolume'),
        totalTradedValue: mapValueOfType<double>(json, r'totalTradedValue'),
        ffmcSum: mapValueOfType<double>(json, r'ffmc_sum'),
      );
    }
    return null;
  }

  static List<IndexMetadata> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <IndexMetadata>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = IndexMetadata.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, IndexMetadata> mapFromJson(dynamic json) {
    final map = <String, IndexMetadata>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = IndexMetadata.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of IndexMetadata-objects as value to a dart map
  static Map<String, List<IndexMetadata>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<IndexMetadata>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = IndexMetadata.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

