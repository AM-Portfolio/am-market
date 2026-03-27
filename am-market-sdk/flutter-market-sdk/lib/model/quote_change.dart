// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class QuoteChange {
  /// Returns a new [QuoteChange] instance.
  QuoteChange({
    this.lastPrice,
    this.open,
    this.high,
    this.low,
    this.close,
    this.previousClose,
    this.change,
    this.changePercent,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? lastPrice;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? open;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? high;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? low;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? close;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? previousClose;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? change;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  double? changePercent;

  @override
  bool operator ==(Object other) => identical(this, other) || other is QuoteChange &&
    other.lastPrice == lastPrice &&
    other.open == open &&
    other.high == high &&
    other.low == low &&
    other.close == close &&
    other.previousClose == previousClose &&
    other.change == change &&
    other.changePercent == changePercent;

  @override
  int get hashCode =>
    (lastPrice == null ? 0 : lastPrice!.hashCode) +
    (open == null ? 0 : open!.hashCode) +
    (high == null ? 0 : high!.hashCode) +
    (low == null ? 0 : low!.hashCode) +
    (close == null ? 0 : close!.hashCode) +
    (previousClose == null ? 0 : previousClose!.hashCode) +
    (change == null ? 0 : change!.hashCode) +
    (changePercent == null ? 0 : changePercent!.hashCode);

  @override
  String toString() => 'QuoteChange[lastPrice=$lastPrice, open=$open, high=$high, low=$low, close=$close, previousClose=$previousClose, change=$change, changePercent=$changePercent]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.lastPrice != null) {
      json[r'lastPrice'] = this.lastPrice;
    } else {
      json[r'lastPrice'] = null;
    }
    if (this.open != null) {
      json[r'open'] = this.open;
    } else {
      json[r'open'] = null;
    }
    if (this.high != null) {
      json[r'high'] = this.high;
    } else {
      json[r'high'] = null;
    }
    if (this.low != null) {
      json[r'low'] = this.low;
    } else {
      json[r'low'] = null;
    }
    if (this.close != null) {
      json[r'close'] = this.close;
    } else {
      json[r'close'] = null;
    }
    if (this.previousClose != null) {
      json[r'previousClose'] = this.previousClose;
    } else {
      json[r'previousClose'] = null;
    }
    if (this.change != null) {
      json[r'change'] = this.change;
    } else {
      json[r'change'] = null;
    }
    if (this.changePercent != null) {
      json[r'changePercent'] = this.changePercent;
    } else {
      json[r'changePercent'] = null;
    }
    return json;
  }

  /// Returns a new [QuoteChange] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static QuoteChange? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "QuoteChange[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "QuoteChange[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return QuoteChange(
        lastPrice: mapValueOfType<double>(json, r'lastPrice'),
        open: mapValueOfType<double>(json, r'open'),
        high: mapValueOfType<double>(json, r'high'),
        low: mapValueOfType<double>(json, r'low'),
        close: mapValueOfType<double>(json, r'close'),
        previousClose: mapValueOfType<double>(json, r'previousClose'),
        change: mapValueOfType<double>(json, r'change'),
        changePercent: mapValueOfType<double>(json, r'changePercent'),
      );
    }
    return null;
  }

  static List<QuoteChange> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <QuoteChange>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = QuoteChange.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, QuoteChange> mapFromJson(dynamic json) {
    final map = <String, QuoteChange>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = QuoteChange.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of QuoteChange-objects as value to a dart map
  static Map<String, List<QuoteChange>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<QuoteChange>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = QuoteChange.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

