//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class JobStatusResponse {
  /// Returns a new [JobStatusResponse] instance.
  JobStatusResponse({
    required this.jobId,
    required this.status,
    required this.progress,
    this.result,
    this.errorMessage,
    required this.createdAt,
    this.startedAt,
    this.completedAt,
    this.estimatedRemainingTime,
  });

  String jobId;

  JobStatus status;

  JobProgress progress;

  Object? result;

  String? errorMessage;

  DateTime createdAt;

  DateTime? startedAt;

  DateTime? completedAt;

  String? estimatedRemainingTime;

  @override
  bool operator ==(Object other) => identical(this, other) || other is JobStatusResponse &&
    other.jobId == jobId &&
    other.status == status &&
    other.progress == progress &&
    other.result == result &&
    other.errorMessage == errorMessage &&
    other.createdAt == createdAt &&
    other.startedAt == startedAt &&
    other.completedAt == completedAt &&
    other.estimatedRemainingTime == estimatedRemainingTime;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (jobId.hashCode) +
    (status.hashCode) +
    (progress.hashCode) +
    (result == null ? 0 : result!.hashCode) +
    (errorMessage == null ? 0 : errorMessage!.hashCode) +
    (createdAt.hashCode) +
    (startedAt == null ? 0 : startedAt!.hashCode) +
    (completedAt == null ? 0 : completedAt!.hashCode) +
    (estimatedRemainingTime == null ? 0 : estimatedRemainingTime!.hashCode);

  @override
  String toString() => 'JobStatusResponse[jobId=$jobId, status=$status, progress=$progress, result=$result, errorMessage=$errorMessage, createdAt=$createdAt, startedAt=$startedAt, completedAt=$completedAt, estimatedRemainingTime=$estimatedRemainingTime]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'job_id'] = this.jobId;
      json[r'status'] = this.status;
      json[r'progress'] = this.progress;
    if (this.result != null) {
      json[r'result'] = this.result;
    } else {
      json[r'result'] = null;
    }
    if (this.errorMessage != null) {
      json[r'error_message'] = this.errorMessage;
    } else {
      json[r'error_message'] = null;
    }
      json[r'created_at'] = this.createdAt.toUtc().toIso8601String();
    if (this.startedAt != null) {
      json[r'started_at'] = this.startedAt!.toUtc().toIso8601String();
    } else {
      json[r'started_at'] = null;
    }
    if (this.completedAt != null) {
      json[r'completed_at'] = this.completedAt!.toUtc().toIso8601String();
    } else {
      json[r'completed_at'] = null;
    }
    if (this.estimatedRemainingTime != null) {
      json[r'estimated_remaining_time'] = this.estimatedRemainingTime;
    } else {
      json[r'estimated_remaining_time'] = null;
    }
    return json;
  }

  /// Returns a new [JobStatusResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static JobStatusResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "JobStatusResponse[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "JobStatusResponse[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return JobStatusResponse(
        jobId: mapValueOfType<String>(json, r'job_id')!,
        status: JobStatus.fromJson(json[r'status'])!,
        progress: JobProgress.fromJson(json[r'progress'])!,
        result: mapValueOfType<Object>(json, r'result'),
        errorMessage: mapValueOfType<String>(json, r'error_message'),
        createdAt: mapDateTime(json, r'created_at', r'')!,
        startedAt: mapDateTime(json, r'started_at', r''),
        completedAt: mapDateTime(json, r'completed_at', r''),
        estimatedRemainingTime: mapValueOfType<String>(json, r'estimated_remaining_time'),
      );
    }
    return null;
  }

  static List<JobStatusResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <JobStatusResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = JobStatusResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, JobStatusResponse> mapFromJson(dynamic json) {
    final map = <String, JobStatusResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = JobStatusResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of JobStatusResponse-objects as value to a dart map
  static Map<String, List<JobStatusResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<JobStatusResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = JobStatusResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'job_id',
    'status',
    'progress',
    'created_at',
  };
}

