// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class OHLCVTPoint {
  /// Returns a new [OHLCVTPoint] instance.
  OHLCVTPoint({
    this.time,
    this.open,
    this.high,
    this.low,
    this.close,
    this.volume,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? time;

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
  double? close;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? volume;

  @override
  bool operator ==(Object other) => identical(this, other) || other is OHLCVTPoint &&
    other.time == time &&
    other.open == open &&
    other.high == high &&
    other.low == low &&
    other.close == close &&
    other.volume == volume;

  @override
  int get hashCode =>
    (time == null ? 0 : time!.hashCode) +
    (open == null ? 0 : open!.hashCode) +
    (high == null ? 0 : high!.hashCode) +
    (low == null ? 0 : low!.hashCode) +
    (close == null ? 0 : close!.hashCode) +
    (volume == null ? 0 : volume!.hashCode);

  @override
  String toString() => 'OHLCVTPoint[time=$time, open=$open, high=$high, low=$low, close=$close, volume=$volume]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.time != null) {
      json[r'time'] = this.time!.toUtc().toIso8601String();
    } else {
      json[r'time'] = null;
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
    if (this.close != null) {
      json[r'close'] = this.close;
    } else {
      json[r'close'] = null;
    }
    if (this.volume != null) {
      json[r'volume'] = this.volume;
    } else {
      json[r'volume'] = null;
    }
    return json;
  }

  /// Returns a new [OHLCVTPoint] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static OHLCVTPoint? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "OHLCVTPoint[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "OHLCVTPoint[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return OHLCVTPoint(
        time: mapDateTime(json, r'time', r''),
        open: mapValueOfType<double>(json, r'open'),
        high: mapValueOfType<double>(json, r'high'),
        low: mapValueOfType<double>(json, r'low'),
        close: mapValueOfType<double>(json, r'close'),
        volume: mapValueOfType<int>(json, r'volume'),
      );
    }
    return null;
  }

  static List<OHLCVTPoint> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <OHLCVTPoint>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = OHLCVTPoint.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, OHLCVTPoint> mapFromJson(dynamic json) {
    final map = <String, OHLCVTPoint>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = OHLCVTPoint.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of OHLCVTPoint-objects as value to a dart map
  static Map<String, List<OHLCVTPoint>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<OHLCVTPoint>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = OHLCVTPoint.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

