# am_market_client.api.MarketDataApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**generateSession**](MarketDataApi.md#generatesession) | **GET** /v1/market-data/auth/session | Generate session from request token
[**getHistoricalData**](MarketDataApi.md#gethistoricaldata) | **POST** /v1/market-data/historical-data | Get historical market data
[**getLiveLTP**](MarketDataApi.md#getliveltp) | **GET** /v1/market-data/live-ltp | Get live LTP with change calculation
[**getLivePrices**](MarketDataApi.md#getliveprices) | **GET** /v1/market-data/live-prices | Get live market prices
[**getLoginUrl**](MarketDataApi.md#getloginurl) | **GET** /v1/market-data/auth/login-url | Get login URL for broker authentication
[**getMutualFundDetails**](MarketDataApi.md#getmutualfunddetails) | **GET** /v1/market-data/mutual-fund/{schemeCode} | Get mutual fund details
[**getMutualFundNavHistory**](MarketDataApi.md#getmutualfundnavhistory) | **GET** /v1/market-data/mutual-fund/{schemeCode}/history | Get mutual fund NAV history
[**getOHLC**](MarketDataApi.md#getohlc) | **POST** /v1/market-data/ohlc | Get OHLC data for multiple symbols
[**getOptionChain**](MarketDataApi.md#getoptionchain) | **GET** /v1/market-data/option-chain | Get option chain data
[**getQuotes**](MarketDataApi.md#getquotes) | **GET** /v1/market-data/quotes | Get quotes for multiple symbols
[**getQuotesPost**](MarketDataApi.md#getquotespost) | **POST** /v1/market-data/quotes | Get quotes for multiple symbols (POST)
[**getSymbolsForExchange**](MarketDataApi.md#getsymbolsforexchange) | **GET** /v1/market-data/symbols/{exchange} | Get symbols for a specific exchange
[**logout**](MarketDataApi.md#logout) | **POST** /v1/market-data/auth/logout | Logout and invalidate session


# **generateSession**
> Object generateSession(requestToken, requestToken2, code, status)

Generate session from request token

Creates a new authenticated session using the request token obtained from broker login

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final requestToken = requestToken_example; // String | 
final requestToken2 = requestToken_example; // String | 
final code = code_example; // String | 
final status = status_example; // String | 

try {
    final result = api_instance.generateSession(requestToken, requestToken2, code, status);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->generateSession: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **requestToken** | **String**|  | [optional] 
 **requestToken2** | **String**|  | [optional] 
 **code** | **String**|  | [optional] 
 **status** | **String**|  | [optional] [default to 'success']

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getHistoricalData**
> HistoricalDataResponseV1 getHistoricalData(historicalDataRequest)

Get historical market data

Retrieves historical price and volume data for one or more instruments with filtering options

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final historicalDataRequest = HistoricalDataRequest(); // HistoricalDataRequest | 

try {
    final result = api_instance.getHistoricalData(historicalDataRequest);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getHistoricalData: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **historicalDataRequest** | [**HistoricalDataRequest**](HistoricalDataRequest.md)|  | 

### Return type

[**HistoricalDataResponseV1**](HistoricalDataResponseV1.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getLiveLTP**
> Map<String, Object> getLiveLTP(symbols, timeframe, isIndexSymbol, refresh)

Get live LTP with change calculation

Retrieves current LTP and calculates change based on historical closing price for the specified timeframe

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final symbols = symbols_example; // String | 
final timeframe = timeframe_example; // String | 
final isIndexSymbol = true; // bool | 
final refresh = true; // bool | 

try {
    final result = api_instance.getLiveLTP(symbols, timeframe, isIndexSymbol, refresh);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getLiveLTP: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbols** | **String**|  | 
 **timeframe** | **String**|  | [optional] [default to '1D']
 **isIndexSymbol** | **bool**|  | [optional] [default to true]
 **refresh** | **bool**|  | [optional] [default to false]

### Return type

[**Map<String, Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getLivePrices**
> Map<String, Object> getLivePrices(symbols, isIndexSymbol, refresh)

Get live market prices

Retrieves real-time market prices for specified symbols or all available symbols

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final symbols = symbols_example; // String | 
final isIndexSymbol = true; // bool | 
final refresh = true; // bool | 

try {
    final result = api_instance.getLivePrices(symbols, isIndexSymbol, refresh);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getLivePrices: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbols** | **String**|  | [optional] 
 **isIndexSymbol** | **bool**|  | [optional] 
 **refresh** | **bool**|  | [optional] [default to false]

### Return type

[**Map<String, Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getLoginUrl**
> Map<String, String> getLoginUrl(provider)

Get login URL for broker authentication

Returns a URL that can be used to authenticate with the broker's login page

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final provider = provider_example; // String | 

try {
    final result = api_instance.getLoginUrl(provider);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getLoginUrl: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provider** | **String**|  | [optional] 

### Return type

**Map<String, String>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getMutualFundDetails**
> Map<String, Object> getMutualFundDetails(schemeCode, refresh)

Get mutual fund details

Retrieves detailed information about a mutual fund including NAV, returns, and other metrics

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final schemeCode = schemeCode_example; // String | 
final refresh = true; // bool | 

try {
    final result = api_instance.getMutualFundDetails(schemeCode, refresh);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getMutualFundDetails: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **schemeCode** | **String**|  | 
 **refresh** | **bool**|  | [optional] [default to false]

### Return type

[**Map<String, Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getMutualFundNavHistory**
> Map<String, Object> getMutualFundNavHistory(schemeCode, from, to, refresh)

Get mutual fund NAV history

Retrieves historical Net Asset Value (NAV) data for a mutual fund over a specified date range

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final schemeCode = schemeCode_example; // String | 
final from = from_example; // String | 
final to = to_example; // String | 
final refresh = true; // bool | 

try {
    final result = api_instance.getMutualFundNavHistory(schemeCode, from, to, refresh);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getMutualFundNavHistory: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **schemeCode** | **String**|  | 
 **from** | **String**|  | 
 **to** | **String**|  | 
 **refresh** | **bool**|  | [optional] [default to false]

### Return type

[**Map<String, Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getOHLC**
> Object getOHLC(oHLCRequest)

Get OHLC data for multiple symbols

Retrieves Open-High-Low-Close data for multiple symbols with support for different timeframes

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final oHLCRequest = OHLCRequest(); // OHLCRequest | 

try {
    final result = api_instance.getOHLC(oHLCRequest);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getOHLC: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **oHLCRequest** | [**OHLCRequest**](OHLCRequest.md)|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getOptionChain**
> Map<String, Object> getOptionChain(symbol, expiryDate, refresh)

Get option chain data

Retrieves option chain data including calls and puts for a given underlying instrument

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final symbol = symbol_example; // String | 
final expiryDate = expiryDate_example; // String | 
final refresh = true; // bool | 

try {
    final result = api_instance.getOptionChain(symbol, expiryDate, refresh);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getOptionChain: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **String**|  | 
 **expiryDate** | **String**|  | [optional] 
 **refresh** | **bool**|  | [optional] [default to false]

### Return type

[**Map<String, Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getQuotes**
> Map<String, Object> getQuotes(symbols, timeFrame, refresh)

Get quotes for multiple symbols

Retrieves latest quotes for multiple symbols with support for different timeframes

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final symbols = symbols_example; // String | 
final timeFrame = timeFrame_example; // String | 
final refresh = true; // bool | 

try {
    final result = api_instance.getQuotes(symbols, timeFrame, refresh);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getQuotes: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbols** | **String**|  | 
 **timeFrame** | **String**|  | [optional] [default to '5m']
 **refresh** | **bool**|  | [optional] [default to false]

### Return type

[**Map<String, Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getQuotesPost**
> Map<String, Object> getQuotesPost(quotesRequest)

Get quotes for multiple symbols (POST)

Retrieves latest quotes for multiple symbols with support for different timeframes using POST request

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final quotesRequest = QuotesRequest(); // QuotesRequest | 

try {
    final result = api_instance.getQuotesPost(quotesRequest);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getQuotesPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **quotesRequest** | [**QuotesRequest**](QuotesRequest.md)|  | 

### Return type

[**Map<String, Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getSymbolsForExchange**
> List<Object> getSymbolsForExchange(exchange)

Get symbols for a specific exchange

Retrieves all available trading symbols for a specific exchange

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();
final exchange = exchange_example; // String | 

try {
    final result = api_instance.getSymbolsForExchange(exchange);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->getSymbolsForExchange: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **exchange** | **String**|  | 

### Return type

[**List<Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **logout**
> Map<String, Object> logout()

Logout and invalidate session

Invalidates the current broker session and clears authentication tokens

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataApi();

try {
    final result = api_instance.logout();
    print(result);
} catch (e) {
    print('Exception when calling MarketDataApi->logout: $e\n');
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Map<String, Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

