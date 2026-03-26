# MarketDataStreamApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**connect**](MarketDataStreamApi.md#connect) | **POST** /v1/market-data/stream/connect | Connect to market data stream |
| [**connectWithHttpInfo**](MarketDataStreamApi.md#connectWithHttpInfo) | **POST** /v1/market-data/stream/connect | Connect to market data stream |
| [**disconnect**](MarketDataStreamApi.md#disconnect) | **POST** /v1/market-data/stream/disconnect | Disconnect market data stream |
| [**disconnectWithHttpInfo**](MarketDataStreamApi.md#disconnectWithHttpInfo) | **POST** /v1/market-data/stream/disconnect | Disconnect market data stream |
| [**initiate**](MarketDataStreamApi.md#initiate) | **POST** /v1/market-data/stream/initiate | Connect to market data stream |
| [**initiateWithHttpInfo**](MarketDataStreamApi.md#initiateWithHttpInfo) | **POST** /v1/market-data/stream/initiate | Connect to market data stream |



## connect

> String connect(streamConnectRequest)

Connect to market data stream

Initiates a WebSocket connection for the specified provider and instruments

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataStreamApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataStreamApi apiInstance = new MarketDataStreamApi(defaultClient);
        StreamConnectRequest streamConnectRequest = new StreamConnectRequest(); // StreamConnectRequest | 
        try {
            String result = apiInstance.connect(streamConnectRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataStreamApi#connect");
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
| **streamConnectRequest** | [**StreamConnectRequest**](StreamConnectRequest.md)|  | |

### Return type

**String**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## connectWithHttpInfo

> ApiResponse<String> connect connectWithHttpInfo(streamConnectRequest)

Connect to market data stream

Initiates a WebSocket connection for the specified provider and instruments

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataStreamApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataStreamApi apiInstance = new MarketDataStreamApi(defaultClient);
        StreamConnectRequest streamConnectRequest = new StreamConnectRequest(); // StreamConnectRequest | 
        try {
            ApiResponse<String> response = apiInstance.connectWithHttpInfo(streamConnectRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataStreamApi#connect");
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
| **streamConnectRequest** | [**StreamConnectRequest**](StreamConnectRequest.md)|  | |

### Return type

ApiResponse<**String**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |


## disconnect

> String disconnect(provider)

Disconnect market data stream

Disconnects the WebSocket stream for the specified provider

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataStreamApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataStreamApi apiInstance = new MarketDataStreamApi(defaultClient);
        String provider = "provider_example"; // String | 
        try {
            String result = apiInstance.disconnect(provider);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataStreamApi#disconnect");
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

## disconnectWithHttpInfo

> ApiResponse<String> disconnect disconnectWithHttpInfo(provider)

Disconnect market data stream

Disconnects the WebSocket stream for the specified provider

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataStreamApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataStreamApi apiInstance = new MarketDataStreamApi(defaultClient);
        String provider = "provider_example"; // String | 
        try {
            ApiResponse<String> response = apiInstance.disconnectWithHttpInfo(provider);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataStreamApi#disconnect");
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


## initiate

> StreamConnectResponse initiate(streamConnectRequest)

Connect to market data stream

Initiates a WebSocket connection and returns a structured response

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataStreamApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataStreamApi apiInstance = new MarketDataStreamApi(defaultClient);
        StreamConnectRequest streamConnectRequest = new StreamConnectRequest(); // StreamConnectRequest | 
        try {
            StreamConnectResponse result = apiInstance.initiate(streamConnectRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataStreamApi#initiate");
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
| **streamConnectRequest** | [**StreamConnectRequest**](StreamConnectRequest.md)|  | |

### Return type

[**StreamConnectResponse**](StreamConnectResponse.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## initiateWithHttpInfo

> ApiResponse<StreamConnectResponse> initiate initiateWithHttpInfo(streamConnectRequest)

Connect to market data stream

Initiates a WebSocket connection and returns a structured response

### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.MarketDataStreamApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MarketDataStreamApi apiInstance = new MarketDataStreamApi(defaultClient);
        StreamConnectRequest streamConnectRequest = new StreamConnectRequest(); // StreamConnectRequest | 
        try {
            ApiResponse<StreamConnectResponse> response = apiInstance.initiateWithHttpInfo(streamConnectRequest);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MarketDataStreamApi#initiate");
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
| **streamConnectRequest** | [**StreamConnectRequest**](StreamConnectRequest.md)|  | |

### Return type

ApiResponse<[**StreamConnectResponse**](StreamConnectResponse.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

