# am_market_client.api.IndicesApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getAvailableIndices**](IndicesApi.md#getavailableindices) | **GET** /v1/indices/available | Get available indices
[**getLatestIndicesData**](IndicesApi.md#getlatestindicesdata) | **POST** /v1/indices/batch | Get latest market data for multiple indices


# **getAvailableIndices**
> String getAvailableIndices()

Get available indices

Retrieves the list of available indices (Broad and Sector)

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = IndicesApi();

try {
    final result = api_instance.getAvailableIndices();
    print(result);
} catch (e) {
    print('Exception when calling IndicesApi->getAvailableIndices: $e\n');
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getLatestIndicesData**
> StockIndicesMarketData getLatestIndicesData(requestBody, forceRefresh)

Get latest market data for multiple indices

Retrieves the latest market data for multiple indices in a single request

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = IndicesApi();
final requestBody = [List<String>()]; // List<String> | 
final forceRefresh = true; // bool | Force refresh from source instead of using cache

try {
    final result = api_instance.getLatestIndicesData(requestBody, forceRefresh);
    print(result);
} catch (e) {
    print('Exception when calling IndicesApi->getLatestIndicesData: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **requestBody** | [**List<String>**](String.md)|  | 
 **forceRefresh** | **bool**| Force refresh from source instead of using cache | [optional] [default to false]

### Return type

[**StockIndicesMarketData**](StockIndicesMarketData.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

