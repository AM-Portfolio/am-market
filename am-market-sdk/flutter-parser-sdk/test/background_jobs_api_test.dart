//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

import 'package:am_parser_client/api.dart';
import 'package:test/test.dart';


/// tests for BackgroundJobsApi
void main() {
  // final instance = BackgroundJobsApi();

  group('tests for BackgroundJobsApi', () {
    // Cancel Job
    //
    // Cancel a pending or running job
    //
    //Future<Object> cancelJobV1JobIdDelete(String jobId) async
    test('test cancelJobV1JobIdDelete', () async {
      // TODO
    });

    // Fix Stuck Job
    //
    // Admin endpoint to fix stuck jobs Used when jobs get stuck due to server restarts
    //
    //Future<Object> fixStuckJobV1AdminFixStuckJobJobIdPost(String jobId, { bool markAsFailed }) async
    test('test fixStuckJobV1AdminFixStuckJobJobIdPost', () async {
      // TODO
    });

    // Get Job Result
    //
    // Get the result of a completed job
    //
    //Future<Object> getJobResultV1JobIdResultGet(String jobId) async
    test('test getJobResultV1JobIdResultGet', () async {
      // TODO
    });

    // Get Job Status
    //
    // Get the current status of a background job
    //
    //Future<JobStatusResponse> getJobStatusV1JobIdStatusGet(String jobId) async
    test('test getJobStatusV1JobIdStatusGet', () async {
      // TODO
    });

    // List Jobs
    //
    // List background jobs with optional filtering
    //
    //Future<Object> listJobsV1Get({ JobStatus jobStatus, String userId, int limit }) async
    test('test listJobsV1Get', () async {
      // TODO
    });

    // Recover All Stuck Jobs
    //
    // Admin endpoint to recover all stuck jobs Useful after server restarts
    //
    //Future<Object> recoverAllStuckJobsV1AdminRecoverStuckJobsPost() async
    test('test recoverAllStuckJobsV1AdminRecoverStuckJobsPost', () async {
      // TODO
    });

    // Upload Excel Async
    //
    // Upload Excel file for async background processing Returns immediately with job ID, processes in background
    //
    //Future<JobResponse> uploadExcelAsyncV1UploadExcelAsyncPost(MultipartFile file, { String parseMethod, String callbackUrl, String userId }) async
    test('test uploadExcelAsyncV1UploadExcelAsyncPost', () async {
      // TODO
    });

  });
}
