//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

/// Type of uploaded file
class FileType {
  /// Instantiate a new enum with the provided [value].
  const FileType._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const excel = FileType._(r'excel');
  static const sheet = FileType._(r'sheet');
  static const csv = FileType._(r'csv');

  /// List of all possible values in this [enum][FileType].
  static const values = <FileType>[
    excel,
    sheet,
    csv,
  ];

  static FileType? fromJson(dynamic value) => FileTypeTypeTransformer().decode(value);

  static List<FileType> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <FileType>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = FileType.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [FileType] to String,
/// and [decode] dynamic data back to [FileType].
class FileTypeTypeTransformer {
  factory FileTypeTypeTransformer() => _instance ??= const FileTypeTypeTransformer._();

  const FileTypeTypeTransformer._();

  String encode(FileType data) => data.value;

  /// Decodes a [dynamic value][data] to a FileType.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  FileType? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'excel': return FileType.excel;
        case r'sheet': return FileType.sheet;
        case r'csv': return FileType.csv;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [FileTypeTypeTransformer] instance.
  static FileTypeTypeTransformer? _instance;
}

