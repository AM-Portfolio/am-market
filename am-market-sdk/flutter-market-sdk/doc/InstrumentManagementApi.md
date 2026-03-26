# am_market_client.api.InstrumentManagementApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**searchInstruments**](InstrumentManagementApi.md#searchinstruments) | **POST** /v1/instruments/search | Search Instruments
[**updateInstruments**](InstrumentManagementApi.md#updateinstruments) | **POST** /v1/instruments/update | Update Instruments from File


# **searchInstruments**
> List<Object> searchInstruments(instrumentSearchCriteria)

Search Instruments

Search for instruments using criteria: list of symbols ('gym balls'), exchanges, and instrument types. Supports semantic text search combined with filters.

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = InstrumentManagementApi();
final instrumentSearchCriteria = InstrumentSearchCriteria(); // InstrumentSearchCriteria | 

try {
    final result = api_instance.searchInstruments(instrumentSearchCriteria);
    print(result);
} catch (e) {
    print('Exception when calling InstrumentManagementApi->searchInstruments: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **instrumentSearchCriteria** | [**InstrumentSearchCriteria**](InstrumentSearchCriteria.md)|  | 

### Return type

[**List<Object>**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateInstruments**
> String updateInstruments(filePath, provider)

Update Instruments from File

Triggers an update of instruments from the local JSON file (NSE.json). Uses streaming to handle large files.

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = InstrumentManagementApi();
final filePath = filePath_example; // String | 
final provider = provider_example; // String | 

try {
    final result = api_instance.updateInstruments(filePath, provider);
    print(result);
} catch (e) {
    print('Exception when calling InstrumentManagementApi->updateInstruments: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **filePath** | **String**|  | [optional] 
 **provider** | **String**|  | [optional] [default to 'UPSTOX']

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

