// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class CookieInfo {
  /// Returns a new [CookieInfo] instance.
  CookieInfo({
    this.name,
    this.value,
    this.domain,
    this.path,
    this.secure,
    this.httpOnly,
    this.sameSite,
    this.expiry,
  });

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
  String? value;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? domain;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? path;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? secure;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  bool? httpOnly;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? sameSite;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? expiry;

  @override
  bool operator ==(Object other) => identical(this, other) || other is CookieInfo &&
    other.name == name &&
    other.value == value &&
    other.domain == domain &&
    other.path == path &&
    other.secure == secure &&
    other.httpOnly == httpOnly &&
    other.sameSite == sameSite &&
    other.expiry == expiry;

  @override
  int get hashCode =>
    (name == null ? 0 : name!.hashCode) +
    (value == null ? 0 : value!.hashCode) +
    (domain == null ? 0 : domain!.hashCode) +
    (path == null ? 0 : path!.hashCode) +
    (secure == null ? 0 : secure!.hashCode) +
    (httpOnly == null ? 0 : httpOnly!.hashCode) +
    (sameSite == null ? 0 : sameSite!.hashCode) +
    (expiry == null ? 0 : expiry!.hashCode);

  @override
  String toString() => 'CookieInfo[name=$name, value=$value, domain=$domain, path=$path, secure=$secure, httpOnly=$httpOnly, sameSite=$sameSite, expiry=$expiry]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.name != null) {
      json[r'name'] = this.name;
    } else {
      json[r'name'] = null;
    }
    if (this.value != null) {
      json[r'value'] = this.value;
    } else {
      json[r'value'] = null;
    }
    if (this.domain != null) {
      json[r'domain'] = this.domain;
    } else {
      json[r'domain'] = null;
    }
    if (this.path != null) {
      json[r'path'] = this.path;
    } else {
      json[r'path'] = null;
    }
    if (this.secure != null) {
      json[r'secure'] = this.secure;
    } else {
      json[r'secure'] = null;
    }
    if (this.httpOnly != null) {
      json[r'httpOnly'] = this.httpOnly;
    } else {
      json[r'httpOnly'] = null;
    }
    if (this.sameSite != null) {
      json[r'sameSite'] = this.sameSite;
    } else {
      json[r'sameSite'] = null;
    }
    if (this.expiry != null) {
      json[r'expiry'] = this.expiry;
    } else {
      json[r'expiry'] = null;
    }
    return json;
  }

  /// Returns a new [CookieInfo] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static CookieInfo? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "CookieInfo[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "CookieInfo[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return CookieInfo(
        name: mapValueOfType<String>(json, r'name'),
        value: mapValueOfType<String>(json, r'value'),
        domain: mapValueOfType<String>(json, r'domain'),
        path: mapValueOfType<String>(json, r'path'),
        secure: mapValueOfType<bool>(json, r'secure'),
        httpOnly: mapValueOfType<bool>(json, r'httpOnly'),
        sameSite: mapValueOfType<String>(json, r'sameSite'),
        expiry: mapValueOfType<int>(json, r'expiry'),
      );
    }
    return null;
  }

  static List<CookieInfo> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <CookieInfo>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = CookieInfo.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, CookieInfo> mapFromJson(dynamic json) {
    final map = <String, CookieInfo>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = CookieInfo.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of CookieInfo-objects as value to a dart map
  static Map<String, List<CookieInfo>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<CookieInfo>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = CookieInfo.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

