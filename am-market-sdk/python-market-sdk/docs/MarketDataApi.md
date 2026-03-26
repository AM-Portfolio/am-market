# am_market_client.MarketDataApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**generate_session**](MarketDataApi.md#generate_session) | **GET** /v1/market-data/auth/session | Generate session from request token
[**get_historical_data**](MarketDataApi.md#get_historical_data) | **POST** /v1/market-data/historical-data | Get historical market data
[**get_live_ltp**](MarketDataApi.md#get_live_ltp) | **GET** /v1/market-data/live-ltp | Get live LTP with change calculation
[**get_live_prices**](MarketDataApi.md#get_live_prices) | **GET** /v1/market-data/live-prices | Get live market prices
[**get_login_url**](MarketDataApi.md#get_login_url) | **GET** /v1/market-data/auth/login-url | Get login URL for broker authentication
[**get_mutual_fund_details**](MarketDataApi.md#get_mutual_fund_details) | **GET** /v1/market-data/mutual-fund/{schemeCode} | Get mutual fund details
[**get_mutual_fund_nav_history**](MarketDataApi.md#get_mutual_fund_nav_history) | **GET** /v1/market-data/mutual-fund/{schemeCode}/history | Get mutual fund NAV history
[**get_ohlc**](MarketDataApi.md#get_ohlc) | **POST** /v1/market-data/ohlc | Get OHLC data for multiple symbols
[**get_option_chain**](MarketDataApi.md#get_option_chain) | **GET** /v1/market-data/option-chain | Get option chain data
[**get_quotes**](MarketDataApi.md#get_quotes) | **GET** /v1/market-data/quotes | Get quotes for multiple symbols
[**get_quotes_post**](MarketDataApi.md#get_quotes_post) | **POST** /v1/market-data/quotes | Get quotes for multiple symbols (POST)
[**get_symbols_for_exchange**](MarketDataApi.md#get_symbols_for_exchange) | **GET** /v1/market-data/symbols/{exchange} | Get symbols for a specific exchange
[**logout**](MarketDataApi.md#logout) | **POST** /v1/market-data/auth/logout | Logout and invalidate session


# **generate_session**
> object generate_session(request_token=request_token, request_token2=request_token2, code=code, status=status)

Generate session from request token

Creates a new authenticated session using the request token obtained from broker login

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
    api_instance = am_market_client.MarketDataApi(api_client)
    request_token = 'request_token_example' # str |  (optional)
    request_token2 = 'request_token_example' # str |  (optional)
    code = 'code_example' # str |  (optional)
    status = 'success' # str |  (optional) (default to 'success')

    try:
        # Generate session from request token
        api_response = api_instance.generate_session(request_token=request_token, request_token2=request_token2, code=code, status=status)
        print("The response of MarketDataApi->generate_session:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->generate_session: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **request_token** | **str**|  | [optional] 
 **request_token2** | **str**|  | [optional] 
 **code** | **str**|  | [optional] 
 **status** | **str**|  | [optional] [default to &#39;success&#39;]

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
**200** | Session generated successfully |  -  |
**500** | Internal server error |  -  |
**400** | Invalid request token or authentication failed |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_historical_data**
> HistoricalDataResponseV1 get_historical_data(historical_data_request)

Get historical market data

Retrieves historical price and volume data for one or more instruments with filtering options

### Example


```python
import am_market_client
from am_market_client.models.historical_data_request import HistoricalDataRequest
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
    api_instance = am_market_client.MarketDataApi(api_client)
    historical_data_request = am_market_client.HistoricalDataRequest() # HistoricalDataRequest | 

    try:
        # Get historical market data
        api_response = api_instance.get_historical_data(historical_data_request)
        print("The response of MarketDataApi->get_historical_data:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_historical_data: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **historical_data_request** | [**HistoricalDataRequest**](HistoricalDataRequest.md)|  | 

### Return type

[**HistoricalDataResponseV1**](HistoricalDataResponseV1.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Historical data retrieved successfully |  -  |
**400** | Invalid request parameters |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_live_ltp**
> Dict[str, object] get_live_ltp(symbols, timeframe=timeframe, is_index_symbol=is_index_symbol, refresh=refresh)

Get live LTP with change calculation

Retrieves current LTP and calculates change based on historical closing price for the specified timeframe

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
    api_instance = am_market_client.MarketDataApi(api_client)
    symbols = 'symbols_example' # str | 
    timeframe = '1D' # str |  (optional) (default to '1D')
    is_index_symbol = True # bool |  (optional) (default to True)
    refresh = False # bool |  (optional) (default to False)

    try:
        # Get live LTP with change calculation
        api_response = api_instance.get_live_ltp(symbols, timeframe=timeframe, is_index_symbol=is_index_symbol, refresh=refresh)
        print("The response of MarketDataApi->get_live_ltp:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_live_ltp: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbols** | **str**|  | 
 **timeframe** | **str**|  | [optional] [default to &#39;1D&#39;]
 **is_index_symbol** | **bool**|  | [optional] [default to True]
 **refresh** | **bool**|  | [optional] [default to False]

### Return type

**Dict[str, object]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**500** | Internal server error |  -  |
**200** | Live LTP with change retrieved successfully |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_live_prices**
> Dict[str, object] get_live_prices(symbols=symbols, is_index_symbol=is_index_symbol, refresh=refresh)

Get live market prices

Retrieves real-time market prices for specified symbols or all available symbols

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
    api_instance = am_market_client.MarketDataApi(api_client)
    symbols = 'symbols_example' # str |  (optional)
    is_index_symbol = True # bool |  (optional)
    refresh = False # bool |  (optional) (default to False)

    try:
        # Get live market prices
        api_response = api_instance.get_live_prices(symbols=symbols, is_index_symbol=is_index_symbol, refresh=refresh)
        print("The response of MarketDataApi->get_live_prices:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_live_prices: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbols** | **str**|  | [optional] 
 **is_index_symbol** | **bool**|  | [optional] 
 **refresh** | **bool**|  | [optional] [default to False]

### Return type

**Dict[str, object]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**500** | Internal server error |  -  |
**200** | Live prices retrieved successfully |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_login_url**
> Dict[str, str] get_login_url(provider=provider)

Get login URL for broker authentication

Returns a URL that can be used to authenticate with the broker's login page

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
    api_instance = am_market_client.MarketDataApi(api_client)
    provider = 'provider_example' # str |  (optional)

    try:
        # Get login URL for broker authentication
        api_response = api_instance.get_login_url(provider=provider)
        print("The response of MarketDataApi->get_login_url:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_login_url: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **provider** | **str**|  | [optional] 

### Return type

**Dict[str, str]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**500** | Internal server error |  -  |
**200** | Login URL generated successfully |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_mutual_fund_details**
> Dict[str, object] get_mutual_fund_details(scheme_code, refresh=refresh)

Get mutual fund details

Retrieves detailed information about a mutual fund including NAV, returns, and other metrics

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
    api_instance = am_market_client.MarketDataApi(api_client)
    scheme_code = 'scheme_code_example' # str | 
    refresh = False # bool |  (optional) (default to False)

    try:
        # Get mutual fund details
        api_response = api_instance.get_mutual_fund_details(scheme_code, refresh=refresh)
        print("The response of MarketDataApi->get_mutual_fund_details:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_mutual_fund_details: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **scheme_code** | **str**|  | 
 **refresh** | **bool**|  | [optional] [default to False]

### Return type

**Dict[str, object]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Mutual fund details retrieved successfully |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_mutual_fund_nav_history**
> Dict[str, object] get_mutual_fund_nav_history(scheme_code, var_from, to, refresh=refresh)

Get mutual fund NAV history

Retrieves historical Net Asset Value (NAV) data for a mutual fund over a specified date range

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
    api_instance = am_market_client.MarketDataApi(api_client)
    scheme_code = 'scheme_code_example' # str | 
    var_from = 'var_from_example' # str | 
    to = 'to_example' # str | 
    refresh = False # bool |  (optional) (default to False)

    try:
        # Get mutual fund NAV history
        api_response = api_instance.get_mutual_fund_nav_history(scheme_code, var_from, to, refresh=refresh)
        print("The response of MarketDataApi->get_mutual_fund_nav_history:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_mutual_fund_nav_history: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **scheme_code** | **str**|  | 
 **var_from** | **str**|  | 
 **to** | **str**|  | 
 **refresh** | **bool**|  | [optional] [default to False]

### Return type

**Dict[str, object]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**500** | Internal server error |  -  |
**200** | NAV history retrieved successfully |  -  |
**400** | Invalid date format or request parameters |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_ohlc**
> object get_ohlc(ohlc_request)

Get OHLC data for multiple symbols

Retrieves Open-High-Low-Close data for multiple symbols with support for different timeframes

### Example


```python
import am_market_client
from am_market_client.models.ohlc_request import OHLCRequest
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
    api_instance = am_market_client.MarketDataApi(api_client)
    ohlc_request = am_market_client.OHLCRequest() # OHLCRequest | 

    try:
        # Get OHLC data for multiple symbols
        api_response = api_instance.get_ohlc(ohlc_request)
        print("The response of MarketDataApi->get_ohlc:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_ohlc: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ohlc_request** | [**OHLCRequest**](OHLCRequest.md)|  | 

### Return type

**object**

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
**200** | OHLC data retrieved successfully |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_option_chain**
> Dict[str, object] get_option_chain(symbol, expiry_date=expiry_date, refresh=refresh)

Get option chain data

Retrieves option chain data including calls and puts for a given underlying instrument

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
    api_instance = am_market_client.MarketDataApi(api_client)
    symbol = 'symbol_example' # str | 
    expiry_date = 'expiry_date_example' # str |  (optional)
    refresh = False # bool |  (optional) (default to False)

    try:
        # Get option chain data
        api_response = api_instance.get_option_chain(symbol, expiry_date=expiry_date, refresh=refresh)
        print("The response of MarketDataApi->get_option_chain:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_option_chain: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbol** | **str**|  | 
 **expiry_date** | **str**|  | [optional] 
 **refresh** | **bool**|  | [optional] [default to False]

### Return type

**Dict[str, object]**

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
**200** | Option chain data retrieved successfully |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_quotes**
> Dict[str, object] get_quotes(symbols, time_frame=time_frame, refresh=refresh)

Get quotes for multiple symbols

Retrieves latest quotes for multiple symbols with support for different timeframes

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
    api_instance = am_market_client.MarketDataApi(api_client)
    symbols = 'symbols_example' # str | 
    time_frame = '5m' # str |  (optional) (default to '5m')
    refresh = False # bool |  (optional) (default to False)

    try:
        # Get quotes for multiple symbols
        api_response = api_instance.get_quotes(symbols, time_frame=time_frame, refresh=refresh)
        print("The response of MarketDataApi->get_quotes:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_quotes: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **symbols** | **str**|  | 
 **time_frame** | **str**|  | [optional] [default to &#39;5m&#39;]
 **refresh** | **bool**|  | [optional] [default to False]

### Return type

**Dict[str, object]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Quotes retrieved successfully |  -  |
**400** | Invalid request parameters |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_quotes_post**
> Dict[str, object] get_quotes_post(quotes_request)

Get quotes for multiple symbols (POST)

Retrieves latest quotes for multiple symbols with support for different timeframes using POST request

### Example


```python
import am_market_client
from am_market_client.models.quotes_request import QuotesRequest
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
    api_instance = am_market_client.MarketDataApi(api_client)
    quotes_request = am_market_client.QuotesRequest() # QuotesRequest | 

    try:
        # Get quotes for multiple symbols (POST)
        api_response = api_instance.get_quotes_post(quotes_request)
        print("The response of MarketDataApi->get_quotes_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_quotes_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **quotes_request** | [**QuotesRequest**](QuotesRequest.md)|  | 

### Return type

**Dict[str, object]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Quotes retrieved successfully |  -  |
**400** | Invalid request parameters |  -  |
**500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_symbols_for_exchange**
> List[object] get_symbols_for_exchange(exchange)

Get symbols for a specific exchange

Retrieves all available trading symbols for a specific exchange

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
    api_instance = am_market_client.MarketDataApi(api_client)
    exchange = 'exchange_example' # str | 

    try:
        # Get symbols for a specific exchange
        api_response = api_instance.get_symbols_for_exchange(exchange)
        print("The response of MarketDataApi->get_symbols_for_exchange:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->get_symbols_for_exchange: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **exchange** | **str**|  | 

### Return type

**List[object]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**500** | Internal server error |  -  |
**200** | Symbols retrieved successfully |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **logout**
> Dict[str, object] logout()

Logout and invalidate session

Invalidates the current broker session and clears authentication tokens

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
    api_instance = am_market_client.MarketDataApi(api_client)

    try:
        # Logout and invalidate session
        api_response = api_instance.logout()
        print("The response of MarketDataApi->logout:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MarketDataApi->logout: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

**Dict[str, object]**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**500** | Internal server error |  -  |
**200** | Logout successful |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

