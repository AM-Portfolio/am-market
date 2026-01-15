import 'package:am_market_common/models/market_data.dart';
import '../../services/api_service.dart';

class MarketDataRepository {
  final ApiService _apiService;

  MarketDataRepository(this._apiService);

  Future<MarketData> getMarketOverview() async {
    // Implementation needed - likely calls API
    // For now returning mock or empty
    return MarketData(indices: [], globalIndices: []);
  }
}
