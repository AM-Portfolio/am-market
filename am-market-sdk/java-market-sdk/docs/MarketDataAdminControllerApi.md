# MarketDataAdminControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getJobDetails**](MarketDataAdminControllerApi.md#getJobDetails) | **GET** /v1/admin/logs/{jobId} |  |
| [**getJobDetailsWithHttpInfo**](MarketDataAdminControllerApi.md#getJobDetailsWithHttpInfo) | **GET** /v1/admin/logs/{jobId} |  |
| [**getLogs**](MarketDataAdminControllerApi.md#getLogs) | **GET** /v1/admin/logs |  |
| [**getLogsWithHttpInfo**](MarketDataAdminControllerApi.md#getLogsWithHttpInfo) | **GET** /v1/admin/logs |  |
| [**startIngestion**](MarketDataAdminControllerApi.md#startIngestion) | **POST** /v1/admin/ingestion/start |  |
| [**startIngestionWithHttpInfo**](MarketDataAdminControllerApi.md#startIngestionWithHttpInfo) | **POST** /v1/admin/ingestion/start |  |
| [**stopIngestion**](MarketDataAdminControllerApi.md#stopIngestion) | **POST** /v1/admin/ingestion/stop |  |
| [**stopIngestionWithHttpInfo**](MarketDataAdminControllerApi.md#stopIngestionWithHttpInfo) | **POST** /v1/admin/ingestion/stop |  |
| [**triggerHistoricalSync**](MarketDataAdminControllerApi.md#triggerHistoricalSync) | **POST** /v1/admin/sync/historical |  |
| [**triggerHistoricalSyncWithHttpInfo**](MarketDataAdminControllerApi.md#triggerHistoricalSyncWithHttpInfo) | **POST** /v1/admin/sync/historical |  |



## getJobDetails

> IngestionJobLog getJobDetails(jobId)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        try {
            IngestionJobLog result = apiInstance.getJobDetails(jobId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#getJobDetails");
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
| **jobId** | **String**|  | |

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
| **200** | OK |  -  |

## getJobDetailsWithHttpInfo

> ApiResponse<IngestionJobLog> getJobDetails getJobDetailsWithHttpInfo(jobId)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        try {
            ApiResponse<IngestionJobLog> response = apiInstance.getJobDetailsWithHttpInfo(jobId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#getJobDetails");
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
| **jobId** | **String**|  | |

### Return type

ApiResponse<[**IngestionJobLog**](IngestionJobLog.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## getLogs

> List<IngestionJobLog> getLogs(page, size, startDate, endDate)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        Integer page = 0; // Integer | 
        Integer size = 10; // Integer | 
        LocalDate startDate = LocalDate.now(); // LocalDate | 
        LocalDate endDate = LocalDate.now(); // LocalDate | 
        try {
            List<IngestionJobLog> result = apiInstance.getLogs(page, size, startDate, endDate);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#getLogs");
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
| **page** | **Integer**|  | [optional] [default to 0] |
| **size** | **Integer**|  | [optional] [default to 10] |
| **startDate** | **LocalDate**|  | [optional] |
| **endDate** | **LocalDate**|  | [optional] |

### Return type

[**List&lt;IngestionJobLog&gt;**](IngestionJobLog.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## getLogsWithHttpInfo

> ApiResponse<List<IngestionJobLog>> getLogs getLogsWithHttpInfo(page, size, startDate, endDate)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        Integer page = 0; // Integer | 
        Integer size = 10; // Integer | 
        LocalDate startDate = LocalDate.now(); // LocalDate | 
        LocalDate endDate = LocalDate.now(); // LocalDate | 
        try {
            ApiResponse<List<IngestionJobLog>> response = apiInstance.getLogsWithHttpInfo(page, size, startDate, endDate);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#getLogs");
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
| **page** | **Integer**|  | [optional] [default to 0] |
| **size** | **Integer**|  | [optional] [default to 10] |
| **startDate** | **LocalDate**|  | [optional] |
| **endDate** | **LocalDate**|  | [optional] |

### Return type

ApiResponse<[**List&lt;IngestionJobLog&gt;**](IngestionJobLog.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## startIngestion

> String startIngestion(provider, symbols)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        String provider = "UPSTOX"; // String | 
        List<String> symbols = Arrays.asList(); // List<String> | 
        try {
            String result = apiInstance.startIngestion(provider, symbols);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#startIngestion");
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
| **provider** | **String**|  | [optional] [default to UPSTOX] |
| **symbols** | [**List&lt;String&gt;**](String.md)|  | [optional] |

### Return type

**String**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## startIngestionWithHttpInfo

> ApiResponse<String> startIngestion startIngestionWithHttpInfo(provider, symbols)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        String provider = "UPSTOX"; // String | 
        List<String> symbols = Arrays.asList(); // List<String> | 
        try {
            ApiResponse<String> response = apiInstance.startIngestionWithHttpInfo(provider, symbols);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#startIngestion");
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
| **provider** | **String**|  | [optional] [default to UPSTOX] |
| **symbols** | [**List&lt;String&gt;**](String.md)|  | [optional] |

### Return type

ApiResponse<**String**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## stopIngestion

> String stopIngestion(provider)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        String provider = "provider_example"; // String | 
        try {
            String result = apiInstance.stopIngestion(provider);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#stopIngestion");
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
| **provider** | **String**|  | |

### Return type

**String**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## stopIngestionWithHttpInfo

> ApiResponse<String> stopIngestion stopIngestionWithHttpInfo(provider)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        String provider = "provider_example"; // String | 
        try {
            ApiResponse<String> response = apiInstance.stopIngestionWithHttpInfo(provider);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#stopIngestion");
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
| **provider** | **String**|  | |

### Return type

ApiResponse<**String**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## triggerHistoricalSync

> String triggerHistoricalSync(symbol, forceRefresh, fetchIndexStocks)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        Boolean forceRefresh = true; // Boolean | 
        Boolean fetchIndexStocks = false; // Boolean | 
        try {
            String result = apiInstance.triggerHistoricalSync(symbol, forceRefresh, fetchIndexStocks);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#triggerHistoricalSync");
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
| **symbol** | **String**|  | [optional] |
| **forceRefresh** | **Boolean**|  | [optional] [default to true] |
| **fetchIndexStocks** | **Boolean**|  | [optional] [default to false] |

### Return type

**String**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## triggerHistoricalSyncWithHttpInfo

> ApiResponse<String> triggerHistoricalSync triggerHistoricalSyncWithHttpInfo(symbol, forceRefresh, fetchIndexStocks)



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataAdminControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataAdminControllerApi apiInstance = new MarketDataAdminControllerApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        Boolean forceRefresh = true; // Boolean | 
        Boolean fetchIndexStocks = false; // Boolean | 
        try {
            ApiResponse<String> response = apiInstance.triggerHistoricalSyncWithHttpInfo(symbol, forceRefresh, fetchIndexStocks);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataAdminControllerApi#triggerHistoricalSync");
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
| **symbol** | **String**|  | [optional] |
| **forceRefresh** | **Boolean**|  | [optional] [default to true] |
| **fetchIndexStocks** | **Boolean**|  | [optional] [default to false] |

### Return type

ApiResponse<**String**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

