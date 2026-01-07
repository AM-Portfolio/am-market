/// Model class to hold price test page state
class PriceTestState {
  // Symbol selection
  final bool useIndexDropdown;
  final Set<String> selectedIndices;
  final String symbolText;
  
  // Filters
  final String selectedInterval;
  final DateTime? fromDate;
  final DateTime? toDate;
  final bool showDateRange;
  final bool showFilters;
  
  // Advanced filters
  final bool isIndexSymbol;
  final bool forceRefresh;
  final bool continuous;
  final String instrumentType;
  
  // UI state
  final bool isLoading;
  final String? error;
  final Map<String, dynamic>? liveData;
  final Set<String> expandedCards;
  final Map<String, Map<String, dynamic>> historicalDataCache;

  const PriceTestState({
    this.useIndexDropdown = true,
    this.selectedIndices = const {},
    this.symbolText = '',
    this.selectedInterval = '1D',
    this.fromDate,
    this.toDate,
    this.showDateRange = false,
    this.showFilters = false,
    this.isIndexSymbol = false,
    this.forceRefresh = false,
    this.continuous = false,
    this.instrumentType = 'STOCK',
    this.isLoading = false,
    this.error,
    this.liveData,
    this.expandedCards = const {},
    this.historicalDataCache = const {},
  });

  PriceTestState copyWith({
    bool? useIndexDropdown,
    Set<String>? selectedIndices,
    String? symbolText,
    String? selectedInterval,
    DateTime? fromDate,
    DateTime? toDate,
    bool? showDateRange,
    bool? showFilters,
    bool? isIndexSymbol,
    bool? forceRefresh,
    bool? continuous,
    String? instrumentType,
    bool? isLoading,
    String? error,
    Map<String, dynamic>? liveData,
    Set<String>? expandedCards,
    Map<String, Map<String, dynamic>>? historicalDataCache,
  }) {
    return PriceTestState(
      useIndexDropdown: useIndexDropdown ?? this.useIndexDropdown,
      selectedIndices: selectedIndices ?? this.selectedIndices,
      symbolText: symbolText ?? this.symbolText,
      selectedInterval: selectedInterval ?? this.selectedInterval,
      fromDate: fromDate ?? this.fromDate,
      toDate: toDate ?? this.toDate,
      showDateRange: showDateRange ?? this.showDateRange,
      showFilters: showFilters ?? this.showFilters,
      isIndexSymbol: isIndexSymbol ?? this.isIndexSymbol,
      forceRefresh: forceRefresh ?? this.forceRefresh,
      continuous: continuous ?? this.continuous,
      instrumentType: instrumentType ?? this.instrumentType,
      isLoading: isLoading ?? this.isLoading,
      error: error,
      liveData: liveData ?? this.liveData,
      expandedCards: expandedCards ?? this.expandedCards,
      historicalDataCache: historicalDataCache ?? this.historicalDataCache,
    );
  }
}
