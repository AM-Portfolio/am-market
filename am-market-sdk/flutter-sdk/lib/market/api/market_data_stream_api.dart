//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of market.api;


class MarketDataStreamApi {
  MarketDataStreamApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Connect to market data stream
  ///
  /// Initiates a WebSocket connection for the specified provider and instruments
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [StreamConnectRequest] streamConnectRequest (required):
  Future<Response> connectWithHttpInfo(StreamConnectRequest streamConnectRequest,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/market-data/stream/connect';

    // ignore: prefer_final_locals
    Object? postBody = streamConnectRequest;

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

  /// Connect to market data stream
  ///
  /// Initiates a WebSocket connection for the specified provider and instruments
  ///
  /// Parameters:
  ///
  /// * [StreamConnectRequest] streamConnectRequest (required):
  Future<String?> connect(StreamConnectRequest streamConnectRequest,) async {
    final response = await connectWithHttpInfo(streamConnectRequest,);
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

  /// Disconnect market data stream
  ///
  /// Disconnects the WebSocket stream for the specified provider
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] provider (required):
  Future<Response> disconnectWithHttpInfo(String provider,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/market-data/stream/disconnect';

    // ignore: prefer_final_locals
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

  /// Disconnect market data stream
  ///
  /// Disconnects the WebSocket stream for the specified provider
  ///
  /// Parameters:
  ///
  /// * [String] provider (required):
  Future<String?> disconnect(String provider,) async {
    final response = await disconnectWithHttpInfo(provider,);
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

  /// Connect to market data stream
  ///
  /// Initiates a WebSocket connection and returns a structured response
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [StreamConnectRequest] streamConnectRequest (required):
  Future<Response> initiateWithHttpInfo(StreamConnectRequest streamConnectRequest,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/market-data/stream/initiate';

    // ignore: prefer_final_locals
    Object? postBody = streamConnectRequest;

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

  /// Connect to market data stream
  ///
  /// Initiates a WebSocket connection and returns a structured response
  ///
  /// Parameters:
  ///
  /// * [StreamConnectRequest] streamConnectRequest (required):
  Future<StreamConnectResponse?> initiate(StreamConnectRequest streamConnectRequest,) async {
    final response = await initiateWithHttpInfo(streamConnectRequest,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'StreamConnectResponse',) as StreamConnectResponse;
    
    }
    return null;
  }
}
