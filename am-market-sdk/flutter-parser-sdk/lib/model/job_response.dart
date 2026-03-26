//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;

class JobResponse {
  /// Returns a new [JobResponse] instance.
  JobResponse({
    required this.jobId,
    required this.status,
    required this.message,
    this.estimatedCompletionTime,
    required this.statusUrl,
    this.webhookUrl,
  });

  String jobId;

  JobStatus status;

  String message;

  String? estimatedCompletionTime;

  String statusUrl;

  String? webhookUrl;

  @override
  bool operator ==(Object other) => identical(this, other) || other is JobResponse &&
    other.jobId == jobId &&
    other.status == status &&
    other.message == message &&
    other.estimatedCompletionTime == estimatedCompletionTime &&
    other.statusUrl == statusUrl &&
    other.webhookUrl == webhookUrl;

  @override
  int get hashCode =>
    // ignore: unnecessary_parenthesis
    (jobId.hashCode) +
    (status.hashCode) +
    (message.hashCode) +
    (estimatedCompletionTime == null ? 0 : estimatedCompletionTime!.hashCode) +
    (statusUrl.hashCode) +
    (webhookUrl == null ? 0 : webhookUrl!.hashCode);

  @override
  String toString() => 'JobResponse[jobId=$jobId, status=$status, message=$message, estimatedCompletionTime=$estimatedCompletionTime, statusUrl=$statusUrl, webhookUrl=$webhookUrl]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'job_id'] = this.jobId;
      json[r'status'] = this.status;
      json[r'message'] = this.message;
    if (this.estimatedCompletionTime != null) {
      json[r'estimated_completion_time'] = this.estimatedCompletionTime;
    } else {
      json[r'estimated_completion_time'] = null;
    }
      json[r'status_url'] = this.statusUrl;
    if (this.webhookUrl != null) {
      json[r'webhook_url'] = this.webhookUrl;
    } else {
      json[r'webhook_url'] = null;
    }
    return json;
  }

  /// Returns a new [JobResponse] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  // ignore: prefer_constructors_over_static_methods
  static JobResponse? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "JobResponse[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "JobResponse[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return JobResponse(
        jobId: mapValueOfType<String>(json, r'job_id')!,
        status: JobStatus.fromJson(json[r'status'])!,
        message: mapValueOfType<String>(json, r'message')!,
        estimatedCompletionTime: mapValueOfType<String>(json, r'estimated_completion_time'),
        statusUrl: mapValueOfType<String>(json, r'status_url')!,
        webhookUrl: mapValueOfType<String>(json, r'webhook_url'),
      );
    }
    return null;
  }

  static List<JobResponse> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <JobResponse>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = JobResponse.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, JobResponse> mapFromJson(dynamic json) {
    final map = <String, JobResponse>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); // ignore: parameter_assignments
      for (final entry in json.entries) {
        final value = JobResponse.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of JobResponse-objects as value to a dart map
  static Map<String, List<JobResponse>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<JobResponse>>{};
    if (json is Map && json.isNotEmpty) {
      // ignore: parameter_assignments
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = JobResponse.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
    'job_id',
    'status',
    'message',
    'status_url',
  };
}

