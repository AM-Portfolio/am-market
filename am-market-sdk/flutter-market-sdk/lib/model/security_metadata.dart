// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class SecurityMetadata {
  /// Returns a new [SecurityMetadata] instance.
  SecurityMetadata({
    this.sector,
    this.industry,
    this.marketCapValue,
    this.marketCapType,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sector;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? industry;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? marketCapValue;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? marketCapType;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SecurityMetadata &&
    other.sector == sector &&
    other.industry == industry &&
    other.marketCapValue == marketCapValue &&
    other.marketCapType == marketCapType;

  @override
  int get hashCode =>
    (sector == null ? 0 : sector!.hashCode) +
    (industry == null ? 0 : industry!.hashCode) +
    (marketCapValue == null ? 0 : marketCapValue!.hashCode) +
    (marketCapType == null ? 0 : marketCapType!.hashCode);

  @override
  String toString() => 'SecurityMetadata[sector=$sector, industry=$industry, marketCapValue=$marketCapValue, marketCapType=$marketCapType]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.sector != null) {
      json[r'sector'] = this.sector;
    } else {
      json[r'sector'] = null;
    }
    if (this.industry != null) {
      json[r'industry'] = this.industry;
    } else {
      json[r'industry'] = null;
    }
    if (this.marketCapValue != null) {
      json[r'marketCapValue'] = this.marketCapValue;
    } else {
      json[r'marketCapValue'] = null;
    }
    if (this.marketCapType != null) {
      json[r'marketCapType'] = this.marketCapType;
    } else {
      json[r'marketCapType'] = null;
    }
    return json;
  }

  /// Returns a new [SecurityMetadata] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static SecurityMetadata? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "SecurityMetadata[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "SecurityMetadata[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return SecurityMetadata(
        sector: mapValueOfType<String>(json, r'sector'),
        industry: mapValueOfType<String>(json, r'industry'),
        marketCapValue: mapValueOfType<int>(json, r'marketCapValue'),
        marketCapType: mapValueOfType<String>(json, r'marketCapType'),
      );
    }
    return null;
  }

  static List<SecurityMetadata> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SecurityMetadata>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SecurityMetadata.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SecurityMetadata> mapFromJson(dynamic json) {
    final map = <String, SecurityMetadata>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = SecurityMetadata.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SecurityMetadata-objects as value to a dart map
  static Map<String, List<SecurityMetadata>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SecurityMetadata>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SecurityMetadata.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

