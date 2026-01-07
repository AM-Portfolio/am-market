import 'package:am_auth_ui/core/services/secure_storage_service.dart';

import 'package:am_market_sdk_flutter/am_market_sdk_flutter.dart';

/// Service class to configure and provide Market Data SDK API clients
class MarketDataSdkService {
  static const String baseUrl = 'https://am.munish.org/api/market';
  
  late final ApiClient _apiClient;
  late final MarketDataApi marketDataApi;
  late final IndicesApi marketIndexApi;
  late final BrokerageCalculatorApi brokerageApi;
  late final MarketAnalyticsApi analyticsApi;
  late final InstrumentManagementApi instrumentApi;
  late final SecurityExplorerApi securityApi;
  late final MarginCalculatorApi marginApi;

  MarketDataSdkService() {
    _apiClient = ApiClient(basePath: baseUrl);
    
    // Initialize all API clients
    marketDataApi = MarketDataApi(_apiClient);
    marketIndexApi = IndicesApi(_apiClient);
    brokerageApi = BrokerageCalculatorApi(_apiClient);
    analyticsApi = MarketAnalyticsApi(_apiClient);
    instrumentApi = InstrumentManagementApi(_apiClient);
    securityApi = SecurityExplorerApi(_apiClient);
    marginApi = MarginCalculatorApi(_apiClient);
  }

  /// Configure authentication if needed
  void setAuthentication(String token) {
    // Add authentication headers if required
    _apiClient.addDefaultHeader('Authorization', 'Bearer $token');
  }
}
