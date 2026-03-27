// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class IngestionJobLog {
  /// Returns a new [IngestionJobLog] instance.
  IngestionJobLog({
    this.id,
    this.jobId,
    this.startTime,
    this.endTime,
    this.status,
    this.totalSymbols,
    this.successCount,
    this.failureCount,
    this.failedSymbols = const [],
    this.durationMs,
    this.payloadSize,
    this.message,
    this.logs = const [],
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
  String? jobId;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? startTime;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  DateTime? endTime;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? status;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? totalSymbols;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? successCount;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? failureCount;

  List<String> failedSymbols;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? durationMs;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  int? payloadSize;

  ///
  /// Please note: This property should have been non-nullable! Since the specification file
  /// does not include a default value (using the "default:" property), however, the generated
  /// source code must fall back to having a nullable type.
  /// Consider adding a "default:" property in the specification file to hide this note.
  ///
  String? message;

  List<String> logs;

  @override
  bool operator ==(Object other) => identical(this, other) || other is IngestionJobLog &&
    other.id == id &&
    other.jobId == jobId &&
    other.startTime == startTime &&
    other.endTime == endTime &&
    other.status == status &&
    other.totalSymbols == totalSymbols &&
    other.successCount == successCount &&
    other.failureCount == failureCount &&
    _deepEquality.equals(other.failedSymbols, failedSymbols) &&
    other.durationMs == durationMs &&
    other.payloadSize == payloadSize &&
    other.message == message &&
    _deepEquality.equals(other.logs, logs);

  @override
  int get hashCode =>
    (id == null ? 0 : id!.hashCode) +
    (jobId == null ? 0 : jobId!.hashCode) +
    (startTime == null ? 0 : startTime!.hashCode) +
    (endTime == null ? 0 : endTime!.hashCode) +
    (status == null ? 0 : status!.hashCode) +
    (totalSymbols == null ? 0 : totalSymbols!.hashCode) +
    (successCount == null ? 0 : successCount!.hashCode) +
    (failureCount == null ? 0 : failureCount!.hashCode) +
    (failedSymbols.hashCode) +
    (durationMs == null ? 0 : durationMs!.hashCode) +
    (payloadSize == null ? 0 : payloadSize!.hashCode) +
    (message == null ? 0 : message!.hashCode) +
    (logs.hashCode);

  @override
  String toString() => 'IngestionJobLog[id=$id, jobId=$jobId, startTime=$startTime, endTime=$endTime, status=$status, totalSymbols=$totalSymbols, successCount=$successCount, failureCount=$failureCount, failedSymbols=$failedSymbols, durationMs=$durationMs, payloadSize=$payloadSize, message=$message, logs=$logs]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
    if (this.id != null) {
      json[r'id'] = this.id;
    } else {
      json[r'id'] = null;
    }
    if (this.jobId != null) {
      json[r'jobId'] = this.jobId;
    } else {
      json[r'jobId'] = null;
    }
    if (this.startTime != null) {
      json[r'startTime'] = this.startTime!.toUtc().toIso8601String();
    } else {
      json[r'startTime'] = null;
    }
    if (this.endTime != null) {
      json[r'endTime'] = this.endTime!.toUtc().toIso8601String();
    } else {
      json[r'endTime'] = null;
    }
    if (this.status != null) {
      json[r'status'] = this.status;
    } else {
      json[r'status'] = null;
    }
    if (this.totalSymbols != null) {
      json[r'totalSymbols'] = this.totalSymbols;
    } else {
      json[r'totalSymbols'] = null;
    }
    if (this.successCount != null) {
      json[r'successCount'] = this.successCount;
    } else {
      json[r'successCount'] = null;
    }
    if (this.failureCount != null) {
      json[r'failureCount'] = this.failureCount;
    } else {
      json[r'failureCount'] = null;
    }
      json[r'failedSymbols'] = this.failedSymbols;
    if (this.durationMs != null) {
      json[r'durationMs'] = this.durationMs;
    } else {
      json[r'durationMs'] = null;
    }
    if (this.payloadSize != null) {
      json[r'payloadSize'] = this.payloadSize;
    } else {
      json[r'payloadSize'] = null;
    }
    if (this.message != null) {
      json[r'message'] = this.message;
    } else {
      json[r'message'] = null;
    }
      json[r'logs'] = this.logs;
    return json;
  }

  /// Returns a new [IngestionJobLog] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static IngestionJobLog? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "IngestionJobLog[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "IngestionJobLog[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return IngestionJobLog(
        id: mapValueOfType<String>(json, r'id'),
        jobId: mapValueOfType<String>(json, r'jobId'),
        startTime: mapDateTime(json, r'startTime', r''),
        endTime: mapDateTime(json, r'endTime', r''),
        status: mapValueOfType<String>(json, r'status'),
        totalSymbols: mapValueOfType<int>(json, r'totalSymbols'),
        successCount: mapValueOfType<int>(json, r'successCount'),
        failureCount: mapValueOfType<int>(json, r'failureCount'),
        failedSymbols: json[r'failedSymbols'] is Iterable
            ? (json[r'failedSymbols'] as Iterable).cast<String>().toList(growable: false)
            : const [],
        durationMs: mapValueOfType<int>(json, r'durationMs'),
        payloadSize: mapValueOfType<int>(json, r'payloadSize'),
        message: mapValueOfType<String>(json, r'message'),
        logs: json[r'logs'] is Iterable
            ? (json[r'logs'] as Iterable).cast<String>().toList(growable: false)
            : const [],
      );
    }
    return null;
  }

  static List<IngestionJobLog> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <IngestionJobLog>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = IngestionJobLog.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, IngestionJobLog> mapFromJson(dynamic json) {
    final map = <String, IngestionJobLog>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = IngestionJobLog.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of IngestionJobLog-objects as value to a dart map
  static Map<String, List<IngestionJobLog>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<IngestionJobLog>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = IngestionJobLog.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

