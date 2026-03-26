# MutualFundsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getFileStatusV1FilesFileIdGet**](MutualFundsApi.md#getFileStatusV1FilesFileIdGet) | **GET** /v1/files/{file_id} | Get File Status |
| [**getFileStatusV1FilesFileIdGetWithHttpInfo**](MutualFundsApi.md#getFileStatusV1FilesFileIdGetWithHttpInfo) | **GET** /v1/files/{file_id} | Get File Status |
| [**getFundStatisticsV1FundsFundNameStatisticsGet**](MutualFundsApi.md#getFundStatisticsV1FundsFundNameStatisticsGet) | **GET** /v1/funds/{fund_name}/statistics | Get Fund Statistics |
| [**getFundStatisticsV1FundsFundNameStatisticsGetWithHttpInfo**](MutualFundsApi.md#getFundStatisticsV1FundsFundNameStatisticsGetWithHttpInfo) | **GET** /v1/funds/{fund_name}/statistics | Get Fund Statistics |
| [**getHoldingsByIsinV1HoldingsIsinCodeGet**](MutualFundsApi.md#getHoldingsByIsinV1HoldingsIsinCodeGet) | **GET** /v1/holdings/{isin_code} | Get Holdings By Isin |
| [**getHoldingsByIsinV1HoldingsIsinCodeGetWithHttpInfo**](MutualFundsApi.md#getHoldingsByIsinV1HoldingsIsinCodeGetWithHttpInfo) | **GET** /v1/holdings/{isin_code} | Get Holdings By Isin |
| [**getPortfolioV1PortfoliosPortfolioIdGet**](MutualFundsApi.md#getPortfolioV1PortfoliosPortfolioIdGet) | **GET** /v1/portfolios/{portfolio_id} | Get Portfolio |
| [**getPortfolioV1PortfoliosPortfolioIdGetWithHttpInfo**](MutualFundsApi.md#getPortfolioV1PortfoliosPortfolioIdGetWithHttpInfo) | **GET** /v1/portfolios/{portfolio_id} | Get Portfolio |
| [**listFilesV1FilesGet**](MutualFundsApi.md#listFilesV1FilesGet) | **GET** /v1/files | List Files |
| [**listFilesV1FilesGetWithHttpInfo**](MutualFundsApi.md#listFilesV1FilesGetWithHttpInfo) | **GET** /v1/files | List Files |
| [**listPortfoliosV1PortfoliosGet**](MutualFundsApi.md#listPortfoliosV1PortfoliosGet) | **GET** /v1/portfolios | List Portfolios |
| [**listPortfoliosV1PortfoliosGetWithHttpInfo**](MutualFundsApi.md#listPortfoliosV1PortfoliosGetWithHttpInfo) | **GET** /v1/portfolios | List Portfolios |
| [**parseAllSheetsV1ParseAllFileIdPost**](MutualFundsApi.md#parseAllSheetsV1ParseAllFileIdPost) | **POST** /v1/parse-all/{file_id} | Parse All Sheets |
| [**parseAllSheetsV1ParseAllFileIdPostWithHttpInfo**](MutualFundsApi.md#parseAllSheetsV1ParseAllFileIdPostWithHttpInfo) | **POST** /v1/parse-all/{file_id} | Parse All Sheets |
| [**parseSheetV1ParseSheetIdPost**](MutualFundsApi.md#parseSheetV1ParseSheetIdPost) | **POST** /v1/parse/{sheet_id} | Parse Sheet |
| [**parseSheetV1ParseSheetIdPostWithHttpInfo**](MutualFundsApi.md#parseSheetV1ParseSheetIdPostWithHttpInfo) | **POST** /v1/parse/{sheet_id} | Parse Sheet |
| [**processFileV1ProcessFileIdPost**](MutualFundsApi.md#processFileV1ProcessFileIdPost) | **POST** /v1/process/{file_id} | Process File |
| [**processFileV1ProcessFileIdPostWithHttpInfo**](MutualFundsApi.md#processFileV1ProcessFileIdPostWithHttpInfo) | **POST** /v1/process/{file_id} | Process File |
| [**savePortfolioV1PortfoliosPost**](MutualFundsApi.md#savePortfolioV1PortfoliosPost) | **POST** /v1/portfolios | Save Portfolio |
| [**savePortfolioV1PortfoliosPostWithHttpInfo**](MutualFundsApi.md#savePortfolioV1PortfoliosPostWithHttpInfo) | **POST** /v1/portfolios | Save Portfolio |
| [**searchPortfoliosV1PortfoliosSearchGet**](MutualFundsApi.md#searchPortfoliosV1PortfoliosSearchGet) | **GET** /v1/portfolios/search | Search Portfolios |
| [**searchPortfoliosV1PortfoliosSearchGetWithHttpInfo**](MutualFundsApi.md#searchPortfoliosV1PortfoliosSearchGetWithHttpInfo) | **GET** /v1/portfolios/search | Search Portfolios |
| [**uploadExcelCompleteV1UploadExcelPost**](MutualFundsApi.md#uploadExcelCompleteV1UploadExcelPost) | **POST** /v1/upload/excel | Upload Excel Complete |
| [**uploadExcelCompleteV1UploadExcelPostWithHttpInfo**](MutualFundsApi.md#uploadExcelCompleteV1UploadExcelPostWithHttpInfo) | **POST** /v1/upload/excel | Upload Excel Complete |
| [**uploadFileV1UploadPost**](MutualFundsApi.md#uploadFileV1UploadPost) | **POST** /v1/upload | Upload File |
| [**uploadFileV1UploadPostWithHttpInfo**](MutualFundsApi.md#uploadFileV1UploadPostWithHttpInfo) | **POST** /v1/upload | Upload File |



## getFileStatusV1FilesFileIdGet

> Object getFileStatusV1FilesFileIdGet(fileId)

Get File Status

Get detailed status information for a file and its sheets  - **file_id**: ID of the file to check

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fileId = "fileId_example"; // String | 
        try {
            Object result = apiInstance.getFileStatusV1FilesFileIdGet(fileId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#getFileStatusV1FilesFileIdGet");
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
| **fileId** | **String**|  | |

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

## getFileStatusV1FilesFileIdGetWithHttpInfo

> ApiResponse<Object> getFileStatusV1FilesFileIdGet getFileStatusV1FilesFileIdGetWithHttpInfo(fileId)

Get File Status

Get detailed status information for a file and its sheets  - **file_id**: ID of the file to check

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fileId = "fileId_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.getFileStatusV1FilesFileIdGetWithHttpInfo(fileId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#getFileStatusV1FilesFileIdGet");
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
| **fileId** | **String**|  | |

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


## getFundStatisticsV1FundsFundNameStatisticsGet

> Object getFundStatisticsV1FundsFundNameStatisticsGet(fundName)

Get Fund Statistics

Get statistics for a specific fund  Args:     fund_name: Name of the mutual fund      Returns:     Fund statistics

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fundName = "fundName_example"; // String | 
        try {
            Object result = apiInstance.getFundStatisticsV1FundsFundNameStatisticsGet(fundName);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#getFundStatisticsV1FundsFundNameStatisticsGet");
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
| **fundName** | **String**|  | |

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

## getFundStatisticsV1FundsFundNameStatisticsGetWithHttpInfo

> ApiResponse<Object> getFundStatisticsV1FundsFundNameStatisticsGet getFundStatisticsV1FundsFundNameStatisticsGetWithHttpInfo(fundName)

Get Fund Statistics

Get statistics for a specific fund  Args:     fund_name: Name of the mutual fund      Returns:     Fund statistics

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fundName = "fundName_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.getFundStatisticsV1FundsFundNameStatisticsGetWithHttpInfo(fundName);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#getFundStatisticsV1FundsFundNameStatisticsGet");
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
| **fundName** | **String**|  | |

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


## getHoldingsByIsinV1HoldingsIsinCodeGet

> Object getHoldingsByIsinV1HoldingsIsinCodeGet(isinCode)

Get Holdings By Isin

Get all holdings with specific ISIN code  Args:     isin_code: ISIN code to search for      Returns:     List of holdings with the specified ISIN

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String isinCode = "isinCode_example"; // String | 
        try {
            Object result = apiInstance.getHoldingsByIsinV1HoldingsIsinCodeGet(isinCode);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#getHoldingsByIsinV1HoldingsIsinCodeGet");
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
| **isinCode** | **String**|  | |

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

## getHoldingsByIsinV1HoldingsIsinCodeGetWithHttpInfo

> ApiResponse<Object> getHoldingsByIsinV1HoldingsIsinCodeGet getHoldingsByIsinV1HoldingsIsinCodeGetWithHttpInfo(isinCode)

Get Holdings By Isin

Get all holdings with specific ISIN code  Args:     isin_code: ISIN code to search for      Returns:     List of holdings with the specified ISIN

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String isinCode = "isinCode_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.getHoldingsByIsinV1HoldingsIsinCodeGetWithHttpInfo(isinCode);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#getHoldingsByIsinV1HoldingsIsinCodeGet");
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
| **isinCode** | **String**|  | |

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


## getPortfolioV1PortfoliosPortfolioIdGet

> Object getPortfolioV1PortfoliosPortfolioIdGet(portfolioId)

Get Portfolio

Get a specific portfolio by ID  Args:     portfolio_id: MongoDB ObjectId of the portfolio      Returns:     Portfolio data if found

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String portfolioId = "portfolioId_example"; // String | 
        try {
            Object result = apiInstance.getPortfolioV1PortfoliosPortfolioIdGet(portfolioId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#getPortfolioV1PortfoliosPortfolioIdGet");
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
| **portfolioId** | **String**|  | |

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

## getPortfolioV1PortfoliosPortfolioIdGetWithHttpInfo

> ApiResponse<Object> getPortfolioV1PortfoliosPortfolioIdGet getPortfolioV1PortfoliosPortfolioIdGetWithHttpInfo(portfolioId)

Get Portfolio

Get a specific portfolio by ID  Args:     portfolio_id: MongoDB ObjectId of the portfolio      Returns:     Portfolio data if found

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String portfolioId = "portfolioId_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.getPortfolioV1PortfoliosPortfolioIdGetWithHttpInfo(portfolioId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#getPortfolioV1PortfoliosPortfolioIdGet");
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
| **portfolioId** | **String**|  | |

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


## listFilesV1FilesGet

> FileListResponse listFilesV1FilesGet(skip, limit, statusFilter)

List Files

List uploaded files with optional filtering  - **skip**: Number of records to skip (for pagination) - **limit**: Maximum number of records to return - **status_filter**: Filter by processing status

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        Integer skip = 0; // Integer | 
        Integer limit = 100; // Integer | 
        String statusFilter = "statusFilter_example"; // String | 
        try {
            FileListResponse result = apiInstance.listFilesV1FilesGet(skip, limit, statusFilter);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#listFilesV1FilesGet");
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
| **skip** | **Integer**|  | [optional] [default to 0] |
| **limit** | **Integer**|  | [optional] [default to 100] |
| **statusFilter** | **String**|  | [optional] |

### Return type

[**FileListResponse**](FileListResponse.md)


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

## listFilesV1FilesGetWithHttpInfo

> ApiResponse<FileListResponse> listFilesV1FilesGet listFilesV1FilesGetWithHttpInfo(skip, limit, statusFilter)

List Files

List uploaded files with optional filtering  - **skip**: Number of records to skip (for pagination) - **limit**: Maximum number of records to return - **status_filter**: Filter by processing status

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        Integer skip = 0; // Integer | 
        Integer limit = 100; // Integer | 
        String statusFilter = "statusFilter_example"; // String | 
        try {
            ApiResponse<FileListResponse> response = apiInstance.listFilesV1FilesGetWithHttpInfo(skip, limit, statusFilter);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#listFilesV1FilesGet");
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
| **skip** | **Integer**|  | [optional] [default to 0] |
| **limit** | **Integer**|  | [optional] [default to 100] |
| **statusFilter** | **String**|  | [optional] |

### Return type

ApiResponse<[**FileListResponse**](FileListResponse.md)>


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


## listPortfoliosV1PortfoliosGet

> Object listPortfoliosV1PortfoliosGet(fundName, limit)

List Portfolios

List all portfolios or filter by fund name  Args:     fund_name: Optional fund name to filter by     limit: Maximum number of portfolios to return (default: 50)      Returns:     List of portfolio summaries

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fundName = "fundName_example"; // String | 
        Integer limit = 50; // Integer | 
        try {
            Object result = apiInstance.listPortfoliosV1PortfoliosGet(fundName, limit);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#listPortfoliosV1PortfoliosGet");
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
| **fundName** | **String**|  | [optional] |
| **limit** | **Integer**|  | [optional] [default to 50] |

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

## listPortfoliosV1PortfoliosGetWithHttpInfo

> ApiResponse<Object> listPortfoliosV1PortfoliosGet listPortfoliosV1PortfoliosGetWithHttpInfo(fundName, limit)

List Portfolios

List all portfolios or filter by fund name  Args:     fund_name: Optional fund name to filter by     limit: Maximum number of portfolios to return (default: 50)      Returns:     List of portfolio summaries

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fundName = "fundName_example"; // String | 
        Integer limit = 50; // Integer | 
        try {
            ApiResponse<Object> response = apiInstance.listPortfoliosV1PortfoliosGetWithHttpInfo(fundName, limit);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#listPortfoliosV1PortfoliosGet");
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
| **fundName** | **String**|  | [optional] |
| **limit** | **Integer**|  | [optional] [default to 50] |

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


## parseAllSheetsV1ParseAllFileIdPost

> Object parseAllSheetsV1ParseAllFileIdPost(fileId, method, apiKey)

Parse All Sheets

Parse all sheets for a given Excel file  - **file_id**: ID of the Excel file - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fileId = "fileId_example"; // String | 
        String method = "manual"; // String | 
        String apiKey = "apiKey_example"; // String | 
        try {
            Object result = apiInstance.parseAllSheetsV1ParseAllFileIdPost(fileId, method, apiKey);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#parseAllSheetsV1ParseAllFileIdPost");
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
| **fileId** | **String**|  | |
| **method** | **String**|  | [optional] [default to manual] |
| **apiKey** | **String**|  | [optional] |

### Return type

**Object**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/x-www-form-urlencoded
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |

## parseAllSheetsV1ParseAllFileIdPostWithHttpInfo

> ApiResponse<Object> parseAllSheetsV1ParseAllFileIdPost parseAllSheetsV1ParseAllFileIdPostWithHttpInfo(fileId, method, apiKey)

Parse All Sheets

Parse all sheets for a given Excel file  - **file_id**: ID of the Excel file - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fileId = "fileId_example"; // String | 
        String method = "manual"; // String | 
        String apiKey = "apiKey_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.parseAllSheetsV1ParseAllFileIdPostWithHttpInfo(fileId, method, apiKey);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#parseAllSheetsV1ParseAllFileIdPost");
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
| **fileId** | **String**|  | |
| **method** | **String**|  | [optional] [default to manual] |
| **apiKey** | **String**|  | [optional] |

### Return type

ApiResponse<**Object**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/x-www-form-urlencoded
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |


## parseSheetV1ParseSheetIdPost

> Object parseSheetV1ParseSheetIdPost(sheetId, method, apiKey)

Parse Sheet

Parse an individual sheet file to extract portfolio data  - **sheet_id**: ID of the sheet file to parse - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing (required for &#39;together&#39; method)

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String sheetId = "sheetId_example"; // String | 
        String method = "manual"; // String | 
        String apiKey = "apiKey_example"; // String | 
        try {
            Object result = apiInstance.parseSheetV1ParseSheetIdPost(sheetId, method, apiKey);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#parseSheetV1ParseSheetIdPost");
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
| **sheetId** | **String**|  | |
| **method** | **String**|  | [optional] [default to manual] |
| **apiKey** | **String**|  | [optional] |

### Return type

**Object**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/x-www-form-urlencoded
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |

## parseSheetV1ParseSheetIdPostWithHttpInfo

> ApiResponse<Object> parseSheetV1ParseSheetIdPost parseSheetV1ParseSheetIdPostWithHttpInfo(sheetId, method, apiKey)

Parse Sheet

Parse an individual sheet file to extract portfolio data  - **sheet_id**: ID of the sheet file to parse - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing (required for &#39;together&#39; method)

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String sheetId = "sheetId_example"; // String | 
        String method = "manual"; // String | 
        String apiKey = "apiKey_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.parseSheetV1ParseSheetIdPostWithHttpInfo(sheetId, method, apiKey);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#parseSheetV1ParseSheetIdPost");
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
| **sheetId** | **String**|  | |
| **method** | **String**|  | [optional] [default to manual] |
| **apiKey** | **String**|  | [optional] |

### Return type

ApiResponse<**Object**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/x-www-form-urlencoded
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Successful Response |  -  |
| **422** | Validation Error |  -  |


## processFileV1ProcessFileIdPost

> Object processFileV1ProcessFileIdPost(fileId)

Process File

Process an uploaded Excel file by splitting it into individual sheet files  - **file_id**: ID of the uploaded Excel file  This endpoint splits the Excel file into individual sheet files and stores them

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fileId = "fileId_example"; // String | 
        try {
            Object result = apiInstance.processFileV1ProcessFileIdPost(fileId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#processFileV1ProcessFileIdPost");
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
| **fileId** | **String**|  | |

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

## processFileV1ProcessFileIdPostWithHttpInfo

> ApiResponse<Object> processFileV1ProcessFileIdPost processFileV1ProcessFileIdPostWithHttpInfo(fileId)

Process File

Process an uploaded Excel file by splitting it into individual sheet files  - **file_id**: ID of the uploaded Excel file  This endpoint splits the Excel file into individual sheet files and stores them

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fileId = "fileId_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.processFileV1ProcessFileIdPostWithHttpInfo(fileId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#processFileV1ProcessFileIdPost");
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
| **fileId** | **String**|  | |

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


## savePortfolioV1PortfoliosPost

> Object savePortfolioV1PortfoliosPost(body)

Save Portfolio

Save a mutual fund portfolio to the database  Args:     portfolio_data: JSON data containing mutual fund portfolio information      Returns:     Saved portfolio data with database ID

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        Object body = null; // Object | 
        try {
            Object result = apiInstance.savePortfolioV1PortfoliosPost(body);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#savePortfolioV1PortfoliosPost");
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
| **body** | **Object**|  | |

### Return type

**Object**


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Successful Response |  -  |
| **422** | Validation Error |  -  |

## savePortfolioV1PortfoliosPostWithHttpInfo

> ApiResponse<Object> savePortfolioV1PortfoliosPost savePortfolioV1PortfoliosPostWithHttpInfo(body)

Save Portfolio

Save a mutual fund portfolio to the database  Args:     portfolio_data: JSON data containing mutual fund portfolio information      Returns:     Saved portfolio data with database ID

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        Object body = null; // Object | 
        try {
            ApiResponse<Object> response = apiInstance.savePortfolioV1PortfoliosPostWithHttpInfo(body);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#savePortfolioV1PortfoliosPost");
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
| **body** | **Object**|  | |

### Return type

ApiResponse<**Object**>


### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Successful Response |  -  |
| **422** | Validation Error |  -  |


## searchPortfoliosV1PortfoliosSearchGet

> Object searchPortfoliosV1PortfoliosSearchGet(fundName)

Search Portfolios

Search portfolios by fund name  Args:     fund_name: Fund name to search for      Returns:     List of matching portfolio summaries

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fundName = "fundName_example"; // String | 
        try {
            Object result = apiInstance.searchPortfoliosV1PortfoliosSearchGet(fundName);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#searchPortfoliosV1PortfoliosSearchGet");
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
| **fundName** | **String**|  | |

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

## searchPortfoliosV1PortfoliosSearchGetWithHttpInfo

> ApiResponse<Object> searchPortfoliosV1PortfoliosSearchGet searchPortfoliosV1PortfoliosSearchGetWithHttpInfo(fundName)

Search Portfolios

Search portfolios by fund name  Args:     fund_name: Fund name to search for      Returns:     List of matching portfolio summaries

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        String fundName = "fundName_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.searchPortfoliosV1PortfoliosSearchGetWithHttpInfo(fundName);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#searchPortfoliosV1PortfoliosSearchGet");
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
| **fundName** | **String**|  | |

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


## uploadExcelCompleteV1UploadExcelPost

> Object uploadExcelCompleteV1UploadExcelPost(_file, parseMethod)

Upload Excel Complete

🚀 Complete Excel Upload Workflow - Does EVERYTHING automatically!  This endpoint handles the complete workflow: 1. ✅ Upload Excel file 2. ✅ Persist main file to database   3. ✅ Split Excel into individual sheet files 4. ✅ Persist all sheet files to database 5. ✅ Parse each sheet using manual or LLM parsing 6. ✅ Save all parsed portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: \&quot;together\&quot; (default) or \&quot;manual\&quot;  Returns: Complete results with all parsed portfolios

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        File _file = new File("/path/to/file"); // File | 
        String parseMethod = "together"; // String | 
        try {
            Object result = apiInstance.uploadExcelCompleteV1UploadExcelPost(_file, parseMethod);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#uploadExcelCompleteV1UploadExcelPost");
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
| **_file** | **File**|  | |
| **parseMethod** | **String**|  | [optional] [default to together] |

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

## uploadExcelCompleteV1UploadExcelPostWithHttpInfo

> ApiResponse<Object> uploadExcelCompleteV1UploadExcelPost uploadExcelCompleteV1UploadExcelPostWithHttpInfo(_file, parseMethod)

Upload Excel Complete

🚀 Complete Excel Upload Workflow - Does EVERYTHING automatically!  This endpoint handles the complete workflow: 1. ✅ Upload Excel file 2. ✅ Persist main file to database   3. ✅ Split Excel into individual sheet files 4. ✅ Persist all sheet files to database 5. ✅ Parse each sheet using manual or LLM parsing 6. ✅ Save all parsed portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: \&quot;together\&quot; (default) or \&quot;manual\&quot;  Returns: Complete results with all parsed portfolios

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        File _file = new File("/path/to/file"); // File | 
        String parseMethod = "together"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.uploadExcelCompleteV1UploadExcelPostWithHttpInfo(_file, parseMethod);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#uploadExcelCompleteV1UploadExcelPost");
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
| **_file** | **File**|  | |
| **parseMethod** | **String**|  | [optional] [default to together] |

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


## uploadFileV1UploadPost

> Object uploadFileV1UploadPost(_file, parseMethod)

Upload File

Upload an Excel file and do ALL the work automatically: 1. Upload and persist main file to database 2. Split Excel into individual sheet files   3. Persist all sheet files to database 4. Parse each sheet and save portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: Parsing method (\&quot;manual\&quot; or \&quot;together\&quot;) - defaults to \&quot;together\&quot;  Returns complete processing results with all parsed portfolios

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        File _file = new File("/path/to/file"); // File | 
        String parseMethod = "together"; // String | 
        try {
            Object result = apiInstance.uploadFileV1UploadPost(_file, parseMethod);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#uploadFileV1UploadPost");
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
| **_file** | **File**|  | |
| **parseMethod** | **String**|  | [optional] [default to together] |

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

## uploadFileV1UploadPostWithHttpInfo

> ApiResponse<Object> uploadFileV1UploadPost uploadFileV1UploadPostWithHttpInfo(_file, parseMethod)

Upload File

Upload an Excel file and do ALL the work automatically: 1. Upload and persist main file to database 2. Split Excel into individual sheet files   3. Persist all sheet files to database 4. Parse each sheet and save portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: Parsing method (\&quot;manual\&quot; or \&quot;together\&quot;) - defaults to \&quot;together\&quot;  Returns complete processing results with all parsed portfolios

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.MutualFundsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        MutualFundsApi apiInstance = new MutualFundsApi(defaultClient);
        File _file = new File("/path/to/file"); // File | 
        String parseMethod = "together"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.uploadFileV1UploadPostWithHttpInfo(_file, parseMethod);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling MutualFundsApi#uploadFileV1UploadPost");
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
| **_file** | **File**|  | |
| **parseMethod** | **String**|  | [optional] [default to together] |

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

