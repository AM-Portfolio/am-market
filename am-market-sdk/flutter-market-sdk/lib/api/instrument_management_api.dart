// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;


class InstrumentManagementApi {
  InstrumentManagementApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Search Instruments
  ///
  /// Search for instruments using criteria: list of symbols ('gym balls'), exchanges, and instrument types. Supports semantic text search combined with filters.
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [InstrumentSearchCriteria] instrumentSearchCriteria (required):
  Future<Response> searchInstrumentsWithHttpInfo(InstrumentSearchCriteria instrumentSearchCriteria,) async {
    final path = r'/v1/instruments/search';
    Object? postBody = instrumentSearchCriteria;

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

  /// Search Instruments
  ///
  /// Search for instruments using criteria: list of symbols ('gym balls'), exchanges, and instrument types. Supports semantic text search combined with filters.
  ///
  /// Parameters:
  ///
  /// * [InstrumentSearchCriteria] instrumentSearchCriteria (required):
  Future<List<Object>?> searchInstruments(InstrumentSearchCriteria instrumentSearchCriteria,) async {
    final response = await searchInstrumentsWithHttpInfo(instrumentSearchCriteria,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      final responseBody = await _decodeBodyBytes(response);
      return (await apiClient.deserializeAsync(responseBody, 'List<Object>') as List)
        .cast<Object>()
        .toList(growable: false);

    }
    return null;
  }

  /// Update Instruments from File
  ///
  /// Triggers an update of instruments from the local JSON file (NSE.json). Uses streaming to handle large files.
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] filePath:
  ///
  /// * [String] provider:
  Future<Response> updateInstrumentsWithHttpInfo({ String? filePath, String? provider, }) async {
    final path = r'/v1/instruments/update';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (filePath != null) {
      queryParams.addAll(_queryParams('', 'filePath', filePath));
    }
    if (provider != null) {
      queryParams.addAll(_queryParams('', 'provider', provider));
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

  /// Update Instruments from File
  ///
  /// Triggers an update of instruments from the local JSON file (NSE.json). Uses streaming to handle large files.
  ///
  /// Parameters:
  ///
  /// * [String] filePath:
  ///
  /// * [String] provider:
  Future<String?> updateInstruments({ String? filePath, String? provider, }) async {
    final response = await updateInstrumentsWithHttpInfo( filePath: filePath, provider: provider, );
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
