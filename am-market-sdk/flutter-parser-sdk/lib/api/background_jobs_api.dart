// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;


class BackgroundJobsApi {
  BackgroundJobsApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Cancel Job
  ///
  /// Cancel a pending or running job
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] jobId (required):
  Future<Response> cancelJobV1JobIdDeleteWithHttpInfo(String jobId,) async {
    final path = r'/v1/{job_id}'
      .replaceAll('{job_id}', jobId);
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'DELETE',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Cancel Job
  ///
  /// Cancel a pending or running job
  ///
  /// Parameters:
  ///
  /// * [String] jobId (required):
  Future<Object?> cancelJobV1JobIdDelete(String jobId,) async {
    final response = await cancelJobV1JobIdDeleteWithHttpInfo(jobId,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Fix Stuck Job
  ///
  /// Admin endpoint to fix stuck jobs Used when jobs get stuck due to server restarts
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] jobId (required):
  ///
  /// * [bool] markAsFailed:
  Future<Response> fixStuckJobV1AdminFixStuckJobJobIdPostWithHttpInfo(String jobId, { bool? markAsFailed, }) async {
    final path = r'/v1/admin/fix-stuck-job/{job_id}'
      .replaceAll('{job_id}', jobId);
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (markAsFailed != null) {
      queryParams.addAll(_queryParams('', 'mark_as_failed', markAsFailed));
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Fix Stuck Job
  ///
  /// Admin endpoint to fix stuck jobs Used when jobs get stuck due to server restarts
  ///
  /// Parameters:
  ///
  /// * [String] jobId (required):
  ///
  /// * [bool] markAsFailed:
  Future<Object?> fixStuckJobV1AdminFixStuckJobJobIdPost(String jobId, { bool? markAsFailed, }) async {
    final response = await fixStuckJobV1AdminFixStuckJobJobIdPostWithHttpInfo(jobId,  markAsFailed: markAsFailed, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Get Job Result
  ///
  /// Get the result of a completed job
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] jobId (required):
  Future<Response> getJobResultV1JobIdResultGetWithHttpInfo(String jobId,) async {
    final path = r'/v1/{job_id}/result'
      .replaceAll('{job_id}', jobId);
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Get Job Result
  ///
  /// Get the result of a completed job
  ///
  /// Parameters:
  ///
  /// * [String] jobId (required):
  Future<Object?> getJobResultV1JobIdResultGet(String jobId,) async {
    final response = await getJobResultV1JobIdResultGetWithHttpInfo(jobId,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Get Job Status
  ///
  /// Get the current status of a background job
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] jobId (required):
  Future<Response> getJobStatusV1JobIdStatusGetWithHttpInfo(String jobId,) async {
    final path = r'/v1/{job_id}/status'
      .replaceAll('{job_id}', jobId);
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Get Job Status
  ///
  /// Get the current status of a background job
  ///
  /// Parameters:
  ///
  /// * [String] jobId (required):
  Future<JobStatusResponse?> getJobStatusV1JobIdStatusGet(String jobId,) async {
    final response = await getJobStatusV1JobIdStatusGetWithHttpInfo(jobId,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'JobStatusResponse',) as JobStatusResponse;
    
    }
    return null;
  }

  /// List Jobs
  ///
  /// List background jobs with optional filtering
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [JobStatus] jobStatus:
  ///
  /// * [String] userId:
  ///
  /// * [int] limit:
  Future<Response> listJobsV1GetWithHttpInfo({ JobStatus? jobStatus, String? userId, int? limit, }) async {
    final path = r'/v1/';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (jobStatus != null) {
      queryParams.addAll(_queryParams('', 'job_status', jobStatus));
    }
    if (userId != null) {
      queryParams.addAll(_queryParams('', 'user_id', userId));
    }
    if (limit != null) {
      queryParams.addAll(_queryParams('', 'limit', limit));
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// List Jobs
  ///
  /// List background jobs with optional filtering
  ///
  /// Parameters:
  ///
  /// * [JobStatus] jobStatus:
  ///
  /// * [String] userId:
  ///
  /// * [int] limit:
  Future<Object?> listJobsV1Get({ JobStatus? jobStatus, String? userId, int? limit, }) async {
    final response = await listJobsV1GetWithHttpInfo( jobStatus: jobStatus, userId: userId, limit: limit, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Recover All Stuck Jobs
  ///
  /// Admin endpoint to recover all stuck jobs Useful after server restarts
  ///
  /// Note: This method returns the HTTP [Response].
  Future<Response> recoverAllStuckJobsV1AdminRecoverStuckJobsPostWithHttpInfo() async {
    final path = r'/v1/admin/recover-stuck-jobs';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Recover All Stuck Jobs
  ///
  /// Admin endpoint to recover all stuck jobs Useful after server restarts
  Future<Object?> recoverAllStuckJobsV1AdminRecoverStuckJobsPost() async {
    final response = await recoverAllStuckJobsV1AdminRecoverStuckJobsPostWithHttpInfo();
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Upload Excel Async
  ///
  /// Upload Excel file for async background processing Returns immediately with job ID, processes in background
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [MultipartFile] file (required):
  ///
  /// * [String] parseMethod:
  ///
  /// * [String] callbackUrl:
  ///
  /// * [String] userId:
  Future<Response> uploadExcelAsyncV1UploadExcelAsyncPostWithHttpInfo(MultipartFile file, { String? parseMethod, String? callbackUrl, String? userId, }) async {
    final path = r'/v1/upload-excel-async';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>['multipart/form-data'];

    bool hasFields = false;
    final mp = MultipartRequest('POST', Uri.parse(path));
    if (file != null) {
      hasFields = true;
      mp.fields[r'file'] = file.field;
      mp.files.add(file);
    }
    if (parseMethod != null) {
      hasFields = true;
      mp.fields[r'parse_method'] = parameterToString(parseMethod);
    }
    if (callbackUrl != null) {
      hasFields = true;
      mp.fields[r'callback_url'] = parameterToString(callbackUrl);
    }
    if (userId != null) {
      hasFields = true;
      mp.fields[r'user_id'] = parameterToString(userId);
    }
    if (hasFields) {
      postBody = mp;
    }

    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Upload Excel Async
  ///
  /// Upload Excel file for async background processing Returns immediately with job ID, processes in background
  ///
  /// Parameters:
  ///
  /// * [MultipartFile] file (required):
  ///
  /// * [String] parseMethod:
  ///
  /// * [String] callbackUrl:
  ///
  /// * [String] userId:
  Future<JobResponse?> uploadExcelAsyncV1UploadExcelAsyncPost(MultipartFile file, { String? parseMethod, String? callbackUrl, String? userId, }) async {
    final response = await uploadExcelAsyncV1UploadExcelAsyncPostWithHttpInfo(file,  parseMethod: parseMethod, callbackUrl: callbackUrl, userId: userId, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'JobResponse',) as JobResponse;
    
    }
    return null;
  }
}
