// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

part of openapi.api;


class MarketDataApi {
  MarketDataApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Generate session from request token
  ///
  /// Creates a new authenticated session using the request token obtained from broker login
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] requestToken:
  ///
  /// * [String] requestToken2:
  ///
  /// * [String] code:
  ///
  /// * [String] status:
  Future<Response> generateSessionWithHttpInfo({ String? requestToken, String? requestToken2, String? code, String? status, }) async {
    final path = r'/v1/market-data/auth/session';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (requestToken != null) {
      queryParams.addAll(_queryParams('', 'request_token', requestToken));
    }
    if (requestToken2 != null) {
      queryParams.addAll(_queryParams('', 'requestToken', requestToken2));
    }
    if (code != null) {
      queryParams.addAll(_queryParams('', 'code', code));
    }
    if (status != null) {
      queryParams.addAll(_queryParams('', 'status', status));
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

  /// Generate session from request token
  ///
  /// Creates a new authenticated session using the request token obtained from broker login
  ///
  /// Parameters:
  ///
  /// * [String] requestToken:
  ///
  /// * [String] requestToken2:
  ///
  /// * [String] code:
  ///
  /// * [String] status:
  Future<Object?> generateSession({ String? requestToken, String? requestToken2, String? code, String? status, }) async {
    final response = await generateSessionWithHttpInfo( requestToken: requestToken, requestToken2: requestToken2, code: code, status: status, );
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

  /// Get historical market data
  ///
  /// Retrieves historical price and volume data for one or more instruments with filtering options
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [HistoricalDataRequest] historicalDataRequest (required):
  Future<Response> getHistoricalDataWithHttpInfo(HistoricalDataRequest historicalDataRequest,) async {
    final path = r'/v1/market-data/historical-data';
    Object? postBody = historicalDataRequest;

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

  /// Get historical market data
  ///
  /// Retrieves historical price and volume data for one or more instruments with filtering options
  ///
  /// Parameters:
  ///
  /// * [HistoricalDataRequest] historicalDataRequest (required):
  Future<HistoricalDataResponseV1?> getHistoricalData(HistoricalDataRequest historicalDataRequest,) async {
    final response = await getHistoricalDataWithHttpInfo(historicalDataRequest,);
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

  /// Get live LTP with change calculation
  ///
  /// Retrieves current LTP and calculates change based on historical closing price for the specified timeframe
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] symbols (required):
  ///
  /// * [String] timeframe:
  ///
  /// * [bool] isIndexSymbol:
  ///
  /// * [bool] refresh:
  Future<Response> getLiveLTPWithHttpInfo(String symbols, { String? timeframe, bool? isIndexSymbol, bool? refresh, }) async {
    final path = r'/v1/market-data/live-ltp';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'symbols', symbols));
    if (timeframe != null) {
      queryParams.addAll(_queryParams('', 'timeframe', timeframe));
    }
    if (isIndexSymbol != null) {
      queryParams.addAll(_queryParams('', 'isIndexSymbol', isIndexSymbol));
    }
    if (refresh != null) {
      queryParams.addAll(_queryParams('', 'refresh', refresh));
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

  /// Get live LTP with change calculation
  ///
  /// Retrieves current LTP and calculates change based on historical closing price for the specified timeframe
  ///
  /// Parameters:
  ///
  /// * [String] symbols (required):
  ///
  /// * [String] timeframe:
  ///
  /// * [bool] isIndexSymbol:
  ///
  /// * [bool] refresh:
  Future<Map<String, Object>?> getLiveLTP(String symbols, { String? timeframe, bool? isIndexSymbol, bool? refresh, }) async {
    final response = await getLiveLTPWithHttpInfo(symbols,  timeframe: timeframe, isIndexSymbol: isIndexSymbol, refresh: refresh, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, Object>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, Object>'),);

    }
    return null;
  }

  /// Get live market prices
  ///
  /// Retrieves real-time market prices for specified symbols or all available symbols
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] symbols:
  ///
  /// * [bool] isIndexSymbol:
  ///
  /// * [bool] refresh:
  Future<Response> getLivePricesWithHttpInfo({ String? symbols, bool? isIndexSymbol, bool? refresh, }) async {
    final path = r'/v1/market-data/live-prices';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (symbols != null) {
      queryParams.addAll(_queryParams('', 'symbols', symbols));
    }
    if (isIndexSymbol != null) {
      queryParams.addAll(_queryParams('', 'isIndexSymbol', isIndexSymbol));
    }
    if (refresh != null) {
      queryParams.addAll(_queryParams('', 'refresh', refresh));
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

  /// Get live market prices
  ///
  /// Retrieves real-time market prices for specified symbols or all available symbols
  ///
  /// Parameters:
  ///
  /// * [String] symbols:
  ///
  /// * [bool] isIndexSymbol:
  ///
  /// * [bool] refresh:
  Future<Map<String, Object>?> getLivePrices({ String? symbols, bool? isIndexSymbol, bool? refresh, }) async {
    final response = await getLivePricesWithHttpInfo( symbols: symbols, isIndexSymbol: isIndexSymbol, refresh: refresh, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, Object>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, Object>'),);

    }
    return null;
  }

  /// Get login URL for broker authentication
  ///
  /// Returns a URL that can be used to authenticate with the broker's login page
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] provider:
  Future<Response> getLoginUrlWithHttpInfo({ String? provider, }) async {
    final path = r'/v1/market-data/auth/login-url';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (provider != null) {
      queryParams.addAll(_queryParams('', 'provider', provider));
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

  /// Get login URL for broker authentication
  ///
  /// Returns a URL that can be used to authenticate with the broker's login page
  ///
  /// Parameters:
  ///
  /// * [String] provider:
  Future<Map<String, String>?> getLoginUrl({ String? provider, }) async {
    final response = await getLoginUrlWithHttpInfo( provider: provider, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, String>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, String>'),);

    }
    return null;
  }

  /// Get mutual fund details
  ///
  /// Retrieves detailed information about a mutual fund including NAV, returns, and other metrics
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] schemeCode (required):
  ///
  /// * [bool] refresh:
  Future<Response> getMutualFundDetailsWithHttpInfo(String schemeCode, { bool? refresh, }) async {
    final path = r'/v1/market-data/mutual-fund/{schemeCode}'
      .replaceAll('{schemeCode}', schemeCode);
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (refresh != null) {
      queryParams.addAll(_queryParams('', 'refresh', refresh));
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

  /// Get mutual fund details
  ///
  /// Retrieves detailed information about a mutual fund including NAV, returns, and other metrics
  ///
  /// Parameters:
  ///
  /// * [String] schemeCode (required):
  ///
  /// * [bool] refresh:
  Future<Map<String, Object>?> getMutualFundDetails(String schemeCode, { bool? refresh, }) async {
    final response = await getMutualFundDetailsWithHttpInfo(schemeCode,  refresh: refresh, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, Object>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, Object>'),);

    }
    return null;
  }

  /// Get mutual fund NAV history
  ///
  /// Retrieves historical Net Asset Value (NAV) data for a mutual fund over a specified date range
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] schemeCode (required):
  ///
  /// * [String] from (required):
  ///
  /// * [String] to (required):
  ///
  /// * [bool] refresh:
  Future<Response> getMutualFundNavHistoryWithHttpInfo(String schemeCode, String from, String to, { bool? refresh, }) async {
    final path = r'/v1/market-data/mutual-fund/{schemeCode}/history'
      .replaceAll('{schemeCode}', schemeCode);
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'from', from));
      queryParams.addAll(_queryParams('', 'to', to));
    if (refresh != null) {
      queryParams.addAll(_queryParams('', 'refresh', refresh));
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

  /// Get mutual fund NAV history
  ///
  /// Retrieves historical Net Asset Value (NAV) data for a mutual fund over a specified date range
  ///
  /// Parameters:
  ///
  /// * [String] schemeCode (required):
  ///
  /// * [String] from (required):
  ///
  /// * [String] to (required):
  ///
  /// * [bool] refresh:
  Future<Map<String, Object>?> getMutualFundNavHistory(String schemeCode, String from, String to, { bool? refresh, }) async {
    final response = await getMutualFundNavHistoryWithHttpInfo(schemeCode, from, to,  refresh: refresh, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, Object>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, Object>'),);

    }
    return null;
  }

  /// Get OHLC data for multiple symbols
  ///
  /// Retrieves Open-High-Low-Close data for multiple symbols with support for different timeframes
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [OHLCRequest] oHLCRequest (required):
  Future<Response> getOHLCWithHttpInfo(OHLCRequest oHLCRequest,) async {
    final path = r'/v1/market-data/ohlc';
    Object? postBody = oHLCRequest;

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

  /// Get OHLC data for multiple symbols
  ///
  /// Retrieves Open-High-Low-Close data for multiple symbols with support for different timeframes
  ///
  /// Parameters:
  ///
  /// * [OHLCRequest] oHLCRequest (required):
  Future<Object?> getOHLC(OHLCRequest oHLCRequest,) async {
    final response = await getOHLCWithHttpInfo(oHLCRequest,);
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

  /// Get option chain data
  ///
  /// Retrieves option chain data including calls and puts for a given underlying instrument
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  ///
  /// * [String] expiryDate:
  ///
  /// * [bool] refresh:
  Future<Response> getOptionChainWithHttpInfo(String symbol, { String? expiryDate, bool? refresh, }) async {
    final path = r'/v1/market-data/option-chain';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'symbol', symbol));
    if (expiryDate != null) {
      queryParams.addAll(_queryParams('', 'expiryDate', expiryDate));
    }
    if (refresh != null) {
      queryParams.addAll(_queryParams('', 'refresh', refresh));
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

  /// Get option chain data
  ///
  /// Retrieves option chain data including calls and puts for a given underlying instrument
  ///
  /// Parameters:
  ///
  /// * [String] symbol (required):
  ///
  /// * [String] expiryDate:
  ///
  /// * [bool] refresh:
  Future<Map<String, Object>?> getOptionChain(String symbol, { String? expiryDate, bool? refresh, }) async {
    final response = await getOptionChainWithHttpInfo(symbol,  expiryDate: expiryDate, refresh: refresh, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, Object>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, Object>'),);

    }
    return null;
  }

  /// Get quotes for multiple symbols
  ///
  /// Retrieves latest quotes for multiple symbols with support for different timeframes
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] symbols (required):
  ///
  /// * [String] timeFrame:
  ///
  /// * [bool] refresh:
  Future<Response> getQuotesWithHttpInfo(String symbols, { String? timeFrame, bool? refresh, }) async {
    final path = r'/v1/market-data/quotes';
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'symbols', symbols));
    if (timeFrame != null) {
      queryParams.addAll(_queryParams('', 'timeFrame', timeFrame));
    }
    if (refresh != null) {
      queryParams.addAll(_queryParams('', 'refresh', refresh));
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

  /// Get quotes for multiple symbols
  ///
  /// Retrieves latest quotes for multiple symbols with support for different timeframes
  ///
  /// Parameters:
  ///
  /// * [String] symbols (required):
  ///
  /// * [String] timeFrame:
  ///
  /// * [bool] refresh:
  Future<Map<String, Object>?> getQuotes(String symbols, { String? timeFrame, bool? refresh, }) async {
    final response = await getQuotesWithHttpInfo(symbols,  timeFrame: timeFrame, refresh: refresh, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, Object>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, Object>'),);

    }
    return null;
  }

  /// Get quotes for multiple symbols (POST)
  ///
  /// Retrieves latest quotes for multiple symbols with support for different timeframes using POST request
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [QuotesRequest] quotesRequest (required):
  Future<Response> getQuotesPostWithHttpInfo(QuotesRequest quotesRequest,) async {
    final path = r'/v1/market-data/quotes';
    Object? postBody = quotesRequest;

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

  /// Get quotes for multiple symbols (POST)
  ///
  /// Retrieves latest quotes for multiple symbols with support for different timeframes using POST request
  ///
  /// Parameters:
  ///
  /// * [QuotesRequest] quotesRequest (required):
  Future<Map<String, Object>?> getQuotesPost(QuotesRequest quotesRequest,) async {
    final response = await getQuotesPostWithHttpInfo(quotesRequest,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, Object>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, Object>'),);

    }
    return null;
  }

  /// Get symbols for a specific exchange
  ///
  /// Retrieves all available trading symbols for a specific exchange
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] exchange (required):
  Future<Response> getSymbolsForExchangeWithHttpInfo(String exchange,) async {
    final path = r'/v1/market-data/symbols/{exchange}'
      .replaceAll('{exchange}', exchange);
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

  /// Get symbols for a specific exchange
  ///
  /// Retrieves all available trading symbols for a specific exchange
  ///
  /// Parameters:
  ///
  /// * [String] exchange (required):
  Future<List<Object>?> getSymbolsForExchange(String exchange,) async {
    final response = await getSymbolsForExchangeWithHttpInfo(exchange,);
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

  /// Logout and invalidate session
  ///
  /// Invalidates the current broker session and clears authentication tokens
  ///
  /// Note: This method returns the HTTP [Response].
  Future<Response> logoutWithHttpInfo() async {
    final path = r'/v1/market-data/auth/logout';
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

  /// Logout and invalidate session
  ///
  /// Invalidates the current broker session and clears authentication tokens
  Future<Map<String, Object>?> logout() async {
    final response = await logoutWithHttpInfo();
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return Map<String, Object>.from(await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Map<String, Object>'),);

    }
    return null;
  }
}
