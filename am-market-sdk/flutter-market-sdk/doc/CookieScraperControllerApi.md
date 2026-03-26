# am_market_client.api.CookieScraperControllerApi

## Load the API package
```dart
import 'package:am_market_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**scrapeCookies**](CookieScraperControllerApi.md#scrapecookies) | **GET** /api/scraper/cookies | 


# **scrapeCookies**
> List<WebsiteCookies> scrapeCookies()



### Example
```dart
import 'package:am_market_client/api.dart';

final api_instance = CookieScraperControllerApi();

try {
    final result = api_instance.scrapeCookies();
    print(result);
} catch (e) {
    print('Exception when calling CookieScraperControllerApi->scrapeCookies: $e\n');
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**List<WebsiteCookies>**](WebsiteCookies.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

