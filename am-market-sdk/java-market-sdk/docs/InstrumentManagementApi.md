# InstrumentManagementApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**searchInstruments**](InstrumentManagementApi.md#searchInstruments) | **POST** /v1/instruments/search | Search Instruments |
| [**searchInstrumentsWithHttpInfo**](InstrumentManagementApi.md#searchInstrumentsWithHttpInfo) | **POST** /v1/instruments/search | Search Instruments |
| [**updateInstruments**](InstrumentManagementApi.md#updateInstruments) | **POST** /v1/instruments/update | Update Instruments from File |
| [**updateInstrumentsWithHttpInfo**](InstrumentManagementApi.md#updateInstrumentsWithHttpInfo) | **POST** /v1/instruments/update | Update Instruments from File |



## searchInstruments

> List<Object> searchInstruments(instrumentSearchCriteria)

Search Instruments

Search for instruments using criteria: list of symbols (&#39;gym balls&#39;), exchanges, and instrument types. Supports semantic text search combined with filters.

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.InstrumentManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        InstrumentManagementApi apiInstance = new InstrumentManagementApi(defaultClient);
        InstrumentSearchCriteria instrumentSearchCriteria = new InstrumentSearchCriteria(); // InstrumentSearchCriteria | 
        try {
            List<Object> result = apiInstance.searchInstruments(instrumentSearchCriteria);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling InstrumentManagementApi#searchInstruments");
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
| **instrumentSearchCriteria** | [**InstrumentSearchCriteria**](InstrumentSearchCriteria.md)|  | |

### Return type

**List&lt;Object&gt;**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## searchInstrumentsWithHttpInfo

> ApiResponse<List<Object>> searchInstruments searchInstrumentsWithHttpInfo(instrumentSearchCriteria)

Search Instruments

Search for instruments using criteria: list of symbols (&#39;gym balls&#39;), exchanges, and instrument types. Supports semantic text search combined with filters.

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.InstrumentManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        InstrumentManagementApi apiInstance = new InstrumentManagementApi(defaultClient);
        InstrumentSearchCriteria instrumentSearchCriteria = new InstrumentSearchCriteria(); // InstrumentSearchCriteria | 
        try {
            ApiResponse<List<Object>> response = apiInstance.searchInstrumentsWithHttpInfo(instrumentSearchCriteria);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling InstrumentManagementApi#searchInstruments");
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
| **instrumentSearchCriteria** | [**InstrumentSearchCriteria**](InstrumentSearchCriteria.md)|  | |

### Return type

ApiResponse<**List&lt;Object&gt;**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## updateInstruments

> String updateInstruments(filePath, provider)

Update Instruments from File

Triggers an update of instruments from the local JSON file (NSE.json). Uses streaming to handle large files.

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.InstrumentManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        InstrumentManagementApi apiInstance = new InstrumentManagementApi(defaultClient);
        String filePath = "filePath_example"; // String | 
        String provider = "UPSTOX"; // String | 
        try {
            String result = apiInstance.updateInstruments(filePath, provider);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling InstrumentManagementApi#updateInstruments");
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
| **filePath** | **String**|  | [optional] |
| **provider** | **String**|  | [optional] [default to UPSTOX] |

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

## updateInstrumentsWithHttpInfo

> ApiResponse<String> updateInstruments updateInstrumentsWithHttpInfo(filePath, provider)

Update Instruments from File

Triggers an update of instruments from the local JSON file (NSE.json). Uses streaming to handle large files.

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.InstrumentManagementApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        InstrumentManagementApi apiInstance = new InstrumentManagementApi(defaultClient);
        String filePath = "filePath_example"; // String | 
        String provider = "UPSTOX"; // String | 
        try {
            ApiResponse<String> response = apiInstance.updateInstrumentsWithHttpInfo(filePath, provider);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling InstrumentManagementApi#updateInstruments");
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
| **filePath** | **String**|  | [optional] |
| **provider** | **String**|  | [optional] [default to UPSTOX] |

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

