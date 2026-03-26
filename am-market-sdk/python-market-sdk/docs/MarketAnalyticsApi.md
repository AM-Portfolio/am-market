# am_market_client.MarketAnalyticsApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_historical_charts**](MarketAnalyticsApi.md#get_historical_charts) | **GET** /v1/market-analytics/historical-charts/{symbol} | Get historical charts data
[**get_movers**](MarketAnalyticsApi.md#get_movers) | **GET** /v1/market-analytics/movers | Get Top Gainers/Losers
[**get_sector_performance**](MarketAnalyticsApi.md#get_sector_performance) | **GET** /v1/market-analytics/sectors | Get Sector Performance


# **get_historical_charts**
> HistoricalDataResponseV1 get_historical_charts(symbol, range=range)

Get historical charts data

Retrieves historical data for charts with various time frames (10m, 1H, 1D, 1W, 1M, 5Y, etc.)

### Example


```python
import am_market_client
from am_market_client.models.historical_data_response_v1 import HistoricalDataResponseV1
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
    api_instance = am_market_client.MarketAnalyticsApi(api_client)
    symbol = 'symbol_example' # str | 
    range = '1D' # str |  (optional) (default to '1D')

    try:
        # Get historical charts data
        api_response = api_instance.get_historical_charts(symbol, range=range)
        print("The response of MarketAnalyticsApi->get_historical_charts:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketAnalyticsApi->get_historical_charts: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **str**|  | 
 **range** | **str**|  | [optional] [default to &#39;1D&#39;]

### Return type

[**HistoricalDataResponseV1**](HistoricalDataResponseV1.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**400** | Invalid request parameters |  -  |
**500** | Internal server error |  -  |
**200** | Chart data retrieved successfully |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_movers**
> List[Dict[str, object]] get_movers(type=type, limit=limit, index_symbol=index_symbol, time_frame=time_frame, expand_indices=expand_indices)

Get Top Gainers/Losers

Retrieves top performing or worst performing stocks from the specified market index

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
    api_instance = am_market_client.MarketAnalyticsApi(api_client)
    type = 'gainers' # str |  (optional) (default to 'gainers')
    limit = 10 # int |  (optional) (default to 10)
    index_symbol = 'index_symbol_example' # str |  (optional)
    time_frame = 'time_frame_example' # str |  (optional)
    expand_indices = False # bool |  (optional) (default to False)

    try:
        # Get Top Gainers/Losers
        api_response = api_instance.get_movers(type=type, limit=limit, index_symbol=index_symbol, time_frame=time_frame, expand_indices=expand_indices)
        print("The response of MarketAnalyticsApi->get_movers:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketAnalyticsApi->get_movers: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **type** | **str**|  | [optional] [default to &#39;gainers&#39;]
 **limit** | **int**|  | [optional] [default to 10]
 **index_symbol** | **str**|  | [optional] 
 **time_frame** | **str**|  | [optional] 
 **expand_indices** | **bool**|  | [optional] [default to False]

### Return type

**List[Dict[str, object]]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Data retrieved successfully |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_sector_performance**
> List[Dict[str, object]] get_sector_performance(index_symbol=index_symbol, time_frame=time_frame, expand_indices=expand_indices)

Get Sector Performance

Aggregates market performance by sector (Industry) from the specified index

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
    api_instance = am_market_client.MarketAnalyticsApi(api_client)
    index_symbol = 'index_symbol_example' # str |  (optional)
    time_frame = 'time_frame_example' # str |  (optional)
    expand_indices = False # bool |  (optional) (default to False)

    try:
        # Get Sector Performance
        api_response = api_instance.get_sector_performance(index_symbol=index_symbol, time_frame=time_frame, expand_indices=expand_indices)
        print("The response of MarketAnalyticsApi->get_sector_performance:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketAnalyticsApi->get_sector_performance: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **index_symbol** | **str**|  | [optional] 
 **time_frame** | **str**|  | [optional] 
 **expand_indices** | **bool**|  | [optional] [default to False]

### Return type

**List[Dict[str, object]]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Data retrieved successfully |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

