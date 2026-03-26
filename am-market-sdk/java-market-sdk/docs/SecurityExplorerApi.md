# SecurityExplorerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**search**](SecurityExplorerApi.md#search) | **GET** /v1/securities/search | Search securities by symbol or ISIN |
| [**searchWithHttpInfo**](SecurityExplorerApi.md#searchWithHttpInfo) | **GET** /v1/securities/search | Search securities by symbol or ISIN |
| [**searchAdvanced**](SecurityExplorerApi.md#searchAdvanced) | **POST** /v1/securities/search | Advanced search securities with filters |
| [**searchAdvancedWithHttpInfo**](SecurityExplorerApi.md#searchAdvancedWithHttpInfo) | **POST** /v1/securities/search | Advanced search securities with filters |



## search

> List<SecurityDocument> search(query)

Search securities by symbol or ISIN

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.SecurityExplorerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        SecurityExplorerApi apiInstance = new SecurityExplorerApi(defaultClient);
        String query = "query_example"; // String | 
        try {
            List<SecurityDocument> result = apiInstance.search(query);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SecurityExplorerApi#search");
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
| **query** | **String**|  | |

### Return type

[**List&lt;SecurityDocument&gt;**](SecurityDocument.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## searchWithHttpInfo

> ApiResponse<List<SecurityDocument>> search searchWithHttpInfo(query)

Search securities by symbol or ISIN

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.SecurityExplorerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        SecurityExplorerApi apiInstance = new SecurityExplorerApi(defaultClient);
        String query = "query_example"; // String | 
        try {
            ApiResponse<List<SecurityDocument>> response = apiInstance.searchWithHttpInfo(query);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling SecurityExplorerApi#search");
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
| **query** | **String**|  | |

### Return type

ApiResponse<[**List&lt;SecurityDocument&gt;**](SecurityDocument.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## searchAdvanced

> List<SecurityDocument> searchAdvanced(securitySearchRequest)

Advanced search securities with filters

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.SecurityExplorerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        SecurityExplorerApi apiInstance = new SecurityExplorerApi(defaultClient);
        SecuritySearchRequest securitySearchRequest = new SecuritySearchRequest(); // SecuritySearchRequest | 
        try {
            List<SecurityDocument> result = apiInstance.searchAdvanced(securitySearchRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling SecurityExplorerApi#searchAdvanced");
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
| **securitySearchRequest** | [**SecuritySearchRequest**](SecuritySearchRequest.md)|  | |

### Return type

[**List&lt;SecurityDocument&gt;**](SecurityDocument.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## searchAdvancedWithHttpInfo

> ApiResponse<List<SecurityDocument>> searchAdvanced searchAdvancedWithHttpInfo(securitySearchRequest)

Advanced search securities with filters

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.SecurityExplorerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        SecurityExplorerApi apiInstance = new SecurityExplorerApi(defaultClient);
        SecuritySearchRequest securitySearchRequest = new SecuritySearchRequest(); // SecuritySearchRequest | 
        try {
            ApiResponse<List<SecurityDocument>> response = apiInstance.searchAdvancedWithHttpInfo(securitySearchRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling SecurityExplorerApi#searchAdvanced");
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
| **securitySearchRequest** | [**SecuritySearchRequest**](SecuritySearchRequest.md)|  | |

### Return type

ApiResponse<[**List&lt;SecurityDocument&gt;**](SecurityDocument.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

