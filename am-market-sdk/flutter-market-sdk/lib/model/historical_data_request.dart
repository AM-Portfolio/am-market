// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class HistoricalDataRequest {
  /// Returns a new [HistoricalDataRequest] instance.
  HistoricalDataRequest({
    this.symbols,
    this.from,
    this.to,
    this.interval,
    this.continuous,
    this.instrumentType,
    this.forceRefresh,
    this.filterType,
    this.filterFrequency,
    this.additionalParams = const {},
    this.isIndexSymbol,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? symbols;

  /// Start date in yyyy-MM-dd format
  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? from;

  /// End date in yyyy-MM-dd format (optional, defaults to current date)
  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? to;

  HistoricalDataRequestIntervalEnum? interval;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? continuous;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? instrumentType;

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
  String? filterType;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? filterFrequency;

  Map<String, Object> additionalParams;

  /// Whether the symbols represent indices that should be expanded to constituent stocks
  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? isIndexSymbol;

  @override
  bool operator ==(Object other) => identical(this, other) || other is HistoricalDataRequest &&
    other.symbols == symbols &&
    other.from == from &&
    other.to == to &&
    other.interval == interval &&
    other.continuous == continuous &&
    other.instrumentType == instrumentType &&
    other.forceRefresh == forceRefresh &&
    other.filterType == filterType &&
    other.filterFrequency == filterFrequency &&
    _deepEquality.equals(other.additionalParams, additionalParams) &&
    other.isIndexSymbol == isIndexSymbol;

  @override
  int get hashCode =>
    (symbols == null ? 0 : symbols!.hashCode) +
    (from == null ? 0 : from!.hashCode) +
    (to == null ? 0 : to!.hashCode) +
    (interval == null ? 0 : interval!.hashCode) +
    (continuous == null ? 0 : continuous!.hashCode) +
    (instrumentType == null ? 0 : instrumentType!.hashCode) +
    (forceRefresh == null ? 0 : forceRefresh!.hashCode) +
    (filterType == null ? 0 : filterType!.hashCode) +
    (filterFrequency == null ? 0 : filterFrequency!.hashCode) +
    (additionalParams.hashCode) +
    (isIndexSymbol == null ? 0 : isIndexSymbol!.hashCode);

  @override
  String toString() => 'HistoricalDataRequest[symbols=$symbols, from=$from, to=$to, interval=$interval, continuous=$continuous, instrumentType=$instrumentType, forceRefresh=$forceRefresh, filterType=$filterType, filterFrequency=$filterFrequency, additionalParams=$additionalParams, isIndexSymbol=$isIndexSymbol]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.symbols != null) {
      json[r'symbols'] = this.symbols;
    } else {
      json[r'symbols'] = null;
    }
    if (this.from != null) {
      json[r'from'] = this.from;
    } else {
      json[r'from'] = null;
    }
    if (this.to != null) {
      json[r'to'] = this.to;
    } else {
      json[r'to'] = null;
    }
    if (this.interval != null) {
      json[r'interval'] = this.interval;
    } else {
      json[r'interval'] = null;
    }
    if (this.continuous != null) {
      json[r'continuous'] = this.continuous;
    } else {
      json[r'continuous'] = null;
    }
    if (this.instrumentType != null) {
      json[r'instrumentType'] = this.instrumentType;
    } else {
      json[r'instrumentType'] = null;
    }
    if (this.forceRefresh != null) {
      json[r'forceRefresh'] = this.forceRefresh;
    } else {
      json[r'forceRefresh'] = null;
    }
    if (this.filterType != null) {
      json[r'filterType'] = this.filterType;
    } else {
      json[r'filterType'] = null;
    }
    if (this.filterFrequency != null) {
      json[r'filterFrequency'] = this.filterFrequency;
    } else {
      json[r'filterFrequency'] = null;
    }
      json[r'additionalParams'] = this.additionalParams;
    if (this.isIndexSymbol != null) {
      json[r'isIndexSymbol'] = this.isIndexSymbol;
    } else {
      json[r'isIndexSymbol'] = null;
    }
    return json;
  }

  /// Returns a new [HistoricalDataRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static HistoricalDataRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "HistoricalDataRequest[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "HistoricalDataRequest[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return HistoricalDataRequest(
        symbols: mapValueOfType<String>(json, r'symbols'),
        from: mapValueOfType<String>(json, r'from'),
        to: mapValueOfType<String>(json, r'to'),
        interval: HistoricalDataRequestIntervalEnum.fromJson(json[r'interval']),
        continuous: mapValueOfType<bool>(json, r'continuous'),
        instrumentType: mapValueOfType<String>(json, r'instrumentType'),
        forceRefresh: mapValueOfType<bool>(json, r'forceRefresh'),
        filterType: mapValueOfType<String>(json, r'filterType'),
        filterFrequency: mapValueOfType<int>(json, r'filterFrequency'),
        additionalParams: mapCastOfType<String, Object>(json, r'additionalParams') ?? const {},
        isIndexSymbol: mapValueOfType<bool>(json, r'isIndexSymbol'),
      );
    }
    return null;
  }

  static List<HistoricalDataRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <HistoricalDataRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = HistoricalDataRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, HistoricalDataRequest> mapFromJson(dynamic json) {
    final map = <String, HistoricalDataRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = HistoricalDataRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of HistoricalDataRequest-objects as value to a dart map
  static Map<String, List<HistoricalDataRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<HistoricalDataRequest>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = HistoricalDataRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}


class HistoricalDataRequestIntervalEnum {
  /// Instantiate a new enum with the provided [value].
  const HistoricalDataRequestIntervalEnum._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const MINUTE = HistoricalDataRequestIntervalEnum._(r'MINUTE');
  static const THREE_MINUTE = HistoricalDataRequestIntervalEnum._(r'THREE_MINUTE');
  static const FIVE_MINUTE = HistoricalDataRequestIntervalEnum._(r'FIVE_MINUTE');
  static const TEN_MINUTE = HistoricalDataRequestIntervalEnum._(r'TEN_MINUTE');
  static const FIFTEEN_MINUTE = HistoricalDataRequestIntervalEnum._(r'FIFTEEN_MINUTE');
  static const THIRTY_MINUTE = HistoricalDataRequestIntervalEnum._(r'THIRTY_MINUTE');
  static const HOUR = HistoricalDataRequestIntervalEnum._(r'HOUR');
  static const FOUR_HOUR = HistoricalDataRequestIntervalEnum._(r'FOUR_HOUR');
  static const DAY = HistoricalDataRequestIntervalEnum._(r'DAY');
  static const WEEK = HistoricalDataRequestIntervalEnum._(r'WEEK');
  static const MONTH = HistoricalDataRequestIntervalEnum._(r'MONTH');
  static const YEAR = HistoricalDataRequestIntervalEnum._(r'YEAR');

  /// List of all possible values in this [enum][HistoricalDataRequestIntervalEnum].
  static const values = <HistoricalDataRequestIntervalEnum>[
    MINUTE,
    THREE_MINUTE,
    FIVE_MINUTE,
    TEN_MINUTE,
    FIFTEEN_MINUTE,
    THIRTY_MINUTE,
    HOUR,
    FOUR_HOUR,
    DAY,
    WEEK,
    MONTH,
    YEAR,
  ];

  static HistoricalDataRequestIntervalEnum? fromJson(dynamic value) => HistoricalDataRequestIntervalEnumTypeTransformer().decode(value);

  static List<HistoricalDataRequestIntervalEnum> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <HistoricalDataRequestIntervalEnum>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = HistoricalDataRequestIntervalEnum.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [HistoricalDataRequestIntervalEnum] to String,
/// and [decode] dynamic data back to [HistoricalDataRequestIntervalEnum].
class HistoricalDataRequestIntervalEnumTypeTransformer {
  factory HistoricalDataRequestIntervalEnumTypeTransformer() => _instance ??= const HistoricalDataRequestIntervalEnumTypeTransformer._();

  const HistoricalDataRequestIntervalEnumTypeTransformer._();

  String encode(HistoricalDataRequestIntervalEnum data) => data.value;

  /// Decodes a [dynamic value][data] to a HistoricalDataRequestIntervalEnum.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  HistoricalDataRequestIntervalEnum? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data as Object?) {
        case r'MINUTE': return HistoricalDataRequestIntervalEnum.MINUTE;
        case r'THREE_MINUTE': return HistoricalDataRequestIntervalEnum.THREE_MINUTE;
        case r'FIVE_MINUTE': return HistoricalDataRequestIntervalEnum.FIVE_MINUTE;
        case r'TEN_MINUTE': return HistoricalDataRequestIntervalEnum.TEN_MINUTE;
        case r'FIFTEEN_MINUTE': return HistoricalDataRequestIntervalEnum.FIFTEEN_MINUTE;
        case r'THIRTY_MINUTE': return HistoricalDataRequestIntervalEnum.THIRTY_MINUTE;
        case r'HOUR': return HistoricalDataRequestIntervalEnum.HOUR;
        case r'FOUR_HOUR': return HistoricalDataRequestIntervalEnum.FOUR_HOUR;
        case r'DAY': return HistoricalDataRequestIntervalEnum.DAY;
        case r'WEEK': return HistoricalDataRequestIntervalEnum.WEEK;
        case r'MONTH': return HistoricalDataRequestIntervalEnum.MONTH;
        case r'YEAR': return HistoricalDataRequestIntervalEnum.YEAR;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [HistoricalDataRequestIntervalEnumTypeTransformer] instance.
  static HistoricalDataRequestIntervalEnumTypeTransformer? _instance;
}


