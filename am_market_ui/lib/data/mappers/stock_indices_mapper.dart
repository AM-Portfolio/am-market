import 'package:am_market_sdk/market/api.dart' as sdk;
import '../../models/market_data.dart';

/// Mapper to convert SDK StockIndicesMarketData to app model
class StockIndicesMapper {
  /// Convert SDK model to app model
  static StockIndicesMarketData fromSdk(sdk.StockIndicesMarketData sdkModel) {
    return StockIndicesMarketData(
      indexSymbol: sdkModel.indexSymbol ?? '',
      data: sdkModel.data?.map(_mapStockData).toList() ?? [],
      metadata: _mapIndexMetadata(sdkModel.metadata),
      docVersion: sdkModel.docVersion,
    );
  }

  /// Map SDK StockData to app StockData
  static StockData _mapStockData(sdk.StockData sdkData) {
    return StockData(
      symbol: sdkData.symbol ?? '',
      identifier: sdkData.identifier ?? '',
      series: sdkData.series ?? '',
      name: sdkData.name ?? '',
      ffmc: sdkData.ffmc?.toInt() ?? 0,
      companyName: sdkData.companyName ?? '',
      isin: sdkData.isin ?? '',
      industry: sdkData.industry ?? '',
    );
  }

  /// Map SDK IndexMetadata to app IndexMetadata
  static IndexMetadata? _mapIndexMetadata(sdk.IndexMetadata? sdkMetadata) {
    if (sdkMetadata == null) return null;

    return IndexMetadata(
      indexName: sdkMetadata.indexName ?? '',
      open: sdkMetadata.open ?? 0.0,
      high: sdkMetadata.high ?? 0.0,
      low: sdkMetadata.low ?? 0.0,
      previousClose: sdkMetadata.previousClose ?? 0.0,
      last: sdkMetadata.last ?? 0.0,
      percChange: sdkMetadata.percChange ?? 0.0,
      change: sdkMetadata.change ?? 0.0,
      timeVal: sdkMetadata.timeVal ?? '',
      yearHigh: sdkMetadata.yearHigh ?? 0.0,
      yearLow: sdkMetadata.yearLow ?? 0.0,
      totalTradedVolume: sdkMetadata.totalTradedVolume?.toInt() ?? 0,
      totalTradedValue: sdkMetadata.totalTradedValue ?? 0.0,
    );
  }
}
