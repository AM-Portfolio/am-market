// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

/// Status of file processing
class ProcessingStatus {
  /// Instantiate a new enum with the provided [value].
  const ProcessingStatus._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const uploaded = ProcessingStatus._(r'uploaded');
  static const processing = ProcessingStatus._(r'processing');
  static const splitting = ProcessingStatus._(r'splitting');
  static const completed = ProcessingStatus._(r'completed');
  static const failed = ProcessingStatus._(r'failed');
  static const parsed = ProcessingStatus._(r'parsed');

  /// List of all possible values in this [enum][ProcessingStatus].
  static const values = <ProcessingStatus>[
    uploaded,
    processing,
    splitting,
    completed,
    failed,
    parsed,
  ];

  static ProcessingStatus? fromJson(dynamic value) => ProcessingStatusTypeTransformer().decode(value);

  static List<ProcessingStatus> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <ProcessingStatus>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = ProcessingStatus.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [ProcessingStatus] to String,
/// and [decode] dynamic data back to [ProcessingStatus].
class ProcessingStatusTypeTransformer {
  factory ProcessingStatusTypeTransformer() => _instance ??= const ProcessingStatusTypeTransformer._();

  const ProcessingStatusTypeTransformer._();

  String encode(ProcessingStatus data) => data.value;

  /// Decodes a [dynamic value][data] to a ProcessingStatus.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  ProcessingStatus? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data as Object?) {
        case r'uploaded': return ProcessingStatus.uploaded;
        case r'processing': return ProcessingStatus.processing;
        case r'splitting': return ProcessingStatus.splitting;
        case r'completed': return ProcessingStatus.completed;
        case r'failed': return ProcessingStatus.failed;
        case r'parsed': return ProcessingStatus.parsed;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [ProcessingStatusTypeTransformer] instance.
  static ProcessingStatusTypeTransformer? _instance;
}

