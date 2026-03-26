//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

// ignore_for_file: unused_element, unused_import
// ignore_for_file: always_put_required_named_parameters_first
// ignore_for_file: constant_identifier_names
// ignore_for_file: lines_longer_than_80_chars

import 'package:am_parser_client/api.dart';
import 'package:test/test.dart';


/// tests for MutualFundsApi
void main() {
  // final instance = MutualFundsApi();

  group('tests for MutualFundsApi', () {
    // Get File Status
    //
    // Get detailed status information for a file and its sheets  - **file_id**: ID of the file to check
    //
    //Future<Object> getFileStatusV1FilesFileIdGet(String fileId) async
    test('test getFileStatusV1FilesFileIdGet', () async {
      // TODO
    });

    // Get Fund Statistics
    //
    // Get statistics for a specific fund  Args:     fund_name: Name of the mutual fund      Returns:     Fund statistics
    //
    //Future<Object> getFundStatisticsV1FundsFundNameStatisticsGet(String fundName) async
    test('test getFundStatisticsV1FundsFundNameStatisticsGet', () async {
      // TODO
    });

    // Get Holdings By Isin
    //
    // Get all holdings with specific ISIN code  Args:     isin_code: ISIN code to search for      Returns:     List of holdings with the specified ISIN
    //
    //Future<Object> getHoldingsByIsinV1HoldingsIsinCodeGet(String isinCode) async
    test('test getHoldingsByIsinV1HoldingsIsinCodeGet', () async {
      // TODO
    });

    // Get Portfolio
    //
    // Get a specific portfolio by ID  Args:     portfolio_id: MongoDB ObjectId of the portfolio      Returns:     Portfolio data if found
    //
    //Future<Object> getPortfolioV1PortfoliosPortfolioIdGet(String portfolioId) async
    test('test getPortfolioV1PortfoliosPortfolioIdGet', () async {
      // TODO
    });

    // List Files
    //
    // List uploaded files with optional filtering  - **skip**: Number of records to skip (for pagination) - **limit**: Maximum number of records to return - **status_filter**: Filter by processing status
    //
    //Future<FileListResponse> listFilesV1FilesGet({ int skip, int limit, String statusFilter }) async
    test('test listFilesV1FilesGet', () async {
      // TODO
    });

    // List Portfolios
    //
    // List all portfolios or filter by fund name  Args:     fund_name: Optional fund name to filter by     limit: Maximum number of portfolios to return (default: 50)      Returns:     List of portfolio summaries
    //
    //Future<Object> listPortfoliosV1PortfoliosGet({ String fundName, int limit }) async
    test('test listPortfoliosV1PortfoliosGet', () async {
      // TODO
    });

    // Parse All Sheets
    //
    // Parse all sheets for a given Excel file  - **file_id**: ID of the Excel file - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing
    //
    //Future<Object> parseAllSheetsV1ParseAllFileIdPost(String fileId, { String method, String apiKey }) async
    test('test parseAllSheetsV1ParseAllFileIdPost', () async {
      // TODO
    });

    // Parse Sheet
    //
    // Parse an individual sheet file to extract portfolio data  - **sheet_id**: ID of the sheet file to parse - **method**: Parsing method (manual, llm, together) - **api_key**: API key for LLM parsing (required for 'together' method)
    //
    //Future<Object> parseSheetV1ParseSheetIdPost(String sheetId, { String method, String apiKey }) async
    test('test parseSheetV1ParseSheetIdPost', () async {
      // TODO
    });

    // Process File
    //
    // Process an uploaded Excel file by splitting it into individual sheet files  - **file_id**: ID of the uploaded Excel file  This endpoint splits the Excel file into individual sheet files and stores them
    //
    //Future<Object> processFileV1ProcessFileIdPost(String fileId) async
    test('test processFileV1ProcessFileIdPost', () async {
      // TODO
    });

    // Save Portfolio
    //
    // Save a mutual fund portfolio to the database  Args:     portfolio_data: JSON data containing mutual fund portfolio information      Returns:     Saved portfolio data with database ID
    //
    //Future<Object> savePortfolioV1PortfoliosPost(Object body) async
    test('test savePortfolioV1PortfoliosPost', () async {
      // TODO
    });

    // Search Portfolios
    //
    // Search portfolios by fund name  Args:     fund_name: Fund name to search for      Returns:     List of matching portfolio summaries
    //
    //Future<Object> searchPortfoliosV1PortfoliosSearchGet(String fundName) async
    test('test searchPortfoliosV1PortfoliosSearchGet', () async {
      // TODO
    });

    // Upload Excel Complete
    //
    // 🚀 Complete Excel Upload Workflow - Does EVERYTHING automatically!  This endpoint handles the complete workflow: 1. ✅ Upload Excel file 2. ✅ Persist main file to database   3. ✅ Split Excel into individual sheet files 4. ✅ Persist all sheet files to database 5. ✅ Parse each sheet using manual or LLM parsing 6. ✅ Save all parsed portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: \"together\" (default) or \"manual\"  Returns: Complete results with all parsed portfolios
    //
    //Future<Object> uploadExcelCompleteV1UploadExcelPost(MultipartFile file, { String parseMethod }) async
    test('test uploadExcelCompleteV1UploadExcelPost', () async {
      // TODO
    });

    // Upload File
    //
    // Upload an Excel file and do ALL the work automatically: 1. Upload and persist main file to database 2. Split Excel into individual sheet files   3. Persist all sheet files to database 4. Parse each sheet and save portfolios to database  - **file**: Excel file to upload (.xlsx, .xls) - **parse_method**: Parsing method (\"manual\" or \"together\") - defaults to \"together\"  Returns complete processing results with all parsed portfolios
    //
    //Future<Object> uploadFileV1UploadPost(MultipartFile file, { String parseMethod }) async
    test('test uploadFileV1UploadPost', () async {
      // TODO
    });

  });
}
