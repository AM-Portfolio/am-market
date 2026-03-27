// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;

class JobProgress {
  /// Returns a new [JobProgress] instance.
  JobProgress({
    this.totalItems = 0,
    this.completedItems = 0,
    this.failedItems = 0,
    this.currentItem,
  });

  int totalItems;

  int completedItems;

  int failedItems;

  String? currentItem;

  @override
  bool operator ==(Object other) => identical(this, other) || other is JobProgress &&
    other.totalItems == totalItems &&
    other.completedItems == completedItems &&
    other.failedItems == failedItems &&
    other.currentItem == currentItem;

  @override
  int get hashCode =>
    (totalItems.hashCode) +
    (completedItems.hashCode) +
    (failedItems.hashCode) +
    (currentItem == null ? 0 : currentItem!.hashCode);

  @override
  String toString() => 'JobProgress[totalItems=$totalItems, completedItems=$completedItems, failedItems=$failedItems, currentItem=$currentItem]';

  Map<String, dynamic> toJson() {
    final json = <String, dynamic>{};
      json[r'total_items'] = this.totalItems;
      json[r'completed_items'] = this.completedItems;
      json[r'failed_items'] = this.failedItems;
    if (this.currentItem != null) {
      json[r'current_item'] = this.currentItem;
    } else {
      json[r'current_item'] = null;
    }
    return json;
  }

  /// Returns a new [JobProgress] instance and imports its values from
  /// [value] if it's a [Map], null otherwise.
  static JobProgress? fromJson(dynamic value) {
    if (value is Map) {
      final json = value.cast<String, dynamic>();

      // Ensure that the map contains the required keys.
      // Note 1: the values aren't checked for validity beyond being non-null.
      // Note 2: this code is stripped in release mode!
      assert(() {
        requiredKeys.forEach((key) {
          assert(json.containsKey(key), 'Required key "JobProgress[$key]" is missing from JSON.');
          assert(json[key] != null, 'Required key "JobProgress[$key]" has a null value in JSON.');
        });
        return true;
      }());

      return JobProgress(
        totalItems: mapValueOfType<int>(json, r'total_items') ?? 0,
        completedItems: mapValueOfType<int>(json, r'completed_items') ?? 0,
        failedItems: mapValueOfType<int>(json, r'failed_items') ?? 0,
        currentItem: mapValueOfType<String>(json, r'current_item'),
      );
    }
    return null;
  }

  static List<JobProgress> listFromJson(dynamic json, {bool growable = false,}) {
    final result = <JobProgress>[];
    if (json is List && json.isNotEmpty) {
      for (final row in json) {
        final value = JobProgress.fromJson(row);
        if (value != null) {
          result.add(value);
        }
      }
    }
    return result.toList(growable: growable);
  }

  static Map<String, JobProgress> mapFromJson(dynamic json) {
    final map = <String, JobProgress>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>(); 
      for (final entry in json.entries) {
        final value = JobProgress.fromJson(entry.value);
        if (value != null) {
          map[entry.key] = value;
        }
      }
    }
    return map;
  }

  // maps a json object with a list of JobProgress-objects as value to a dart map
  static Map<String, List<JobProgress>> mapListFromJson(dynamic json, {bool growable = false,}) {
    final map = <String, List<JobProgress>>{};
    if (json is Map && json.isNotEmpty) {
      json = json.cast<String, dynamic>();
      for (final entry in json.entries) {
        map[entry.key] = JobProgress.listFromJson(entry.value, growable: growable,);
      }
    }
    return map;
  }

  /// The list of required keys that must be present in a JSON.
  static const requiredKeys = <String>{
  };
}

