# am_market_client.InstrumentManagementApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**search_instruments**](InstrumentManagementApi.md#search_instruments) | **POST** /v1/instruments/search | Search Instruments
[**update_instruments**](InstrumentManagementApi.md#update_instruments) | **POST** /v1/instruments/update | Update Instruments from File


# **search_instruments**
> List[object] search_instruments(instrument_search_criteria)

Search Instruments

Search for instruments using criteria: list of symbols ('gym balls'), exchanges, and instrument types. Supports semantic text search combined with filters.

### Example


```python
import am_market_client
from am_market_client.models.instrument_search_criteria import InstrumentSearchCriteria
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
    api_instance = am_market_client.InstrumentManagementApi(api_client)
    instrument_search_criteria = am_market_client.InstrumentSearchCriteria() # InstrumentSearchCriteria | 

    try:
        # Search Instruments
        api_response = api_instance.search_instruments(instrument_search_criteria)
        print("The response of InstrumentManagementApi->search_instruments:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling InstrumentManagementApi->search_instruments: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **instrument_search_criteria** | [**InstrumentSearchCriteria**](InstrumentSearchCriteria.md)|  | 

### Return type

**List[object]**

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

# **update_instruments**
> str update_instruments(file_path=file_path, provider=provider)

Update Instruments from File

Triggers an update of instruments from the local JSON file (NSE.json). Uses streaming to handle large files.

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
    api_instance = am_market_client.InstrumentManagementApi(api_client)
    file_path = 'file_path_example' # str |  (optional)
    provider = 'UPSTOX' # str |  (optional) (default to 'UPSTOX')

    try:
        # Update Instruments from File
        api_response = api_instance.update_instruments(file_path=file_path, provider=provider)
        print("The response of InstrumentManagementApi->update_instruments:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling InstrumentManagementApi->update_instruments: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file_path** | **str**|  | [optional] 
 **provider** | **str**|  | [optional] [default to &#39;UPSTOX&#39;]

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

