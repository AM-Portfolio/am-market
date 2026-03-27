// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;


class BrokerageCalculatorApi {
  BrokerageCalculatorApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Calculate breakeven price
  ///
  /// Calculate the breakeven price for a stock considering all charges
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  ///
  /// * [double] price (required):
  ///
  /// * [int] quantity (required):
  ///
  /// * [String] exchange (required):
  ///
  /// * [String] tradeType (required):
  ///
  /// * [String] brokerType (required):
  Future<Response> calculateBreakevenWithHttpInfo(String symbol, double price, int quantity, String exchange, String tradeType, String brokerType,) async {
    final path = r'/v1/brokerage/breakeven';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'symbol', symbol));
      queryParams.addAll(_queryParams('', 'price', price));
      queryParams.addAll(_queryParams('', 'quantity', quantity));
      queryParams.addAll(_queryParams('', 'exchange', exchange));
      queryParams.addAll(_queryParams('', 'tradeType', tradeType));
      queryParams.addAll(_queryParams('', 'brokerType', brokerType));

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

  /// Calculate breakeven price
  ///
  /// Calculate the breakeven price for a stock considering all charges
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  ///
  /// * [double] price (required):
  ///
  /// * [int] quantity (required):
  ///
  /// * [String] exchange (required):
  ///
  /// * [String] tradeType (required):
  ///
  /// * [String] brokerType (required):
  Future<BrokerageCalculationResponse?> calculateBreakeven(String symbol, double price, int quantity, String exchange, String tradeType, String brokerType,) async {
    final response = await calculateBreakevenWithHttpInfo(symbol, price, quantity, exchange, tradeType, brokerType,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'BrokerageCalculationResponse',) as BrokerageCalculationResponse;
    
    }
    return null;
  }

  /// Calculate brokerage and taxes
  ///
  /// Calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [BrokerageCalculationRequest] brokerageCalculationRequest (required):
  Future<Response> calculateBrokerageWithHttpInfo(BrokerageCalculationRequest brokerageCalculationRequest,) async {
    final path = r'/v1/brokerage/calculate';
    Object? postBody = brokerageCalculationRequest;

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

  /// Calculate brokerage and taxes
  ///
  /// Calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade
  ///
  /// Parameters:
  ///
  /// * [BrokerageCalculationRequest] brokerageCalculationRequest (required):
  Future<BrokerageCalculationResponse?> calculateBrokerage(BrokerageCalculationRequest brokerageCalculationRequest,) async {
    final response = await calculateBrokerageWithHttpInfo(brokerageCalculationRequest,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'BrokerageCalculationResponse',) as BrokerageCalculationResponse;
    
    }
    return null;
  }

  /// Calculate brokerage and taxes asynchronously
  ///
  /// Asynchronously calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [BrokerageCalculationRequest] brokerageCalculationRequest (required):
  Future<Response> calculateBrokerageAsyncWithHttpInfo(BrokerageCalculationRequest brokerageCalculationRequest,) async {
    final path = r'/v1/brokerage/calculate-async';
    Object? postBody = brokerageCalculationRequest;

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

  /// Calculate brokerage and taxes asynchronously
  ///
  /// Asynchronously calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade
  ///
  /// Parameters:
  ///
  /// * [BrokerageCalculationRequest] brokerageCalculationRequest (required):
  Future<BrokerageCalculationResponse?> calculateBrokerageAsync(BrokerageCalculationRequest brokerageCalculationRequest,) async {
    final response = await calculateBrokerageAsyncWithHttpInfo(brokerageCalculationRequest,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'BrokerageCalculationResponse',) as BrokerageCalculationResponse;
    
    }
    return null;
  }
}
