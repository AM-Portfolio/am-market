//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unnecessary_null_comparison, unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class ETFHoldingsApi {
  ETFHoldingsApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Fetch All Etf Holdings
  ///
  /// Fetch holdings for all ETFs with ISINs from moneycontrol API Returns immediately with job ID, processes in background Smart caching: Only fetches if data is missing or stale
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] callbackUrl:
  ///
  /// * [String] userId:
  ///
  /// * [int] limit:
  ///   Limit number of ETFs to process
  ///
  /// * [bool] forceRefresh:
  ///   Force refresh even if data exists for today
  Future<Response> fetchAllEtfHoldingsV1FetchAllHoldingsPostWithHttpInfo({ String? callbackUrl, String? userId, int? limit, bool? forceRefresh, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/fetch-all-holdings';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (callbackUrl != null) {
      queryParams.addAll(_queryParams('', 'callback_url', callbackUrl));
    }
    if (userId != null) {
      queryParams.addAll(_queryParams('', 'user_id', userId));
    }
    if (limit != null) {
      queryParams.addAll(_queryParams('', 'limit', limit));
    }
    if (forceRefresh != null) {
      queryParams.addAll(_queryParams('', 'force_refresh', forceRefresh));
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

  /// Fetch All Etf Holdings
  ///
  /// Fetch holdings for all ETFs with ISINs from moneycontrol API Returns immediately with job ID, processes in background Smart caching: Only fetches if data is missing or stale
  ///
  /// Parameters:
  ///
  /// * [String] callbackUrl:
  ///
  /// * [String] userId:
  ///
  /// * [int] limit:
  ///   Limit number of ETFs to process
  ///
  /// * [bool] forceRefresh:
  ///   Force refresh even if data exists for today
  Future<JobResponse?> fetchAllEtfHoldingsV1FetchAllHoldingsPost({ String? callbackUrl, String? userId, int? limit, bool? forceRefresh, }) async {
    final response = await fetchAllEtfHoldingsV1FetchAllHoldingsPostWithHttpInfo( callbackUrl: callbackUrl, userId: userId, limit: limit, forceRefresh: forceRefresh, );
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

  /// Fetch Holdings For Etf
  ///
  /// Fetch holdings for a specific ETF by symbol Returns immediately with job ID, processes in background
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  ///
  /// * [String] callbackUrl:
  ///
  /// * [String] userId:
  Future<Response> fetchHoldingsForEtfV1FetchHoldingsSymbolPostWithHttpInfo(String symbol, { String? callbackUrl, String? userId, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/fetch-holdings/{symbol}'
      .replaceAll('{symbol}', symbol);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (callbackUrl != null) {
      queryParams.addAll(_queryParams('', 'callback_url', callbackUrl));
    }
    if (userId != null) {
      queryParams.addAll(_queryParams('', 'user_id', userId));
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

  /// Fetch Holdings For Etf
  ///
  /// Fetch holdings for a specific ETF by symbol Returns immediately with job ID, processes in background
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  ///
  /// * [String] callbackUrl:
  ///
  /// * [String] userId:
  Future<Object?> fetchHoldingsForEtfV1FetchHoldingsSymbolPost(String symbol, { String? callbackUrl, String? userId, }) async {
    final response = await fetchHoldingsForEtfV1FetchHoldingsSymbolPostWithHttpInfo(symbol,  callbackUrl: callbackUrl, userId: userId, );
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

  /// Get Cache Statistics
  ///
  /// Get ETF holdings cache statistics
  ///
  /// Note: This method returns the HTTP [Response].
  Future<Response> getCacheStatisticsV1CacheStatsGetWithHttpInfo() async {
    // ignore: prefer_const_declarations
    final path = r'/v1/cache-stats';

    // ignore: prefer_final_locals
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

  /// Get Cache Statistics
  ///
  /// Get ETF holdings cache statistics
  Future<Object?> getCacheStatisticsV1CacheStatsGet() async {
    final response = await getCacheStatisticsV1CacheStatsGetWithHttpInfo();
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

  /// Get Etf Holdings
  ///
  /// Get stored holdings for a specific ETF
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  Future<Response> getEtfHoldingsV1HoldingsSymbolGetWithHttpInfo(String symbol,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/holdings/{symbol}'
      .replaceAll('{symbol}', symbol);

    // ignore: prefer_final_locals
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

  /// Get Etf Holdings
  ///
  /// Get stored holdings for a specific ETF
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  Future<Object?> getEtfHoldingsV1HoldingsSymbolGet(String symbol,) async {
    final response = await getEtfHoldingsV1HoldingsSymbolGetWithHttpInfo(symbol,);
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

  /// Get Etf Stats
  ///
  /// Get ETF database statistics
  ///
  /// Note: This method returns the HTTP [Response].
  Future<Response> getEtfStatsV1StatsGetWithHttpInfo() async {
    // ignore: prefer_const_declarations
    final path = r'/v1/stats';

    // ignore: prefer_final_locals
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

  /// Get Etf Stats
  ///
  /// Get ETF database statistics
  Future<Object?> getEtfStatsV1StatsGet() async {
    final response = await getEtfStatsV1StatsGetWithHttpInfo();
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

  /// Load Etfs From Json
  ///
  /// Load ETF data from JSON file Accepts etf_details.json and loads all ETFs into database
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [MultipartFile] file (required):
  ///   ETF details JSON file
  ///
  /// * [bool] dryRun:
  ///   Validate only, don't persist
  Future<Response> loadEtfsFromJsonV1LoadFromJsonPostWithHttpInfo(MultipartFile file, { bool? dryRun, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/load-from-json';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (dryRun != null) {
      queryParams.addAll(_queryParams('', 'dry_run', dryRun));
    }

    const contentTypes = <String>['multipart/form-data'];

    bool hasFields = false;
    final mp = MultipartRequest('POST', Uri.parse(path));
    if (file != null) {
      hasFields = true;
      mp.fields[r'file'] = file.field;
      mp.files.add(file);
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

  /// Load Etfs From Json
  ///
  /// Load ETF data from JSON file Accepts etf_details.json and loads all ETFs into database
  ///
  /// Parameters:
  ///
  /// * [MultipartFile] file (required):
  ///   ETF details JSON file
  ///
  /// * [bool] dryRun:
  ///   Validate only, don't persist
  Future<Object?> loadEtfsFromJsonV1LoadFromJsonPost(MultipartFile file, { bool? dryRun, }) async {
    final response = await loadEtfsFromJsonV1LoadFromJsonPostWithHttpInfo(file,  dryRun: dryRun, );
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

  /// Search Etfs
  ///
  /// Search ETFs by symbol, name, or ISIN
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] query (required):
  ///   Search by symbol, name, or ISIN
  ///
  /// * [int] limit:
  ///   Maximum results to return
  Future<Response> searchEtfsV1SearchGetWithHttpInfo(String query, { int? limit, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/search';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'query', query));
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

  /// Search Etfs
  ///
  /// Search ETFs by symbol, name, or ISIN
  ///
  /// Parameters:
  ///
  /// * [String] query (required):
  ///   Search by symbol, name, or ISIN
  ///
  /// * [int] limit:
  ///   Maximum results to return
  Future<Object?> searchEtfsV1SearchGet(String query, { int? limit, }) async {
    final response = await searchEtfsV1SearchGetWithHttpInfo(query,  limit: limit, );
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
}

