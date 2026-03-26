# BackgroundJobsApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**cancelJobV1JobIdDelete**](BackgroundJobsApi.md#cancelJobV1JobIdDelete) | **DELETE** /v1/{job_id} | Cancel Job |
| [**cancelJobV1JobIdDeleteWithHttpInfo**](BackgroundJobsApi.md#cancelJobV1JobIdDeleteWithHttpInfo) | **DELETE** /v1/{job_id} | Cancel Job |
| [**fixStuckJobV1AdminFixStuckJobJobIdPost**](BackgroundJobsApi.md#fixStuckJobV1AdminFixStuckJobJobIdPost) | **POST** /v1/admin/fix-stuck-job/{job_id} | Fix Stuck Job |
| [**fixStuckJobV1AdminFixStuckJobJobIdPostWithHttpInfo**](BackgroundJobsApi.md#fixStuckJobV1AdminFixStuckJobJobIdPostWithHttpInfo) | **POST** /v1/admin/fix-stuck-job/{job_id} | Fix Stuck Job |
| [**getJobResultV1JobIdResultGet**](BackgroundJobsApi.md#getJobResultV1JobIdResultGet) | **GET** /v1/{job_id}/result | Get Job Result |
| [**getJobResultV1JobIdResultGetWithHttpInfo**](BackgroundJobsApi.md#getJobResultV1JobIdResultGetWithHttpInfo) | **GET** /v1/{job_id}/result | Get Job Result |
| [**getJobStatusV1JobIdStatusGet**](BackgroundJobsApi.md#getJobStatusV1JobIdStatusGet) | **GET** /v1/{job_id}/status | Get Job Status |
| [**getJobStatusV1JobIdStatusGetWithHttpInfo**](BackgroundJobsApi.md#getJobStatusV1JobIdStatusGetWithHttpInfo) | **GET** /v1/{job_id}/status | Get Job Status |
| [**listJobsV1Get**](BackgroundJobsApi.md#listJobsV1Get) | **GET** /v1/ | List Jobs |
| [**listJobsV1GetWithHttpInfo**](BackgroundJobsApi.md#listJobsV1GetWithHttpInfo) | **GET** /v1/ | List Jobs |
| [**recoverAllStuckJobsV1AdminRecoverStuckJobsPost**](BackgroundJobsApi.md#recoverAllStuckJobsV1AdminRecoverStuckJobsPost) | **POST** /v1/admin/recover-stuck-jobs | Recover All Stuck Jobs |
| [**recoverAllStuckJobsV1AdminRecoverStuckJobsPostWithHttpInfo**](BackgroundJobsApi.md#recoverAllStuckJobsV1AdminRecoverStuckJobsPostWithHttpInfo) | **POST** /v1/admin/recover-stuck-jobs | Recover All Stuck Jobs |
| [**uploadExcelAsyncV1UploadExcelAsyncPost**](BackgroundJobsApi.md#uploadExcelAsyncV1UploadExcelAsyncPost) | **POST** /v1/upload-excel-async | Upload Excel Async |
| [**uploadExcelAsyncV1UploadExcelAsyncPostWithHttpInfo**](BackgroundJobsApi.md#uploadExcelAsyncV1UploadExcelAsyncPostWithHttpInfo) | **POST** /v1/upload-excel-async | Upload Excel Async |



## cancelJobV1JobIdDelete

> Object cancelJobV1JobIdDelete(jobId)

Cancel Job

Cancel a pending or running job

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        try {
            Object result = apiInstance.cancelJobV1JobIdDelete(jobId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#cancelJobV1JobIdDelete");
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

## cancelJobV1JobIdDeleteWithHttpInfo

> ApiResponse<Object> cancelJobV1JobIdDelete cancelJobV1JobIdDeleteWithHttpInfo(jobId)

Cancel Job

Cancel a pending or running job

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.cancelJobV1JobIdDeleteWithHttpInfo(jobId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#cancelJobV1JobIdDelete");
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


## fixStuckJobV1AdminFixStuckJobJobIdPost

> Object fixStuckJobV1AdminFixStuckJobJobIdPost(jobId, markAsFailed)

Fix Stuck Job

Admin endpoint to fix stuck jobs Used when jobs get stuck due to server restarts

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        Boolean markAsFailed = true; // Boolean | 
        try {
            Object result = apiInstance.fixStuckJobV1AdminFixStuckJobJobIdPost(jobId, markAsFailed);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#fixStuckJobV1AdminFixStuckJobJobIdPost");
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
| **markAsFailed** | **Boolean**|  | [optional] [default to true] |

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

## fixStuckJobV1AdminFixStuckJobJobIdPostWithHttpInfo

> ApiResponse<Object> fixStuckJobV1AdminFixStuckJobJobIdPost fixStuckJobV1AdminFixStuckJobJobIdPostWithHttpInfo(jobId, markAsFailed)

Fix Stuck Job

Admin endpoint to fix stuck jobs Used when jobs get stuck due to server restarts

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        Boolean markAsFailed = true; // Boolean | 
        try {
            ApiResponse<Object> response = apiInstance.fixStuckJobV1AdminFixStuckJobJobIdPostWithHttpInfo(jobId, markAsFailed);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#fixStuckJobV1AdminFixStuckJobJobIdPost");
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
| **markAsFailed** | **Boolean**|  | [optional] [default to true] |

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


## getJobResultV1JobIdResultGet

> Object getJobResultV1JobIdResultGet(jobId)

Get Job Result

Get the result of a completed job

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        try {
            Object result = apiInstance.getJobResultV1JobIdResultGet(jobId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#getJobResultV1JobIdResultGet");
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

## getJobResultV1JobIdResultGetWithHttpInfo

> ApiResponse<Object> getJobResultV1JobIdResultGet getJobResultV1JobIdResultGetWithHttpInfo(jobId)

Get Job Result

Get the result of a completed job

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        try {
            ApiResponse<Object> response = apiInstance.getJobResultV1JobIdResultGetWithHttpInfo(jobId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#getJobResultV1JobIdResultGet");
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


## getJobStatusV1JobIdStatusGet

> JobStatusResponse getJobStatusV1JobIdStatusGet(jobId)

Get Job Status

Get the current status of a background job

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        try {
            JobStatusResponse result = apiInstance.getJobStatusV1JobIdStatusGet(jobId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#getJobStatusV1JobIdStatusGet");
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

[**JobStatusResponse**](JobStatusResponse.md)


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

## getJobStatusV1JobIdStatusGetWithHttpInfo

> ApiResponse<JobStatusResponse> getJobStatusV1JobIdStatusGet getJobStatusV1JobIdStatusGetWithHttpInfo(jobId)

Get Job Status

Get the current status of a background job

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        String jobId = "jobId_example"; // String | 
        try {
            ApiResponse<JobStatusResponse> response = apiInstance.getJobStatusV1JobIdStatusGetWithHttpInfo(jobId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#getJobStatusV1JobIdStatusGet");
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

ApiResponse<[**JobStatusResponse**](JobStatusResponse.md)>


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


## listJobsV1Get

> Object listJobsV1Get(jobStatus, userId, limit)

List Jobs

List background jobs with optional filtering

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        JobStatus jobStatus = JobStatus.fromValue("pending"); // JobStatus | 
        String userId = "userId_example"; // String | 
        Integer limit = 50; // Integer | 
        try {
            Object result = apiInstance.listJobsV1Get(jobStatus, userId, limit);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#listJobsV1Get");
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
| **jobStatus** | [**JobStatus**](.md)|  | [optional] [enum: pending, running, completed, failed, cancelled] |
| **userId** | **String**|  | [optional] |
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

## listJobsV1GetWithHttpInfo

> ApiResponse<Object> listJobsV1Get listJobsV1GetWithHttpInfo(jobStatus, userId, limit)

List Jobs

List background jobs with optional filtering

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        JobStatus jobStatus = JobStatus.fromValue("pending"); // JobStatus | 
        String userId = "userId_example"; // String | 
        Integer limit = 50; // Integer | 
        try {
            ApiResponse<Object> response = apiInstance.listJobsV1GetWithHttpInfo(jobStatus, userId, limit);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#listJobsV1Get");
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
| **jobStatus** | [**JobStatus**](.md)|  | [optional] [enum: pending, running, completed, failed, cancelled] |
| **userId** | **String**|  | [optional] |
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


## recoverAllStuckJobsV1AdminRecoverStuckJobsPost

> Object recoverAllStuckJobsV1AdminRecoverStuckJobsPost()

Recover All Stuck Jobs

Admin endpoint to recover all stuck jobs Useful after server restarts

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        try {
            Object result = apiInstance.recoverAllStuckJobsV1AdminRecoverStuckJobsPost();
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#recoverAllStuckJobsV1AdminRecoverStuckJobsPost");
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

## recoverAllStuckJobsV1AdminRecoverStuckJobsPostWithHttpInfo

> ApiResponse<Object> recoverAllStuckJobsV1AdminRecoverStuckJobsPost recoverAllStuckJobsV1AdminRecoverStuckJobsPostWithHttpInfo()

Recover All Stuck Jobs

Admin endpoint to recover all stuck jobs Useful after server restarts

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        try {
            ApiResponse<Object> response = apiInstance.recoverAllStuckJobsV1AdminRecoverStuckJobsPostWithHttpInfo();
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#recoverAllStuckJobsV1AdminRecoverStuckJobsPost");
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


## uploadExcelAsyncV1UploadExcelAsyncPost

> JobResponse uploadExcelAsyncV1UploadExcelAsyncPost(_file, parseMethod, callbackUrl, userId)

Upload Excel Async

Upload Excel file for async background processing Returns immediately with job ID, processes in background

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        File _file = new File("/path/to/file"); // File | 
        String parseMethod = "together"; // String | 
        String callbackUrl = "callbackUrl_example"; // String | 
        String userId = "userId_example"; // String | 
        try {
            JobResponse result = apiInstance.uploadExcelAsyncV1UploadExcelAsyncPost(_file, parseMethod, callbackUrl, userId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#uploadExcelAsyncV1UploadExcelAsyncPost");
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
| **callbackUrl** | **String**|  | [optional] |
| **userId** | **String**|  | [optional] |

### Return type

[**JobResponse**](JobResponse.md)


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

## uploadExcelAsyncV1UploadExcelAsyncPostWithHttpInfo

> ApiResponse<JobResponse> uploadExcelAsyncV1UploadExcelAsyncPost uploadExcelAsyncV1UploadExcelAsyncPostWithHttpInfo(_file, parseMethod, callbackUrl, userId)

Upload Excel Async

Upload Excel file for async background processing Returns immediately with job ID, processes in background

### Example

```java
// Import classes:
import com.am.portfolio.client.parser.invoker.ApiClient;
import com.am.portfolio.client.parser.invoker.ApiException;
import com.am.portfolio.client.parser.invoker.ApiResponse;
import com.am.portfolio.client.parser.invoker.Configuration;
import com.am.portfolio.client.parser.invoker.models.*;
import com.am.portfolio.client.parser.api.BackgroundJobsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("http://localhost");

        BackgroundJobsApi apiInstance = new BackgroundJobsApi(defaultClient);
        File _file = new File("/path/to/file"); // File | 
        String parseMethod = "together"; // String | 
        String callbackUrl = "callbackUrl_example"; // String | 
        String userId = "userId_example"; // String | 
        try {
            ApiResponse<JobResponse> response = apiInstance.uploadExcelAsyncV1UploadExcelAsyncPostWithHttpInfo(_file, parseMethod, callbackUrl, userId);
            System.out.println("Status code: " + response.getStatusCode());
            System.out.println("Response headers: " + response.getHeaders());
            System.out.println("Response body: " + response.getData());
        } catch (ApiException e) {
            System.err.println("Exception when calling BackgroundJobsApi#uploadExcelAsyncV1UploadExcelAsyncPost");
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
| **callbackUrl** | **String**|  | [optional] |
| **userId** | **String**|  | [optional] |

### Return type

ApiResponse<[**JobResponse**](JobResponse.md)>


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

