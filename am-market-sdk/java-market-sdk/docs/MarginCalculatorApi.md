# MarginCalculatorApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**calculateMargin**](MarginCalculatorApi.md#calculateMargin) | **POST** /v1/margin/calculate | Calculate margin requirements |
| [**calculateMarginWithHttpInfo**](MarginCalculatorApi.md#calculateMarginWithHttpInfo) | **POST** /v1/margin/calculate | Calculate margin requirements |
| [**calculateMarginAsync**](MarginCalculatorApi.md#calculateMarginAsync) | **POST** /v1/margin/calculate-async | Calculate margin requirements asynchronously |
| [**calculateMarginAsyncWithHttpInfo**](MarginCalculatorApi.md#calculateMarginAsyncWithHttpInfo) | **POST** /v1/margin/calculate-async | Calculate margin requirements asynchronously |



## calculateMargin

> MarginCalculationResponse calculateMargin(marginCalculationRequest)

Calculate margin requirements

Calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarginCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarginCalculatorApi apiInstance = new MarginCalculatorApi(defaultClient);
        MarginCalculationRequest marginCalculationRequest = new MarginCalculationRequest(); // MarginCalculationRequest | 
        try {
            MarginCalculationResponse result = apiInstance.calculateMargin(marginCalculationRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarginCalculatorApi#calculateMargin");
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
| **marginCalculationRequest** | [**MarginCalculationRequest**](MarginCalculationRequest.md)|  | |

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
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |
| **200** | Margin calculation successful |  -  |

## calculateMarginWithHttpInfo

> ApiResponse<MarginCalculationResponse> calculateMargin calculateMarginWithHttpInfo(marginCalculationRequest)

Calculate margin requirements

Calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarginCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarginCalculatorApi apiInstance = new MarginCalculatorApi(defaultClient);
        MarginCalculationRequest marginCalculationRequest = new MarginCalculationRequest(); // MarginCalculationRequest | 
        try {
            ApiResponse<MarginCalculationResponse> response = apiInstance.calculateMarginWithHttpInfo(marginCalculationRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarginCalculatorApi#calculateMargin");
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
| **marginCalculationRequest** | [**MarginCalculationRequest**](MarginCalculationRequest.md)|  | |

### Return type

ApiResponse<[**MarginCalculationResponse**](MarginCalculationResponse.md)>


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
| **200** | Margin calculation successful |  -  |


## calculateMarginAsync

> MarginCalculationResponse calculateMarginAsync(marginCalculationRequest)

Calculate margin requirements asynchronously

Asynchronously calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarginCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarginCalculatorApi apiInstance = new MarginCalculatorApi(defaultClient);
        MarginCalculationRequest marginCalculationRequest = new MarginCalculationRequest(); // MarginCalculationRequest | 
        try {
            MarginCalculationResponse result = apiInstance.calculateMarginAsync(marginCalculationRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarginCalculatorApi#calculateMarginAsync");
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
| **marginCalculationRequest** | [**MarginCalculationRequest**](MarginCalculationRequest.md)|  | |

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
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |
| **200** | Margin calculation successful |  -  |

## calculateMarginAsyncWithHttpInfo

> ApiResponse<MarginCalculationResponse> calculateMarginAsync calculateMarginAsyncWithHttpInfo(marginCalculationRequest)

Calculate margin requirements asynchronously

Asynchronously calculate SPAN margin, exposure margin, and total margin requirements for a list of positions across different segments

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarginCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarginCalculatorApi apiInstance = new MarginCalculatorApi(defaultClient);
        MarginCalculationRequest marginCalculationRequest = new MarginCalculationRequest(); // MarginCalculationRequest | 
        try {
            ApiResponse<MarginCalculationResponse> response = apiInstance.calculateMarginAsyncWithHttpInfo(marginCalculationRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarginCalculatorApi#calculateMarginAsync");
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
| **marginCalculationRequest** | [**MarginCalculationRequest**](MarginCalculationRequest.md)|  | |

### Return type

ApiResponse<[**MarginCalculationResponse**](MarginCalculationResponse.md)>


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
| **200** | Margin calculation successful |  -  |

