# am_parser_client.api.MutualFundsApi

## Load the API package
```dart
import 'package:am_parser_client/api.dart';
```

All URIs are relative to *http://localhost*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getFileStatusV1FilesFileIdGet**](MutualFundsApi.md#getfilestatusv1filesfileidget) | **GET** /v1/files/{file_id} | Get File Status
[**getFundStatisticsV1FundsFundNameStatisticsGet**](MutualFundsApi.md#getfundstatisticsv1fundsfundnamestatisticsget) | **GET** /v1/funds/{fund_name}/statistics | Get Fund Statistics
[**getHoldingsByIsinV1HoldingsIsinCodeGet**](MutualFundsApi.md#getholdingsbyisinv1holdingsisincodeget) | **GET** /v1/holdings/{isin_code} | Get Holdings By Isin
[**getPortfolioV1PortfoliosPortfolioIdGet**](MutualFundsApi.md#getportfoliov1portfoliosportfolioidget) | **GET** /v1/portfolios/{portfolio_id} | Get Portfolio
[**listFilesV1FilesGet**](MutualFundsApi.md#listfilesv1filesget) | **GET** /v1/files | List Files
[**listPortfoliosV1PortfoliosGet**](MutualFundsApi.md#listportfoliosv1portfoliosget) | **GET** /v1/portfolios | List Portfolios
[**parseAllSheetsV1ParseAllFileIdPost**](MutualFundsApi.md#parseallsheetsv1parseallfileidpost) | **POST** /v1/parse-all/{file_id} | Parse All Sheets
[**parseSheetV1ParseSheetIdPost**](MutualFundsApi.md#parsesheetv1parsesheetidpost) | **POST** /v1/parse/{sheet_id} | Parse Sheet
[**processFileV1ProcessFileIdPost**](MutualFundsApi.md#processfilev1processfileidpost) | **POST** /v1/process/{file_id} | Process File
[**savePortfolioV1PortfoliosPost**](MutualFundsApi.md#saveportfoliov1portfoliospost) | **POST** /v1/portfolios | Save Portfolio
[**searchPortfoliosV1PortfoliosSearchGet**](MutualFundsApi.md#searchportfoliosv1portfoliossearchget) | **GET** /v1/portfolios/search | Search Portfolios
[**uploadExcelCompleteV1UploadExcelPost**](MutualFundsApi.md#uploadexcelcompletev1uploadexcelpost) | **POST** /v1/upload/excel | Upload Excel Complete
[**uploadFileV1UploadPost**](MutualFundsApi.md#uploadfilev1uploadpost) | **POST** /v1/upload | Upload File


# **getFileStatusV1FilesFileIdGet**
> Object getFileStatusV1FilesFileIdGet(fileId)

Get File Status

Get detailed status information for a file and its sheets  - **file_id**: ID of the file to check

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final fileId = fileId_example; // String | 

try {
    final result = api_instance.getFileStatusV1FilesFileIdGet(fileId);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->getFileStatusV1FilesFileIdGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fileId** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getFundStatisticsV1FundsFundNameStatisticsGet**
> Object getFundStatisticsV1FundsFundNameStatisticsGet(fundName)

Get Fund Statistics

Get statistics for a specific fund  Args:     fund_name: Name of the mutual fund      Returns:     Fund statistics

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final fundName = fundName_example; // String | 

try {
    final result = api_instance.getFundStatisticsV1FundsFundNameStatisticsGet(fundName);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->getFundStatisticsV1FundsFundNameStatisticsGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fundName** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getHoldingsByIsinV1HoldingsIsinCodeGet**
> Object getHoldingsByIsinV1HoldingsIsinCodeGet(isinCode)

Get Holdings By Isin

Get all holdings with specific ISIN code  Args:     isin_code: ISIN code to search for      Returns:     List of holdings with the specified ISIN

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final isinCode = isinCode_example; // String | 

try {
    final result = api_instance.getHoldingsByIsinV1HoldingsIsinCodeGet(isinCode);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->getHoldingsByIsinV1HoldingsIsinCodeGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **isinCode** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPortfolioV1PortfoliosPortfolioIdGet**
> Object getPortfolioV1PortfoliosPortfolioIdGet(portfolioId)

Get Portfolio

Get a specific portfolio by ID  Args:     portfolio_id: MongoDB ObjectId of the portfolio      Returns:     Portfolio data if found

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final portfolioId = portfolioId_example; // String | 

try {
    final result = api_instance.getPortfolioV1PortfoliosPortfolioIdGet(portfolioId);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->getPortfolioV1PortfoliosPortfolioIdGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **portfolioId** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listFilesV1FilesGet**
> FileListResponse listFilesV1FilesGet(skip, limit, statusFilter)

List Files

List uploaded files with optional filtering  - **skip**: Number of records to skip (for pagination) - **limit**: Maximum number of records to return - **status_filter**: Filter by processing status

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final skip = 56; // int | 
final limit = 56; // int | 
final statusFilter = statusFilter_example; // String | 

try {
    final result = api_instance.listFilesV1FilesGet(skip, limit, statusFilter);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->listFilesV1FilesGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **skip** | **int**|  | [optional] [default to 0]
 **limit** | **int**|  | [optional] [default to 100]
 **statusFilter** | **String**|  | [optional] 

### Return type

[**FileListResponse**](FileListResponse.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listPortfoliosV1PortfoliosGet**
> Object listPortfoliosV1PortfoliosGet(fundName, limit)

List Portfolios

List all portfolios or filter by fund name  Args:     fund_name: Optional fund name to filter by     limit: Maximum number of portfolios to return (default: 50)      Returns:     List of portfolio summaries

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final fundName = fundName_example; // String | 
final limit = 56; // int | 

try {
    final result = api_instance.listPortfoliosV1PortfoliosGet(fundName, limit);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->listPortfoliosV1PortfoliosGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fundName** | **String**|  | [optional] 
 **limit** | **int**|  | [optional] [default to 50]

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **parseAllSheetsV1ParseAllFileIdPost**
> Object parseAllSheetsV1ParseAllFileIdPost(fileId, method, apiKey)

Parse All Sheets

Parse all sheets for a given Excel file  - **file_id**: ID of the Excel file - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final fileId = fileId_example; // String | 
final method = method_example; // String | 
final apiKey = apiKey_example; // String | 

try {
    final result = api_instance.parseAllSheetsV1ParseAllFileIdPost(fileId, method, apiKey);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->parseAllSheetsV1ParseAllFileIdPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fileId** | **String**|  | 
 **method** | **String**|  | [optional] [default to 'manual']
 **apiKey** | **String**|  | [optional] 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/x-www-form-urlencoded
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **parseSheetV1ParseSheetIdPost**
> Object parseSheetV1ParseSheetIdPost(sheetId, method, apiKey)

Parse Sheet

Parse an individual sheet file to extract portfolio data  - **sheet_id**: ID of the sheet file to parse - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing (required for 'together' method)

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final sheetId = sheetId_example; // String | 
final method = method_example; // String | 
final apiKey = apiKey_example; // String | 

try {
    final result = api_instance.parseSheetV1ParseSheetIdPost(sheetId, method, apiKey);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->parseSheetV1ParseSheetIdPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **sheetId** | **String**|  | 
 **method** | **String**|  | [optional] [default to 'manual']
 **apiKey** | **String**|  | [optional] 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/x-www-form-urlencoded
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **processFileV1ProcessFileIdPost**
> Object processFileV1ProcessFileIdPost(fileId)

Process File

Process an uploaded Excel file by splitting it into individual sheet files  - **file_id**: ID of the uploaded Excel file  This endpoint splits the Excel file into individual sheet files and stores them

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final fileId = fileId_example; // String | 

try {
    final result = api_instance.processFileV1ProcessFileIdPost(fileId);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->processFileV1ProcessFileIdPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fileId** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **savePortfolioV1PortfoliosPost**
> Object savePortfolioV1PortfoliosPost(body)

Save Portfolio

Save a mutual fund portfolio to the database  Args:     portfolio_data: JSON data containing mutual fund portfolio information      Returns:     Saved portfolio data with database ID

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final body = Object(); // Object | 

try {
    final result = api_instance.savePortfolioV1PortfoliosPost(body);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->savePortfolioV1PortfoliosPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | **Object**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **searchPortfoliosV1PortfoliosSearchGet**
> Object searchPortfoliosV1PortfoliosSearchGet(fundName)

Search Portfolios

Search portfolios by fund name  Args:     fund_name: Fund name to search for      Returns:     List of matching portfolio summaries

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final fundName = fundName_example; // String | 

try {
    final result = api_instance.searchPortfoliosV1PortfoliosSearchGet(fundName);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->searchPortfoliosV1PortfoliosSearchGet: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **fundName** | **String**|  | 

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **uploadExcelCompleteV1UploadExcelPost**
> Object uploadExcelCompleteV1UploadExcelPost(file, parseMethod)

Upload Excel Complete

🚀 Complete Excel Upload Workflow - Does EVERYTHING automatically!  This endpoint handles the complete workflow: 1. ✅ Upload Excel file 2. ✅ Persist main file to database   3. ✅ Split Excel into individual sheet files 4. ✅ Persist all sheet files to database 5. ✅ Parse each sheet using manual or LLM parsing 6. ✅ Save all parsed portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: \"together\" (default) or \"manual\"  Returns: Complete results with all parsed portfolios

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final file = BINARY_DATA_HERE; // MultipartFile | 
final parseMethod = parseMethod_example; // String | 

try {
    final result = api_instance.uploadExcelCompleteV1UploadExcelPost(file, parseMethod);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->uploadExcelCompleteV1UploadExcelPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **MultipartFile**|  | 
 **parseMethod** | **String**|  | [optional] [default to 'together']

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **uploadFileV1UploadPost**
> Object uploadFileV1UploadPost(file, parseMethod)

Upload File

Upload an Excel file and do ALL the work automatically: 1. Upload and persist main file to database 2. Split Excel into individual sheet files   3. Persist all sheet files to database 4. Parse each sheet and save portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: Parsing method (\"manual\" or \"together\") - defaults to \"together\"  Returns complete processing results with all parsed portfolios

### Example
```dart
import 'package:am_parser_client/api.dart';

final api_instance = MutualFundsApi();
final file = BINARY_DATA_HERE; // MultipartFile | 
final parseMethod = parseMethod_example; // String | 

try {
    final result = api_instance.uploadFileV1UploadPost(file, parseMethod);
    print(result);
} catch (e) {
    print('Exception when calling MutualFundsApi->uploadFileV1UploadPost: $e\n');
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **file** | **MultipartFile**|  | 
 **parseMethod** | **String**|  | [optional] [default to 'together']

### Return type

[**Object**](Object.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

