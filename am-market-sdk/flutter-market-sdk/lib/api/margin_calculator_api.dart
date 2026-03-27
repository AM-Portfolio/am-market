// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;


class MarginCalculatorApi {
  MarginCalculatorApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Calculate margin requirements
  ///
  /// Calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [MarginCalculationRequest] marginCalculationRequest (required):
  Future<Response> calculateMarginWithHttpInfo(MarginCalculationRequest marginCalculationRequest,) async {
    final path = r'/v1/margin/calculate';
    Object? postBody = marginCalculationRequest;

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

  /// Calculate margin requirements
  ///
  /// Calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments
  ///
  /// Parameters:
  ///
  /// * [MarginCalculationRequest] marginCalculationRequest (required):
  Future<MarginCalculationResponse?> calculateMargin(MarginCalculationRequest marginCalculationRequest,) async {
    final response = await calculateMarginWithHttpInfo(marginCalculationRequest,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'MarginCalculationResponse',) as MarginCalculationResponse;
    
    }
    return null;
  }

  /// Calculate margin requirements asynchronously
  ///
  /// Asynchronously calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [MarginCalculationRequest] marginCalculationRequest (required):
  Future<Response> calculateMarginAsyncWithHttpInfo(MarginCalculationRequest marginCalculationRequest,) async {
    final path = r'/v1/margin/calculate-async';
    Object? postBody = marginCalculationRequest;

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

  /// Calculate margin requirements asynchronously
  ///
  /// Asynchronously calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments
  ///
  /// Parameters:
  ///
  /// * [MarginCalculationRequest] marginCalculationRequest (required):
  Future<MarginCalculationResponse?> calculateMarginAsync(MarginCalculationRequest marginCalculationRequest,) async {
    final response = await calculateMarginAsyncWithHttpInfo(marginCalculationRequest,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'MarginCalculationResponse',) as MarginCalculationResponse;
    
    }
    return null;
  }
}
