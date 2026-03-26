# am_market_client.api.MarketDataStreamApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**connect**](MarketDataStreamApi.md#connect) | **POST** /v1/market-data/stream/connect | Connect to market data stream
[**disconnect**](MarketDataStreamApi.md#disconnect) | **POST** /v1/market-data/stream/disconnect | Disconnect market data stream
[**initiate**](MarketDataStreamApi.md#initiate) | **POST** /v1/market-data/stream/initiate | Connect to market data stream


# **connect**
> String connect(streamConnectRequest)

Connect to market data stream

Initiates a WebSocket connection for the specified provider and instruments

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataStreamApi();
final streamConnectRequest = StreamConnectRequest(); // StreamConnectRequest | 

try {
    final result = api_instance.connect(streamConnectRequest);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataStreamApi->connect: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **streamConnectRequest** | [**StreamConnectRequest**](StreamConnectRequest.md)|  | 

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **disconnect**
> String disconnect(provider)

Disconnect market data stream

Disconnects the WebSocket stream for the specified provider

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataStreamApi();
final provider = provider_example; // String | 

try {
    final result = api_instance.disconnect(provider);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataStreamApi->disconnect: $e\n');
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

# **initiate**
> StreamConnectResponse initiate(streamConnectRequest)

Connect to market data stream

Initiates a WebSocket connection and returns a structured response

### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = MarketDataStreamApi();
final streamConnectRequest = StreamConnectRequest(); // StreamConnectRequest | 

try {
    final result = api_instance.initiate(streamConnectRequest);
    print(result);
} catch (e) {
    print('Exception when calling MarketDataStreamApi->initiate: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **streamConnectRequest** | [**StreamConnectRequest**](StreamConnectRequest.md)|  | 

### Return type

[**StreamConnectResponse**](StreamConnectResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

