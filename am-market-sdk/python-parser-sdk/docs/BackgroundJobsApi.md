# am_parser_client.BackgroundJobsApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**cancel_job_v1_job_id_delete**](BackgroundJobsApi.md#cancel_job_v1_job_id_delete) | **DELETE** /v1/{job_id} | Cancel Job
[**fix_stuck_job_v1_admin_fix_stuck_job_job_id_post**](BackgroundJobsApi.md#fix_stuck_job_v1_admin_fix_stuck_job_job_id_post) | **POST** /v1/admin/fix-stuck-job/{job_id} | Fix Stuck Job
[**get_job_result_v1_job_id_result_get**](BackgroundJobsApi.md#get_job_result_v1_job_id_result_get) | **GET** /v1/{job_id}/result | Get Job Result
[**get_job_status_v1_job_id_status_get**](BackgroundJobsApi.md#get_job_status_v1_job_id_status_get) | **GET** /v1/{job_id}/status | Get Job Status
[**list_jobs_v1_get**](BackgroundJobsApi.md#list_jobs_v1_get) | **GET** /v1/ | List Jobs
[**recover_all_stuck_jobs_v1_admin_recover_stuck_jobs_post**](BackgroundJobsApi.md#recover_all_stuck_jobs_v1_admin_recover_stuck_jobs_post) | **POST** /v1/admin/recover-stuck-jobs | Recover All Stuck Jobs
[**upload_excel_async_v1_upload_excel_async_post**](BackgroundJobsApi.md#upload_excel_async_v1_upload_excel_async_post) | **POST** /v1/upload-excel-async | Upload Excel Async


# **cancel_job_v1_job_id_delete**
> object cancel_job_v1_job_id_delete(job_id)

Cancel Job

Cancel a pending or running job

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.BackgroundJobsApi(api_client)
    job_id = 'job_id_example' # str | 

    try:
        # Cancel Job
        api_response = api_instance.cancel_job_v1_job_id_delete(job_id)
        print("The response of BackgroundJobsApi->cancel_job_v1_job_id_delete:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BackgroundJobsApi->cancel_job_v1_job_id_delete: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **job_id** | **str**|  | 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **fix_stuck_job_v1_admin_fix_stuck_job_job_id_post**
> object fix_stuck_job_v1_admin_fix_stuck_job_job_id_post(job_id, mark_as_failed=mark_as_failed)

Fix Stuck Job

Admin endpoint to fix stuck jobs
Used when jobs get stuck due to server restarts

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.BackgroundJobsApi(api_client)
    job_id = 'job_id_example' # str | 
    mark_as_failed = True # bool |  (optional) (default to True)

    try:
        # Fix Stuck Job
        api_response = api_instance.fix_stuck_job_v1_admin_fix_stuck_job_job_id_post(job_id, mark_as_failed=mark_as_failed)
        print("The response of BackgroundJobsApi->fix_stuck_job_v1_admin_fix_stuck_job_job_id_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BackgroundJobsApi->fix_stuck_job_v1_admin_fix_stuck_job_job_id_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **job_id** | **str**|  | 
 **mark_as_failed** | **bool**|  | [optional] [default to True]

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_job_result_v1_job_id_result_get**
> object get_job_result_v1_job_id_result_get(job_id)

Get Job Result

Get the result of a completed job

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.BackgroundJobsApi(api_client)
    job_id = 'job_id_example' # str | 

    try:
        # Get Job Result
        api_response = api_instance.get_job_result_v1_job_id_result_get(job_id)
        print("The response of BackgroundJobsApi->get_job_result_v1_job_id_result_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BackgroundJobsApi->get_job_result_v1_job_id_result_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **job_id** | **str**|  | 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **get_job_status_v1_job_id_status_get**
> JobStatusResponse get_job_status_v1_job_id_status_get(job_id)

Get Job Status

Get the current status of a background job

### Example


```python
import am_parser_client
from am_parser_client.models.job_status_response import JobStatusResponse
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.BackgroundJobsApi(api_client)
    job_id = 'job_id_example' # str | 

    try:
        # Get Job Status
        api_response = api_instance.get_job_status_v1_job_id_status_get(job_id)
        print("The response of BackgroundJobsApi->get_job_status_v1_job_id_status_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BackgroundJobsApi->get_job_status_v1_job_id_status_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **job_id** | **str**|  | 

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
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **list_jobs_v1_get**
> object list_jobs_v1_get(job_status=job_status, user_id=user_id, limit=limit)

List Jobs

List background jobs with optional filtering

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.BackgroundJobsApi(api_client)
    job_status = am_parser_client.JobStatus() # JobStatus |  (optional)
    user_id = 'user_id_example' # str |  (optional)
    limit = 50 # int |  (optional) (default to 50)

    try:
        # List Jobs
        api_response = api_instance.list_jobs_v1_get(job_status=job_status, user_id=user_id, limit=limit)
        print("The response of BackgroundJobsApi->list_jobs_v1_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BackgroundJobsApi->list_jobs_v1_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **job_status** | [**JobStatus**](.md)|  | [optional] 
 **user_id** | **str**|  | [optional] 
 **limit** | **int**|  | [optional] [default to 50]

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recover_all_stuck_jobs_v1_admin_recover_stuck_jobs_post**
> object recover_all_stuck_jobs_v1_admin_recover_stuck_jobs_post()

Recover All Stuck Jobs

Admin endpoint to recover all stuck jobs
Useful after server restarts

### Example


```python
import am_parser_client
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.BackgroundJobsApi(api_client)

    try:
        # Recover All Stuck Jobs
        api_response = api_instance.recover_all_stuck_jobs_v1_admin_recover_stuck_jobs_post()
        print("The response of BackgroundJobsApi->recover_all_stuck_jobs_v1_admin_recover_stuck_jobs_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BackgroundJobsApi->recover_all_stuck_jobs_v1_admin_recover_stuck_jobs_post: %s\n" % e)
```



### Parameters

This endpoint does not need any parameter.

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **upload_excel_async_v1_upload_excel_async_post**
> JobResponse upload_excel_async_v1_upload_excel_async_post(file, parse_method=parse_method, callback_url=callback_url, user_id=user_id)

Upload Excel Async

Upload Excel file for async background processing
Returns immediately with job ID, processes in background

### Example


```python
import am_parser_client
from am_parser_client.models.job_response import JobResponse
from am_parser_client.rest import ApiException
from pprint import pprint

# Defining the host is optional and defaults to http://localhost
# See configuration.py for a list of all supported configuration parameters.
configuration = am_parser_client.Configuration(
    host = "http://localhost"
)


# Enter a context with an instance of the API client
with am_parser_client.ApiClient(configuration) as api_client:
    # Create an instance of the API class
    api_instance = am_parser_client.BackgroundJobsApi(api_client)
    file = None # bytearray | 
    parse_method = 'together' # str |  (optional) (default to 'together')
    callback_url = 'callback_url_example' # str |  (optional)
    user_id = 'user_id_example' # str |  (optional)

    try:
        # Upload Excel Async
        api_response = api_instance.upload_excel_async_v1_upload_excel_async_post(file, parse_method=parse_method, callback_url=callback_url, user_id=user_id)
        print("The response of BackgroundJobsApi->upload_excel_async_v1_upload_excel_async_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling BackgroundJobsApi->upload_excel_async_v1_upload_excel_async_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **bytearray**|  | 
 **parse_method** | **str**|  | [optional] [default to &#39;together&#39;]
 **callback_url** | **str**|  | [optional] 
 **user_id** | **str**|  | [optional] 

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
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

