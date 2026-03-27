// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class StockIndicesMarketData {
  /// Returns a new [StockIndicesMarketData] instance.
  StockIndicesMarketData({
    this.indexSymbol,
    this.data = const [],
    this.metadata,
    this.docVersion,
    this.audit,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? indexSymbol;

  List<StockData> data;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  IndexMetadata? metadata;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? docVersion;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  AuditData? audit;

  @override
  bool operator ==(Object other) => identical(this, other) || other is StockIndicesMarketData &&
    other.indexSymbol == indexSymbol &&
    _deepEquality.equals(other.data, data) &&
    other.metadata == metadata &&
    other.docVersion == docVersion &&
    other.audit == audit;

  @override
  int get hashCode =>
    (indexSymbol == null ? 0 : indexSymbol!.hashCode) +
    (data.hashCode) +
    (metadata == null ? 0 : metadata!.hashCode) +
    (docVersion == null ? 0 : docVersion!.hashCode) +
    (audit == null ? 0 : audit!.hashCode);

  @override
  String toString() => 'StockIndicesMarketData[indexSymbol=$indexSymbol, data=$data, metadata=$metadata, docVersion=$docVersion, audit=$audit]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.indexSymbol != null) {
      json[r'indexSymbol'] = this.indexSymbol;
    } else {
      json[r'indexSymbol'] = null;
    }
      json[r'data'] = this.data;
    if (this.metadata != null) {
      json[r'metadata'] = this.metadata;
    } else {
      json[r'metadata'] = null;
    }
    if (this.docVersion != null) {
      json[r'docVersion'] = this.docVersion;
    } else {
      json[r'docVersion'] = null;
    }
    if (this.audit != null) {
      json[r'audit'] = this.audit;
    } else {
      json[r'audit'] = null;
    }
    return json;
  }

  /// Returns a new [StockIndicesMarketData] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static StockIndicesMarketData? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "StockIndicesMarketData[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "StockIndicesMarketData[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return StockIndicesMarketData(
        indexSymbol: mapValueOfType<String>(json, r'indexSymbol'),
        data: StockData.listFromJson(json[r'data']),
        metadata: IndexMetadata.fromJson(json[r'metadata']),
        docVersion: mapValueOfType<String>(json, r'docVersion'),
        audit: AuditData.fromJson(json[r'audit']),
      );
    }
    return null;
  }

  static List<StockIndicesMarketData> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <StockIndicesMarketData>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = StockIndicesMarketData.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, StockIndicesMarketData> mapFromJson(dynamic json) {
    final map = <String, StockIndicesMarketData>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = StockIndicesMarketData.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of StockIndicesMarketData-objects as value to a dart map
  static Map<String, List<StockIndicesMarketData>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<StockIndicesMarketData>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = StockIndicesMarketData.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

