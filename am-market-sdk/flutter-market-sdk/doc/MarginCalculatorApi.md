# am_market_client.api.MarginCalculatorApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**calculateMargin**](MarginCalculatorApi.md#calculatemargin) | **POST** /v1/margin/calculate | Calculate margin requirements
[**calculateMarginAsync**](MarginCalculatorApi.md#calculatemarginasync) | **POST** /v1/margin/calculate-async | Calculate margin requirements asynchronously


# **calculateMargin**
> MarginCalculationResponse calculateMargin(marginCalculationRequest)

Calculate margin requirements

Calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarginCalculatorApi();
final marginCalculationRequest = MarginCalculationRequest(); // MarginCalculationRequest | 

try {
    final result = api_instance.calculateMargin(marginCalculationRequest);
    print(result);
} catch (e) {
    print('Exception when calling MarginCalculatorApi->calculateMargin: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **marginCalculationRequest** | [**MarginCalculationRequest**](MarginCalculationRequest.md)|  | 

### Return type

[**MarginCalculationResponse**](MarginCalculationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **calculateMarginAsync**
> MarginCalculationResponse calculateMarginAsync(marginCalculationRequest)

Calculate margin requirements asynchronously

Asynchronously calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarginCalculatorApi();
final marginCalculationRequest = MarginCalculationRequest(); // MarginCalculationRequest | 

try {
    final result = api_instance.calculateMarginAsync(marginCalculationRequest);
    print(result);
} catch (e) {
    print('Exception when calling MarginCalculatorApi->calculateMarginAsync: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **marginCalculationRequest** | [**MarginCalculationRequest**](MarginCalculationRequest.md)|  | 

### Return type

[**MarginCalculationResponse**](MarginCalculationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

