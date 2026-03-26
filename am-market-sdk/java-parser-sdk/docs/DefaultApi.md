# DefaultApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**healthCheckHealthGet**](DefaultApi.md#healthCheckHealthGet) | **GET** /health | Health Check |
| [**healthCheckHealthGetWithHttpInfo**](DefaultApi.md#healthCheckHealthGetWithHttpInfo) | **GET** /health | Health Check |
| [**rootGet**](DefaultApi.md#rootGet) | **GET** / | Root |
| [**rootGetWithHttpInfo**](DefaultApi.md#rootGetWithHttpInfo) | **GET** / | Root |



## healthCheckHealthGet

> Object healthCheckHealthGet()

Health Check

Health check endpoint

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.DefaultApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        DefaultApi apiInstance = new DefaultApi(defaultClient);
        try {
            Object result = apiInstance.healthCheckHealthGet();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#healthCheckHealthGet");
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

## healthCheckHealthGetWithHttpInfo

> ApiResponse<Object> healthCheckHealthGet healthCheckHealthGetWithHttpInfo()

Health Check

Health check endpoint

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.DefaultApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        DefaultApi apiInstance = new DefaultApi(defaultClient);
        try {
            ApiResponse<Object> response = apiInstance.healthCheckHealthGetWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#healthCheckHealthGet");
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


## rootGet

> Object rootGet()

Root

Root endpoint with API information

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.DefaultApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        DefaultApi apiInstance = new DefaultApi(defaultClient);
        try {
            Object result = apiInstance.rootGet();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#rootGet");
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

## rootGetWithHttpInfo

> ApiResponse<Object> rootGet rootGetWithHttpInfo()

Root

Root endpoint with API information

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.DefaultApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        DefaultApi apiInstance = new DefaultApi(defaultClient);
        try {
            ApiResponse<Object> response = apiInstance.rootGetWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#rootGet");
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

