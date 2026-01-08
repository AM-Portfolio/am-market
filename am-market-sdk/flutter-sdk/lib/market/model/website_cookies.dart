//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of market.api;

class WebsiteCookies {
  /// Returns a new [WebsiteCookies] instance.
  WebsiteCookies({
    this.websiteUrl,
    this.websiteName,
    this.cookies = const [],
    this.cookiesString,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? websiteUrl;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? websiteName;

  List<CookieInfo> cookies;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? cookiesString;

  @override
  bool operator ==(Object other) => identical(this, other) || other is WebsiteCookies &&
    other.websiteUrl == websiteUrl &&
    other.websiteName == websiteName &&
    _deepEquality.equals(other.cookies, cookies) &&
    other.cookiesString == cookiesString;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (websiteUrl == null ? 0 : websiteUrl!.hashCode) +
    (websiteName == null ? 0 : websiteName!.hashCode) +
    (cookies.hashCode) +
    (cookiesString == null ? 0 : cookiesString!.hashCode);

  @override
  String toString() => 'WebsiteCookies[websiteUrl=$websiteUrl, websiteName=$websiteName, cookies=$cookies, cookiesString=$cookiesString]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.websiteUrl != null) {
      json[r'websiteUrl'] = this.websiteUrl;
    } else {
      json[r'websiteUrl'] = null;
    }
    if (this.websiteName != null) {
      json[r'websiteName'] = this.websiteName;
    } else {
      json[r'websiteName'] = null;
    }
      json[r'cookies'] = this.cookies;
    if (this.cookiesString != null) {
      json[r'cookiesString'] = this.cookiesString;
    } else {
      json[r'cookiesString'] = null;
    }
    return json;
  }

  /// Returns a new [WebsiteCookies] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static WebsiteCookies? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "WebsiteCookies[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "WebsiteCookies[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return WebsiteCookies(
        websiteUrl: mapValueOfType<String>(json, r'websiteUrl'),
        websiteName: mapValueOfType<String>(json, r'websiteName'),
        cookies: CookieInfo.listFromJson(json[r'cookies']),
        cookiesString: mapValueOfType<String>(json, r'cookiesString'),
      );
    }
    return null;
  }

  static List<WebsiteCookies> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <WebsiteCookies>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = WebsiteCookies.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, WebsiteCookies> mapFromJson(dynamic json) {
    final map = <String, WebsiteCookies>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = WebsiteCookies.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of WebsiteCookies-objects as value to a dart map
  static Map<String, List<WebsiteCookies>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<WebsiteCookies>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = WebsiteCookies.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

