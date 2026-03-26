# am_market_client.MarginCalculatorApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**calculate_margin**](MarginCalculatorApi.md#calculate_margin) | **POST** /v1/margin/calculate | Calculate margin requirements
[**calculate_margin_async**](MarginCalculatorApi.md#calculate_margin_async) | **POST** /v1/margin/calculate-async | Calculate margin requirements asynchronously


# **calculate_margin**
> MarginCalculationResponse calculate_margin(margin_calculation_request)

Calculate margin requirements

Calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments

### Example


```python
import am_market_client
from am_market_client.models.margin_calculation_request import MarginCalculationRequest
from am_market_client.models.margin_calculation_response import MarginCalculationResponse
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
    api_instance = am_market_client.MarginCalculatorApi(api_client)
    margin_calculation_request = am_market_client.MarginCalculationRequest() # MarginCalculationRequest | 

    try:
        # Calculate margin requirements
        api_response = api_instance.calculate_margin(margin_calculation_request)
        print("The response of MarginCalculatorApi->calculate_margin:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarginCalculatorApi->calculate_margin: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **margin_calculation_request** | [**MarginCalculationRequest**](MarginCalculationRequest.md)|  | 

### Return type

[**MarginCalculationResponse**](MarginCalculationResponse.md)

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
**200** | Margin calculation successful |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **calculate_margin_async**
> MarginCalculationResponse calculate_margin_async(margin_calculation_request)

Calculate margin requirements asynchronously

Asynchronously calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments

### Example


```python
import am_market_client
from am_market_client.models.margin_calculation_request import MarginCalculationRequest
from am_market_client.models.margin_calculation_response import MarginCalculationResponse
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
    api_instance = am_market_client.MarginCalculatorApi(api_client)
    margin_calculation_request = am_market_client.MarginCalculationRequest() # MarginCalculationRequest | 

    try:
        # Calculate margin requirements asynchronously
        api_response = api_instance.calculate_margin_async(margin_calculation_request)
        print("The response of MarginCalculatorApi->calculate_margin_async:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarginCalculatorApi->calculate_margin_async: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **margin_calculation_request** | [**MarginCalculationRequest**](MarginCalculationRequest.md)|  | 

### Return type

[**MarginCalculationResponse**](MarginCalculationResponse.md)

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
**200** | Margin calculation successful |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

