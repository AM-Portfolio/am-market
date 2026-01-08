//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of parser.api;

/// Job processing status
class JobStatus {
  /// Instantiate a new enum with the provided [value].
  const JobStatus._(this.value);

  /// The underlying value of this enum member.
  final String value;

  @override
  String toString() => value;

  String toJson() => value;

  static const pending = JobStatus._(r'pending');
  static const running = JobStatus._(r'running');
  static const completed = JobStatus._(r'completed');
  static const failed = JobStatus._(r'failed');
  static const cancelled = JobStatus._(r'cancelled');

  /// List of all possible values in this [enum][JobStatus].
  static const values = <JobStatus>[
    pending,
    running,
    completed,
    failed,
    cancelled,
  ];

  static JobStatus? fromJson(dynamic value) => JobStatusTypeTransformer().decode(value);

  static List<JobStatus> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <JobStatus>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = JobStatus.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }
}

/// Transformation class that can [encode] an instance of [JobStatus] to String,
/// and [decode] dynamic data back to [JobStatus].
class JobStatusTypeTransformer {
  factory JobStatusTypeTransformer() => _instance ??= const JobStatusTypeTransformer._();

  const JobStatusTypeTransformer._();

  String encode(JobStatus data) => data.value;

  /// Decodes a [dynamic value][data] to a JobStatus.
  ///
  /// If [allowNull] is true and the [dynamic value][data] cannot be decoded successfully,
  /// then null is returned. However, if [allowNull] is false and the [dynamic value][data]
  /// cannot be decoded successfully, then an [UnimplementedError] is thrown.
  ///
  /// The [allowNull] is very handy when an API changes and a new enum value is added or removed,
  /// and users are still using an old app with the old code.
  JobStatus? decode(dynamic data, {bool allowNull = true}) {
    if (data != null) {
      switch (data) {
        case r'pending': return JobStatus.pending;
        case r'running': return JobStatus.running;
        case r'completed': return JobStatus.completed;
        case r'failed': return JobStatus.failed;
        case r'cancelled': return JobStatus.cancelled;
        default:
          if (!allowNull) {
            throw ArgumentError('Unknown enum value to decode: $data');
          }
      }
    }
    return null;
  }

  /// Singleton [JobStatusTypeTransformer] instance.
  static JobStatusTypeTransformer? _instance;
}

