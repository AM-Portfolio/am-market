# BrokerageCalculatorApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**calculateBreakeven**](BrokerageCalculatorApi.md#calculateBreakeven) | **GET** /v1/brokerage/breakeven | Calculate breakeven price |
| [**calculateBreakevenWithHttpInfo**](BrokerageCalculatorApi.md#calculateBreakevenWithHttpInfo) | **GET** /v1/brokerage/breakeven | Calculate breakeven price |
| [**calculateBrokerage**](BrokerageCalculatorApi.md#calculateBrokerage) | **POST** /v1/brokerage/calculate | Calculate brokerage and taxes |
| [**calculateBrokerageWithHttpInfo**](BrokerageCalculatorApi.md#calculateBrokerageWithHttpInfo) | **POST** /v1/brokerage/calculate | Calculate brokerage and taxes |
| [**calculateBrokerageAsync**](BrokerageCalculatorApi.md#calculateBrokerageAsync) | **POST** /v1/brokerage/calculate-async | Calculate brokerage and taxes asynchronously |
| [**calculateBrokerageAsyncWithHttpInfo**](BrokerageCalculatorApi.md#calculateBrokerageAsyncWithHttpInfo) | **POST** /v1/brokerage/calculate-async | Calculate brokerage and taxes asynchronously |



## calculateBreakeven

> BrokerageCalculationResponse calculateBreakeven(symbol, price, quantity, exchange, tradeType, brokerType)

Calculate breakeven price

Calculate the breakeven price for a stock considering all charges

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.BrokerageCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BrokerageCalculatorApi apiInstance = new BrokerageCalculatorApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        Double price = 3.4D; // Double | 
        Integer quantity = 56; // Integer | 
        String exchange = "exchange_example"; // String | 
        String tradeType = "tradeType_example"; // String | 
        String brokerType = "brokerType_example"; // String | 
        try {
            BrokerageCalculationResponse result = apiInstance.calculateBreakeven(symbol, price, quantity, exchange, tradeType, brokerType);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BrokerageCalculatorApi#calculateBreakeven");
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
| **price** | **Double**|  | |
| **quantity** | **Integer**|  | |
| **exchange** | **String**|  | |
| **tradeType** | **String**|  | |
| **brokerType** | **String**|  | |

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Breakeven calculation successful |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |

## calculateBreakevenWithHttpInfo

> ApiResponse<BrokerageCalculationResponse> calculateBreakeven calculateBreakevenWithHttpInfo(symbol, price, quantity, exchange, tradeType, brokerType)

Calculate breakeven price

Calculate the breakeven price for a stock considering all charges

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.BrokerageCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BrokerageCalculatorApi apiInstance = new BrokerageCalculatorApi(defaultClient);
        String symbol = "symbol_example"; // String | 
        Double price = 3.4D; // Double | 
        Integer quantity = 56; // Integer | 
        String exchange = "exchange_example"; // String | 
        String tradeType = "tradeType_example"; // String | 
        String brokerType = "brokerType_example"; // String | 
        try {
            ApiResponse<BrokerageCalculationResponse> response = apiInstance.calculateBreakevenWithHttpInfo(symbol, price, quantity, exchange, tradeType, brokerType);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BrokerageCalculatorApi#calculateBreakeven");
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
| **price** | **Double**|  | |
| **quantity** | **Integer**|  | |
| **exchange** | **String**|  | |
| **tradeType** | **String**|  | |
| **brokerType** | **String**|  | |

### Return type

ApiResponse<[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Breakeven calculation successful |  -  |
| **400** | Invalid request parameters |  -  |
| **500** | Internal server error |  -  |


## calculateBrokerage

> BrokerageCalculationResponse calculateBrokerage(brokerageCalculationRequest)

Calculate brokerage and taxes

Calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.BrokerageCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BrokerageCalculatorApi apiInstance = new BrokerageCalculatorApi(defaultClient);
        BrokerageCalculationRequest brokerageCalculationRequest = new BrokerageCalculationRequest(); // BrokerageCalculationRequest | 
        try {
            BrokerageCalculationResponse result = apiInstance.calculateBrokerage(brokerageCalculationRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BrokerageCalculatorApi#calculateBrokerage");
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
| **brokerageCalculationRequest** | [**BrokerageCalculationRequest**](BrokerageCalculationRequest.md)|  | |

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)


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
| **200** | Brokerage calculation successful |  -  |

## calculateBrokerageWithHttpInfo

> ApiResponse<BrokerageCalculationResponse> calculateBrokerage calculateBrokerageWithHttpInfo(brokerageCalculationRequest)

Calculate brokerage and taxes

Calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.BrokerageCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BrokerageCalculatorApi apiInstance = new BrokerageCalculatorApi(defaultClient);
        BrokerageCalculationRequest brokerageCalculationRequest = new BrokerageCalculationRequest(); // BrokerageCalculationRequest | 
        try {
            ApiResponse<BrokerageCalculationResponse> response = apiInstance.calculateBrokerageWithHttpInfo(brokerageCalculationRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BrokerageCalculatorApi#calculateBrokerage");
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
| **brokerageCalculationRequest** | [**BrokerageCalculationRequest**](BrokerageCalculationRequest.md)|  | |

### Return type

ApiResponse<[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)>


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
| **200** | Brokerage calculation successful |  -  |


## calculateBrokerageAsync

> BrokerageCalculationResponse calculateBrokerageAsync(brokerageCalculationRequest)

Calculate brokerage and taxes asynchronously

Asynchronously calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.BrokerageCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BrokerageCalculatorApi apiInstance = new BrokerageCalculatorApi(defaultClient);
        BrokerageCalculationRequest brokerageCalculationRequest = new BrokerageCalculationRequest(); // BrokerageCalculationRequest | 
        try {
            BrokerageCalculationResponse result = apiInstance.calculateBrokerageAsync(brokerageCalculationRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BrokerageCalculatorApi#calculateBrokerageAsync");
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
| **brokerageCalculationRequest** | [**BrokerageCalculationRequest**](BrokerageCalculationRequest.md)|  | |

### Return type

[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)


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
| **200** | Brokerage calculation successful |  -  |

## calculateBrokerageAsyncWithHttpInfo

> ApiResponse<BrokerageCalculationResponse> calculateBrokerageAsync calculateBrokerageAsyncWithHttpInfo(brokerageCalculationRequest)

Calculate brokerage and taxes asynchronously

Asynchronously calculate brokerage, STT, GST, exchange charges, SEBI charges, stamp duty, and DP charges for a trade

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.BrokerageCalculatorApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BrokerageCalculatorApi apiInstance = new BrokerageCalculatorApi(defaultClient);
        BrokerageCalculationRequest brokerageCalculationRequest = new BrokerageCalculationRequest(); // BrokerageCalculationRequest | 
        try {
            ApiResponse<BrokerageCalculationResponse> response = apiInstance.calculateBrokerageAsyncWithHttpInfo(brokerageCalculationRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BrokerageCalculatorApi#calculateBrokerageAsync");
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
| **brokerageCalculationRequest** | [**BrokerageCalculationRequest**](BrokerageCalculationRequest.md)|  | |

### Return type

ApiResponse<[**BrokerageCalculationResponse**](BrokerageCalculationResponse.md)>


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
| **200** | Brokerage calculation successful |  -  |

