# MarketAnalyticsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getHistoricalCharts**](MarketAnalyticsApi.md#getHistoricalCharts) | **GET** /v1/market-analytics/historical-charts/{symbol} | Get historical charts data |
| [**getHistoricalChartsWithHttpInfo**](MarketAnalyticsApi.md#getHistoricalChartsWithHttpInfo) | **GET** /v1/market-analytics/historical-charts/{symbol} | Get historical charts data |
| [**getMovers**](MarketAnalyticsApi.md#getMovers) | **GET** /v1/market-analytics/movers | Get Top Gainers/Losers |
| [**getMoversWithHttpInfo**](MarketAnalyticsApi.md#getMoversWithHttpInfo) | **GET** /v1/market-analytics/movers | Get Top Gainers/Losers |
| [**getSectorPerformance**](MarketAnalyticsApi.md#getSectorPerformance) | **GET** /v1/market-analytics/sectors | Get Sector Performance |
| [**getSectorPerformanceWithHttpInfo**](MarketAnalyticsApi.md#getSectorPerformanceWithHttpInfo) | **GET** /v1/market-analytics/sectors | Get Sector Performance |



## getHistoricalCharts

> HistoricalDataResponseV1 getHistoricalCharts(symbol, range)

Get historical charts data

Retrieves historical data for charts with various time frames (10m, 1H, 1D, 1W, 1M, 5Y, etc.)

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketAnalyticsApi apiInstance = new MarketAnalyticsApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        String range = "1D"; // String | 
        try {
            HistoricalDataResponseV1 result = apiInstance.getHistoricalCharts(symbol, range);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketAnalyticsApi#getHistoricalCharts");
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
| **range** | **String**|  | [optional] [default to 1D] |

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
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |
| **200** | Chart data retrieved successfully |  -  |

## getHistoricalChartsWithHttpInfo

> ApiResponse<HistoricalDataResponseV1> getHistoricalCharts getHistoricalChartsWithHttpInfo(symbol, range)

Get historical charts data

Retrieves historical data for charts with various time frames (10m, 1H, 1D, 1W, 1M, 5Y, etc.)

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketAnalyticsApi apiInstance = new MarketAnalyticsApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        String range = "1D"; // String | 
        try {
            ApiResponse<HistoricalDataResponseV1> response = apiInstance.getHistoricalChartsWithHttpInfo(symbol, range);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketAnalyticsApi#getHistoricalCharts");
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
| **range** | **String**|  | [optional] [default to 1D] |

### Return type

ApiResponse<[**HistoricalDataResponseV1**](HistoricalDataResponseV1.md)>


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
| **200** | Chart data retrieved successfully |  -  |


## getMovers

> List<Map<String, Object>> getMovers(type, limit, indexSymbol, timeFrame, expandIndices)

Get Top Gainers/Losers

Retrieves top performing or worst performing stocks from the specified market index

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketAnalyticsApi apiInstance = new MarketAnalyticsApi(defaultClient);
        String type = "gainers"; // String | 
        Integer limit = 10; // Integer | 
        String indexSymbol = "indexSymbol_example"; // String | 
        String timeFrame = "timeFrame_example"; // String | 
        Boolean expandIndices = false; // Boolean | 
        try {
            List<Map<String, Object>> result = apiInstance.getMovers(type, limit, indexSymbol, timeFrame, expandIndices);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketAnalyticsApi#getMovers");
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
| **type** | **String**|  | [optional] [default to gainers] |
| **limit** | **Integer**|  | [optional] [default to 10] |
| **indexSymbol** | **String**|  | [optional] |
| **timeFrame** | **String**|  | [optional] |
| **expandIndices** | **Boolean**|  | [optional] [default to false] |

### Return type

[**List&lt;Map&lt;String, Object&gt;&gt;**](Map.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Data retrieved successfully |  -  |
| **500** | Internal server error |  -  |

## getMoversWithHttpInfo

> ApiResponse<List<Map<String, Object>>> getMovers getMoversWithHttpInfo(type, limit, indexSymbol, timeFrame, expandIndices)

Get Top Gainers/Losers

Retrieves top performing or worst performing stocks from the specified market index

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketAnalyticsApi apiInstance = new MarketAnalyticsApi(defaultClient);
        String type = "gainers"; // String | 
        Integer limit = 10; // Integer | 
        String indexSymbol = "indexSymbol_example"; // String | 
        String timeFrame = "timeFrame_example"; // String | 
        Boolean expandIndices = false; // Boolean | 
        try {
            ApiResponse<List<Map<String, Object>>> response = apiInstance.getMoversWithHttpInfo(type, limit, indexSymbol, timeFrame, expandIndices);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketAnalyticsApi#getMovers");
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
| **type** | **String**|  | [optional] [default to gainers] |
| **limit** | **Integer**|  | [optional] [default to 10] |
| **indexSymbol** | **String**|  | [optional] |
| **timeFrame** | **String**|  | [optional] |
| **expandIndices** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<[**List&lt;Map&lt;String, Object&gt;&gt;**](Map.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Data retrieved successfully |  -  |
| **500** | Internal server error |  -  |


## getSectorPerformance

> List<Map<String, Object>> getSectorPerformance(indexSymbol, timeFrame, expandIndices)

Get Sector Performance

Aggregates market performance by sector (Industry) from the specified index

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketAnalyticsApi apiInstance = new MarketAnalyticsApi(defaultClient);
        String indexSymbol = "indexSymbol_example"; // String | 
        String timeFrame = "timeFrame_example"; // String | 
        Boolean expandIndices = false; // Boolean | 
        try {
            List<Map<String, Object>> result = apiInstance.getSectorPerformance(indexSymbol, timeFrame, expandIndices);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketAnalyticsApi#getSectorPerformance");
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
| **indexSymbol** | **String**|  | [optional] |
| **timeFrame** | **String**|  | [optional] |
| **expandIndices** | **Boolean**|  | [optional] [default to false] |

### Return type

[**List&lt;Map&lt;String, Object&gt;&gt;**](Map.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Data retrieved successfully |  -  |
| **500** | Internal server error |  -  |

## getSectorPerformanceWithHttpInfo

> ApiResponse<List<Map<String, Object>>> getSectorPerformance getSectorPerformanceWithHttpInfo(indexSymbol, timeFrame, expandIndices)

Get Sector Performance

Aggregates market performance by sector (Industry) from the specified index

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketAnalyticsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketAnalyticsApi apiInstance = new MarketAnalyticsApi(defaultClient);
        String indexSymbol = "indexSymbol_example"; // String | 
        String timeFrame = "timeFrame_example"; // String | 
        Boolean expandIndices = false; // Boolean | 
        try {
            ApiResponse<List<Map<String, Object>>> response = apiInstance.getSectorPerformanceWithHttpInfo(indexSymbol, timeFrame, expandIndices);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketAnalyticsApi#getSectorPerformance");
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
| **indexSymbol** | **String**|  | [optional] |
| **timeFrame** | **String**|  | [optional] |
| **expandIndices** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<[**List&lt;Map&lt;String, Object&gt;&gt;**](Map.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Data retrieved successfully |  -  |
| **500** | Internal server error |  -  |

