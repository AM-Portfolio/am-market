# am_parser_client.ETFHoldingsApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**fetch_all_etf_holdings_v1_fetch_all_holdings_post**](ETFHoldingsApi.md#fetch_all_etf_holdings_v1_fetch_all_holdings_post) | **POST** /v1/fetch-all-holdings | Fetch All Etf Holdings
[**fetch_holdings_for_etf_v1_fetch_holdings_symbol_post**](ETFHoldingsApi.md#fetch_holdings_for_etf_v1_fetch_holdings_symbol_post) | **POST** /v1/fetch-holdings/{symbol} | Fetch Holdings For Etf
[**get_cache_statistics_v1_cache_stats_get**](ETFHoldingsApi.md#get_cache_statistics_v1_cache_stats_get) | **GET** /v1/cache-stats | Get Cache Statistics
[**get_etf_holdings_v1_holdings_symbol_get**](ETFHoldingsApi.md#get_etf_holdings_v1_holdings_symbol_get) | **GET** /v1/holdings/{symbol} | Get Etf Holdings
[**get_etf_stats_v1_stats_get**](ETFHoldingsApi.md#get_etf_stats_v1_stats_get) | **GET** /v1/stats | Get Etf Stats
[**load_etfs_from_json_v1_load_from_json_post**](ETFHoldingsApi.md#load_etfs_from_json_v1_load_from_json_post) | **POST** /v1/load-from-json | Load Etfs From Json
[**search_etfs_v1_search_get**](ETFHoldingsApi.md#search_etfs_v1_search_get) | **GET** /v1/search | Search Etfs


# **fetch_all_etf_holdings_v1_fetch_all_holdings_post**
> JobResponse fetch_all_etf_holdings_v1_fetch_all_holdings_post(callback_url=callback_url, user_id=user_id, limit=limit, force_refresh=force_refresh)

Fetch All Etf Holdings

Fetch holdings for all ETFs with ISINs from moneycontrol API
Returns immediately with job ID, processes in background
Smart caching: Only fetches if data is missing or stale

### Example


```python
import am_parser_client
from am_parser_client.models.job_response import JobResponse
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.ETFHoldingsApi(api_client)
    callback_url = 'callback_url_example' # str |  (optional)
    user_id = 'user_id_example' # str |  (optional)
    limit = 56 # int | Limit number of ETFs to process (optional)
    force_refresh = False # bool | Force refresh even if data exists for today (optional) (default to False)

    try:
        # Fetch All Etf Holdings
        api_response = api_instance.fetch_all_etf_holdings_v1_fetch_all_holdings_post(callback_url=callback_url, user_id=user_id, limit=limit, force_refresh=force_refresh)
        print("The response of ETFHoldingsApi->fetch_all_etf_holdings_v1_fetch_all_holdings_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ETFHoldingsApi->fetch_all_etf_holdings_v1_fetch_all_holdings_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **callback_url** | **str**|  | [optional] 
 **user_id** | **str**|  | [optional] 
 **limit** | **int**| Limit number of ETFs to process | [optional] 
 **force_refresh** | **bool**| Force refresh even if data exists for today | [optional] [default to False]

### Return type

[**JobResponse**](JobResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **fetch_holdings_for_etf_v1_fetch_holdings_symbol_post**
> object fetch_holdings_for_etf_v1_fetch_holdings_symbol_post(symbol, callback_url=callback_url, user_id=user_id)

Fetch Holdings For Etf

Fetch holdings for a specific ETF by symbol
Returns immediately with job ID, processes in background

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.ETFHoldingsApi(api_client)
    symbol = 'symbol_example' # str | 
    callback_url = 'callback_url_example' # str |  (optional)
    user_id = 'user_id_example' # str |  (optional)

    try:
        # Fetch Holdings For Etf
        api_response = api_instance.fetch_holdings_for_etf_v1_fetch_holdings_symbol_post(symbol, callback_url=callback_url, user_id=user_id)
        print("The response of ETFHoldingsApi->fetch_holdings_for_etf_v1_fetch_holdings_symbol_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ETFHoldingsApi->fetch_holdings_for_etf_v1_fetch_holdings_symbol_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **str**|  | 
 **callback_url** | **str**|  | [optional] 
 **user_id** | **str**|  | [optional] 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_cache_statistics_v1_cache_stats_get**
> object get_cache_statistics_v1_cache_stats_get()

Get Cache Statistics

Get ETF holdings cache statistics

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.ETFHoldingsApi(api_client)

    try:
        # Get Cache Statistics
        api_response = api_instance.get_cache_statistics_v1_cache_stats_get()
        print("The response of ETFHoldingsApi->get_cache_statistics_v1_cache_stats_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ETFHoldingsApi->get_cache_statistics_v1_cache_stats_get: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_etf_holdings_v1_holdings_symbol_get**
> object get_etf_holdings_v1_holdings_symbol_get(symbol)

Get Etf Holdings

Get stored holdings for a specific ETF

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.ETFHoldingsApi(api_client)
    symbol = 'symbol_example' # str | 

    try:
        # Get Etf Holdings
        api_response = api_instance.get_etf_holdings_v1_holdings_symbol_get(symbol)
        print("The response of ETFHoldingsApi->get_etf_holdings_v1_holdings_symbol_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ETFHoldingsApi->get_etf_holdings_v1_holdings_symbol_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **str**|  | 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_etf_stats_v1_stats_get**
> object get_etf_stats_v1_stats_get()

Get Etf Stats

Get ETF database statistics

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.ETFHoldingsApi(api_client)

    try:
        # Get Etf Stats
        api_response = api_instance.get_etf_stats_v1_stats_get()
        print("The response of ETFHoldingsApi->get_etf_stats_v1_stats_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ETFHoldingsApi->get_etf_stats_v1_stats_get: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **load_etfs_from_json_v1_load_from_json_post**
> object load_etfs_from_json_v1_load_from_json_post(file, dry_run=dry_run)

Load Etfs From Json

Load ETF data from JSON file
Accepts etf_details.json and loads all ETFs into database

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.ETFHoldingsApi(api_client)
    file = None # bytearray | ETF details JSON file
    dry_run = False # bool | Validate only, don't persist (optional) (default to False)

    try:
        # Load Etfs From Json
        api_response = api_instance.load_etfs_from_json_v1_load_from_json_post(file, dry_run=dry_run)
        print("The response of ETFHoldingsApi->load_etfs_from_json_v1_load_from_json_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ETFHoldingsApi->load_etfs_from_json_v1_load_from_json_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **bytearray**| ETF details JSON file | 
 **dry_run** | **bool**| Validate only, don&#39;t persist | [optional] [default to False]

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **search_etfs_v1_search_get**
> object search_etfs_v1_search_get(query, limit=limit)

Search Etfs

Search ETFs by symbol, name, or ISIN

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.ETFHoldingsApi(api_client)
    query = 'query_example' # str | Search by symbol, name, or ISIN
    limit = 10 # int | Maximum results to return (optional) (default to 10)

    try:
        # Search Etfs
        api_response = api_instance.search_etfs_v1_search_get(query, limit=limit)
        print("The response of ETFHoldingsApi->search_etfs_v1_search_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling ETFHoldingsApi->search_etfs_v1_search_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **query** | **str**| Search by symbol, name, or ISIN | 
 **limit** | **int**| Maximum results to return | [optional] [default to 10]

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

