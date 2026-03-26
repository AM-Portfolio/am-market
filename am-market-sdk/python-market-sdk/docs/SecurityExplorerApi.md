# am_market_client.SecurityExplorerApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**search**](SecurityExplorerApi.md#search) | **GET** /v1/securities/search | Search securities by symbol or ISIN
[**search_advanced**](SecurityExplorerApi.md#search_advanced) | **POST** /v1/securities/search | Advanced search securities with filters


# **search**
> List[SecurityDocument] search(query)

Search securities by symbol or ISIN

### Example


```python
import am_market_client
from am_market_client.models.security_document import SecurityDocument
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
    api_instance = am_market_client.SecurityExplorerApi(api_client)
    query = 'query_example' # str | 

    try:
        # Search securities by symbol or ISIN
        api_response = api_instance.search(query)
        print("The response of SecurityExplorerApi->search:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SecurityExplorerApi->search: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **query** | **str**|  | 

### Return type

[**List[SecurityDocument]**](SecurityDocument.md)

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

# **search_advanced**
> List[SecurityDocument] search_advanced(security_search_request)

Advanced search securities with filters

### Example


```python
import am_market_client
from am_market_client.models.security_document import SecurityDocument
from am_market_client.models.security_search_request import SecuritySearchRequest
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
    api_instance = am_market_client.SecurityExplorerApi(api_client)
    security_search_request = am_market_client.SecuritySearchRequest() # SecuritySearchRequest | 

    try:
        # Advanced search securities with filters
        api_response = api_instance.search_advanced(security_search_request)
        print("The response of SecurityExplorerApi->search_advanced:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling SecurityExplorerApi->search_advanced: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **security_search_request** | [**SecuritySearchRequest**](SecuritySearchRequest.md)|  | 

### Return type

[**List[SecurityDocument]**](SecurityDocument.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

