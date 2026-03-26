# am_parser_client.MutualFundsApi

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**get_file_status_v1_files_file_id_get**](MutualFundsApi.md#get_file_status_v1_files_file_id_get) | **GET** /v1/files/{file_id} | Get File Status
[**get_fund_statistics_v1_funds_fund_name_statistics_get**](MutualFundsApi.md#get_fund_statistics_v1_funds_fund_name_statistics_get) | **GET** /v1/funds/{fund_name}/statistics | Get Fund Statistics
[**get_holdings_by_isin_v1_holdings_isin_code_get**](MutualFundsApi.md#get_holdings_by_isin_v1_holdings_isin_code_get) | **GET** /v1/holdings/{isin_code} | Get Holdings By Isin
[**get_portfolio_v1_portfolios_portfolio_id_get**](MutualFundsApi.md#get_portfolio_v1_portfolios_portfolio_id_get) | **GET** /v1/portfolios/{portfolio_id} | Get Portfolio
[**list_files_v1_files_get**](MutualFundsApi.md#list_files_v1_files_get) | **GET** /v1/files | List Files
[**list_portfolios_v1_portfolios_get**](MutualFundsApi.md#list_portfolios_v1_portfolios_get) | **GET** /v1/portfolios | List Portfolios
[**parse_all_sheets_v1_parse_all_file_id_post**](MutualFundsApi.md#parse_all_sheets_v1_parse_all_file_id_post) | **POST** /v1/parse-all/{file_id} | Parse All Sheets
[**parse_sheet_v1_parse_sheet_id_post**](MutualFundsApi.md#parse_sheet_v1_parse_sheet_id_post) | **POST** /v1/parse/{sheet_id} | Parse Sheet
[**process_file_v1_process_file_id_post**](MutualFundsApi.md#process_file_v1_process_file_id_post) | **POST** /v1/process/{file_id} | Process File
[**save_portfolio_v1_portfolios_post**](MutualFundsApi.md#save_portfolio_v1_portfolios_post) | **POST** /v1/portfolios | Save Portfolio
[**search_portfolios_v1_portfolios_search_get**](MutualFundsApi.md#search_portfolios_v1_portfolios_search_get) | **GET** /v1/portfolios/search | Search Portfolios
[**upload_excel_complete_v1_upload_excel_post**](MutualFundsApi.md#upload_excel_complete_v1_upload_excel_post) | **POST** /v1/upload/excel | Upload Excel Complete
[**upload_file_v1_upload_post**](MutualFundsApi.md#upload_file_v1_upload_post) | **POST** /v1/upload | Upload File


# **get_file_status_v1_files_file_id_get**
> object get_file_status_v1_files_file_id_get(file_id)

Get File Status

Get detailed status information for a file and its sheets

- **file_id**: ID of the file to check

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    file_id = 'file_id_example' # str | 

    try:
        # Get File Status
        api_response = api_instance.get_file_status_v1_files_file_id_get(file_id)
        print("The response of MutualFundsApi->get_file_status_v1_files_file_id_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->get_file_status_v1_files_file_id_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file_id** | **str**|  | 

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

# **get_fund_statistics_v1_funds_fund_name_statistics_get**
> object get_fund_statistics_v1_funds_fund_name_statistics_get(fund_name)

Get Fund Statistics

Get statistics for a specific fund

Args:
    fund_name: Name of the mutual fund
    
Returns:
    Fund statistics

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    fund_name = 'fund_name_example' # str | 

    try:
        # Get Fund Statistics
        api_response = api_instance.get_fund_statistics_v1_funds_fund_name_statistics_get(fund_name)
        print("The response of MutualFundsApi->get_fund_statistics_v1_funds_fund_name_statistics_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->get_fund_statistics_v1_funds_fund_name_statistics_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fund_name** | **str**|  | 

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

# **get_holdings_by_isin_v1_holdings_isin_code_get**
> object get_holdings_by_isin_v1_holdings_isin_code_get(isin_code)

Get Holdings By Isin

Get all holdings with specific ISIN code

Args:
    isin_code: ISIN code to search for
    
Returns:
    List of holdings with the specified ISIN

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    isin_code = 'isin_code_example' # str | 

    try:
        # Get Holdings By Isin
        api_response = api_instance.get_holdings_by_isin_v1_holdings_isin_code_get(isin_code)
        print("The response of MutualFundsApi->get_holdings_by_isin_v1_holdings_isin_code_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->get_holdings_by_isin_v1_holdings_isin_code_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **isin_code** | **str**|  | 

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

# **get_portfolio_v1_portfolios_portfolio_id_get**
> object get_portfolio_v1_portfolios_portfolio_id_get(portfolio_id)

Get Portfolio

Get a specific portfolio by ID

Args:
    portfolio_id: MongoDB ObjectId of the portfolio
    
Returns:
    Portfolio data if found

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    portfolio_id = 'portfolio_id_example' # str | 

    try:
        # Get Portfolio
        api_response = api_instance.get_portfolio_v1_portfolios_portfolio_id_get(portfolio_id)
        print("The response of MutualFundsApi->get_portfolio_v1_portfolios_portfolio_id_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->get_portfolio_v1_portfolios_portfolio_id_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **portfolio_id** | **str**|  | 

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

# **list_files_v1_files_get**
> FileListResponse list_files_v1_files_get(skip=skip, limit=limit, status_filter=status_filter)

List Files

List uploaded files with optional filtering

- **skip**: Number of records to skip (for pagination)
- **limit**: Maximum number of records to return
- **status_filter**: Filter by processing status

### Example


```python
import am_parser_client
from am_parser_client.models.file_list_response import FileListResponse
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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    skip = 0 # int |  (optional) (default to 0)
    limit = 100 # int |  (optional) (default to 100)
    status_filter = 'status_filter_example' # str |  (optional)

    try:
        # List Files
        api_response = api_instance.list_files_v1_files_get(skip=skip, limit=limit, status_filter=status_filter)
        print("The response of MutualFundsApi->list_files_v1_files_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->list_files_v1_files_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **skip** | **int**|  | [optional] [default to 0]
 **limit** | **int**|  | [optional] [default to 100]
 **status_filter** | **str**|  | [optional] 

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
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **list_portfolios_v1_portfolios_get**
> object list_portfolios_v1_portfolios_get(fund_name=fund_name, limit=limit)

List Portfolios

List all portfolios or filter by fund name

Args:
    fund_name: Optional fund name to filter by
    limit: Maximum number of portfolios to return (default: 50)
    
Returns:
    List of portfolio summaries

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    fund_name = 'fund_name_example' # str |  (optional)
    limit = 50 # int |  (optional) (default to 50)

    try:
        # List Portfolios
        api_response = api_instance.list_portfolios_v1_portfolios_get(fund_name=fund_name, limit=limit)
        print("The response of MutualFundsApi->list_portfolios_v1_portfolios_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->list_portfolios_v1_portfolios_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fund_name** | **str**|  | [optional] 
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

# **parse_all_sheets_v1_parse_all_file_id_post**
> object parse_all_sheets_v1_parse_all_file_id_post(file_id, method=method, api_key=api_key)

Parse All Sheets

Parse all sheets for a given Excel file

- **file_id**: ID of the Excel file
- **method**: Parsing method (manual, llm, together)
- **api_key**: API key for LLM parsing

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    file_id = 'file_id_example' # str | 
    method = 'manual' # str |  (optional) (default to 'manual')
    api_key = 'api_key_example' # str |  (optional)

    try:
        # Parse All Sheets
        api_response = api_instance.parse_all_sheets_v1_parse_all_file_id_post(file_id, method=method, api_key=api_key)
        print("The response of MutualFundsApi->parse_all_sheets_v1_parse_all_file_id_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->parse_all_sheets_v1_parse_all_file_id_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file_id** | **str**|  | 
 **method** | **str**|  | [optional] [default to &#39;manual&#39;]
 **api_key** | **str**|  | [optional] 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/x-www-form-urlencoded
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **parse_sheet_v1_parse_sheet_id_post**
> object parse_sheet_v1_parse_sheet_id_post(sheet_id, method=method, api_key=api_key)

Parse Sheet

Parse an individual sheet file to extract portfolio data

- **sheet_id**: ID of the sheet file to parse
- **method**: Parsing method (manual, llm, together)
- **api_key**: API key for LLM parsing (required for 'together' method)

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    sheet_id = 'sheet_id_example' # str | 
    method = 'manual' # str |  (optional) (default to 'manual')
    api_key = 'api_key_example' # str |  (optional)

    try:
        # Parse Sheet
        api_response = api_instance.parse_sheet_v1_parse_sheet_id_post(sheet_id, method=method, api_key=api_key)
        print("The response of MutualFundsApi->parse_sheet_v1_parse_sheet_id_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->parse_sheet_v1_parse_sheet_id_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **sheet_id** | **str**|  | 
 **method** | **str**|  | [optional] [default to &#39;manual&#39;]
 **api_key** | **str**|  | [optional] 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/x-www-form-urlencoded
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **process_file_v1_process_file_id_post**
> object process_file_v1_process_file_id_post(file_id)

Process File

Process an uploaded Excel file by splitting it into individual sheet files

- **file_id**: ID of the uploaded Excel file

This endpoint splits the Excel file into individual sheet files and stores them

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    file_id = 'file_id_example' # str | 

    try:
        # Process File
        api_response = api_instance.process_file_v1_process_file_id_post(file_id)
        print("The response of MutualFundsApi->process_file_v1_process_file_id_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->process_file_v1_process_file_id_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file_id** | **str**|  | 

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

# **save_portfolio_v1_portfolios_post**
> object save_portfolio_v1_portfolios_post(body)

Save Portfolio

Save a mutual fund portfolio to the database

Args:
    portfolio_data: JSON data containing mutual fund portfolio information
    
Returns:
    Saved portfolio data with database ID

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    body = None # object | 

    try:
        # Save Portfolio
        api_response = api_instance.save_portfolio_v1_portfolios_post(body)
        print("The response of MutualFundsApi->save_portfolio_v1_portfolios_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->save_portfolio_v1_portfolios_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | **object**|  | 

### Return type

**object**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details

| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Successful Response |  -  |
**422** | Validation Error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **search_portfolios_v1_portfolios_search_get**
> object search_portfolios_v1_portfolios_search_get(fund_name)

Search Portfolios

Search portfolios by fund name

Args:
    fund_name: Fund name to search for
    
Returns:
    List of matching portfolio summaries

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    fund_name = 'fund_name_example' # str | 

    try:
        # Search Portfolios
        api_response = api_instance.search_portfolios_v1_portfolios_search_get(fund_name)
        print("The response of MutualFundsApi->search_portfolios_v1_portfolios_search_get:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->search_portfolios_v1_portfolios_search_get: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fund_name** | **str**|  | 

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

# **upload_excel_complete_v1_upload_excel_post**
> object upload_excel_complete_v1_upload_excel_post(file, parse_method=parse_method)

Upload Excel Complete

🚀 Complete Excel Upload Workflow - Does EVERYTHING automatically!

This endpoint handles the complete workflow:
1. ✅ Upload Excel file
2. ✅ Persist main file to database  
3. ✅ Split Excel into individual sheet files
4. ✅ Persist all sheet files to database
5. ✅ Parse each sheet using manual or LLM parsing
6. ✅ Save all parsed portfolios to database

- **file**: Excel file to upload (.xlsx, .xls)
- **parse_method**: "together" (default) or "manual"

Returns: Complete results with all parsed portfolios

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    file = None # bytearray | 
    parse_method = 'together' # str |  (optional) (default to 'together')

    try:
        # Upload Excel Complete
        api_response = api_instance.upload_excel_complete_v1_upload_excel_post(file, parse_method=parse_method)
        print("The response of MutualFundsApi->upload_excel_complete_v1_upload_excel_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->upload_excel_complete_v1_upload_excel_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **bytearray**|  | 
 **parse_method** | **str**|  | [optional] [default to &#39;together&#39;]

### Return type

**object**

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

# **upload_file_v1_upload_post**
> object upload_file_v1_upload_post(file, parse_method=parse_method)

Upload File

Upload an Excel file and do ALL the work automatically:
1. Upload and persist main file to database
2. Split Excel into individual sheet files  
3. Persist all sheet files to database
4. Parse each sheet and save portfolios to database

- **file**: Excel file to upload (.xlsx, .xls)
- **parse_method**: Parsing method ("manual" or "together") - defaults to "together"

Returns complete processing results with all parsed portfolios

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
    api_instance = am_parser_client.MutualFundsApi(api_client)
    file = None # bytearray | 
    parse_method = 'together' # str |  (optional) (default to 'together')

    try:
        # Upload File
        api_response = api_instance.upload_file_v1_upload_post(file, parse_method=parse_method)
        print("The response of MutualFundsApi->upload_file_v1_upload_post:\n")
        pprint(api_response)
    except Exception as e:
        print("Exception when calling MutualFundsApi->upload_file_v1_upload_post: %s\n" % e)
```



### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **bytearray**|  | 
 **parse_method** | **str**|  | [optional] [default to &#39;together&#39;]

### Return type

**object**

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

