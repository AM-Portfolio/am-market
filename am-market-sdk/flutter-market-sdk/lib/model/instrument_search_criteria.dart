// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class InstrumentSearchCriteria {
  /// Returns a new [InstrumentSearchCriteria] instance.
  InstrumentSearchCriteria({
    this.queries = const [],
    this.exchanges = const [],
    this.instrumentTypes = const [],
    this.segments = const [],
    this.isins = const [],
    this.tradingSymbols = const [],
    this.weekly,
    this.provider,
  });

  List<String> queries;

  List<String> exchanges;

  List<String> instrumentTypes;

  List<String> segments;

  List<String> isins;

  List<String> tradingSymbols;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? weekly;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? provider;

  @override
  bool operator ==(Object other) => identical(this, other) || other is InstrumentSearchCriteria &&
    _deepEquality.equals(other.queries, queries) &&
    _deepEquality.equals(other.exchanges, exchanges) &&
    _deepEquality.equals(other.instrumentTypes, instrumentTypes) &&
    _deepEquality.equals(other.segments, segments) &&
    _deepEquality.equals(other.isins, isins) &&
    _deepEquality.equals(other.tradingSymbols, tradingSymbols) &&
    other.weekly == weekly &&
    other.provider == provider;

  @override
  int get hashCode =>
    (queries.hashCode) +
    (exchanges.hashCode) +
    (instrumentTypes.hashCode) +
    (segments.hashCode) +
    (isins.hashCode) +
    (tradingSymbols.hashCode) +
    (weekly == null ? 0 : weekly!.hashCode) +
    (provider == null ? 0 : provider!.hashCode);

  @override
  String toString() => 'InstrumentSearchCriteria[queries=$queries, exchanges=$exchanges, instrumentTypes=$instrumentTypes, segments=$segments, isins=$isins, tradingSymbols=$tradingSymbols, weekly=$weekly, provider=$provider]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'queries'] = this.queries;
      json[r'exchanges'] = this.exchanges;
      json[r'instrumentTypes'] = this.instrumentTypes;
      json[r'segments'] = this.segments;
      json[r'isins'] = this.isins;
      json[r'tradingSymbols'] = this.tradingSymbols;
    if (this.weekly != null) {
      json[r'weekly'] = this.weekly;
    } else {
      json[r'weekly'] = null;
    }
    if (this.provider != null) {
      json[r'provider'] = this.provider;
    } else {
      json[r'provider'] = null;
    }
    return json;
  }

  /// Returns a new [InstrumentSearchCriteria] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static InstrumentSearchCriteria? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "InstrumentSearchCriteria[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "InstrumentSearchCriteria[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return InstrumentSearchCriteria(
        queries: json[r'queries'] is Iterable
            ? (json[r'queries'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        exchanges: json[r'exchanges'] is Iterable
            ? (json[r'exchanges'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        instrumentTypes: json[r'instrumentTypes'] is Iterable
            ? (json[r'instrumentTypes'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        segments: json[r'segments'] is Iterable
            ? (json[r'segments'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        isins: json[r'isins'] is Iterable
            ? (json[r'isins'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        tradingSymbols: json[r'tradingSymbols'] is Iterable
            ? (json[r'tradingSymbols'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        weekly: mapValueOfType<bool>(json, r'weekly'),
        provider: mapValueOfType<String>(json, r'provider'),
      );
    }
    return null;
  }

  static List<InstrumentSearchCriteria> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <InstrumentSearchCriteria>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = InstrumentSearchCriteria.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, InstrumentSearchCriteria> mapFromJson(dynamic json) {
    final map = <String, InstrumentSearchCriteria>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = InstrumentSearchCriteria.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of InstrumentSearchCriteria-objects as value to a dart map
  static Map<String, List<InstrumentSearchCriteria>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<InstrumentSearchCriteria>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = InstrumentSearchCriteria.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

