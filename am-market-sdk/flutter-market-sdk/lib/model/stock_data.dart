// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class StockData {
  /// Returns a new [StockData] instance.
  StockData({
    this.symbol,
    this.identifier,
    this.series,
    this.name,
    this.ffmc,
    this.companyName,
    this.isin,
    this.industry,
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
  String? identifier;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? series;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? name;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? ffmc;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? companyName;

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
  String? industry;

  @override
  bool operator ==(Object other) => identical(this, other) || other is StockData &&
    other.symbol == symbol &&
    other.identifier == identifier &&
    other.series == series &&
    other.name == name &&
    other.ffmc == ffmc &&
    other.companyName == companyName &&
    other.isin == isin &&
    other.industry == industry;

  @override
  int get hashCode =>
    (symbol == null ? 0 : symbol!.hashCode) +
    (identifier == null ? 0 : identifier!.hashCode) +
    (series == null ? 0 : series!.hashCode) +
    (name == null ? 0 : name!.hashCode) +
    (ffmc == null ? 0 : ffmc!.hashCode) +
    (companyName == null ? 0 : companyName!.hashCode) +
    (isin == null ? 0 : isin!.hashCode) +
    (industry == null ? 0 : industry!.hashCode);

  @override
  String toString() => 'StockData[symbol=$symbol, identifier=$identifier, series=$series, name=$name, ffmc=$ffmc, companyName=$companyName, isin=$isin, industry=$industry]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.symbol != null) {
      json[r'symbol'] = this.symbol;
    } else {
      json[r'symbol'] = null;
    }
    if (this.identifier != null) {
      json[r'identifier'] = this.identifier;
    } else {
      json[r'identifier'] = null;
    }
    if (this.series != null) {
      json[r'series'] = this.series;
    } else {
      json[r'series'] = null;
    }
    if (this.name != null) {
      json[r'name'] = this.name;
    } else {
      json[r'name'] = null;
    }
    if (this.ffmc != null) {
      json[r'ffmc'] = this.ffmc;
    } else {
      json[r'ffmc'] = null;
    }
    if (this.companyName != null) {
      json[r'companyName'] = this.companyName;
    } else {
      json[r'companyName'] = null;
    }
    if (this.isin != null) {
      json[r'isin'] = this.isin;
    } else {
      json[r'isin'] = null;
    }
    if (this.industry != null) {
      json[r'industry'] = this.industry;
    } else {
      json[r'industry'] = null;
    }
    return json;
  }

  /// Returns a new [StockData] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static StockData? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "StockData[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "StockData[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return StockData(
        symbol: mapValueOfType<String>(json, r'symbol'),
        identifier: mapValueOfType<String>(json, r'identifier'),
        series: mapValueOfType<String>(json, r'series'),
        name: mapValueOfType<String>(json, r'name'),
        ffmc: mapValueOfType<int>(json, r'ffmc'),
        companyName: mapValueOfType<String>(json, r'companyName'),
        isin: mapValueOfType<String>(json, r'isin'),
        industry: mapValueOfType<String>(json, r'industry'),
      );
    }
    return null;
  }

  static List<StockData> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <StockData>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = StockData.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, StockData> mapFromJson(dynamic json) {
    final map = <String, StockData>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = StockData.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of StockData-objects as value to a dart map
  static Map<String, List<StockData>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<StockData>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = StockData.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

