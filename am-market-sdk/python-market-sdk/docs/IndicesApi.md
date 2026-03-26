# am_market_client.IndicesApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_available_indices**](IndicesApi.md#get_available_indices) | **GET** /v1/indices/available | Get available indices
[**get_latest_indices_data**](IndicesApi.md#get_latest_indices_data) | **POST** /v1/indices/batch | Get latest market data for multiple indices


# **get_available_indices**
> str get_available_indices()

Get available indices

Retrieves the list of available indices (Broad and Sector)

### Example


```python
import am_market_client
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
    api_instance = am_market_client.IndicesApi(api_client)

    try:
        # Get available indices
        api_response = api_instance.get_available_indices()
        print("The response of IndicesApi->get_available_indices:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling IndicesApi->get_available_indices: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

**str**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Indices retrieved successfully |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_latest_indices_data**
> StockIndicesMarketData get_latest_indices_data(request_body, force_refresh=force_refresh)

Get latest market data for multiple indices

Retrieves the latest market data for multiple indices in a single request

### Example


```python
import am_market_client
from am_market_client.models.stock_indices_market_data import StockIndicesMarketData
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
    api_instance = am_market_client.IndicesApi(api_client)
    request_body = ['request_body_example'] # List[str] | 
    force_refresh = False # bool | Force refresh from source instead of using cache (optional) (default to False)

    try:
        # Get latest market data for multiple indices
        api_response = api_instance.get_latest_indices_data(request_body, force_refresh=force_refresh)
        print("The response of IndicesApi->get_latest_indices_data:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling IndicesApi->get_latest_indices_data: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **request_body** | [**List[str]**](str.md)|  | 
 **force_refresh** | **bool**| Force refresh from source instead of using cache | [optional] [default to False]

### Return type

[**StockIndicesMarketData**](StockIndicesMarketData.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Indices data retrieved successfully |  -  |
**400** | Invalid request parameters |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

