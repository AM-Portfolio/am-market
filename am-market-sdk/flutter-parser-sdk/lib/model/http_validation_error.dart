// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class HTTPValidationError {
  /// Returns a new [HTTPValidationError] instance.
  HTTPValidationError({
    this.detail = const [],
  });

  List<ValidationError> detail;

  @override
  bool operator ==(Object other) => identical(this, other) || other is HTTPValidationError &&
    _deepEquality.equals(other.detail, detail);

  @override
  int get hashCode =>
    (detail.hashCode);

  @override
  String toString() => 'HTTPValidationError[detail=$detail]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'detail'] = this.detail;
    return json;
  }

  /// Returns a new [HTTPValidationError] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static HTTPValidationError? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "HTTPValidationError[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "HTTPValidationError[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return HTTPValidationError(
        detail: ValidationError.listFromJson(json[r'detail']),
      );
    }
    return null;
  }

  static List<HTTPValidationError> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <HTTPValidationError>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = HTTPValidationError.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, HTTPValidationError> mapFromJson(dynamic json) {
    final map = <String, HTTPValidationError>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = HTTPValidationError.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of HTTPValidationError-objects as value to a dart map
  static Map<String, List<HTTPValidationError>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<HTTPValidationError>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = HTTPValidationError.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

