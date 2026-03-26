# am_market_client.MarketDataStreamApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**connect**](MarketDataStreamApi.md#connect) | **POST** /v1/market-data/stream/connect | Connect to market data stream
[**disconnect**](MarketDataStreamApi.md#disconnect) | **POST** /v1/market-data/stream/disconnect | Disconnect market data stream
[**initiate**](MarketDataStreamApi.md#initiate) | **POST** /v1/market-data/stream/initiate | Connect to market data stream


# **connect**
> str connect(stream_connect_request)

Connect to market data stream

Initiates a WebSocket connection for the specified provider and instruments

### Example


```python
import am_market_client
from am_market_client.models.stream_connect_request import StreamConnectRequest
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
    api_instance = am_market_client.MarketDataStreamApi(api_client)
    stream_connect_request = am_market_client.StreamConnectRequest() # StreamConnectRequest | 

    try:
        # Connect to market data stream
        api_response = api_instance.connect(stream_connect_request)
        print("The response of MarketDataStreamApi->connect:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataStreamApi->connect: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **stream_connect_request** | [**StreamConnectRequest**](StreamConnectRequest.md)|  | 

### Return type

**str**

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

# **disconnect**
> str disconnect(provider)

Disconnect market data stream

Disconnects the WebSocket stream for the specified provider

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
    api_instance = am_market_client.MarketDataStreamApi(api_client)
    provider = 'provider_example' # str | 

    try:
        # Disconnect market data stream
        api_response = api_instance.disconnect(provider)
        print("The response of MarketDataStreamApi->disconnect:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataStreamApi->disconnect: %s\n" % e)
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

# **initiate**
> StreamConnectResponse initiate(stream_connect_request)

Connect to market data stream

Initiates a WebSocket connection and returns a structured response

### Example


```python
import am_market_client
from am_market_client.models.stream_connect_request import StreamConnectRequest
from am_market_client.models.stream_connect_response import StreamConnectResponse
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
    api_instance = am_market_client.MarketDataStreamApi(api_client)
    stream_connect_request = am_market_client.StreamConnectRequest() # StreamConnectRequest | 

    try:
        # Connect to market data stream
        api_response = api_instance.initiate(stream_connect_request)
        print("The response of MarketDataStreamApi->initiate:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataStreamApi->initiate: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **stream_connect_request** | [**StreamConnectRequest**](StreamConnectRequest.md)|  | 

### Return type

[**StreamConnectResponse**](StreamConnectResponse.md)

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

