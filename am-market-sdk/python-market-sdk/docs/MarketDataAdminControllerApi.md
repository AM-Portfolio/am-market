# am_market_client.MarketDataAdminControllerApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_job_details**](MarketDataAdminControllerApi.md#get_job_details) | **GET** /v1/admin/logs/{jobId} | 
[**get_logs**](MarketDataAdminControllerApi.md#get_logs) | **GET** /v1/admin/logs | 
[**start_ingestion**](MarketDataAdminControllerApi.md#start_ingestion) | **POST** /v1/admin/ingestion/start | 
[**stop_ingestion**](MarketDataAdminControllerApi.md#stop_ingestion) | **POST** /v1/admin/ingestion/stop | 
[**trigger_historical_sync**](MarketDataAdminControllerApi.md#trigger_historical_sync) | **POST** /v1/admin/sync/historical | 


# **get_job_details**
> IngestionJobLog get_job_details(job_id)

### Example


```python
import am_market_client
from am_market_client.models.ingestion_job_log import IngestionJobLog
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
    api_instance = am_market_client.MarketDataAdminControllerApi(api_client)
    job_id = 'job_id_example' # str | 

    try:
        api_response = api_instance.get_job_details(job_id)
        print("The response of MarketDataAdminControllerApi->get_job_details:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataAdminControllerApi->get_job_details: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **job_id** | **str**|  | 

### Return type

[**IngestionJobLog**](IngestionJobLog.md)

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

# **get_logs**
> List[IngestionJobLog] get_logs(page=page, size=size, start_date=start_date, end_date=end_date)

### Example


```python
import am_market_client
from am_market_client.models.ingestion_job_log import IngestionJobLog
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
    api_instance = am_market_client.MarketDataAdminControllerApi(api_client)
    page = 0 # int |  (optional) (default to 0)
    size = 10 # int |  (optional) (default to 10)
    start_date = '2013-10-20' # date |  (optional)
    end_date = '2013-10-20' # date |  (optional)

    try:
        api_response = api_instance.get_logs(page=page, size=size, start_date=start_date, end_date=end_date)
        print("The response of MarketDataAdminControllerApi->get_logs:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataAdminControllerApi->get_logs: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **page** | **int**|  | [optional] [default to 0]
 **size** | **int**|  | [optional] [default to 10]
 **start_date** | **date**|  | [optional] 
 **end_date** | **date**|  | [optional] 

### Return type

[**List[IngestionJobLog]**](IngestionJobLog.md)

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

# **start_ingestion**
> str start_ingestion(provider=provider, symbols=symbols)

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
    api_instance = am_market_client.MarketDataAdminControllerApi(api_client)
    provider = 'UPSTOX' # str |  (optional) (default to 'UPSTOX')
    symbols = ["NIFTY 50","NIFTY BANK"] # List[str] |  (optional) (default to ["NIFTY 50","NIFTY BANK"])

    try:
        api_response = api_instance.start_ingestion(provider=provider, symbols=symbols)
        print("The response of MarketDataAdminControllerApi->start_ingestion:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataAdminControllerApi->start_ingestion: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provider** | **str**|  | [optional] [default to &#39;UPSTOX&#39;]
 **symbols** | [**List[str]**](str.md)|  | [optional] [default to [&quot;NIFTY 50&quot;,&quot;NIFTY BANK&quot;]]

### Return type

**str**

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

# **stop_ingestion**
> str stop_ingestion(provider)

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
    api_instance = am_market_client.MarketDataAdminControllerApi(api_client)
    provider = 'provider_example' # str | 

    try:
        api_response = api_instance.stop_ingestion(provider)
        print("The response of MarketDataAdminControllerApi->stop_ingestion:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataAdminControllerApi->stop_ingestion: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provider** | **str**|  | 

### Return type

**str**

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

# **trigger_historical_sync**
> str trigger_historical_sync(symbol=symbol, force_refresh=force_refresh, fetch_index_stocks=fetch_index_stocks)

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
    api_instance = am_market_client.MarketDataAdminControllerApi(api_client)
    symbol = 'symbol_example' # str |  (optional)
    force_refresh = True # bool |  (optional) (default to True)
    fetch_index_stocks = False # bool |  (optional) (default to False)

    try:
        api_response = api_instance.trigger_historical_sync(symbol=symbol, force_refresh=force_refresh, fetch_index_stocks=fetch_index_stocks)
        print("The response of MarketDataAdminControllerApi->trigger_historical_sync:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataAdminControllerApi->trigger_historical_sync: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **str**|  | [optional] 
 **force_refresh** | **bool**|  | [optional] [default to True]
 **fetch_index_stocks** | **bool**|  | [optional] [default to False]

### Return type

**str**

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

