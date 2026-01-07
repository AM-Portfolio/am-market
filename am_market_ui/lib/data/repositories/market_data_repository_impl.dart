import 'package:am_market_sdk_flutter/am_market_sdk_flutter.dart' as sdk;
import '../../models/market_data.dart';
import '../../models/available_indices.dart';
import '../../services/market_data_sdk_service.dart';
import '../mappers/stock_indices_mapper.dart';
import '../mappers/available_indices_mapper.dart';
import 'market_data_repository.dart';

/// Implementation of MarketDataRepository using the Market Data SDK
/// 
/// This implementation:
/// - Uses the generated SDK for API calls
/// - Maps SDK models to app-specific models via mappers
/// - Provides error handling and logging
class MarketDataRepositoryImpl implements MarketDataRepository {
  final MarketDataSdkService _sdkService;

  MarketDataRepositoryImpl(this._sdkService);

  @override
  Future<AvailableIndices> getAvailableIndices() async {
    try {
      final response = await _sdkService.indexDataApi.getAllAvailableIndices();
      
      if (response == null) {
        throw Exception('No data received from API');
      }

      // Map SDK model to app model
      return AvailableIndicesMapper.fromSdk(response);
    } catch (e) {
      throw Exception('Failed to fetch available indices: $e');
    }
  }

  @override
  Future<StockIndicesMarketData> getIndexData(
    String indexSymbol, {
    bool fetchConstituents = true,
  }) async {
    try {
      final response = await _sdkService.indexDataApi.getIndexData(
        indexSymbol,
        expandToConstituents: fetchConstituents,
      );

      if (response == null) {
        throw Exception('No data received for index: $indexSymbol');
      }

      // Map SDK model to app model
      return StockIndicesMapper.fromSdk(response);
    } catch (e) {
      throw Exception('Failed to fetch index data for $indexSymbol: $e');
    }
  }

  @override
  Future<Map<String, dynamic>> getHistoricalData({
    required List<String> symbols,
    required String fromDate,
    required String toDate,
    String interval = '1d',
  }) async {
    try {
      final request = sdk.HistoricalDataRequest(
        symbols: symbols,
        fromDate: fromDate,
        toDate: toDate,
        interval: interval,
      );

      final response = await _sdkService.marketDataApi.getHistoricalDataV1(
        historicalDataRequest: request,
      );

      if (response == null) {
        throw Exception('No historical data received');
      }

      // For now, return raw SDK response
      // TODO: Create mapper for historical data
      return response.toJson();
    } catch (e) {
      throw Exception('Failed to fetch historical data: $e');
    }
  }

  @override
  Future<Map<String, dynamic>> calculateBrokerage({
    required String tradingSymbol,
    required int quantity,
    required double buyPrice,
    required double sellPrice,
    required String exchange,
    required String tradeType,
  }) async {
    try {
      final request = sdk.BrokerageCalculationRequest(
        tradingSymbol: tradingSymbol,
        quantity: quantity,
        buyPrice: buyPrice,
        sellPrice: sellPrice,
        exchange: exchange,
        tradeType: tradeType as sdk.BrokerageCalculationRequestTradeTypeEnum?,
      );

      final response = await _sdkService.brokerageApi.calculateBrokerage(
        brokerageCalculationRequest: request,
      );

      if (response == null) {
        throw Exception('No brokerage calculation received');
      }

      return response.toJson();
    } catch (e) {
      throw Exception('Failed to calculate brokerage: $e');
    }
  }

  @override
  Future<List<Map<String, dynamic>>> searchSecurities({
    String? query,
    List<String>? symbols,
    String? sector,
    String? industry,
  }) async {
    try {
      final request = sdk.SecuritySearchRequest(
        query: query,
        symbols: symbols,
        sector: sector,
        industry: industry,
      );

      final response = await _sdkService.securityApi.searchSecurities(
        securitySearchRequest: request,
      );

      if (response == null) {
        return [];
      }

      // Map SDK response to list of maps
      return response.map((e) => e.toJson()).toList();
    } catch (e) {
      throw Exception('Failed to search securities: $e');
    }
  }
}
