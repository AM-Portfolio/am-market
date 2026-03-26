# am_market_client.api.MarketAnalyticsApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getHistoricalCharts**](MarketAnalyticsApi.md#gethistoricalcharts) | **GET** /v1/market-analytics/historical-charts/{symbol} | Get historical charts data
[**getMovers**](MarketAnalyticsApi.md#getmovers) | **GET** /v1/market-analytics/movers | Get Top Gainers/Losers
[**getSectorPerformance**](MarketAnalyticsApi.md#getsectorperformance) | **GET** /v1/market-analytics/sectors | Get Sector Performance


# **getHistoricalCharts**
> HistoricalDataResponseV1 getHistoricalCharts(symbol, range)

Get historical charts data

Retrieves historical data for charts with various time frames (10m, 1H, 1D, 1W, 1M, 5Y, etc.)

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketAnalyticsApi();
final symbol = symbol_example; // String | 
final range = range_example; // String | 

try {
    final result = api_instance.getHistoricalCharts(symbol, range);
    print(result);
} catch (e) {
    print('Exception when calling MarketAnalyticsApi->getHistoricalCharts: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **String**|  | 
 **range** | **String**|  | [optional] [default to '1D']

### Return type

[**HistoricalDataResponseV1**](HistoricalDataResponseV1.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getMovers**
> List<Map<String, Object>> getMovers(type, limit, indexSymbol, timeFrame, expandIndices)

Get Top Gainers/Losers

Retrieves top performing or worst performing stocks from the specified market index

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketAnalyticsApi();
final type = type_example; // String | 
final limit = 56; // int | 
final indexSymbol = indexSymbol_example; // String | 
final timeFrame = timeFrame_example; // String | 
final expandIndices = true; // bool | 

try {
    final result = api_instance.getMovers(type, limit, indexSymbol, timeFrame, expandIndices);
    print(result);
} catch (e) {
    print('Exception when calling MarketAnalyticsApi->getMovers: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **type** | **String**|  | [optional] [default to 'gainers']
 **limit** | **int**|  | [optional] [default to 10]
 **indexSymbol** | **String**|  | [optional] 
 **timeFrame** | **String**|  | [optional] 
 **expandIndices** | **bool**|  | [optional] [default to false]

### Return type

[**List<Map<String, Object>>**](Map.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getSectorPerformance**
> List<Map<String, Object>> getSectorPerformance(indexSymbol, timeFrame, expandIndices)

Get Sector Performance

Aggregates market performance by sector (Industry) from the specified index

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketAnalyticsApi();
final indexSymbol = indexSymbol_example; // String | 
final timeFrame = timeFrame_example; // String | 
final expandIndices = true; // bool | 

try {
    final result = api_instance.getSectorPerformance(indexSymbol, timeFrame, expandIndices);
    print(result);
} catch (e) {
    print('Exception when calling MarketAnalyticsApi->getSectorPerformance: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **indexSymbol** | **String**|  | [optional] 
 **timeFrame** | **String**|  | [optional] 
 **expandIndices** | **bool**|  | [optional] [default to false]

### Return type

[**List<Map<String, Object>>**](Map.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

