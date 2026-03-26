# am_market_client.api.BrokerageCalculatorApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**calculateBreakeven**](BrokerageCalculatorApi.md#calculatebreakeven) | **GET** /v1/brokerage/breakeven | Calculate breakeven price
[**calculateBrokerage**](BrokerageCalculatorApi.md#calculatebrokerage) | **POST** /v1/brokerage/calculate | Calculate brokerage and taxes
[**calculateBrokerageAsync**](BrokerageCalculatorApi.md#calculatebrokerageasync) | **POST** /v1/brokerage/calculate-async | Calculate brokerage and taxes asynchronously


# **calculateBreakeven**
> BrokerageCalculationResponse calculateBreakeven(symbol, price, quantity, exchange, tradeType, brokerType)

Calculate breakeven price

Calculate the breakeven price for a stock considering all charges

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = BrokerageCalculatorApi();
final symbol = symbol_example; // String | 
final price = 1.2; // double | 
final quantity = 56; // int | 
final exchange = exchange_example; // String | 
final tradeType = tradeType_example; // String | 
final brokerType = brokerType_example; // String | 

try {
    final result = api_instance.calculateBreakeven(symbol, price, quantity, exchange, tradeType, brokerType);
    print(result);
} catch (e) {
    print('Exception when calling BrokerageCalculatorApi->calculateBreakeven: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **String**|  | 
 **price** | **double**|  | 
 **quantity** | **int**|  | 
 **exchange** | **String**|  | 
 **tradeType** | **String**|  | 
 **brokerType** | **String**|  | 

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **calculateBrokerage**
> BrokerageCalculationResponse calculateBrokerage(brokerageCalculationRequest)

Calculate brokerage and taxes

Calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = BrokerageCalculatorApi();
final brokerageCalculationRequest = BrokerageCalculationRequest(); // BrokerageCalculationRequest | 

try {
    final result = api_instance.calculateBrokerage(brokerageCalculationRequest);
    print(result);
} catch (e) {
    print('Exception when calling BrokerageCalculatorApi->calculateBrokerage: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **brokerageCalculationRequest** | [**BrokerageCalculationRequest**](BrokerageCalculationRequest.md)|  | 

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **calculateBrokerageAsync**
> BrokerageCalculationResponse calculateBrokerageAsync(brokerageCalculationRequest)

Calculate brokerage and taxes asynchronously

Asynchronously calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = BrokerageCalculatorApi();
final brokerageCalculationRequest = BrokerageCalculationRequest(); // BrokerageCalculationRequest | 

try {
    final result = api_instance.calculateBrokerageAsync(brokerageCalculationRequest);
    print(result);
} catch (e) {
    print('Exception when calling BrokerageCalculatorApi->calculateBrokerageAsync: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **brokerageCalculationRequest** | [**BrokerageCalculationRequest**](BrokerageCalculationRequest.md)|  | 

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

