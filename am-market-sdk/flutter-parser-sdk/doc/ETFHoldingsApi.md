# am_parser_client.api.ETFHoldingsApi

## Load the API package
```dart
import 'package:am_parser_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**fetchAllEtfHoldingsV1FetchAllHoldingsPost**](ETFHoldingsApi.md#fetchalletfholdingsv1fetchallholdingspost) | **POST** /v1/fetch-all-holdings | Fetch All Etf Holdings
[**fetchHoldingsForEtfV1FetchHoldingsSymbolPost**](ETFHoldingsApi.md#fetchholdingsforetfv1fetchholdingssymbolpost) | **POST** /v1/fetch-holdings/{symbol} | Fetch Holdings For Etf
[**getCacheStatisticsV1CacheStatsGet**](ETFHoldingsApi.md#getcachestatisticsv1cachestatsget) | **GET** /v1/cache-stats | Get Cache Statistics
[**getEtfHoldingsV1HoldingsSymbolGet**](ETFHoldingsApi.md#getetfholdingsv1holdingssymbolget) | **GET** /v1/holdings/{symbol} | Get Etf Holdings
[**getEtfStatsV1StatsGet**](ETFHoldingsApi.md#getetfstatsv1statsget) | **GET** /v1/stats | Get Etf Stats
[**loadEtfsFromJsonV1LoadFromJsonPost**](ETFHoldingsApi.md#loadetfsfromjsonv1loadfromjsonpost) | **POST** /v1/load-from-json | Load Etfs From Json
[**searchEtfsV1SearchGet**](ETFHoldingsApi.md#searchetfsv1searchget) | **GET** /v1/search | Search Etfs


# **fetchAllEtfHoldingsV1FetchAllHoldingsPost**
> JobResponse fetchAllEtfHoldingsV1FetchAllHoldingsPost(callbackUrl, userId, limit, forceRefresh)

Fetch All Etf Holdings

Fetch holdings for all ETFs with ISINs from moneycontrol API Returns immediately with job ID, processes in background Smart caching: Only fetches if data is missing or stale

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = ETFHoldingsApi();
final callbackUrl = callbackUrl_example; // String | 
final userId = userId_example; // String | 
final limit = 56; // int | Limit number of ETFs to process
final forceRefresh = true; // bool | Force refresh even if data exists for today

try {
    final result = api_instance.fetchAllEtfHoldingsV1FetchAllHoldingsPost(callbackUrl, userId, limit, forceRefresh);
    print(result);
} catch (e) {
    print('Exception when calling ETFHoldingsApi->fetchAllEtfHoldingsV1FetchAllHoldingsPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **callbackUrl** | **String**|  | [optional] 
 **userId** | **String**|  | [optional] 
 **limit** | **int**| Limit number of ETFs to process | [optional] 
 **forceRefresh** | **bool**| Force refresh even if data exists for today | [optional] [default to false]

### Return type

[**JobResponse**](JobResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **fetchHoldingsForEtfV1FetchHoldingsSymbolPost**
> Object fetchHoldingsForEtfV1FetchHoldingsSymbolPost(symbol, callbackUrl, userId)

Fetch Holdings For Etf

Fetch holdings for a specific ETF by symbol Returns immediately with job ID, processes in background

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = ETFHoldingsApi();
final symbol = symbol_example; // String | 
final callbackUrl = callbackUrl_example; // String | 
final userId = userId_example; // String | 

try {
    final result = api_instance.fetchHoldingsForEtfV1FetchHoldingsSymbolPost(symbol, callbackUrl, userId);
    print(result);
} catch (e) {
    print('Exception when calling ETFHoldingsApi->fetchHoldingsForEtfV1FetchHoldingsSymbolPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **String**|  | 
 **callbackUrl** | **String**|  | [optional] 
 **userId** | **String**|  | [optional] 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getCacheStatisticsV1CacheStatsGet**
> Object getCacheStatisticsV1CacheStatsGet()

Get Cache Statistics

Get ETF holdings cache statistics

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = ETFHoldingsApi();

try {
    final result = api_instance.getCacheStatisticsV1CacheStatsGet();
    print(result);
} catch (e) {
    print('Exception when calling ETFHoldingsApi->getCacheStatisticsV1CacheStatsGet: $e\n');
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getEtfHoldingsV1HoldingsSymbolGet**
> Object getEtfHoldingsV1HoldingsSymbolGet(symbol)

Get Etf Holdings

Get stored holdings for a specific ETF

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = ETFHoldingsApi();
final symbol = symbol_example; // String | 

try {
    final result = api_instance.getEtfHoldingsV1HoldingsSymbolGet(symbol);
    print(result);
} catch (e) {
    print('Exception when calling ETFHoldingsApi->getEtfHoldingsV1HoldingsSymbolGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getEtfStatsV1StatsGet**
> Object getEtfStatsV1StatsGet()

Get Etf Stats

Get ETF database statistics

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = ETFHoldingsApi();

try {
    final result = api_instance.getEtfStatsV1StatsGet();
    print(result);
} catch (e) {
    print('Exception when calling ETFHoldingsApi->getEtfStatsV1StatsGet: $e\n');
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **loadEtfsFromJsonV1LoadFromJsonPost**
> Object loadEtfsFromJsonV1LoadFromJsonPost(file, dryRun)

Load Etfs From Json

Load ETF data from JSON file Accepts etf_details.json and loads all ETFs into database

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = ETFHoldingsApi();
final file = BINARY_DATA_HERE; // MultipartFile | ETF details JSON file
final dryRun = true; // bool | Validate only, don't persist

try {
    final result = api_instance.loadEtfsFromJsonV1LoadFromJsonPost(file, dryRun);
    print(result);
} catch (e) {
    print('Exception when calling ETFHoldingsApi->loadEtfsFromJsonV1LoadFromJsonPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **MultipartFile**| ETF details JSON file | 
 **dryRun** | **bool**| Validate only, don't persist | [optional] [default to false]

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **searchEtfsV1SearchGet**
> Object searchEtfsV1SearchGet(query, limit)

Search Etfs

Search ETFs by symbol, name, or ISIN

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = ETFHoldingsApi();
final query = query_example; // String | Search by symbol, name, or ISIN
final limit = 56; // int | Maximum results to return

try {
    final result = api_instance.searchEtfsV1SearchGet(query, limit);
    print(result);
} catch (e) {
    print('Exception when calling ETFHoldingsApi->searchEtfsV1SearchGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **query** | **String**| Search by symbol, name, or ISIN | 
 **limit** | **int**| Maximum results to return | [optional] [default to 10]

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

