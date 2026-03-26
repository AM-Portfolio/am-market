# EtfHoldingsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**fetchAllEtfHoldingsV1FetchAllHoldingsPost**](EtfHoldingsApi.md#fetchAllEtfHoldingsV1FetchAllHoldingsPost) | **POST** /v1/fetch-all-holdings | Fetch All Etf Holdings |
| [**fetchAllEtfHoldingsV1FetchAllHoldingsPostWithHttpInfo**](EtfHoldingsApi.md#fetchAllEtfHoldingsV1FetchAllHoldingsPostWithHttpInfo) | **POST** /v1/fetch-all-holdings | Fetch All Etf Holdings |
| [**fetchHoldingsForEtfV1FetchHoldingsSymbolPost**](EtfHoldingsApi.md#fetchHoldingsForEtfV1FetchHoldingsSymbolPost) | **POST** /v1/fetch-holdings/{symbol} | Fetch Holdings For Etf |
| [**fetchHoldingsForEtfV1FetchHoldingsSymbolPostWithHttpInfo**](EtfHoldingsApi.md#fetchHoldingsForEtfV1FetchHoldingsSymbolPostWithHttpInfo) | **POST** /v1/fetch-holdings/{symbol} | Fetch Holdings For Etf |
| [**getCacheStatisticsV1CacheStatsGet**](EtfHoldingsApi.md#getCacheStatisticsV1CacheStatsGet) | **GET** /v1/cache-stats | Get Cache Statistics |
| [**getCacheStatisticsV1CacheStatsGetWithHttpInfo**](EtfHoldingsApi.md#getCacheStatisticsV1CacheStatsGetWithHttpInfo) | **GET** /v1/cache-stats | Get Cache Statistics |
| [**getEtfHoldingsV1HoldingsSymbolGet**](EtfHoldingsApi.md#getEtfHoldingsV1HoldingsSymbolGet) | **GET** /v1/holdings/{symbol} | Get Etf Holdings |
| [**getEtfHoldingsV1HoldingsSymbolGetWithHttpInfo**](EtfHoldingsApi.md#getEtfHoldingsV1HoldingsSymbolGetWithHttpInfo) | **GET** /v1/holdings/{symbol} | Get Etf Holdings |
| [**getEtfStatsV1StatsGet**](EtfHoldingsApi.md#getEtfStatsV1StatsGet) | **GET** /v1/stats | Get Etf Stats |
| [**getEtfStatsV1StatsGetWithHttpInfo**](EtfHoldingsApi.md#getEtfStatsV1StatsGetWithHttpInfo) | **GET** /v1/stats | Get Etf Stats |
| [**loadEtfsFromJsonV1LoadFromJsonPost**](EtfHoldingsApi.md#loadEtfsFromJsonV1LoadFromJsonPost) | **POST** /v1/load-from-json | Load Etfs From Json |
| [**loadEtfsFromJsonV1LoadFromJsonPostWithHttpInfo**](EtfHoldingsApi.md#loadEtfsFromJsonV1LoadFromJsonPostWithHttpInfo) | **POST** /v1/load-from-json | Load Etfs From Json |
| [**searchEtfsV1SearchGet**](EtfHoldingsApi.md#searchEtfsV1SearchGet) | **GET** /v1/search | Search Etfs |
| [**searchEtfsV1SearchGetWithHttpInfo**](EtfHoldingsApi.md#searchEtfsV1SearchGetWithHttpInfo) | **GET** /v1/search | Search Etfs |



## fetchAllEtfHoldingsV1FetchAllHoldingsPost

> JobResponse fetchAllEtfHoldingsV1FetchAllHoldingsPost(callbackUrl, userId, limit, forceRefresh)

Fetch All Etf Holdings

Fetch holdings for all ETFs with ISINs from moneycontrol API Returns immediately with job ID, processes in background Smart caching: Only fetches if data is missing or stale

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        String callbackUrl = "callbackUrl_example"; // String | 
        String userId = "userId_example"; // String | 
        Integer limit = 56; // Integer | Limit number of ETFs to process
        Boolean forceRefresh = false; // Boolean | Force refresh even if data exists for today
        try {
            JobResponse result = apiInstance.fetchAllEtfHoldingsV1FetchAllHoldingsPost(callbackUrl, userId, limit, forceRefresh);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#fetchAllEtfHoldingsV1FetchAllHoldingsPost");
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
| **callbackUrl** | **String**|  | [optional] |
| **userId** | **String**|  | [optional] |
| **limit** | **Integer**| Limit number of ETFs to process | [optional] |
| **forceRefresh** | **Boolean**| Force refresh even if data exists for today | [optional] [default to false] |

### Return type

[**JobResponse**](JobResponse.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |

## fetchAllEtfHoldingsV1FetchAllHoldingsPostWithHttpInfo

> ApiResponse<JobResponse> fetchAllEtfHoldingsV1FetchAllHoldingsPost fetchAllEtfHoldingsV1FetchAllHoldingsPostWithHttpInfo(callbackUrl, userId, limit, forceRefresh)

Fetch All Etf Holdings

Fetch holdings for all ETFs with ISINs from moneycontrol API Returns immediately with job ID, processes in background Smart caching: Only fetches if data is missing or stale

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        String callbackUrl = "callbackUrl_example"; // String | 
        String userId = "userId_example"; // String | 
        Integer limit = 56; // Integer | Limit number of ETFs to process
        Boolean forceRefresh = false; // Boolean | Force refresh even if data exists for today
        try {
            ApiResponse<JobResponse> response = apiInstance.fetchAllEtfHoldingsV1FetchAllHoldingsPostWithHttpInfo(callbackUrl, userId, limit, forceRefresh);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#fetchAllEtfHoldingsV1FetchAllHoldingsPost");
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
| **callbackUrl** | **String**|  | [optional] |
| **userId** | **String**|  | [optional] |
| **limit** | **Integer**| Limit number of ETFs to process | [optional] |
| **forceRefresh** | **Boolean**| Force refresh even if data exists for today | [optional] [default to false] |

### Return type

ApiResponse<[**JobResponse**](JobResponse.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |


## fetchHoldingsForEtfV1FetchHoldingsSymbolPost

> Object fetchHoldingsForEtfV1FetchHoldingsSymbolPost(symbol, callbackUrl, userId)

Fetch Holdings For Etf

Fetch holdings for a specific ETF by symbol Returns immediately with job ID, processes in background

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        String callbackUrl = "callbackUrl_example"; // String | 
        String userId = "userId_example"; // String | 
        try {
            Object result = apiInstance.fetchHoldingsForEtfV1FetchHoldingsSymbolPost(symbol, callbackUrl, userId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#fetchHoldingsForEtfV1FetchHoldingsSymbolPost");
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
| **callbackUrl** | **String**|  | [optional] |
| **userId** | **String**|  | [optional] |

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
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |

## fetchHoldingsForEtfV1FetchHoldingsSymbolPostWithHttpInfo

> ApiResponse<Object> fetchHoldingsForEtfV1FetchHoldingsSymbolPost fetchHoldingsForEtfV1FetchHoldingsSymbolPostWithHttpInfo(symbol, callbackUrl, userId)

Fetch Holdings For Etf

Fetch holdings for a specific ETF by symbol Returns immediately with job ID, processes in background

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        String callbackUrl = "callbackUrl_example"; // String | 
        String userId = "userId_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.fetchHoldingsForEtfV1FetchHoldingsSymbolPostWithHttpInfo(symbol, callbackUrl, userId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#fetchHoldingsForEtfV1FetchHoldingsSymbolPost");
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
| **callbackUrl** | **String**|  | [optional] |
| **userId** | **String**|  | [optional] |

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
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |


## getCacheStatisticsV1CacheStatsGet

> Object getCacheStatisticsV1CacheStatsGet()

Get Cache Statistics

Get ETF holdings cache statistics

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        try {
            Object result = apiInstance.getCacheStatisticsV1CacheStatsGet();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#getCacheStatisticsV1CacheStatsGet");
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

**Object**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |

## getCacheStatisticsV1CacheStatsGetWithHttpInfo

> ApiResponse<Object> getCacheStatisticsV1CacheStatsGet getCacheStatisticsV1CacheStatsGetWithHttpInfo()

Get Cache Statistics

Get ETF holdings cache statistics

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        try {
            ApiResponse<Object> response = apiInstance.getCacheStatisticsV1CacheStatsGetWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#getCacheStatisticsV1CacheStatsGet");
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

ApiResponse<**Object**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |


## getEtfHoldingsV1HoldingsSymbolGet

> Object getEtfHoldingsV1HoldingsSymbolGet(symbol)

Get Etf Holdings

Get stored holdings for a specific ETF

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        try {
            Object result = apiInstance.getEtfHoldingsV1HoldingsSymbolGet(symbol);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#getEtfHoldingsV1HoldingsSymbolGet");
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
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |

## getEtfHoldingsV1HoldingsSymbolGetWithHttpInfo

> ApiResponse<Object> getEtfHoldingsV1HoldingsSymbolGet getEtfHoldingsV1HoldingsSymbolGetWithHttpInfo(symbol)

Get Etf Holdings

Get stored holdings for a specific ETF

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.getEtfHoldingsV1HoldingsSymbolGetWithHttpInfo(symbol);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#getEtfHoldingsV1HoldingsSymbolGet");
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
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |


## getEtfStatsV1StatsGet

> Object getEtfStatsV1StatsGet()

Get Etf Stats

Get ETF database statistics

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        try {
            Object result = apiInstance.getEtfStatsV1StatsGet();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#getEtfStatsV1StatsGet");
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

**Object**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |

## getEtfStatsV1StatsGetWithHttpInfo

> ApiResponse<Object> getEtfStatsV1StatsGet getEtfStatsV1StatsGetWithHttpInfo()

Get Etf Stats

Get ETF database statistics

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        try {
            ApiResponse<Object> response = apiInstance.getEtfStatsV1StatsGetWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#getEtfStatsV1StatsGet");
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

ApiResponse<**Object**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |


## loadEtfsFromJsonV1LoadFromJsonPost

> Object loadEtfsFromJsonV1LoadFromJsonPost(_file, dryRun)

Load Etfs From Json

Load ETF data from JSON file Accepts etf_details.json and loads all ETFs into database

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        File _file = new File("/path/to/file"); // File | ETF details JSON file
        Boolean dryRun = false; // Boolean | Validate only, don't persist
        try {
            Object result = apiInstance.loadEtfsFromJsonV1LoadFromJsonPost(_file, dryRun);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#loadEtfsFromJsonV1LoadFromJsonPost");
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
| **_file** | **File**| ETF details JSON file | |
| **dryRun** | **Boolean**| Validate only, don&#39;t persist | [optional] [default to false] |

### Return type

**Object**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |

## loadEtfsFromJsonV1LoadFromJsonPostWithHttpInfo

> ApiResponse<Object> loadEtfsFromJsonV1LoadFromJsonPost loadEtfsFromJsonV1LoadFromJsonPostWithHttpInfo(_file, dryRun)

Load Etfs From Json

Load ETF data from JSON file Accepts etf_details.json and loads all ETFs into database

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        File _file = new File("/path/to/file"); // File | ETF details JSON file
        Boolean dryRun = false; // Boolean | Validate only, don't persist
        try {
            ApiResponse<Object> response = apiInstance.loadEtfsFromJsonV1LoadFromJsonPostWithHttpInfo(_file, dryRun);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#loadEtfsFromJsonV1LoadFromJsonPost");
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
| **_file** | **File**| ETF details JSON file | |
| **dryRun** | **Boolean**| Validate only, don&#39;t persist | [optional] [default to false] |

### Return type

ApiResponse<**Object**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: multipart/form-data
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |


## searchEtfsV1SearchGet

> Object searchEtfsV1SearchGet(query, limit)

Search Etfs

Search ETFs by symbol, name, or ISIN

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        String query = "query_example"; // String | Search by symbol, name, or ISIN
        Integer limit = 10; // Integer | Maximum results to return
        try {
            Object result = apiInstance.searchEtfsV1SearchGet(query, limit);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#searchEtfsV1SearchGet");
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
| **query** | **String**| Search by symbol, name, or ISIN | |
| **limit** | **Integer**| Maximum results to return | [optional] [default to 10] |

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
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |

## searchEtfsV1SearchGetWithHttpInfo

> ApiResponse<Object> searchEtfsV1SearchGet searchEtfsV1SearchGetWithHttpInfo(query, limit)

Search Etfs

Search ETFs by symbol, name, or ISIN

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.EtfHoldingsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        EtfHoldingsApi apiInstance = new EtfHoldingsApi(defaultClient);
        String query = "query_example"; // String | Search by symbol, name, or ISIN
        Integer limit = 10; // Integer | Maximum results to return
        try {
            ApiResponse<Object> response = apiInstance.searchEtfsV1SearchGetWithHttpInfo(query, limit);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling EtfHoldingsApi#searchEtfsV1SearchGet");
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
| **query** | **String**| Search by symbol, name, or ISIN | |
| **limit** | **Integer**| Maximum results to return | [optional] [default to 10] |

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
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |

