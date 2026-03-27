// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class HistoricalDataResponseV1 {
  /// Returns a new [HistoricalDataResponseV1] instance.
  HistoricalDataResponseV1({
    this.data = const {},
    this.metadata,
    this.error,
    this.message,
  });

  Map<String, HistoricalData> data;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  HistoricalDataMetadata? metadata;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? error;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? message;

  @override
  bool operator ==(Object other) => identical(this, other) || other is HistoricalDataResponseV1 &&
    _deepEquality.equals(other.data, data) &&
    other.metadata == metadata &&
    other.error == error &&
    other.message == message;

  @override
  int get hashCode =>
    (data.hashCode) +
    (metadata == null ? 0 : metadata!.hashCode) +
    (error == null ? 0 : error!.hashCode) +
    (message == null ? 0 : message!.hashCode);

  @override
  String toString() => 'HistoricalDataResponseV1[data=$data, metadata=$metadata, error=$error, message=$message]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'data'] = this.data;
    if (this.metadata != null) {
      json[r'metadata'] = this.metadata;
    } else {
      json[r'metadata'] = null;
    }
    if (this.error != null) {
      json[r'error'] = this.error;
    } else {
      json[r'error'] = null;
    }
    if (this.message != null) {
      json[r'message'] = this.message;
    } else {
      json[r'message'] = null;
    }
    return json;
  }

  /// Returns a new [HistoricalDataResponseV1] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static HistoricalDataResponseV1? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "HistoricalDataResponseV1[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "HistoricalDataResponseV1[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return HistoricalDataResponseV1(
        data: HistoricalData.mapFromJson(json[r'data']),
        metadata: HistoricalDataMetadata.fromJson(json[r'metadata']),
        error: mapValueOfType<String>(json, r'error'),
        message: mapValueOfType<String>(json, r'message'),
      );
    }
    return null;
  }

  static List<HistoricalDataResponseV1> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <HistoricalDataResponseV1>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = HistoricalDataResponseV1.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, HistoricalDataResponseV1> mapFromJson(dynamic json) {
    final map = <String, HistoricalDataResponseV1>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = HistoricalDataResponseV1.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of HistoricalDataResponseV1-objects as value to a dart map
  static Map<String, List<HistoricalDataResponseV1>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<HistoricalDataResponseV1>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = HistoricalDataResponseV1.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

