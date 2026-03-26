# am_market_client.CookieScraperControllerApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**scrape_cookies**](CookieScraperControllerApi.md#scrape_cookies) | **GET** /api/scraper/cookies | 


# **scrape_cookies**
> List[WebsiteCookies] scrape_cookies()

### Example


```python
import am_market_client
from am_market_client.models.website_cookies import WebsiteCookies
from am_market_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_market_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_market_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_market_client.CookieScraperControllerApi(api_client)

    try:
        api_response = api_instance.scrape_cookies()
        print("The response of CookieScraperControllerApi->scrape_cookies:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling CookieScraperControllerApi->scrape_cookies: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

[**List[WebsiteCookies]**](WebsiteCookies.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

