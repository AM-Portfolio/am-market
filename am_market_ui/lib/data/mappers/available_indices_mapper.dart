import 'package:am_market_sdk/market/api.dart' as sdk;
import '../../models/available_indices.dart';

/// Mapper to convert SDK AvailableIndices to app model
class AvailableIndicesMapper {
  /// Convert SDK model to app model
  static AvailableIndices fromSdk(sdk.AvailableIndices sdkModel) {
    return AvailableIndices(
      broadMarketIndices: sdkModel.broadMarketIndices ?? [],
      sectoralIndices: sdkModel.sectoralIndices ?? [],
      thematicIndices: sdkModel.thematicIndices ?? [],
      strategyIndices: sdkModel.strategyIndices ?? [],
    );
  }
}
