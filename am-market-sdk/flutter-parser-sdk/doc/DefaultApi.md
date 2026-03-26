# am_parser_client.api.DefaultApi

## Load the API package
```dart
import 'package:am_parser_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**healthCheckHealthGet**](DefaultApi.md#healthcheckhealthget) | **GET** /health | Health Check
[**rootGet**](DefaultApi.md#rootget) | **GET** / | Root


# **healthCheckHealthGet**
> Object healthCheckHealthGet()

Health Check

Health check endpoint

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = DefaultApi();

try {
    final result = api_instance.healthCheckHealthGet();
    print(result);
} catch (e) {
    print('Exception when calling DefaultApi->healthCheckHealthGet: $e\n');
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

# **rootGet**
> Object rootGet()

Root

Root endpoint with API information

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = DefaultApi();

try {
    final result = api_instance.rootGet();
    print(result);
} catch (e) {
    print('Exception when calling DefaultApi->rootGet: $e\n');
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

