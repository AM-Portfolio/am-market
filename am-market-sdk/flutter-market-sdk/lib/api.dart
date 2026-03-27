// ignore_for_file: unnecessary_null_comparison, parameter_assignments, unused_import, unused_element, always_put_required_named_parameters_first, constant_identifier_names, lines_longer_than_80_chars, avoid_dynamic_calls, invalid_assignment, undefined_method, undefined_getter, for_in_of_invalid_type, case_expression_type_is_not_switch_expression_subtype, deprecated_member_use_from_same_package
//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//
// @dart=2.18

library openapi.api;

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:collection/collection.dart';
import 'package:http/http.dart';
import 'package:intl/intl.dart';
import 'package:meta/meta.dart';

part 'api_client.dart';
part 'api_helper.dart';
part 'api_exception.dart';
part 'auth/authentication.dart';
part 'auth/api_key_auth.dart';
part 'auth/oauth.dart';
part 'auth/http_basic_auth.dart';
part 'auth/http_bearer_auth.dart';

part 'api/brokerage_calculator_api.dart';
part 'api/cookie_scraper_controller_api.dart';
part 'api/indices_api.dart';
part 'api/instrument_management_api.dart';
part 'api/margin_calculator_api.dart';
part 'api/market_analytics_api.dart';
part 'api/market_data_api.dart';
part 'api/market_data_admin_controller_api.dart';
part 'api/market_data_stream_api.dart';
part 'api/security_explorer_api.dart';

part 'model/audit.dart';
part 'model/audit_data.dart';
part 'model/brokerage_calculation_request.dart';
part 'model/brokerage_calculation_response.dart';
part 'model/cookie_info.dart';
part 'model/historical_data.dart';
part 'model/historical_data_metadata.dart';
part 'model/historical_data_request.dart';
part 'model/historical_data_response_v1.dart';
part 'model/index_metadata.dart';
part 'model/ingestion_job_log.dart';
part 'model/instrument_search_criteria.dart';
part 'model/margin_calculation_request.dart';
part 'model/margin_calculation_response.dart';
part 'model/market_data_update.dart';
part 'model/ohlc_request.dart';
part 'model/ohlcvt_point.dart';
part 'model/position.dart';
part 'model/position_margin.dart';
part 'model/quote_change.dart';
part 'model/quotes_request.dart';
part 'model/security_document.dart';
part 'model/security_key.dart';
part 'model/security_metadata.dart';
part 'model/security_search_request.dart';
part 'model/stock_data.dart';
part 'model/stock_indices_market_data.dart';
part 'model/stream_connect_request.dart';
part 'model/stream_connect_response.dart';
part 'model/website_cookies.dart';


/// An [ApiClient] instance that uses the default values obtained from
/// the OpenAPI specification file.
var defaultApiClient = ApiClient();

const _delimiters = {'csv': ',', 'ssv': ' ', 'tsv': '\t', 'pipes': '|'};
const _dateEpochMarker = 'epoch';
const _deepEquality = DeepCollectionEquality();
final _dateFormatter = DateFormat('yyyy-MM-dd');
final _regList = RegExp(r'^List<(.*)>$');
final _regSet = RegExp(r'^Set<(.*)>$');
final _regMap = RegExp(r'^Map<String,(.*)>$');

bool _isEpochMarker(String? pattern) => pattern == _dateEpochMarker || pattern == '/$_dateEpochMarker/';
