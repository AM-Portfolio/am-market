# am_parser_client.api.BackgroundJobsApi

## Load the API package
```dart
import 'package:am_parser_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**cancelJobV1JobIdDelete**](BackgroundJobsApi.md#canceljobv1jobiddelete) | **DELETE** /v1/{job_id} | Cancel Job
[**fixStuckJobV1AdminFixStuckJobJobIdPost**](BackgroundJobsApi.md#fixstuckjobv1adminfixstuckjobjobidpost) | **POST** /v1/admin/fix-stuck-job/{job_id} | Fix Stuck Job
[**getJobResultV1JobIdResultGet**](BackgroundJobsApi.md#getjobresultv1jobidresultget) | **GET** /v1/{job_id}/result | Get Job Result
[**getJobStatusV1JobIdStatusGet**](BackgroundJobsApi.md#getjobstatusv1jobidstatusget) | **GET** /v1/{job_id}/status | Get Job Status
[**listJobsV1Get**](BackgroundJobsApi.md#listjobsv1get) | **GET** /v1/ | List Jobs
[**recoverAllStuckJobsV1AdminRecoverStuckJobsPost**](BackgroundJobsApi.md#recoverallstuckjobsv1adminrecoverstuckjobspost) | **POST** /v1/admin/recover-stuck-jobs | Recover All Stuck Jobs
[**uploadExcelAsyncV1UploadExcelAsyncPost**](BackgroundJobsApi.md#uploadexcelasyncv1uploadexcelasyncpost) | **POST** /v1/upload-excel-async | Upload Excel Async


# **cancelJobV1JobIdDelete**
> Object cancelJobV1JobIdDelete(jobId)

Cancel Job

Cancel a pending or running job

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = BackgroundJobsApi();
final jobId = jobId_example; // String | 

try {
    final result = api_instance.cancelJobV1JobIdDelete(jobId);
    print(result);
} catch (e) {
    print('Exception when calling BackgroundJobsApi->cancelJobV1JobIdDelete: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **jobId** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **fixStuckJobV1AdminFixStuckJobJobIdPost**
> Object fixStuckJobV1AdminFixStuckJobJobIdPost(jobId, markAsFailed)

Fix Stuck Job

Admin endpoint to fix stuck jobs Used when jobs get stuck due to server restarts

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = BackgroundJobsApi();
final jobId = jobId_example; // String | 
final markAsFailed = true; // bool | 

try {
    final result = api_instance.fixStuckJobV1AdminFixStuckJobJobIdPost(jobId, markAsFailed);
    print(result);
} catch (e) {
    print('Exception when calling BackgroundJobsApi->fixStuckJobV1AdminFixStuckJobJobIdPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **jobId** | **String**|  | 
 **markAsFailed** | **bool**|  | [optional] [default to true]

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getJobResultV1JobIdResultGet**
> Object getJobResultV1JobIdResultGet(jobId)

Get Job Result

Get the result of a completed job

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = BackgroundJobsApi();
final jobId = jobId_example; // String | 

try {
    final result = api_instance.getJobResultV1JobIdResultGet(jobId);
    print(result);
} catch (e) {
    print('Exception when calling BackgroundJobsApi->getJobResultV1JobIdResultGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **jobId** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getJobStatusV1JobIdStatusGet**
> JobStatusResponse getJobStatusV1JobIdStatusGet(jobId)

Get Job Status

Get the current status of a background job

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = BackgroundJobsApi();
final jobId = jobId_example; // String | 

try {
    final result = api_instance.getJobStatusV1JobIdStatusGet(jobId);
    print(result);
} catch (e) {
    print('Exception when calling BackgroundJobsApi->getJobStatusV1JobIdStatusGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **jobId** | **String**|  | 

### Return type

[**JobStatusResponse**](JobStatusResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listJobsV1Get**
> Object listJobsV1Get(jobStatus, userId, limit)

List Jobs

List background jobs with optional filtering

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = BackgroundJobsApi();
final jobStatus = ; // JobStatus | 
final userId = userId_example; // String | 
final limit = 56; // int | 

try {
    final result = api_instance.listJobsV1Get(jobStatus, userId, limit);
    print(result);
} catch (e) {
    print('Exception when calling BackgroundJobsApi->listJobsV1Get: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **jobStatus** | [**JobStatus**](.md)|  | [optional] 
 **userId** | **String**|  | [optional] 
 **limit** | **int**|  | [optional] [default to 50]

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recoverAllStuckJobsV1AdminRecoverStuckJobsPost**
> Object recoverAllStuckJobsV1AdminRecoverStuckJobsPost()

Recover All Stuck Jobs

Admin endpoint to recover all stuck jobs Useful after server restarts

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = BackgroundJobsApi();

try {
    final result = api_instance.recoverAllStuckJobsV1AdminRecoverStuckJobsPost();
    print(result);
} catch (e) {
    print('Exception when calling BackgroundJobsApi->recoverAllStuckJobsV1AdminRecoverStuckJobsPost: $e\n');
}
```

### Parameters
This endpoint does not need any parameter.

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **uploadExcelAsyncV1UploadExcelAsyncPost**
> JobResponse uploadExcelAsyncV1UploadExcelAsyncPost(file, parseMethod, callbackUrl, userId)

Upload Excel Async

Upload Excel file for async background processing Returns immediately with job ID, processes in background

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = BackgroundJobsApi();
final file = BINARY_DATA_HERE; // MultipartFile | 
final parseMethod = parseMethod_example; // String | 
final callbackUrl = callbackUrl_example; // String | 
final userId = userId_example; // String | 

try {
    final result = api_instance.uploadExcelAsyncV1UploadExcelAsyncPost(file, parseMethod, callbackUrl, userId);
    print(result);
} catch (e) {
    print('Exception when calling BackgroundJobsApi->uploadExcelAsyncV1UploadExcelAsyncPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **MultipartFile**|  | 
 **parseMethod** | **String**|  | [optional] [default to 'together']
 **callbackUrl** | **String**|  | [optional] 
 **userId** | **String**|  | [optional] 

### Return type

[**JobResponse**](JobResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

