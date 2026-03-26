# am_market_client.api.MarketDataAdminControllerApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getJobDetails**](MarketDataAdminControllerApi.md#getjobdetails) | **GET** /v1/admin/logs/{jobId} | 
[**getLogs**](MarketDataAdminControllerApi.md#getlogs) | **GET** /v1/admin/logs | 
[**startIngestion**](MarketDataAdminControllerApi.md#startingestion) | **POST** /v1/admin/ingestion/start | 
[**stopIngestion**](MarketDataAdminControllerApi.md#stopingestion) | **POST** /v1/admin/ingestion/stop | 
[**triggerHistoricalSync**](MarketDataAdminControllerApi.md#triggerhistoricalsync) | **POST** /v1/admin/sync/historical | 


# **getJobDetails**
> IngestionJobLog getJobDetails(jobId)



### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataAdminControllerApi();
final jobId = jobId_example; // String | 

try {
    final result = api_instance.getJobDetails(jobId);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataAdminControllerApi->getJobDetails: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **jobId** | **String**|  | 

### Return type

[**IngestionJobLog**](IngestionJobLog.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getLogs**
> List<IngestionJobLog> getLogs(page, size, startDate, endDate)



### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataAdminControllerApi();
final page = 56; // int | 
final size = 56; // int | 
final startDate = 2013-10-20; // DateTime | 
final endDate = 2013-10-20; // DateTime | 

try {
    final result = api_instance.getLogs(page, size, startDate, endDate);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataAdminControllerApi->getLogs: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **page** | **int**|  | [optional] [default to 0]
 **size** | **int**|  | [optional] [default to 10]
 **startDate** | **DateTime**|  | [optional] 
 **endDate** | **DateTime**|  | [optional] 

### Return type

[**List<IngestionJobLog>**](IngestionJobLog.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **startIngestion**
> String startIngestion(provider, symbols)



### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataAdminControllerApi();
final provider = provider_example; // String | 
final symbols = []; // List<String> | 

try {
    final result = api_instance.startIngestion(provider, symbols);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataAdminControllerApi->startIngestion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provider** | **String**|  | [optional] [default to 'UPSTOX']
 **symbols** | [**List<String>**](String.md)|  | [optional] [default to const []]

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **stopIngestion**
> String stopIngestion(provider)



### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataAdminControllerApi();
final provider = provider_example; // String | 

try {
    final result = api_instance.stopIngestion(provider);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataAdminControllerApi->stopIngestion: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provider** | **String**|  | 

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **triggerHistoricalSync**
> String triggerHistoricalSync(symbol, forceRefresh, fetchIndexStocks)



### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataAdminControllerApi();
final symbol = symbol_example; // String | 
final forceRefresh = true; // bool | 
final fetchIndexStocks = true; // bool | 

try {
    final result = api_instance.triggerHistoricalSync(symbol, forceRefresh, fetchIndexStocks);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataAdminControllerApi->triggerHistoricalSync: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **String**|  | [optional] 
 **forceRefresh** | **bool**|  | [optional] [default to true]
 **fetchIndexStocks** | **bool**|  | [optional] [default to false]

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

