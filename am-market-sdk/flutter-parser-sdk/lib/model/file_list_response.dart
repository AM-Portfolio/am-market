// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class FileListResponse {
  /// Returns a new [FileListResponse] instance.
  FileListResponse({
    this.files = const [],
    required this.totalCount,
  });

  List<FileUpload> files;

  int totalCount;

  @override
  bool operator ==(Object other) => identical(this, other) || other is FileListResponse &&
    _deepEquality.equals(other.files, files) &&
    other.totalCount == totalCount;

  @override
  int get hashCode =>
    (files.hashCode) +
    (totalCount.hashCode);

  @override
  String toString() => 'FileListResponse[files=$files, totalCount=$totalCount]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'files'] = this.files;
      json[r'total_count'] = this.totalCount;
    return json;
  }

  /// Returns a new [FileListResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static FileListResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "FileListResponse[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "FileListResponse[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return FileListResponse(
        files: FileUpload.listFromJson(json[r'files']),
        totalCount: mapValueOfType<int>(json, r'total_count')!,
      );
    }
    return null;
  }

  static List<FileListResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <FileListResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = FileListResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, FileListResponse> mapFromJson(dynamic json) {
    final map = <String, FileListResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = FileListResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of FileListResponse-objects as value to a dart map
  static Map<String, List<FileListResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<FileListResponse>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = FileListResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'files',
    'total_count',
  };
}

