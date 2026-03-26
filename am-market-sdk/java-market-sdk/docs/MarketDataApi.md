# MarketDataApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**generateSession**](MarketDataApi.md#generateSession) | **GET** /v1/market-data/auth/session | Generate session from request token |
| [**generateSessionWithHttpInfo**](MarketDataApi.md#generateSessionWithHttpInfo) | **GET** /v1/market-data/auth/session | Generate session from request token |
| [**getHistoricalData**](MarketDataApi.md#getHistoricalData) | **POST** /v1/market-data/historical-data | Get historical market data |
| [**getHistoricalDataWithHttpInfo**](MarketDataApi.md#getHistoricalDataWithHttpInfo) | **POST** /v1/market-data/historical-data | Get historical market data |
| [**getLiveLTP**](MarketDataApi.md#getLiveLTP) | **GET** /v1/market-data/live-ltp | Get live LTP with change calculation |
| [**getLiveLTPWithHttpInfo**](MarketDataApi.md#getLiveLTPWithHttpInfo) | **GET** /v1/market-data/live-ltp | Get live LTP with change calculation |
| [**getLivePrices**](MarketDataApi.md#getLivePrices) | **GET** /v1/market-data/live-prices | Get live market prices |
| [**getLivePricesWithHttpInfo**](MarketDataApi.md#getLivePricesWithHttpInfo) | **GET** /v1/market-data/live-prices | Get live market prices |
| [**getLoginUrl**](MarketDataApi.md#getLoginUrl) | **GET** /v1/market-data/auth/login-url | Get login URL for broker authentication |
| [**getLoginUrlWithHttpInfo**](MarketDataApi.md#getLoginUrlWithHttpInfo) | **GET** /v1/market-data/auth/login-url | Get login URL for broker authentication |
| [**getMutualFundDetails**](MarketDataApi.md#getMutualFundDetails) | **GET** /v1/market-data/mutual-fund/{schemeCode} | Get mutual fund details |
| [**getMutualFundDetailsWithHttpInfo**](MarketDataApi.md#getMutualFundDetailsWithHttpInfo) | **GET** /v1/market-data/mutual-fund/{schemeCode} | Get mutual fund details |
| [**getMutualFundNavHistory**](MarketDataApi.md#getMutualFundNavHistory) | **GET** /v1/market-data/mutual-fund/{schemeCode}/history | Get mutual fund NAV history |
| [**getMutualFundNavHistoryWithHttpInfo**](MarketDataApi.md#getMutualFundNavHistoryWithHttpInfo) | **GET** /v1/market-data/mutual-fund/{schemeCode}/history | Get mutual fund NAV history |
| [**getOHLC**](MarketDataApi.md#getOHLC) | **POST** /v1/market-data/ohlc | Get OHLC data for multiple symbols |
| [**getOHLCWithHttpInfo**](MarketDataApi.md#getOHLCWithHttpInfo) | **POST** /v1/market-data/ohlc | Get OHLC data for multiple symbols |
| [**getOptionChain**](MarketDataApi.md#getOptionChain) | **GET** /v1/market-data/option-chain | Get option chain data |
| [**getOptionChainWithHttpInfo**](MarketDataApi.md#getOptionChainWithHttpInfo) | **GET** /v1/market-data/option-chain | Get option chain data |
| [**getQuotes**](MarketDataApi.md#getQuotes) | **GET** /v1/market-data/quotes | Get quotes for multiple symbols |
| [**getQuotesWithHttpInfo**](MarketDataApi.md#getQuotesWithHttpInfo) | **GET** /v1/market-data/quotes | Get quotes for multiple symbols |
| [**getQuotesPost**](MarketDataApi.md#getQuotesPost) | **POST** /v1/market-data/quotes | Get quotes for multiple symbols (POST) |
| [**getQuotesPostWithHttpInfo**](MarketDataApi.md#getQuotesPostWithHttpInfo) | **POST** /v1/market-data/quotes | Get quotes for multiple symbols (POST) |
| [**getSymbolsForExchange**](MarketDataApi.md#getSymbolsForExchange) | **GET** /v1/market-data/symbols/{exchange} | Get symbols for a specific exchange |
| [**getSymbolsForExchangeWithHttpInfo**](MarketDataApi.md#getSymbolsForExchangeWithHttpInfo) | **GET** /v1/market-data/symbols/{exchange} | Get symbols for a specific exchange |
| [**logout**](MarketDataApi.md#logout) | **POST** /v1/market-data/auth/logout | Logout and invalidate session |
| [**logoutWithHttpInfo**](MarketDataApi.md#logoutWithHttpInfo) | **POST** /v1/market-data/auth/logout | Logout and invalidate session |



## generateSession

> Object generateSession(requestToken, requestToken2, code, status)

Generate session from request token

Creates a new authenticated session using the request token obtained from broker login

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String requestToken = "requestToken_example"; // String | 
        String requestToken2 = "requestToken_example"; // String | 
        String code = "code_example"; // String | 
        String status = "success"; // String | 
        try {
            Object result = apiInstance.generateSession(requestToken, requestToken2, code, status);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#generateSession");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **requestToken** | **String**|  | [optional] |
| **requestToken2** | **String**|  | [optional] |
| **code** | **String**|  | [optional] |
| **status** | **String**|  | [optional] [default to success] |

### Return type

**Object**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Session generated successfully |  -  |
| **500** | Internal server error |  -  |
| **400** | Invalid request token or authentication failed |  -  |

## generateSessionWithHttpInfo

> ApiResponse<Object> generateSession generateSessionWithHttpInfo(requestToken, requestToken2, code, status)

Generate session from request token

Creates a new authenticated session using the request token obtained from broker login

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String requestToken = "requestToken_example"; // String | 
        String requestToken2 = "requestToken_example"; // String | 
        String code = "code_example"; // String | 
        String status = "success"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.generateSessionWithHttpInfo(requestToken, requestToken2, code, status);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#generateSession");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **requestToken** | **String**|  | [optional] |
| **requestToken2** | **String**|  | [optional] |
| **code** | **String**|  | [optional] |
| **status** | **String**|  | [optional] [default to success] |

### Return type

ApiResponse<**Object**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Session generated successfully |  -  |
| **500** | Internal server error |  -  |
| **400** | Invalid request token or authentication failed |  -  |


## getHistoricalData

> HistoricalDataResponseV1 getHistoricalData(historicalDataRequest)

Get historical market data

Retrieves historical price and volume data for one or more instruments with filtering options

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        HistoricalDataRequest historicalDataRequest = new HistoricalDataRequest(); // HistoricalDataRequest | 
        try {
            HistoricalDataResponseV1 result = apiInstance.getHistoricalData(historicalDataRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getHistoricalData");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **historicalDataRequest** | [**HistoricalDataRequest**](HistoricalDataRequest.md)|  | |

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
| **200** | Historical data retrieved successfully |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |

## getHistoricalDataWithHttpInfo

> ApiResponse<HistoricalDataResponseV1> getHistoricalData getHistoricalDataWithHttpInfo(historicalDataRequest)

Get historical market data

Retrieves historical price and volume data for one or more instruments with filtering options

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        HistoricalDataRequest historicalDataRequest = new HistoricalDataRequest(); // HistoricalDataRequest | 
        try {
            ApiResponse<HistoricalDataResponseV1> response = apiInstance.getHistoricalDataWithHttpInfo(historicalDataRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getHistoricalData");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **historicalDataRequest** | [**HistoricalDataRequest**](HistoricalDataRequest.md)|  | |

### Return type

ApiResponse<[**HistoricalDataResponseV1**](HistoricalDataResponseV1.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Historical data retrieved successfully |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |


## getLiveLTP

> Map<String, Object> getLiveLTP(symbols, timeframe, isIndexSymbol, refresh)

Get live LTP with change calculation

Retrieves current LTP and calculates change based on historical closing price for the specified timeframe

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String symbols = "symbols_example"; // String | 
        String timeframe = "1D"; // String | 
        Boolean isIndexSymbol = true; // Boolean | 
        Boolean refresh = false; // Boolean | 
        try {
            Map<String, Object> result = apiInstance.getLiveLTP(symbols, timeframe, isIndexSymbol, refresh);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getLiveLTP");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **symbols** | **String**|  | |
| **timeframe** | **String**|  | [optional] [default to 1D] |
| **isIndexSymbol** | **Boolean**|  | [optional] [default to true] |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

**Map&lt;String, Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Live LTP with change retrieved successfully |  -  |

## getLiveLTPWithHttpInfo

> ApiResponse<Map<String, Object>> getLiveLTP getLiveLTPWithHttpInfo(symbols, timeframe, isIndexSymbol, refresh)

Get live LTP with change calculation

Retrieves current LTP and calculates change based on historical closing price for the specified timeframe

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String symbols = "symbols_example"; // String | 
        String timeframe = "1D"; // String | 
        Boolean isIndexSymbol = true; // Boolean | 
        Boolean refresh = false; // Boolean | 
        try {
            ApiResponse<Map<String, Object>> response = apiInstance.getLiveLTPWithHttpInfo(symbols, timeframe, isIndexSymbol, refresh);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getLiveLTP");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **symbols** | **String**|  | |
| **timeframe** | **String**|  | [optional] [default to 1D] |
| **isIndexSymbol** | **Boolean**|  | [optional] [default to true] |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<**Map&lt;String, Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Live LTP with change retrieved successfully |  -  |


## getLivePrices

> Map<String, Object> getLivePrices(symbols, isIndexSymbol, refresh)

Get live market prices

Retrieves real-time market prices for specified symbols or all available symbols

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String symbols = "symbols_example"; // String | 
        Boolean isIndexSymbol = true; // Boolean | 
        Boolean refresh = false; // Boolean | 
        try {
            Map<String, Object> result = apiInstance.getLivePrices(symbols, isIndexSymbol, refresh);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getLivePrices");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **symbols** | **String**|  | [optional] |
| **isIndexSymbol** | **Boolean**|  | [optional] |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

**Map&lt;String, Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Live prices retrieved successfully |  -  |

## getLivePricesWithHttpInfo

> ApiResponse<Map<String, Object>> getLivePrices getLivePricesWithHttpInfo(symbols, isIndexSymbol, refresh)

Get live market prices

Retrieves real-time market prices for specified symbols or all available symbols

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String symbols = "symbols_example"; // String | 
        Boolean isIndexSymbol = true; // Boolean | 
        Boolean refresh = false; // Boolean | 
        try {
            ApiResponse<Map<String, Object>> response = apiInstance.getLivePricesWithHttpInfo(symbols, isIndexSymbol, refresh);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getLivePrices");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **symbols** | **String**|  | [optional] |
| **isIndexSymbol** | **Boolean**|  | [optional] |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<**Map&lt;String, Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Live prices retrieved successfully |  -  |


## getLoginUrl

> Map<String, String> getLoginUrl(provider)

Get login URL for broker authentication

Returns a URL that can be used to authenticate with the broker&#39;s login page

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String provider = "provider_example"; // String | 
        try {
            Map<String, String> result = apiInstance.getLoginUrl(provider);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getLoginUrl");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **provider** | **String**|  | [optional] |

### Return type

**Map&lt;String, String&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Login URL generated successfully |  -  |

## getLoginUrlWithHttpInfo

> ApiResponse<Map<String, String>> getLoginUrl getLoginUrlWithHttpInfo(provider)

Get login URL for broker authentication

Returns a URL that can be used to authenticate with the broker&#39;s login page

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String provider = "provider_example"; // String | 
        try {
            ApiResponse<Map<String, String>> response = apiInstance.getLoginUrlWithHttpInfo(provider);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getLoginUrl");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **provider** | **String**|  | [optional] |

### Return type

ApiResponse<**Map&lt;String, String&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Login URL generated successfully |  -  |


## getMutualFundDetails

> Map<String, Object> getMutualFundDetails(schemeCode, refresh)

Get mutual fund details

Retrieves detailed information about a mutual fund including NAV, returns, and other metrics

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String schemeCode = "schemeCode_example"; // String | 
        Boolean refresh = false; // Boolean | 
        try {
            Map<String, Object> result = apiInstance.getMutualFundDetails(schemeCode, refresh);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getMutualFundDetails");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **schemeCode** | **String**|  | |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

**Map&lt;String, Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Mutual fund details retrieved successfully |  -  |
| **500** | Internal server error |  -  |

## getMutualFundDetailsWithHttpInfo

> ApiResponse<Map<String, Object>> getMutualFundDetails getMutualFundDetailsWithHttpInfo(schemeCode, refresh)

Get mutual fund details

Retrieves detailed information about a mutual fund including NAV, returns, and other metrics

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String schemeCode = "schemeCode_example"; // String | 
        Boolean refresh = false; // Boolean | 
        try {
            ApiResponse<Map<String, Object>> response = apiInstance.getMutualFundDetailsWithHttpInfo(schemeCode, refresh);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getMutualFundDetails");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **schemeCode** | **String**|  | |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<**Map&lt;String, Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Mutual fund details retrieved successfully |  -  |
| **500** | Internal server error |  -  |


## getMutualFundNavHistory

> Map<String, Object> getMutualFundNavHistory(schemeCode, from, to, refresh)

Get mutual fund NAV history

Retrieves historical Net Asset Value (NAV) data for a mutual fund over a specified date range

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String schemeCode = "schemeCode_example"; // String | 
        String from = "from_example"; // String | 
        String to = "to_example"; // String | 
        Boolean refresh = false; // Boolean | 
        try {
            Map<String, Object> result = apiInstance.getMutualFundNavHistory(schemeCode, from, to, refresh);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getMutualFundNavHistory");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **schemeCode** | **String**|  | |
| **from** | **String**|  | |
| **to** | **String**|  | |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

**Map&lt;String, Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | NAV history retrieved successfully |  -  |
| **400** | Invalid date format or request parameters |  -  |

## getMutualFundNavHistoryWithHttpInfo

> ApiResponse<Map<String, Object>> getMutualFundNavHistory getMutualFundNavHistoryWithHttpInfo(schemeCode, from, to, refresh)

Get mutual fund NAV history

Retrieves historical Net Asset Value (NAV) data for a mutual fund over a specified date range

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String schemeCode = "schemeCode_example"; // String | 
        String from = "from_example"; // String | 
        String to = "to_example"; // String | 
        Boolean refresh = false; // Boolean | 
        try {
            ApiResponse<Map<String, Object>> response = apiInstance.getMutualFundNavHistoryWithHttpInfo(schemeCode, from, to, refresh);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getMutualFundNavHistory");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **schemeCode** | **String**|  | |
| **from** | **String**|  | |
| **to** | **String**|  | |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<**Map&lt;String, Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | NAV history retrieved successfully |  -  |
| **400** | Invalid date format or request parameters |  -  |


## getOHLC

> Object getOHLC(ohLCRequest)

Get OHLC data for multiple symbols

Retrieves Open-High-Low-Close data for multiple symbols with support for different timeframes

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        OHLCRequest ohLCRequest = new OHLCRequest(); // OHLCRequest | 
        try {
            Object result = apiInstance.getOHLC(ohLCRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getOHLC");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **ohLCRequest** | [**OHLCRequest**](OHLCRequest.md)|  | |

### Return type

**Object**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |
| **200** | OHLC data retrieved successfully |  -  |

## getOHLCWithHttpInfo

> ApiResponse<Object> getOHLC getOHLCWithHttpInfo(ohLCRequest)

Get OHLC data for multiple symbols

Retrieves Open-High-Low-Close data for multiple symbols with support for different timeframes

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        OHLCRequest ohLCRequest = new OHLCRequest(); // OHLCRequest | 
        try {
            ApiResponse<Object> response = apiInstance.getOHLCWithHttpInfo(ohLCRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getOHLC");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **ohLCRequest** | [**OHLCRequest**](OHLCRequest.md)|  | |

### Return type

ApiResponse<**Object**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |
| **200** | OHLC data retrieved successfully |  -  |


## getOptionChain

> Map<String, Object> getOptionChain(symbol, expiryDate, refresh)

Get option chain data

Retrieves option chain data including calls and puts for a given underlying instrument

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        String expiryDate = "expiryDate_example"; // String | 
        Boolean refresh = false; // Boolean | 
        try {
            Map<String, Object> result = apiInstance.getOptionChain(symbol, expiryDate, refresh);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getOptionChain");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **symbol** | **String**|  | |
| **expiryDate** | **String**|  | [optional] |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

**Map&lt;String, Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |
| **200** | Option chain data retrieved successfully |  -  |

## getOptionChainWithHttpInfo

> ApiResponse<Map<String, Object>> getOptionChain getOptionChainWithHttpInfo(symbol, expiryDate, refresh)

Get option chain data

Retrieves option chain data including calls and puts for a given underlying instrument

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        String expiryDate = "expiryDate_example"; // String | 
        Boolean refresh = false; // Boolean | 
        try {
            ApiResponse<Map<String, Object>> response = apiInstance.getOptionChainWithHttpInfo(symbol, expiryDate, refresh);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getOptionChain");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **symbol** | **String**|  | |
| **expiryDate** | **String**|  | [optional] |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<**Map&lt;String, Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |
| **200** | Option chain data retrieved successfully |  -  |


## getQuotes

> Map<String, Object> getQuotes(symbols, timeFrame, refresh)

Get quotes for multiple symbols

Retrieves latest quotes for multiple symbols with support for different timeframes

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String symbols = "symbols_example"; // String | 
        String timeFrame = "5m"; // String | 
        Boolean refresh = false; // Boolean | 
        try {
            Map<String, Object> result = apiInstance.getQuotes(symbols, timeFrame, refresh);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getQuotes");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **symbols** | **String**|  | |
| **timeFrame** | **String**|  | [optional] [default to 5m] |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

**Map&lt;String, Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Quotes retrieved successfully |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |

## getQuotesWithHttpInfo

> ApiResponse<Map<String, Object>> getQuotes getQuotesWithHttpInfo(symbols, timeFrame, refresh)

Get quotes for multiple symbols

Retrieves latest quotes for multiple symbols with support for different timeframes

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String symbols = "symbols_example"; // String | 
        String timeFrame = "5m"; // String | 
        Boolean refresh = false; // Boolean | 
        try {
            ApiResponse<Map<String, Object>> response = apiInstance.getQuotesWithHttpInfo(symbols, timeFrame, refresh);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getQuotes");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **symbols** | **String**|  | |
| **timeFrame** | **String**|  | [optional] [default to 5m] |
| **refresh** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<**Map&lt;String, Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Quotes retrieved successfully |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |


## getQuotesPost

> Map<String, Object> getQuotesPost(quotesRequest)

Get quotes for multiple symbols (POST)

Retrieves latest quotes for multiple symbols with support for different timeframes using POST request

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        QuotesRequest quotesRequest = new QuotesRequest(); // QuotesRequest | 
        try {
            Map<String, Object> result = apiInstance.getQuotesPost(quotesRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getQuotesPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **quotesRequest** | [**QuotesRequest**](QuotesRequest.md)|  | |

### Return type

**Map&lt;String, Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Quotes retrieved successfully |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |

## getQuotesPostWithHttpInfo

> ApiResponse<Map<String, Object>> getQuotesPost getQuotesPostWithHttpInfo(quotesRequest)

Get quotes for multiple symbols (POST)

Retrieves latest quotes for multiple symbols with support for different timeframes using POST request

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        QuotesRequest quotesRequest = new QuotesRequest(); // QuotesRequest | 
        try {
            ApiResponse<Map<String, Object>> response = apiInstance.getQuotesPostWithHttpInfo(quotesRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getQuotesPost");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **quotesRequest** | [**QuotesRequest**](QuotesRequest.md)|  | |

### Return type

ApiResponse<**Map&lt;String, Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Quotes retrieved successfully |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |


## getSymbolsForExchange

> List<Object> getSymbolsForExchange(exchange)

Get symbols for a specific exchange

Retrieves all available trading symbols for a specific exchange

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String exchange = "exchange_example"; // String | 
        try {
            List<Object> result = apiInstance.getSymbolsForExchange(exchange);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getSymbolsForExchange");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **exchange** | **String**|  | |

### Return type

**List&lt;Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Symbols retrieved successfully |  -  |

## getSymbolsForExchangeWithHttpInfo

> ApiResponse<List<Object>> getSymbolsForExchange getSymbolsForExchangeWithHttpInfo(exchange)

Get symbols for a specific exchange

Retrieves all available trading symbols for a specific exchange

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        String exchange = "exchange_example"; // String | 
        try {
            ApiResponse<List<Object>> response = apiInstance.getSymbolsForExchangeWithHttpInfo(exchange);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#getSymbolsForExchange");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **exchange** | **String**|  | |

### Return type

ApiResponse<**List&lt;Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Symbols retrieved successfully |  -  |


## logout

> Map<String, Object> logout()

Logout and invalidate session

Invalidates the current broker session and clears authentication tokens

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        try {
            Map<String, Object> result = apiInstance.logout();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#logout");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

**Map&lt;String, Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Logout successful |  -  |

## logoutWithHttpInfo

> ApiResponse<Map<String, Object>> logout logoutWithHttpInfo()

Logout and invalidate session

Invalidates the current broker session and clears authentication tokens

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataApi apiInstance = new MarketDataApi(defaultClient);
        try {
            ApiResponse<Map<String, Object>> response = apiInstance.logoutWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataApi#logout");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Response headers: " + e.getResponseHeaders());
            System.err.println("Reason: " + e.getResponseBody());
            e.printStackTrace();
        }
    }
}
```

### Parameters

This endpoint does not need any parameter.

### Return type

ApiResponse<**Map&lt;String, Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **500** | Internal server error |  -  |
| **200** | Logout successful |  -  |

