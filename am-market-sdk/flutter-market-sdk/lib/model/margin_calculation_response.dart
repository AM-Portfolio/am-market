// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class MarginCalculationResponse {
  /// Returns a new [MarginCalculationResponse] instance.
  MarginCalculationResponse({
    this.totalMarginRequired,
    this.spanMargin,
    this.exposureMargin,
    this.additionalMargin,
    this.positionMargins = const {},
    this.status,
    this.error,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  num? totalMarginRequired;

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

  Map<String, PositionMargin> positionMargins;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? status;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? error;

  @override
  bool operator ==(Object other) => identical(this, other) || other is MarginCalculationResponse &&
    other.totalMarginRequired == totalMarginRequired &&
    other.spanMargin == spanMargin &&
    other.exposureMargin == exposureMargin &&
    other.additionalMargin == additionalMargin &&
    _deepEquality.equals(other.positionMargins, positionMargins) &&
    other.status == status &&
    other.error == error;

  @override
  int get hashCode =>
    (totalMarginRequired == null ? 0 : totalMarginRequired!.hashCode) +
    (spanMargin == null ? 0 : spanMargin!.hashCode) +
    (exposureMargin == null ? 0 : exposureMargin!.hashCode) +
    (additionalMargin == null ? 0 : additionalMargin!.hashCode) +
    (positionMargins.hashCode) +
    (status == null ? 0 : status!.hashCode) +
    (error == null ? 0 : error!.hashCode);

  @override
  String toString() => 'MarginCalculationResponse[totalMarginRequired=$totalMarginRequired, spanMargin=$spanMargin, exposureMargin=$exposureMargin, additionalMargin=$additionalMargin, positionMargins=$positionMargins, status=$status, error=$error]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.totalMarginRequired != null) {
      json[r'totalMarginRequired'] = this.totalMarginRequired;
    } else {
      json[r'totalMarginRequired'] = null;
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
      json[r'positionMargins'] = this.positionMargins;
    if (this.status != null) {
      json[r'status'] = this.status;
    } else {
      json[r'status'] = null;
    }
    if (this.error != null) {
      json[r'error'] = this.error;
    } else {
      json[r'error'] = null;
    }
    return json;
  }

  /// Returns a new [MarginCalculationResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static MarginCalculationResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "MarginCalculationResponse[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "MarginCalculationResponse[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return MarginCalculationResponse(
        totalMarginRequired: num.parse('${json[r'totalMarginRequired']}'),
        spanMargin: num.parse('${json[r'spanMargin']}'),
        exposureMargin: num.parse('${json[r'exposureMargin']}'),
        additionalMargin: num.parse('${json[r'additionalMargin']}'),
        positionMargins: PositionMargin.mapFromJson(json[r'positionMargins']),
        status: mapValueOfType<String>(json, r'status'),
        error: mapValueOfType<String>(json, r'error'),
      );
    }
    return null;
  }

  static List<MarginCalculationResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <MarginCalculationResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = MarginCalculationResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, MarginCalculationResponse> mapFromJson(dynamic json) {
    final map = <String, MarginCalculationResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = MarginCalculationResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of MarginCalculationResponse-objects as value to a dart map
  static Map<String, List<MarginCalculationResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<MarginCalculationResponse>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = MarginCalculationResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

