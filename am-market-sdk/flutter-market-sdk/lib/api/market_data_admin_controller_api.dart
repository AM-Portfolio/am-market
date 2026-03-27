// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;


class MarketDataAdminControllerApi {
  MarketDataAdminControllerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Performs an HTTP 'GET /v1/admin/logs/{jobId}' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] jobId (required):
  Future<Response> getJobDetailsWithHttpInfo(String jobId,) async {
    final path = r'/v1/admin/logs/{jobId}'
      .replaceAll('{jobId}', jobId);
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

  /// Parameters:
  ///
  /// * [String] jobId (required):
  Future<IngestionJobLog?> getJobDetails(String jobId,) async {
    final response = await getJobDetailsWithHttpInfo(jobId,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'IngestionJobLog',) as IngestionJobLog;
    
    }
    return null;
  }

  /// Performs an HTTP 'GET /v1/admin/logs' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [int] page:
  ///
  /// * [int] size:
  ///
  /// * [DateTime] startDate:
  ///
  /// * [DateTime] endDate:
  Future<Response> getLogsWithHttpInfo({ int? page, int? size, DateTime? startDate, DateTime? endDate, }) async {
    final path = r'/v1/admin/logs';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (page != null) {
      queryParams.addAll(_queryParams('', 'page', page));
    }
    if (size != null) {
      queryParams.addAll(_queryParams('', 'size', size));
    }
    if (startDate != null) {
      queryParams.addAll(_queryParams('', 'startDate', startDate));
    }
    if (endDate != null) {
      queryParams.addAll(_queryParams('', 'endDate', endDate));
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

  /// Parameters:
  ///
  /// * [int] page:
  ///
  /// * [int] size:
  ///
  /// * [DateTime] startDate:
  ///
  /// * [DateTime] endDate:
  Future<List<IngestionJobLog>?> getLogs({ int? page, int? size, DateTime? startDate, DateTime? endDate, }) async {
    final response = await getLogsWithHttpInfo( page: page, size: size, startDate: startDate, endDate: endDate, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<IngestionJobLog>') as List)
        .cast<IngestionJobLog>()
        .toList(growable: false);

    }
    return null;
  }

  /// Performs an HTTP 'POST /v1/admin/ingestion/start' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] provider:
  ///
  /// * [List<String>] symbols:
  Future<Response> startIngestionWithHttpInfo({ String? provider, List<String>? symbols, }) async {
    final path = r'/v1/admin/ingestion/start';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (provider != null) {
      queryParams.addAll(_queryParams('', 'provider', provider));
    }
    if (symbols != null) {
      queryParams.addAll(_queryParams('multi', 'symbols', symbols));
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

  /// Parameters:
  ///
  /// * [String] provider:
  ///
  /// * [List<String>] symbols:
  Future<String?> startIngestion({ String? provider, List<String>? symbols, }) async {
    final response = await startIngestionWithHttpInfo( provider: provider, symbols: symbols, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'String',) as String;
    
    }
    return null;
  }

  /// Performs an HTTP 'POST /v1/admin/ingestion/stop' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] provider (required):
  Future<Response> stopIngestionWithHttpInfo(String provider,) async {
    final path = r'/v1/admin/ingestion/stop';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'provider', provider));

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

  /// Parameters:
  ///
  /// * [String] provider (required):
  Future<String?> stopIngestion(String provider,) async {
    final response = await stopIngestionWithHttpInfo(provider,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'String',) as String;
    
    }
    return null;
  }

  /// Performs an HTTP 'POST /v1/admin/sync/historical' operation and returns the [Response].
  /// Parameters:
  ///
  /// * [String] symbol:
  ///
  /// * [bool] forceRefresh:
  ///
  /// * [bool] fetchIndexStocks:
  Future<Response> triggerHistoricalSyncWithHttpInfo({ String? symbol, bool? forceRefresh, bool? fetchIndexStocks, }) async {
    final path = r'/v1/admin/sync/historical';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (symbol != null) {
      queryParams.addAll(_queryParams('', 'symbol', symbol));
    }
    if (forceRefresh != null) {
      queryParams.addAll(_queryParams('', 'forceRefresh', forceRefresh));
    }
    if (fetchIndexStocks != null) {
      queryParams.addAll(_queryParams('', 'fetchIndexStocks', fetchIndexStocks));
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

  /// Parameters:
  ///
  /// * [String] symbol:
  ///
  /// * [bool] forceRefresh:
  ///
  /// * [bool] fetchIndexStocks:
  Future<String?> triggerHistoricalSync({ String? symbol, bool? forceRefresh, bool? fetchIndexStocks, }) async {
    final response = await triggerHistoricalSyncWithHttpInfo( symbol: symbol, forceRefresh: forceRefresh, fetchIndexStocks: fetchIndexStocks, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'String',) as String;
    
    }
    return null;
  }
}
