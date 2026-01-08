//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of parser.api;

class FileUpload {
  /// Returns a new [FileUpload] instance.
  FileUpload({
    this.id,
    required this.fileId,
    required this.originalFilename,
    required this.storedFilename,
    required this.fileType,
    required this.filePath,
    this.parentId,
    this.sheetName,
    this.status = ProcessingStatus.uploaded,
    required this.fileSize,
    this.createdAt,
    this.updatedAt,
    this.errorMessage,
    this.processingMetadata,
  });

  String? id;

  /// Unique identifier for the file
  String fileId;

  /// Original filename uploaded by user
  String originalFilename;

  /// Filename as stored in the system
  String storedFilename;

  /// Type of file (excel, sheet, csv)
  FileType fileType;

  /// Full path to stored file
  String filePath;

  String? parentId;

  String? sheetName;

  /// Current processing status
  ProcessingStatus status;

  /// File size in bytes
  int fileSize;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? createdAt;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? updatedAt;

  String? errorMessage;

  Object? processingMetadata;

  @override
  bool operator ==(Object other) => identical(this, other) || other is FileUpload &&
    other.id == id &&
    other.fileId == fileId &&
    other.originalFilename == originalFilename &&
    other.storedFilename == storedFilename &&
    other.fileType == fileType &&
    other.filePath == filePath &&
    other.parentId == parentId &&
    other.sheetName == sheetName &&
    other.status == status &&
    other.fileSize == fileSize &&
    other.createdAt == createdAt &&
    other.updatedAt == updatedAt &&
    other.errorMessage == errorMessage &&
    other.processingMetadata == processingMetadata;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (id == null ? 0 : id!.hashCode) +
    (fileId.hashCode) +
    (originalFilename.hashCode) +
    (storedFilename.hashCode) +
    (fileType.hashCode) +
    (filePath.hashCode) +
    (parentId == null ? 0 : parentId!.hashCode) +
    (sheetName == null ? 0 : sheetName!.hashCode) +
    (status.hashCode) +
    (fileSize.hashCode) +
    (createdAt == null ? 0 : createdAt!.hashCode) +
    (updatedAt == null ? 0 : updatedAt!.hashCode) +
    (errorMessage == null ? 0 : errorMessage!.hashCode) +
    (processingMetadata == null ? 0 : processingMetadata!.hashCode);

  @override
  String toString() => 'FileUpload[id=$id, fileId=$fileId, originalFilename=$originalFilename, storedFilename=$storedFilename, fileType=$fileType, filePath=$filePath, parentId=$parentId, sheetName=$sheetName, status=$status, fileSize=$fileSize, createdAt=$createdAt, updatedAt=$updatedAt, errorMessage=$errorMessage, processingMetadata=$processingMetadata]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.id != null) {
      json[r'_id'] = this.id;
    } else {
      json[r'_id'] = null;
    }
      json[r'file_id'] = this.fileId;
      json[r'original_filename'] = this.originalFilename;
      json[r'stored_filename'] = this.storedFilename;
      json[r'file_type'] = this.fileType;
      json[r'file_path'] = this.filePath;
    if (this.parentId != null) {
      json[r'parent_id'] = this.parentId;
    } else {
      json[r'parent_id'] = null;
    }
    if (this.sheetName != null) {
      json[r'sheet_name'] = this.sheetName;
    } else {
      json[r'sheet_name'] = null;
    }
      json[r'status'] = this.status;
      json[r'file_size'] = this.fileSize;
    if (this.createdAt != null) {
      json[r'created_at'] = this.createdAt!.toUtc().toIso8601String();
    } else {
      json[r'created_at'] = null;
    }
    if (this.updatedAt != null) {
      json[r'updated_at'] = this.updatedAt!.toUtc().toIso8601String();
    } else {
      json[r'updated_at'] = null;
    }
    if (this.errorMessage != null) {
      json[r'error_message'] = this.errorMessage;
    } else {
      json[r'error_message'] = null;
    }
    if (this.processingMetadata != null) {
      json[r'processing_metadata'] = this.processingMetadata;
    } else {
      json[r'processing_metadata'] = null;
    }
    return json;
  }

  /// Returns a new [FileUpload] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static FileUpload? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "FileUpload[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "FileUpload[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return FileUpload(
        id: mapValueOfType<String>(json, r'_id'),
        fileId: mapValueOfType<String>(json, r'file_id')!,
        originalFilename: mapValueOfType<String>(json, r'original_filename')!,
        storedFilename: mapValueOfType<String>(json, r'stored_filename')!,
        fileType: FileType.fromJson(json[r'file_type'])!,
        filePath: mapValueOfType<String>(json, r'file_path')!,
        parentId: mapValueOfType<String>(json, r'parent_id'),
        sheetName: mapValueOfType<String>(json, r'sheet_name'),
        status: ProcessingStatus.fromJson(json[r'status']) ?? ProcessingStatus.uploaded,
        fileSize: mapValueOfType<int>(json, r'file_size')!,
        createdAt: mapDateTime(json, r'created_at', r''),
        updatedAt: mapDateTime(json, r'updated_at', r''),
        errorMessage: mapValueOfType<String>(json, r'error_message'),
        processingMetadata: mapValueOfType<Object>(json, r'processing_metadata'),
      );
    }
    return null;
  }

  static List<FileUpload> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <FileUpload>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = FileUpload.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, FileUpload> mapFromJson(dynamic json) {
    final map = <String, FileUpload>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = FileUpload.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of FileUpload-objects as value to a dart map
  static Map<String, List<FileUpload>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<FileUpload>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = FileUpload.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'file_id',
    'original_filename',
    'stored_filename',
    'file_type',
    'file_path',
    'file_size',
  };
}

