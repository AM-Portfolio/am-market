// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class SecurityDocument {
  /// Returns a new [SecurityDocument] instance.
  SecurityDocument({
    this.id,
    this.key,
    this.metadata,
    this.audit,
  });

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? id;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  SecurityKey? key;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  SecurityMetadata? metadata;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  Audit? audit;

  @override
  bool operator ==(Object other) => identical(this, other) || other is SecurityDocument &&
    other.id == id &&
    other.key == key &&
    other.metadata == metadata &&
    other.audit == audit;

  @override
  int get hashCode =>
    (id == null ? 0 : id!.hashCode) +
    (key == null ? 0 : key!.hashCode) +
    (metadata == null ? 0 : metadata!.hashCode) +
    (audit == null ? 0 : audit!.hashCode);

  @override
  String toString() => 'SecurityDocument[id=$id, key=$key, metadata=$metadata, audit=$audit]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.id != null) {
      json[r'id'] = this.id;
    } else {
      json[r'id'] = null;
    }
    if (this.key != null) {
      json[r'key'] = this.key;
    } else {
      json[r'key'] = null;
    }
    if (this.metadata != null) {
      json[r'metadata'] = this.metadata;
    } else {
      json[r'metadata'] = null;
    }
    if (this.audit != null) {
      json[r'audit'] = this.audit;
    } else {
      json[r'audit'] = null;
    }
    return json;
  }

  /// Returns a new [SecurityDocument] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static SecurityDocument? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "SecurityDocument[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "SecurityDocument[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return SecurityDocument(
        id: mapValueOfType<String>(json, r'id'),
        key: SecurityKey.fromJson(json[r'key']),
        metadata: SecurityMetadata.fromJson(json[r'metadata']),
        audit: Audit.fromJson(json[r'audit']),
      );
    }
    return null;
  }

  static List<SecurityDocument> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <SecurityDocument>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = SecurityDocument.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, SecurityDocument> mapFromJson(dynamic json) {
    final map = <String, SecurityDocument>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = SecurityDocument.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of SecurityDocument-objects as value to a dart map
  static Map<String, List<SecurityDocument>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<SecurityDocument>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = SecurityDocument.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

