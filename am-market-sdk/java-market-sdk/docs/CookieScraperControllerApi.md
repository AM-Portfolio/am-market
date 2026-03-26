# CookieScraperControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**scrapeCookies**](CookieScraperControllerApi.md#scrapeCookies) | **GET** /api/scraper/cookies |  |
| [**scrapeCookiesWithHttpInfo**](CookieScraperControllerApi.md#scrapeCookiesWithHttpInfo) | **GET** /api/scraper/cookies |  |



## scrapeCookies

> List<WebsiteCookies> scrapeCookies()



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.CookieScraperControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        CookieScraperControllerApi apiInstance = new CookieScraperControllerApi(defaultClient);
        try {
            List<WebsiteCookies> result = apiInstance.scrapeCookies();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling CookieScraperControllerApi#scrapeCookies");
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

[**List&lt;WebsiteCookies&gt;**](WebsiteCookies.md)


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

## scrapeCookiesWithHttpInfo

> ApiResponse<List<WebsiteCookies>> scrapeCookies scrapeCookiesWithHttpInfo()



### Example

```java
// Import classes:
import com.am.portfolio.client.market.invoker.ApiClient;
import com.am.portfolio.client.market.invoker.ApiException;
import com.am.portfolio.client.market.invoker.ApiResponse;
import com.am.portfolio.client.market.invoker.Configuration;
import com.am.portfolio.client.market.invoker.models.*;
import com.am.portfolio.client.market.api.CookieScraperControllerApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        CookieScraperControllerApi apiInstance = new CookieScraperControllerApi(defaultClient);
        try {
            ApiResponse<List<WebsiteCookies>> response = apiInstance.scrapeCookiesWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling CookieScraperControllerApi#scrapeCookies");
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

ApiResponse<[**List&lt;WebsiteCookies&gt;**](WebsiteCookies.md)>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | OK |  -  |

