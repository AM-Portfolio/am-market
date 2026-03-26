//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class SecurityExplorerApi {
  SecurityExplorerApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Search securities by symbol or ISIN
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] query (required):
  Future<Response> searchWithHttpInfo(String query,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/securities/search';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'query', query));

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

  /// Search securities by symbol or ISIN
  ///
  /// Parameters:
  ///
  /// * [String] query (required):
  Future<List<SecurityDocument>?> search(String query,) async {
    final response = await searchWithHttpInfo(query,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<SecurityDocument>') as List)
        .cast<SecurityDocument>()
        .toList(growable: false);

    }
    return null;
  }

  /// Advanced search securities with filters
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [SecuritySearchRequest] securitySearchRequest (required):
  Future<Response> searchAdvancedWithHttpInfo(SecuritySearchRequest securitySearchRequest,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/securities/search';

    // ignore: prefer_final_locals
    Object? postBody = securitySearchRequest;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

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

  /// Advanced search securities with filters
  ///
  /// Parameters:
  ///
  /// * [SecuritySearchRequest] securitySearchRequest (required):
  Future<List<SecurityDocument>?> searchAdvanced(SecuritySearchRequest securitySearchRequest,) async {
    final response = await searchAdvancedWithHttpInfo(securitySearchRequest,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<SecurityDocument>') as List)
        .cast<SecurityDocument>()
        .toList(growable: false);

    }
    return null;
  }
}
