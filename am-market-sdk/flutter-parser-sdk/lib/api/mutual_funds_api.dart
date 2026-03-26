//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unnecessary_null_comparison, unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

part of openapi.api;


class MutualFundsApi {
  MutualFundsApi([ApiClient? apiClient]) : apiClient = apiClient ?? defaultApiClient;

  final ApiClient apiClient;

  /// Get File Status
  ///
  /// Get detailed status information for a file and its sheets  - **file_id**: ID of the file to check
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] fileId (required):
  Future<Response> getFileStatusV1FilesFileIdGetWithHttpInfo(String fileId,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/files/{file_id}'
      .replaceAll('{file_id}', fileId);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Get File Status
  ///
  /// Get detailed status information for a file and its sheets  - **file_id**: ID of the file to check
  ///
  /// Parameters:
  ///
  /// * [String] fileId (required):
  Future<Object?> getFileStatusV1FilesFileIdGet(String fileId,) async {
    final response = await getFileStatusV1FilesFileIdGetWithHttpInfo(fileId,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Get Fund Statistics
  ///
  /// Get statistics for a specific fund  Args:     fund_name: Name of the mutual fund      Returns:     Fund statistics
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] fundName (required):
  Future<Response> getFundStatisticsV1FundsFundNameStatisticsGetWithHttpInfo(String fundName,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/funds/{fund_name}/statistics'
      .replaceAll('{fund_name}', fundName);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Get Fund Statistics
  ///
  /// Get statistics for a specific fund  Args:     fund_name: Name of the mutual fund      Returns:     Fund statistics
  ///
  /// Parameters:
  ///
  /// * [String] fundName (required):
  Future<Object?> getFundStatisticsV1FundsFundNameStatisticsGet(String fundName,) async {
    final response = await getFundStatisticsV1FundsFundNameStatisticsGetWithHttpInfo(fundName,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Get Holdings By Isin
  ///
  /// Get all holdings with specific ISIN code  Args:     isin_code: ISIN code to search for      Returns:     List of holdings with the specified ISIN
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] isinCode (required):
  Future<Response> getHoldingsByIsinV1HoldingsIsinCodeGetWithHttpInfo(String isinCode,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/holdings/{isin_code}'
      .replaceAll('{isin_code}', isinCode);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Get Holdings By Isin
  ///
  /// Get all holdings with specific ISIN code  Args:     isin_code: ISIN code to search for      Returns:     List of holdings with the specified ISIN
  ///
  /// Parameters:
  ///
  /// * [String] isinCode (required):
  Future<Object?> getHoldingsByIsinV1HoldingsIsinCodeGet(String isinCode,) async {
    final response = await getHoldingsByIsinV1HoldingsIsinCodeGetWithHttpInfo(isinCode,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Get Portfolio
  ///
  /// Get a specific portfolio by ID  Args:     portfolio_id: MongoDB ObjectId of the portfolio      Returns:     Portfolio data if found
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] portfolioId (required):
  Future<Response> getPortfolioV1PortfoliosPortfolioIdGetWithHttpInfo(String portfolioId,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/portfolios/{portfolio_id}'
      .replaceAll('{portfolio_id}', portfolioId);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Get Portfolio
  ///
  /// Get a specific portfolio by ID  Args:     portfolio_id: MongoDB ObjectId of the portfolio      Returns:     Portfolio data if found
  ///
  /// Parameters:
  ///
  /// * [String] portfolioId (required):
  Future<Object?> getPortfolioV1PortfoliosPortfolioIdGet(String portfolioId,) async {
    final response = await getPortfolioV1PortfoliosPortfolioIdGetWithHttpInfo(portfolioId,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// List Files
  ///
  /// List uploaded files with optional filtering  - **skip**: Number of records to skip (for pagination) - **limit**: Maximum number of records to return - **status_filter**: Filter by processing status
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [int] skip:
  ///
  /// * [int] limit:
  ///
  /// * [String] statusFilter:
  Future<Response> listFilesV1FilesGetWithHttpInfo({ int? skip, int? limit, String? statusFilter, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/files';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (skip != null) {
      queryParams.addAll(_queryParams('', 'skip', skip));
    }
    if (limit != null) {
      queryParams.addAll(_queryParams('', 'limit', limit));
    }
    if (statusFilter != null) {
      queryParams.addAll(_queryParams('', 'status_filter', statusFilter));
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// List Files
  ///
  /// List uploaded files with optional filtering  - **skip**: Number of records to skip (for pagination) - **limit**: Maximum number of records to return - **status_filter**: Filter by processing status
  ///
  /// Parameters:
  ///
  /// * [int] skip:
  ///
  /// * [int] limit:
  ///
  /// * [String] statusFilter:
  Future<FileListResponse?> listFilesV1FilesGet({ int? skip, int? limit, String? statusFilter, }) async {
    final response = await listFilesV1FilesGetWithHttpInfo( skip: skip, limit: limit, statusFilter: statusFilter, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'FileListResponse',) as FileListResponse;
    
    }
    return null;
  }

  /// List Portfolios
  ///
  /// List all portfolios or filter by fund name  Args:     fund_name: Optional fund name to filter by     limit: Maximum number of portfolios to return (default: 50)      Returns:     List of portfolio summaries
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] fundName:
  ///
  /// * [int] limit:
  Future<Response> listPortfoliosV1PortfoliosGetWithHttpInfo({ String? fundName, int? limit, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/portfolios';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    if (fundName != null) {
      queryParams.addAll(_queryParams('', 'fund_name', fundName));
    }
    if (limit != null) {
      queryParams.addAll(_queryParams('', 'limit', limit));
    }

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// List Portfolios
  ///
  /// List all portfolios or filter by fund name  Args:     fund_name: Optional fund name to filter by     limit: Maximum number of portfolios to return (default: 50)      Returns:     List of portfolio summaries
  ///
  /// Parameters:
  ///
  /// * [String] fundName:
  ///
  /// * [int] limit:
  Future<Object?> listPortfoliosV1PortfoliosGet({ String? fundName, int? limit, }) async {
    final response = await listPortfoliosV1PortfoliosGetWithHttpInfo( fundName: fundName, limit: limit, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Parse All Sheets
  ///
  /// Parse all sheets for a given Excel file  - **file_id**: ID of the Excel file - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] fileId (required):
  ///
  /// * [String] method:
  ///
  /// * [String] apiKey:
  Future<Response> parseAllSheetsV1ParseAllFileIdPostWithHttpInfo(String fileId, { String? method, String? apiKey, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/parse-all/{file_id}'
      .replaceAll('{file_id}', fileId);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>['application/x-www-form-urlencoded'];

    if (method != null) {
      formParams[r'method'] = parameterToString(method);
    }
    if (apiKey != null) {
      formParams[r'api_key'] = parameterToString(apiKey);
    }

    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parse All Sheets
  ///
  /// Parse all sheets for a given Excel file  - **file_id**: ID of the Excel file - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing
  ///
  /// Parameters:
  ///
  /// * [String] fileId (required):
  ///
  /// * [String] method:
  ///
  /// * [String] apiKey:
  Future<Object?> parseAllSheetsV1ParseAllFileIdPost(String fileId, { String? method, String? apiKey, }) async {
    final response = await parseAllSheetsV1ParseAllFileIdPostWithHttpInfo(fileId,  method: method, apiKey: apiKey, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Parse Sheet
  ///
  /// Parse an individual sheet file to extract portfolio data  - **sheet_id**: ID of the sheet file to parse - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing (required for 'together' method)
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] sheetId (required):
  ///
  /// * [String] method:
  ///
  /// * [String] apiKey:
  Future<Response> parseSheetV1ParseSheetIdPostWithHttpInfo(String sheetId, { String? method, String? apiKey, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/parse/{sheet_id}'
      .replaceAll('{sheet_id}', sheetId);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>['application/x-www-form-urlencoded'];

    if (method != null) {
      formParams[r'method'] = parameterToString(method);
    }
    if (apiKey != null) {
      formParams[r'api_key'] = parameterToString(apiKey);
    }

    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Parse Sheet
  ///
  /// Parse an individual sheet file to extract portfolio data  - **sheet_id**: ID of the sheet file to parse - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing (required for 'together' method)
  ///
  /// Parameters:
  ///
  /// * [String] sheetId (required):
  ///
  /// * [String] method:
  ///
  /// * [String] apiKey:
  Future<Object?> parseSheetV1ParseSheetIdPost(String sheetId, { String? method, String? apiKey, }) async {
    final response = await parseSheetV1ParseSheetIdPostWithHttpInfo(sheetId,  method: method, apiKey: apiKey, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Process File
  ///
  /// Process an uploaded Excel file by splitting it into individual sheet files  - **file_id**: ID of the uploaded Excel file  This endpoint splits the Excel file into individual sheet files and stores them
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] fileId (required):
  Future<Response> processFileV1ProcessFileIdPostWithHttpInfo(String fileId,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/process/{file_id}'
      .replaceAll('{file_id}', fileId);

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Process File
  ///
  /// Process an uploaded Excel file by splitting it into individual sheet files  - **file_id**: ID of the uploaded Excel file  This endpoint splits the Excel file into individual sheet files and stores them
  ///
  /// Parameters:
  ///
  /// * [String] fileId (required):
  Future<Object?> processFileV1ProcessFileIdPost(String fileId,) async {
    final response = await processFileV1ProcessFileIdPostWithHttpInfo(fileId,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Save Portfolio
  ///
  /// Save a mutual fund portfolio to the database  Args:     portfolio_data: JSON data containing mutual fund portfolio information      Returns:     Saved portfolio data with database ID
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [Object] body (required):
  Future<Response> savePortfolioV1PortfoliosPostWithHttpInfo(Object body,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/portfolios';

    // ignore: prefer_final_locals
    Object? postBody = body;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>['application/json'];


    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Save Portfolio
  ///
  /// Save a mutual fund portfolio to the database  Args:     portfolio_data: JSON data containing mutual fund portfolio information      Returns:     Saved portfolio data with database ID
  ///
  /// Parameters:
  ///
  /// * [Object] body (required):
  Future<Object?> savePortfolioV1PortfoliosPost(Object body,) async {
    final response = await savePortfolioV1PortfoliosPostWithHttpInfo(body,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Search Portfolios
  ///
  /// Search portfolios by fund name  Args:     fund_name: Fund name to search for      Returns:     List of matching portfolio summaries
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [String] fundName (required):
  Future<Response> searchPortfoliosV1PortfoliosSearchGetWithHttpInfo(String fundName,) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/portfolios/search';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

      queryParams.addAll(_queryParams('', 'fund_name', fundName));

    const contentTypes = <String>[];


    return apiClient.invokeAPI(
      path,
      'GET',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Search Portfolios
  ///
  /// Search portfolios by fund name  Args:     fund_name: Fund name to search for      Returns:     List of matching portfolio summaries
  ///
  /// Parameters:
  ///
  /// * [String] fundName (required):
  Future<Object?> searchPortfoliosV1PortfoliosSearchGet(String fundName,) async {
    final response = await searchPortfoliosV1PortfoliosSearchGetWithHttpInfo(fundName,);
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Upload Excel Complete
  ///
  /// 🚀 Complete Excel Upload Workflow - Does EVERYTHING automatically!  This endpoint handles the complete workflow: 1. ✅ Upload Excel file 2. ✅ Persist main file to database   3. ✅ Split Excel into individual sheet files 4. ✅ Persist all sheet files to database 5. ✅ Parse each sheet using manual or LLM parsing 6. ✅ Save all parsed portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: \"together\" (default) or \"manual\"  Returns: Complete results with all parsed portfolios
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [MultipartFile] file (required):
  ///
  /// * [String] parseMethod:
  Future<Response> uploadExcelCompleteV1UploadExcelPostWithHttpInfo(MultipartFile file, { String? parseMethod, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/upload/excel';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>['multipart/form-data'];

    bool hasFields = false;
    final mp = MultipartRequest('POST', Uri.parse(path));
    if (file != null) {
      hasFields = true;
      mp.fields[r'file'] = file.field;
      mp.files.add(file);
    }
    if (parseMethod != null) {
      hasFields = true;
      mp.fields[r'parse_method'] = parameterToString(parseMethod);
    }
    if (hasFields) {
      postBody = mp;
    }

    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Upload Excel Complete
  ///
  /// 🚀 Complete Excel Upload Workflow - Does EVERYTHING automatically!  This endpoint handles the complete workflow: 1. ✅ Upload Excel file 2. ✅ Persist main file to database   3. ✅ Split Excel into individual sheet files 4. ✅ Persist all sheet files to database 5. ✅ Parse each sheet using manual or LLM parsing 6. ✅ Save all parsed portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: \"together\" (default) or \"manual\"  Returns: Complete results with all parsed portfolios
  ///
  /// Parameters:
  ///
  /// * [MultipartFile] file (required):
  ///
  /// * [String] parseMethod:
  Future<Object?> uploadExcelCompleteV1UploadExcelPost(MultipartFile file, { String? parseMethod, }) async {
    final response = await uploadExcelCompleteV1UploadExcelPostWithHttpInfo(file,  parseMethod: parseMethod, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }

  /// Upload File
  ///
  /// Upload an Excel file and do ALL the work automatically: 1. Upload and persist main file to database 2. Split Excel into individual sheet files   3. Persist all sheet files to database 4. Parse each sheet and save portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: Parsing method (\"manual\" or \"together\") - defaults to \"together\"  Returns complete processing results with all parsed portfolios
  ///
  /// Note: This method returns the HTTP [Response].
  ///
  /// Parameters:
  ///
  /// * [MultipartFile] file (required):
  ///
  /// * [String] parseMethod:
  Future<Response> uploadFileV1UploadPostWithHttpInfo(MultipartFile file, { String? parseMethod, }) async {
    // ignore: prefer_const_declarations
    final path = r'/v1/upload';

    // ignore: prefer_final_locals
    Object? postBody;

    final queryParams = <QueryParam>[];
    final headerParams = <String, String>{};
    final formParams = <String, String>{};

    const contentTypes = <String>['multipart/form-data'];

    bool hasFields = false;
    final mp = MultipartRequest('POST', Uri.parse(path));
    if (file != null) {
      hasFields = true;
      mp.fields[r'file'] = file.field;
      mp.files.add(file);
    }
    if (parseMethod != null) {
      hasFields = true;
      mp.fields[r'parse_method'] = parameterToString(parseMethod);
    }
    if (hasFields) {
      postBody = mp;
    }

    return apiClient.invokeAPI(
      path,
      'POST',
      queryParams,
      postBody,
      headerParams,
      formParams,
      contentTypes.isEmpty ? null : contentTypes.first,
    );
  }

  /// Upload File
  ///
  /// Upload an Excel file and do ALL the work automatically: 1. Upload and persist main file to database 2. Split Excel into individual sheet files   3. Persist all sheet files to database 4. Parse each sheet and save portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: Parsing method (\"manual\" or \"together\") - defaults to \"together\"  Returns complete processing results with all parsed portfolios
  ///
  /// Parameters:
  ///
  /// * [MultipartFile] file (required):
  ///
  /// * [String] parseMethod:
  Future<Object?> uploadFileV1UploadPost(MultipartFile file, { String? parseMethod, }) async {
    final response = await uploadFileV1UploadPostWithHttpInfo(file,  parseMethod: parseMethod, );
    if (response.statusCode >= HttpStatus.badRequest) {
      throw ApiException(response.statusCode, await _decodeBodyBytes(response));
    }
    // When a remote server returns no body with a status of 204, we shall not decode it.
    // At the time of writing this, `dart:convert` will throw an "Unexpected end of input"
    // FormatException when trying to decode an empty string.
    if (response.body.isNotEmpty && response.statusCode != HttpStatus.noContent) {
      return await apiClient.deserializeAsync(await _decodeBodyBytes(response), 'Object',) as Object;
    
    }
    return null;
  }
}

