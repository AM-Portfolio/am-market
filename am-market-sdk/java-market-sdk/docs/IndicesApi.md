# IndicesApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getAvailableIndices**](IndicesApi.md#getAvailableIndices) | **GET** /v1/indices/available | Get available indices |
| [**getAvailableIndicesWithHttpInfo**](IndicesApi.md#getAvailableIndicesWithHttpInfo) | **GET** /v1/indices/available | Get available indices |
| [**getLatestIndicesData**](IndicesApi.md#getLatestIndicesData) | **POST** /v1/indices/batch | Get latest market data for multiple indices |
| [**getLatestIndicesDataWithHttpInfo**](IndicesApi.md#getLatestIndicesDataWithHttpInfo) | **POST** /v1/indices/batch | Get latest market data for multiple indices |



## getAvailableIndices

> String getAvailableIndices()

Get available indices

Retrieves the list of available indices (Broad and Sector)

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.IndicesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        IndicesApi apiInstance = new IndicesApi(defaultClient);
        try {
            String result = apiInstance.getAvailableIndices();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling IndicesApi#getAvailableIndices");
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

**String**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Indices retrieved successfully |  -  |
| **500** | Internal server error |  -  |

## getAvailableIndicesWithHttpInfo

> ApiResponse<String> getAvailableIndices getAvailableIndicesWithHttpInfo()

Get available indices

Retrieves the list of available indices (Broad and Sector)

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.IndicesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        IndicesApi apiInstance = new IndicesApi(defaultClient);
        try {
            ApiResponse<String> response = apiInstance.getAvailableIndicesWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling IndicesApi#getAvailableIndices");
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

ApiResponse<**String**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Indices retrieved successfully |  -  |
| **500** | Internal server error |  -  |


## getLatestIndicesData

> StockIndicesMarketData getLatestIndicesData(requestBody, forceRefresh)

Get latest market data for multiple indices

Retrieves the latest market data for multiple indices in a single request

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.IndicesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        IndicesApi apiInstance = new IndicesApi(defaultClient);
        List<String> requestBody = Arrays.asList(); // List<String> | 
        Boolean forceRefresh = false; // Boolean | Force refresh from source instead of using cache
        try {
            StockIndicesMarketData result = apiInstance.getLatestIndicesData(requestBody, forceRefresh);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling IndicesApi#getLatestIndicesData");
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
| **requestBody** | [**List&lt;String&gt;**](String.md)|  | |
| **forceRefresh** | **Boolean**| Force refresh from source instead of using cache | [optional] [default to false] |

### Return type

[**StockIndicesMarketData**](StockIndicesMarketData.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Indices data retrieved successfully |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |

## getLatestIndicesDataWithHttpInfo

> ApiResponse<StockIndicesMarketData> getLatestIndicesData getLatestIndicesDataWithHttpInfo(requestBody, forceRefresh)

Get latest market data for multiple indices

Retrieves the latest market data for multiple indices in a single request

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.IndicesApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        IndicesApi apiInstance = new IndicesApi(defaultClient);
        List<String> requestBody = Arrays.asList(); // List<String> | 
        Boolean forceRefresh = false; // Boolean | Force refresh from source instead of using cache
        try {
            ApiResponse<StockIndicesMarketData> response = apiInstance.getLatestIndicesDataWithHttpInfo(requestBody, forceRefresh);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling IndicesApi#getLatestIndicesData");
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
| **requestBody** | [**List&lt;String&gt;**](String.md)|  | |
| **forceRefresh** | **Boolean**| Force refresh from source instead of using cache | [optional] [default to false] |

### Return type

ApiResponse<[**StockIndicesMarketData**](StockIndicesMarketData.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Indices data retrieved successfully |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |

