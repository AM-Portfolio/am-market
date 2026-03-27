// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class QuotesRequest {
  /// Returns a new [QuotesRequest] instance.
  QuotesRequest({
    this.symbols,
    this.timeFrame,
    this.forceRefresh,
    this.indexSymbol,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? symbols;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? timeFrame;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? forceRefresh;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? indexSymbol;

  @override
  bool operator ==(Object other) => identical(this, other) || other is QuotesRequest &&
    other.symbols == symbols &&
    other.timeFrame == timeFrame &&
    other.forceRefresh == forceRefresh &&
    other.indexSymbol == indexSymbol;

  @override
  int get hashCode =>
    (symbols == null ? 0 : symbols!.hashCode) +
    (timeFrame == null ? 0 : timeFrame!.hashCode) +
    (forceRefresh == null ? 0 : forceRefresh!.hashCode) +
    (indexSymbol == null ? 0 : indexSymbol!.hashCode);

  @override
  String toString() => 'QuotesRequest[symbols=$symbols, timeFrame=$timeFrame, forceRefresh=$forceRefresh, indexSymbol=$indexSymbol]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.symbols != null) {
      json[r'symbols'] = this.symbols;
    } else {
      json[r'symbols'] = null;
    }
    if (this.timeFrame != null) {
      json[r'timeFrame'] = this.timeFrame;
    } else {
      json[r'timeFrame'] = null;
    }
    if (this.forceRefresh != null) {
      json[r'forceRefresh'] = this.forceRefresh;
    } else {
      json[r'forceRefresh'] = null;
    }
    if (this.indexSymbol != null) {
      json[r'indexSymbol'] = this.indexSymbol;
    } else {
      json[r'indexSymbol'] = null;
    }
    return json;
  }

  /// Returns a new [QuotesRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static QuotesRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "QuotesRequest[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "QuotesRequest[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return QuotesRequest(
        symbols: mapValueOfType<String>(json, r'symbols'),
        timeFrame: mapValueOfType<String>(json, r'timeFrame'),
        forceRefresh: mapValueOfType<bool>(json, r'forceRefresh'),
        indexSymbol: mapValueOfType<bool>(json, r'indexSymbol'),
      );
    }
    return null;
  }

  static List<QuotesRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <QuotesRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = QuotesRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, QuotesRequest> mapFromJson(dynamic json) {
    final map = <String, QuotesRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = QuotesRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of QuotesRequest-objects as value to a dart map
  static Map<String, List<QuotesRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<QuotesRequest>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = QuotesRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

