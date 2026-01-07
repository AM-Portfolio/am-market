import 'dart:async';
import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import '../models/market_data.dart';
import '../models/available_indices.dart';
import '../services/api_service.dart';
import '../data/repositories/market_data_repository.dart';


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

  Map<String, Map<String, dynamic>> _livePrices = {}; // Global live price cache
  final StreamController<Map<String, dynamic>> _livePriceController = StreamController<Map<String, dynamic>>.broadcast();

  AvailableIndices? get availableIndices => _availableIndices;
  StockIndicesMarketData? get currentIndexData => _currentIndexData;
  List<StockIndicesMarketData> get allIndicesData => _allIndicesData;
  Map<String, Map<String, dynamic>> get livePrices => _livePrices;
  Stream<Map<String, dynamic>> get livePriceStream => _livePriceController.stream;
  
  String? get selectedIndex => _selectedIndex;
  bool get isLoading => _isLoading;
  String? get error => _error;
  bool get forceRefresh => _forceRefresh;
  bool get indexSymbol => _indexSymbol;

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

  void updateLivePrice(Map<String, dynamic> data) {
    if (data.containsKey('symbol')) {
      final String rawSymbol = data['symbol'];
      
      // 1. Store with raw key (e.g., "NSE_EQ:TCS")
      _livePrices[rawSymbol] = data;

      // 2. Store with base key (e.g., "TCS") if a prefix exists
      if (rawSymbol.contains(':')) {
        final baseSymbol = rawSymbol.split(':').last;
        _livePrices[baseSymbol] = data;
      }

      // Emit event to stream instead of global notifyListeners
      _livePriceController.add(data);
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
    } else if ([
      "Streamer", 
      "Instrument Explorer", 
      "Security Explorer", 
      "Price Test", 
      "ETF Explorer",
      "Admin Dashboard"
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
}

