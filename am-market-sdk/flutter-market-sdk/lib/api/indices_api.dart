// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;


class IndicesApi {
  IndicesApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Get available indices
  ///
  /// Retrieves the list of available indices (Broad and Sector)
  ///
  /// Note: This method returns the HTTP [Response].
  Future<Response> getAvailableIndicesWithHttpInfo() async {
    final path = r'/v1/indices/available';
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

  /// Get available indices
  ///
  /// Retrieves the list of available indices (Broad and Sector)
  Future<String?> getAvailableIndices() async {
    final response = await getAvailableIndicesWithHttpInfo();
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

  /// Get latest market data for multiple indices
  ///
  /// Retrieves the latest market data for multiple indices in a single request
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [List<String>] requestBody (required):
  ///
  /// * [bool] forceRefresh:
  ///   Force refresh from source instead of using cache
  Future<Response> getLatestIndicesDataWithHttpInfo(List<String> requestBody, { bool? forceRefresh, }) async {
    final path = r'/v1/indices/batch';
    Object? postBody = requestBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (forceRefresh != null) {
      queryParams.addAll(_queryParams('', 'forceRefresh', forceRefresh));
    }

    const contentTypes = <String>['application/json'];


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

  /// Get latest market data for multiple indices
  ///
  /// Retrieves the latest market data for multiple indices in a single request
  ///
  /// Parameters:
  ///
  /// * [List<String>] requestBody (required):
  ///
  /// * [bool] forceRefresh:
  ///   Force refresh from source instead of using cache
  Future<StockIndicesMarketData?> getLatestIndicesData(List<String> requestBody, { bool? forceRefresh, }) async {
    final response = await getLatestIndicesDataWithHttpInfo(requestBody,  forceRefresh: forceRefresh, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'StockIndicesMarketData',) as StockIndicesMarketData;
    
    }
    return null;
  }
}
