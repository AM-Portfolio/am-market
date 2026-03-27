// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class PositionMargin {
  /// Returns a new [PositionMargin] instance.
  PositionMargin({
    this.tradingSymbol,
    this.totalMargin,
    this.spanMargin,
    this.exposureMargin,
    this.additionalMargin,
    this.type,
    this.exchange,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? tradingSymbol;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? totalMargin;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? spanMargin;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? exposureMargin;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? additionalMargin;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? type;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? exchange;

  @override
  bool operator ==(Object other) => identical(this, other) || other is PositionMargin &&
    other.tradingSymbol == tradingSymbol &&
    other.totalMargin == totalMargin &&
    other.spanMargin == spanMargin &&
    other.exposureMargin == exposureMargin &&
    other.additionalMargin == additionalMargin &&
    other.type == type &&
    other.exchange == exchange;

  @override
  int get hashCode =>
    (tradingSymbol == null ? 0 : tradingSymbol!.hashCode) +
    (totalMargin == null ? 0 : totalMargin!.hashCode) +
    (spanMargin == null ? 0 : spanMargin!.hashCode) +
    (exposureMargin == null ? 0 : exposureMargin!.hashCode) +
    (additionalMargin == null ? 0 : additionalMargin!.hashCode) +
    (type == null ? 0 : type!.hashCode) +
    (exchange == null ? 0 : exchange!.hashCode);

  @override
  String toString() => 'PositionMargin[tradingSymbol=$tradingSymbol, totalMargin=$totalMargin, spanMargin=$spanMargin, exposureMargin=$exposureMargin, additionalMargin=$additionalMargin, type=$type, exchange=$exchange]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.tradingSymbol != null) {
      json[r'tradingSymbol'] = this.tradingSymbol;
    } else {
      json[r'tradingSymbol'] = null;
    }
    if (this.totalMargin != null) {
      json[r'totalMargin'] = this.totalMargin;
    } else {
      json[r'totalMargin'] = null;
    }
    if (this.spanMargin != null) {
      json[r'spanMargin'] = this.spanMargin;
    } else {
      json[r'spanMargin'] = null;
    }
    if (this.exposureMargin != null) {
      json[r'exposureMargin'] = this.exposureMargin;
    } else {
      json[r'exposureMargin'] = null;
    }
    if (this.additionalMargin != null) {
      json[r'additionalMargin'] = this.additionalMargin;
    } else {
      json[r'additionalMargin'] = null;
    }
    if (this.type != null) {
      json[r'type'] = this.type;
    } else {
      json[r'type'] = null;
    }
    if (this.exchange != null) {
      json[r'exchange'] = this.exchange;
    } else {
      json[r'exchange'] = null;
    }
    return json;
  }

  /// Returns a new [PositionMargin] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static PositionMargin? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "PositionMargin[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "PositionMargin[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return PositionMargin(
        tradingSymbol: mapValueOfType<String>(json, r'tradingSymbol'),
        totalMargin: num.parse('${json[r'totalMargin']}'),
        spanMargin: num.parse('${json[r'spanMargin']}'),
        exposureMargin: num.parse('${json[r'exposureMargin']}'),
        additionalMargin: num.parse('${json[r'additionalMargin']}'),
        type: mapValueOfType<String>(json, r'type'),
        exchange: mapValueOfType<String>(json, r'exchange'),
      );
    }
    return null;
  }

  static List<PositionMargin> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <PositionMargin>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = PositionMargin.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, PositionMargin> mapFromJson(dynamic json) {
    final map = <String, PositionMargin>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = PositionMargin.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of PositionMargin-objects as value to a dart map
  static Map<String, List<PositionMargin>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<PositionMargin>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = PositionMargin.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

