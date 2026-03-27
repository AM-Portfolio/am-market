// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class StreamConnectRequest {
  /// Returns a new [StreamConnectRequest] instance.
  StreamConnectRequest({
    this.instrumentKeys = const [],
    this.mode,
    this.expandIndices,
    this.timeFrame,
    this.isIndexSymbol,
    this.stream,
    this.provider,
  });

  List<String> instrumentKeys;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? mode;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? expandIndices;

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
  bool? isIndexSymbol;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? stream;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? provider;

  @override
  bool operator ==(Object other) => identical(this, other) || other is StreamConnectRequest &&
    _deepEquality.equals(other.instrumentKeys, instrumentKeys) &&
    other.mode == mode &&
    other.expandIndices == expandIndices &&
    other.timeFrame == timeFrame &&
    other.isIndexSymbol == isIndexSymbol &&
    other.stream == stream &&
    other.provider == provider;

  @override
  int get hashCode =>
    (instrumentKeys.hashCode) +
    (mode == null ? 0 : mode!.hashCode) +
    (expandIndices == null ? 0 : expandIndices!.hashCode) +
    (timeFrame == null ? 0 : timeFrame!.hashCode) +
    (isIndexSymbol == null ? 0 : isIndexSymbol!.hashCode) +
    (stream == null ? 0 : stream!.hashCode) +
    (provider == null ? 0 : provider!.hashCode);

  @override
  String toString() => 'StreamConnectRequest[instrumentKeys=$instrumentKeys, mode=$mode, expandIndices=$expandIndices, timeFrame=$timeFrame, isIndexSymbol=$isIndexSymbol, stream=$stream, provider=$provider]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'instrumentKeys'] = this.instrumentKeys;
    if (this.mode != null) {
      json[r'mode'] = this.mode;
    } else {
      json[r'mode'] = null;
    }
    if (this.expandIndices != null) {
      json[r'expandIndices'] = this.expandIndices;
    } else {
      json[r'expandIndices'] = null;
    }
    if (this.timeFrame != null) {
      json[r'timeFrame'] = this.timeFrame;
    } else {
      json[r'timeFrame'] = null;
    }
    if (this.isIndexSymbol != null) {
      json[r'isIndexSymbol'] = this.isIndexSymbol;
    } else {
      json[r'isIndexSymbol'] = null;
    }
    if (this.stream != null) {
      json[r'stream'] = this.stream;
    } else {
      json[r'stream'] = null;
    }
    if (this.provider != null) {
      json[r'provider'] = this.provider;
    } else {
      json[r'provider'] = null;
    }
    return json;
  }

  /// Returns a new [StreamConnectRequest] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static StreamConnectRequest? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "StreamConnectRequest[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "StreamConnectRequest[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return StreamConnectRequest(
        instrumentKeys: json[r'instrumentKeys'] is Iterable
            ? (json[r'instrumentKeys'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        mode: mapValueOfType<String>(json, r'mode'),
        expandIndices: mapValueOfType<bool>(json, r'expandIndices'),
        timeFrame: mapValueOfType<String>(json, r'timeFrame'),
        isIndexSymbol: mapValueOfType<bool>(json, r'isIndexSymbol'),
        stream: mapValueOfType<bool>(json, r'stream'),
        provider: mapValueOfType<String>(json, r'provider'),
      );
    }
    return null;
  }

  static List<StreamConnectRequest> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <StreamConnectRequest>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = StreamConnectRequest.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, StreamConnectRequest> mapFromJson(dynamic json) {
    final map = <String, StreamConnectRequest>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = StreamConnectRequest.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of StreamConnectRequest-objects as value to a dart map
  static Map<String, List<StreamConnectRequest>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<StreamConnectRequest>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = StreamConnectRequest.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

