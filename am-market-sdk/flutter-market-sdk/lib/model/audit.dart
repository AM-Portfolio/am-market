// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class Audit {
  /// Returns a new [Audit] instance.
  Audit({
    this.createdAt,
    this.version,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? createdAt;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? version;

  @override
  bool operator ==(Object other) => identical(this, other) || other is Audit &&
    other.createdAt == createdAt &&
    other.version == version;

  @override
  int get hashCode =>
    (createdAt == null ? 0 : createdAt!.hashCode) +
    (version == null ? 0 : version!.hashCode);

  @override
  String toString() => 'Audit[createdAt=$createdAt, version=$version]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.createdAt != null) {
      json[r'createdAt'] = this.createdAt!.toUtc().toIso8601String();
    } else {
      json[r'createdAt'] = null;
    }
    if (this.version != null) {
      json[r'version'] = this.version;
    } else {
      json[r'version'] = null;
    }
    return json;
  }

  /// Returns a new [Audit] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static Audit? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "Audit[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "Audit[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return Audit(
        createdAt: mapDateTime(json, r'createdAt', r''),
        version: mapValueOfType<int>(json, r'version'),
      );
    }
    return null;
  }

  static List<Audit> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <Audit>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = Audit.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, Audit> mapFromJson(dynamic json) {
    final map = <String, Audit>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = Audit.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of Audit-objects as value to a dart map
  static Map<String, List<Audit>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<Audit>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = Audit.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

