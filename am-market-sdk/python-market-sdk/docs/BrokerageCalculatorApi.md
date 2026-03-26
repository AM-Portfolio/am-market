# am_market_client.BrokerageCalculatorApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**calculate_breakeven**](BrokerageCalculatorApi.md#calculate_breakeven) | **GET** /v1/brokerage/breakeven | Calculate breakeven price
[**calculate_brokerage**](BrokerageCalculatorApi.md#calculate_brokerage) | **POST** /v1/brokerage/calculate | Calculate brokerage and taxes
[**calculate_brokerage_async**](BrokerageCalculatorApi.md#calculate_brokerage_async) | **POST** /v1/brokerage/calculate-async | Calculate brokerage and taxes asynchronously


# **calculate_breakeven**
> BrokerageCalculationResponse calculate_breakeven(symbol, price, quantity, exchange, trade_type, broker_type)

Calculate breakeven price

Calculate the breakeven price for a stock considering all charges

### Example


```python
import am_market_client
from am_market_client.models.brokerage_calculation_response import BrokerageCalculationResponse
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
    api_instance = am_market_client.BrokerageCalculatorApi(api_client)
    symbol = 'symbol_example' # str | 
    price = 3.4 # float | 
    quantity = 56 # int | 
    exchange = 'exchange_example' # str | 
    trade_type = 'trade_type_example' # str | 
    broker_type = 'broker_type_example' # str | 

    try:
        # Calculate breakeven price
        api_response = api_instance.calculate_breakeven(symbol, price, quantity, exchange, trade_type, broker_type)
        print("The response of BrokerageCalculatorApi->calculate_breakeven:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BrokerageCalculatorApi->calculate_breakeven: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **str**|  | 
 **price** | **float**|  | 
 **quantity** | **int**|  | 
 **exchange** | **str**|  | 
 **trade_type** | **str**|  | 
 **broker_type** | **str**|  | 

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Breakeven calculation successful |  -  |
**400** | Invalid request parameters |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **calculate_brokerage**
> BrokerageCalculationResponse calculate_brokerage(brokerage_calculation_request)

Calculate brokerage and taxes

Calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade

### Example


```python
import am_market_client
from am_market_client.models.brokerage_calculation_request import BrokerageCalculationRequest
from am_market_client.models.brokerage_calculation_response import BrokerageCalculationResponse
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
    api_instance = am_market_client.BrokerageCalculatorApi(api_client)
    brokerage_calculation_request = am_market_client.BrokerageCalculationRequest() # BrokerageCalculationRequest | 

    try:
        # Calculate brokerage and taxes
        api_response = api_instance.calculate_brokerage(brokerage_calculation_request)
        print("The response of BrokerageCalculatorApi->calculate_brokerage:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BrokerageCalculatorApi->calculate_brokerage: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **brokerage_calculation_request** | [**BrokerageCalculationRequest**](BrokerageCalculationRequest.md)|  | 

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**400** | Invalid request parameters |  -  |
**500** | Internal server error |  -  |
**200** | Brokerage calculation successful |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **calculate_brokerage_async**
> BrokerageCalculationResponse calculate_brokerage_async(brokerage_calculation_request)

Calculate brokerage and taxes asynchronously

Asynchronously calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade

### Example


```python
import am_market_client
from am_market_client.models.brokerage_calculation_request import BrokerageCalculationRequest
from am_market_client.models.brokerage_calculation_response import BrokerageCalculationResponse
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
    api_instance = am_market_client.BrokerageCalculatorApi(api_client)
    brokerage_calculation_request = am_market_client.BrokerageCalculationRequest() # BrokerageCalculationRequest | 

    try:
        # Calculate brokerage and taxes asynchronously
        api_response = api_instance.calculate_brokerage_async(brokerage_calculation_request)
        print("The response of BrokerageCalculatorApi->calculate_brokerage_async:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BrokerageCalculatorApi->calculate_brokerage_async: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **brokerage_calculation_request** | [**BrokerageCalculationRequest**](BrokerageCalculationRequest.md)|  | 

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**400** | Invalid request parameters |  -  |
**500** | Internal server error |  -  |
**200** | Brokerage calculation successful |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

