// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class SecurityKey {
  /// Returns a new [SecurityKey] instance.
  SecurityKey({
    this.symbol,
    this.isin,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? symbol;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? isin;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SecurityKey &&
    other.symbol == symbol &&
    other.isin == isin;

  @override
  int get hashCode =>
    (symbol == null ? 0 : symbol!.hashCode) +
    (isin == null ? 0 : isin!.hashCode);

  @override
  String toString() => 'SecurityKey[symbol=$symbol, isin=$isin]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.symbol != null) {
      json[r'symbol'] = this.symbol;
    } else {
      json[r'symbol'] = null;
    }
    if (this.isin != null) {
      json[r'isin'] = this.isin;
    } else {
      json[r'isin'] = null;
    }
    return json;
  }

  /// Returns a new [SecurityKey] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static SecurityKey? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "SecurityKey[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "SecurityKey[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return SecurityKey(
        symbol: mapValueOfType<String>(json, r'symbol'),
        isin: mapValueOfType<String>(json, r'isin'),
      );
    }
    return null;
  }

  static List<SecurityKey> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SecurityKey>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SecurityKey.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SecurityKey> mapFromJson(dynamic json) {
    final map = <String, SecurityKey>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = SecurityKey.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SecurityKey-objects as value to a dart map
  static Map<String, List<SecurityKey>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SecurityKey>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SecurityKey.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

