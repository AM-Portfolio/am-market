# am_market_client.api.SecurityExplorerApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**search**](SecurityExplorerApi.md#search) | **GET** /v1/securities/search | Search securities by symbol or ISIN
[**searchAdvanced**](SecurityExplorerApi.md#searchadvanced) | **POST** /v1/securities/search | Advanced search securities with filters


# **search**
> List<SecurityDocument> search(query)

Search securities by symbol or ISIN

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = SecurityExplorerApi();
final query = query_example; // String | 

try {
    final result = api_instance.search(query);
    print(result);
} catch (e) {
    print('Exception when calling SecurityExplorerApi->search: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **query** | **String**|  | 

### Return type

[**List<SecurityDocument>**](SecurityDocument.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **searchAdvanced**
> List<SecurityDocument> searchAdvanced(securitySearchRequest)

Advanced search securities with filters

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = SecurityExplorerApi();
final securitySearchRequest = SecuritySearchRequest(); // SecuritySearchRequest | 

try {
    final result = api_instance.searchAdvanced(securitySearchRequest);
    print(result);
} catch (e) {
    print('Exception when calling SecurityExplorerApi->searchAdvanced: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **securitySearchRequest** | [**SecuritySearchRequest**](SecuritySearchRequest.md)|  | 

### Return type

[**List<SecurityDocument>**](SecurityDocument.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

