// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class SecuritySearchRequest {
  /// Returns a new [SecuritySearchRequest] instance.
  SecuritySearchRequest({
    this.symbols = const [],
    this.isin,
    this.sector,
    this.industry,
    this.index,
    this.query,
  });

  List<String> symbols;

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
  String? index;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? query;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SecuritySearchRequest &&
    _deepEquality.equals(other.symbols, symbols) &&
    other.isin == isin &&
    other.sector == sector &&
    other.industry == industry &&
    other.index == index &&
    other.query == query;

  @override
  int get hashCode =>
    (symbols.hashCode) +
    (isin == null ? 0 : isin!.hashCode) +
    (sector == null ? 0 : sector!.hashCode) +
    (industry == null ? 0 : industry!.hashCode) +
    (index == null ? 0 : index!.hashCode) +
    (query == null ? 0 : query!.hashCode);

  @override
  String toString() => 'SecuritySearchRequest[symbols=$symbols, isin=$isin, sector=$sector, industry=$industry, index=$index, query=$query]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'symbols'] = this.symbols;
    if (this.isin != null) {
      json[r'isin'] = this.isin;
    } else {
      json[r'isin'] = null;
    }
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
    if (this.index != null) {
      json[r'index'] = this.index;
    } else {
      json[r'index'] = null;
    }
    if (this.query != null) {
      json[r'query'] = this.query;
    } else {
      json[r'query'] = null;
    }
    return json;
  }

  /// Returns a new [SecuritySearchRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static SecuritySearchRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "SecuritySearchRequest[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "SecuritySearchRequest[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return SecuritySearchRequest(
        symbols: json[r'symbols'] is Iterable
            ? (json[r'symbols'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        isin: mapValueOfType<String>(json, r'isin'),
        sector: mapValueOfType<String>(json, r'sector'),
        industry: mapValueOfType<String>(json, r'industry'),
        index: mapValueOfType<String>(json, r'index'),
        query: mapValueOfType<String>(json, r'query'),
      );
    }
    return null;
  }

  static List<SecuritySearchRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SecuritySearchRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SecuritySearchRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SecuritySearchRequest> mapFromJson(dynamic json) {
    final map = <String, SecuritySearchRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = SecuritySearchRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SecuritySearchRequest-objects as value to a dart map
  static Map<String, List<SecuritySearchRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SecuritySearchRequest>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SecuritySearchRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

