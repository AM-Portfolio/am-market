// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;


class MarketAnalyticsApi {
  MarketAnalyticsApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Get historical charts data
  ///
  /// Retrieves historical data for charts with various time frames (10m, 1H, 1D, 1W, 1M, 5Y, etc.)
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  ///
  /// * [String] range:
  Future<Response> getHistoricalChartsWithHttpInfo(String symbol, { String? range, }) async {
    final path = r'/v1/market-analytics/historical-charts/{symbol}'
      .replaceAll('{symbol}', symbol);
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (range != null) {
      queryParams.addAll(_queryParams('', 'range', range));
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

  /// Get historical charts data
  ///
  /// Retrieves historical data for charts with various time frames (10m, 1H, 1D, 1W, 1M, 5Y, etc.)
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  ///
  /// * [String] range:
  Future<HistoricalDataResponseV1?> getHistoricalCharts(String symbol, { String? range, }) async {
    final response = await getHistoricalChartsWithHttpInfo(symbol,  range: range, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'HistoricalDataResponseV1',) as HistoricalDataResponseV1;
    
    }
    return null;
  }

  /// Get Top Gainers/Losers
  ///
  /// Retrieves top performing or worst performing stocks from the specified market index
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] type:
  ///
  /// * [int] limit:
  ///
  /// * [String] indexSymbol:
  ///
  /// * [String] timeFrame:
  ///
  /// * [bool] expandIndices:
  Future<Response> getMoversWithHttpInfo({ String? type, int? limit, String? indexSymbol, String? timeFrame, bool? expandIndices, }) async {
    final path = r'/v1/market-analytics/movers';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (type != null) {
      queryParams.addAll(_queryParams('', 'type', type));
    }
    if (limit != null) {
      queryParams.addAll(_queryParams('', 'limit', limit));
    }
    if (indexSymbol != null) {
      queryParams.addAll(_queryParams('', 'indexSymbol', indexSymbol));
    }
    if (timeFrame != null) {
      queryParams.addAll(_queryParams('', 'timeFrame', timeFrame));
    }
    if (expandIndices != null) {
      queryParams.addAll(_queryParams('', 'expandIndices', expandIndices));
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

  /// Get Top Gainers/Losers
  ///
  /// Retrieves top performing or worst performing stocks from the specified market index
  ///
  /// Parameters:
  ///
  /// * [String] type:
  ///
  /// * [int] limit:
  ///
  /// * [String] indexSymbol:
  ///
  /// * [String] timeFrame:
  ///
  /// * [bool] expandIndices:
  Future<List<Map<String, Object>>?> getMovers({ String? type, int? limit, String? indexSymbol, String? timeFrame, bool? expandIndices, }) async {
    final response = await getMoversWithHttpInfo( type: type, limit: limit, indexSymbol: indexSymbol, timeFrame: timeFrame, expandIndices: expandIndices, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<Map<String, Object>>') as List)
        .cast<Map<String, Object>>()
        .toList(growable: false);

    }
    return null;
  }

  /// Get Sector Performance
  ///
  /// Aggregates market performance by sector (Industry) from the specified index
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] indexSymbol:
  ///
  /// * [String] timeFrame:
  ///
  /// * [bool] expandIndices:
  Future<Response> getSectorPerformanceWithHttpInfo({ String? indexSymbol, String? timeFrame, bool? expandIndices, }) async {
    final path = r'/v1/market-analytics/sectors';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (indexSymbol != null) {
      queryParams.addAll(_queryParams('', 'indexSymbol', indexSymbol));
    }
    if (timeFrame != null) {
      queryParams.addAll(_queryParams('', 'timeFrame', timeFrame));
    }
    if (expandIndices != null) {
      queryParams.addAll(_queryParams('', 'expandIndices', expandIndices));
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

  /// Get Sector Performance
  ///
  /// Aggregates market performance by sector (Industry) from the specified index
  ///
  /// Parameters:
  ///
  /// * [String] indexSymbol:
  ///
  /// * [String] timeFrame:
  ///
  /// * [bool] expandIndices:
  Future<List<Map<String, Object>>?> getSectorPerformance({ String? indexSymbol, String? timeFrame, bool? expandIndices, }) async {
    final response = await getSectorPerformanceWithHttpInfo( indexSymbol: indexSymbol, timeFrame: timeFrame, expandIndices: expandIndices, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<Map<String, Object>>') as List)
        .cast<Map<String, Object>>()
        .toList(growable: false);

    }
    return null;
  }
}
