/// Market Module API endpoint constants
class MarketEndpoints {
  // Base URL
  static const String baseUrl = "http://localhost:8092"; //'https://am.munish.org/api/market';
  
  // Indices
  static const String availableIndices = '/v1/indices/available';
  static const String indicesBatch = '/v1/indices/batch';
  
  // Market Data (Real-time & Historical)
  static const String livePrices = '/v1/market-data/live-prices';
  static const String historicalData = '/v1/market-data/historical-data';
  static const String streamConnect = '/v1/market-data/stream/connect';
  static const String streamDisconnect = '/v1/market-data/stream/disconnect';
  
  // Market Analytics
  static const String movers = '/v1/market-analytics/movers';
  static const String sectors = '/v1/market-analytics/sectors';
  static const String marketCap = '/v1/market-analytics/market-cap';
  static const String historicalCharts = '/v1/market-analytics/historical-charts';
  
  // Instruments
  static const String instrumentsSearch = '/v1/instruments/search';
  
  static const String authLoginUrl = '/v1/market-data/auth/login-url';
  
  // Scraper/System
  static const String refreshCookies = '/api/scraper/cookies';
}
