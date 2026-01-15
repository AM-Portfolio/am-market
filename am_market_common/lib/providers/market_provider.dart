import 'dart:async';
import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import '../models/market_data.dart';
import '../models/available_indices.dart';
import '../models/historical_performance_model.dart';
import '../models/seasonality_model.dart';
import '../services/api_service.dart';
import '../data/repositories/market_data_repository.dart';


import 'package:am_common/core/services/price_service.dart';

class MarketProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  final MarketDataRepository? _repository;

  MarketProvider({MarketDataRepository? repository}) : _repository = repository;

  AvailableIndices? _availableIndices;
  StockIndicesMarketData? _currentIndexData;
  List<StockIndicesMarketData> _allIndicesData = []; // For Market Overview
  
  String? _selectedIndex;
  bool _isLoading = false;
  String? _error;
  bool _forceRefresh = false; // "Force Refresh" toggle state
  bool _indexSymbol = true; // True = fetch index data only, False = expand to constituents

  // Theme Management removed - moved to am_common_ui ThemeCubit

  // PriceService Integration
  PriceService? _priceService;
  
  void setPriceService(PriceService service) {
    _priceService = service;
    CommonLogger.info("PriceService delegated to MarketProvider", tag: "MarketProvider");
    
    // Sync initial cache if available
    _syncWithPriceService();
    
    // Subscribe to price stream to sync internal state (allIndicesData)
    // We use priceStream (full map) or updateStream (event).
    // updateStream is better for individual processing logic we already have.
    _priceService!.updateStream.listen((update) {
       if (update.quotes != null) {
          // Convert QuoteChange to Map<String, dynamic>
          update.quotes!.forEach((symbol, quote) {
             final data = quote.toJson();
             data['symbol'] = symbol; // Ensure symbol is present
             _processSingleUpdate(symbol, data);
          });
       }
    });
  }

  // Legacy Store Support (if needed, or just defer to PriceService)
  // We keep _livePrices as a local cache only if PriceService is null (fallback)
  // But ideally we read from PriceService.
  
  // Stream is now from PriceService
  Stream<Map<String, dynamic>> get livePriceStream {
    if (_priceService != null) {
      return _priceService!.priceStream.map((quotes) {
        return quotes.map((key, value) => MapEntry(key, value.toJson()));
      });
    }
    return _livePriceController.stream;
  }
  
  // Internal Fallback State (initialized if PriceService unavailable)
  final Map<String, Map<String, dynamic>> _internalLivePrices = {}; 
  final StreamController<Map<String, dynamic>> _livePriceController = StreamController<Map<String, dynamic>>.broadcast();

  // Legacy Store Support
  Map<String, Map<String, dynamic>> get livePrices {
      if (_priceService != null) {
          return {}; // UI should use QuoteChange, ignoring legacy map for PriceService path
      }
      return _internalLivePrices; 
  }

  // Unified Price Getter
  Map<String, dynamic>? getPrice(String symbol) {
      if (_priceService != null) {
          final quote = _priceService!.getQuote(symbol);
          if (quote != null) {
              return quote.toJson()..['symbol'] = symbol;
          }
      }
      return _internalLivePrices[symbol];
  }

  // Restored Getters
  AvailableIndices? get availableIndices => _availableIndices;
  StockIndicesMarketData? get currentIndexData => _currentIndexData;
  List<StockIndicesMarketData> get allIndicesData => _allIndicesData;
  
  // Cache for specific index constituents (used by Heatmap/Explorers)
  Map<String, List<StockData>> _indexConstituents = {};
  Map<String, List<StockData>> get indexConstituents => _indexConstituents;

  // Cache for Heatmap Data (Timeframe aware)
  Map<String, List<Map<String, dynamic>>> _heatmapData = {};
  Map<String, List<Map<String, dynamic>>> get heatmapData => _heatmapData;

  // Historical Performance Data (10Y view)
  HistoricalPerformanceResponse? _historicalPerformance;
  HistoricalPerformanceResponse? get historicalPerformance => _historicalPerformance;

  // Seasonality Data
  SeasonalityResponse? _seasonality;
  SeasonalityResponse? get seasonality => _seasonality;

  String? get selectedIndex => _selectedIndex;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get forceRefresh => _forceRefresh;
  bool get indexSymbol => _indexSymbol;

  // New Heatmap Values (Symbol -> Change%)
  Map<String, double>? _heatmapValues;
  Map<String, double>? get heatmapValues => _heatmapValues;

  void toggleForceRefresh(bool value) {
    _forceRefresh = value;
    notifyListeners();
  }

  void toggleIndexSymbol(bool value) {
    _indexSymbol = value;
    notifyListeners();
    // Auto-reload data when toggle changes
    if (_selectedIndex == "All Indices") {
      loadAllIndicesData();
    }
  }

  void updateLivePriceBatch(Map<String, dynamic> quotes) {
     // No-op if using PriceService as it handles its own updates
     if (_priceService != null) return;
     
     // Fallback for legacy
    if (quotes.isEmpty) return;

    // 1. Summary Log (Once per batch)
    final count = quotes.length;
    final firstKey = quotes.keys.first;
    final firstVal = quotes[firstKey] as Map;
    
    String logMsg = "Received update for optimized batch: $count symbols. Sample: $firstKey -> Price: ${firstVal['lastPrice']}";
    if (count > 1) {
      logMsg += " and ${count - 1} others.";
    }
    CommonLogger.info(logMsg, tag: "MarketUI");

    // 2. Process all
    quotes.forEach((symbol, data) {
      if (data is Map<String, dynamic>) {
         _processSingleUpdate(symbol, data);
      }
    });
  }

  void updateLivePrice(Map<String, dynamic> data) {
    if (_priceService != null) return;
    if (data.containsKey('symbol')) {
      _processSingleUpdate(data['symbol'], data);
    }
  }

  void _processSingleUpdate(String rawSymbol, Map<String, dynamic> data) {
      // 1. Store with raw key
      _internalLivePrices[rawSymbol] = data;

      // 2. Store with base key
      if (rawSymbol.contains(':')) {
        final baseSymbol = rawSymbol.split(':').last;
        _internalLivePrices[baseSymbol] = data;
      }
      
      // 3. Update allIndicesData
      String updateSymbolBase = rawSymbol.contains('|') ? rawSymbol.split('|').last : rawSymbol;
      
      for (int i = 0; i < _allIndicesData.length; i++) {
        if (_allIndicesData[i].indexSymbol.toUpperCase() == updateSymbolBase.toUpperCase()) {
             final current = _allIndicesData[i];
             final double? newLtp = data['lastPrice']?.toDouble();
             final double? newPChange = data['changePercent']?.toDouble(); 
             
             if (newLtp != null) {
                 _allIndicesData[i] = current.copyWith(
                     lastPrice: newLtp,
                     pChange: newPChange ?? current.pChange 
                 );
                 notifyListeners();
             }
             break; 
        }
      }

      // Emit event
      _livePriceController.add(data);
  }

  void _syncWithPriceService() {
    if (_priceService == null) {
        CommonLogger.warning("Skipping sync - PriceService is null", tag: "MarketProvider._syncWithPriceService");
        return;
    }
    
    if (_allIndicesData.isEmpty) {
        CommonLogger.warning("Skipping sync - No indices loaded", tag: "MarketProvider._syncWithPriceService");
        return; 
    }

    final symbols = _allIndicesData.map((e) => e.indexSymbol).toList();
    final quotes = _priceService!.getQuotes(symbols);
    
    CommonLogger.info("Attempting to sync prices for ${symbols.length} symbols. Found ${quotes.length} in PriceService cache.", tag: "MarketProvider._syncWithPriceService");

    if (quotes.isNotEmpty) {
       quotes.forEach((symbol, quote) {
          final data = quote.toJson();
          data['symbol'] = symbol; 
          _processSingleUpdate(symbol, data);
       });
    }
  }

  @override
  void dispose() {
    _livePriceController.close();
    super.dispose();
  }

  Future<void> loadIndices() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _availableIndices = await _apiService.fetchAvailableIndices();
      CommonLogger.info("Fetched available indices: ${_availableIndices?.broad.length ?? 0} broad, ${_availableIndices?.sectoral.length ?? 0} sectoral", tag: "MarketProvider.loadIndices");
      if (_availableIndices?.broad.isNotEmpty ?? false) {
        // Auto-select "All Indices" by default to show overview
        selectIndex("All Indices");
      }
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> selectIndex(String indexSymbol) async {
    CommonLogger.info("Selecting: $indexSymbol", tag: "MarketProvider.selectIndex");

    _selectedIndex = indexSymbol;
    if (_selectedIndex == "All Indices") {
      await loadAllIndicesData();
    } else if (_selectedIndex == "Dashboard") {
      // Dashboard needs all indices data just like "All Indices"
      CommonLogger.debug("Loading Dashboard data", tag: "MarketProvider.selectIndex");
      await loadAllIndicesData();
    } else if ([
      "Streamer", 
      "Instrument Explorer", 
      "Security Explorer", 
      "Price Test", 
      "ETF Explorer",
      "Admin Dashboard",
      "Analysis Dashboard",
      "Developer Dashboard",
      // User Mode navigation items (no data fetch required)
      "Market Analysis",
      "Heatmap",
      "Heatmap Explorer",
    ].contains(_selectedIndex)) {
      CommonLogger.debug("Selected view: $_selectedIndex (no data fetch required)", tag: "MarketProvider.selectIndex");
      // Do nothing, just update selection
      notifyListeners();
    } else {
      await refreshIndexData();
    }
  }


  Future<void> refreshIndexData() async {
    if (_selectedIndex == null || _selectedIndex == "All Indices" || _selectedIndex == "Streamer") {
      CommonLogger.debug("Skip refresh for $_selectedIndex", tag: "MarketProvider.refreshIndexData");
      return;
    }
    
    CommonLogger.info("Refreshing data for $_selectedIndex", tag: "MarketProvider.refreshIndexData");
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _currentIndexData = await _apiService.fetchIndexData(_selectedIndex!, forceRefresh: _forceRefresh);
      CommonLogger.debug("Data refreshed for $_selectedIndex. Constituents: ${_currentIndexData?.stocks.length ?? 0}", tag: "MarketProvider.refreshIndexData");

    } catch (e) {
      CommonLogger.error("Error refreshing $_selectedIndex", tag: "MarketProvider.refreshIndexData", error: e);

      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }


  Future<void> loadAllIndicesData() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      // STEP 1: Fetch available indices first if not already loaded
      if (_availableIndices == null) {
        _availableIndices = await _apiService.fetchAvailableIndices();
        CommonLogger.info("Fetched available indices", tag: "MarketProvider.loadAllIndicesData");

      }
      
      // STEP 2: Get all index symbols from available indices
      List<String> allSymbols = [
        ...(_availableIndices?.broad ?? []),
        ...(_availableIndices?.sectoral ?? []),
      ];
      
      if (allSymbols.isEmpty) {
        _error = "No indices available";
        CommonLogger.warning("No indices available", tag: "MarketProvider.loadAllIndicesData");

        return;
      }
      
      CommonLogger.info("Loading ${allSymbols.length} indices: $allSymbols", tag: "MarketProvider.loadAllIndicesData");

      
      // STEP 3: Call batch endpoint with the available symbols
      _allIndicesData = await _apiService.fetchIndicesBatch(
        allSymbols, 
        forceRefresh: _forceRefresh
      );
      
      CommonLogger.info("Successfully loaded ${_allIndicesData.length} indices", tag: "MarketProvider.loadAllIndicesData");

      // STEP 4: Initiate Stream - REMOVED per user request
      // Global PriceService is relied upon. No explicit connect call.
      
      // Attempt to sync from PriceService Cache if available
      _syncWithPriceService();
      
      // Explicit subscription removed per user request (expecting firehose)

    } catch (e) {
      CommonLogger.error("Error loading all indices data", tag: "MarketProvider.loadAllIndicesData", error: e);

      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> refreshCookies() async {
    bool success = await _apiService.refreshCookies();
    if (success) {
      // Re-fetch current view data
      if (_selectedIndex == "All Indices") {
        await loadAllIndicesData();
      } else if (_selectedIndex != null) {
        await refreshIndexData();
      }
    } else {
      _error = "Failed to refresh cookies";
      notifyListeners();
    }
  }

  Future<void> fetchIndexConstituents(String indexSymbol) async {
      CommonLogger.info("Fetching constituents for: $indexSymbol", tag: "MarketProvider.fetchIndexConstituents");
      try {
          final data = await _apiService.fetchIndexData(indexSymbol, forceRefresh: _forceRefresh);
          if (data != null) {
              _indexConstituents[indexSymbol] = data.stocks;
              notifyListeners();
          }
      } catch (e) {
          CommonLogger.error("Error fetching constituents for $indexSymbol", tag: "MarketProvider.fetchIndexConstituents", error: e);
          // Don't set global error to avoid disrupting other views
      }
  }

  Future<void> fetchHeatmapData(String indexSymbol, String timeFrame) async {
      final key = "$indexSymbol:$timeFrame";
      CommonLogger.info("Fetching heatmap data to key: $key", tag: "MarketProvider.fetchHeatmapData");
      
      try {
          // Use new dedicated endpoint for full index performance
          final result = await _apiService.fetchIndexPerformance(
              indexSymbol: indexSymbol,
              timeFrame: timeFrame,
          );
          
          _heatmapData[key] = result;
          notifyListeners();
          
      } catch (e) {
          CommonLogger.error("Error fetching heatmap data", tag: "MarketProvider.fetchHeatmapData", error: e);
      }
  }

  Future<void> loadHistoricalPerformance(String symbol) async {
      CommonLogger.info("Loading historical performance for $symbol", tag: "MarketProvider.loadHistoricalPerformance");
      _isLoading = true; 
      // Don't clear previous data immediately to avoid flicker, or maybe clear if symbol changed
      // For now, let's keep it simple
      notifyListeners();

      try {
          // Hardcoded 10 years as per requirement
          _historicalPerformance = await _apiService.fetchHistoricalPerformance(symbol, years: 10);
      } catch (e) {
          CommonLogger.error("Error loading historical performance", tag: "MarketProvider.loadHistoricalPerformance", error: e);
          _error = e.toString();
      } finally {
          _isLoading = false;
          notifyListeners();
      }
  }

  Future<void> loadHeatmap(String symbol, String timeframe) async {
    CommonLogger.info("Loading heatmap for $symbol ($timeframe)", tag: "MarketProvider.loadHeatmap");
    try {
      _heatmapValues = await _apiService.fetchHeatmap(symbol, timeframe: timeframe);
      notifyListeners();
    } catch (e) {
      CommonLogger.error("Error loading heatmap", tag: "MarketProvider.loadHeatmap", error: e);
      _heatmapValues = {}; // Clear or keep previous?
      // Better to clear or show error
      notifyListeners();
    }
  }

  Future<void> loadSeasonality(String symbol) async {
      CommonLogger.info("Loading seasonality for $symbol", tag: "MarketProvider.loadSeasonality");
      try {
          _seasonality = await _apiService.fetchSeasonality(symbol);
          notifyListeners();
      } catch (e) {
          CommonLogger.error("Error loading seasonality", tag: "MarketProvider.loadSeasonality", error: e);
      }
  }
}
