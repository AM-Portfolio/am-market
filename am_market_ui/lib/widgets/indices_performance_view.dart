import 'package:am_design_system/am_design_system.dart';
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/market_provider.dart';
import '../models/market_data.dart';
import '../services/api_service.dart';
import '../services/stream_service.dart';
import '../widgets/multi_index_chart.dart';


class IndicesPerformanceView extends StatefulWidget {
  const IndicesPerformanceView({Key? key}) : super(key: key);

  @override
  State<IndicesPerformanceView> createState() => _IndicesPerformanceViewState();
}

class _IndicesPerformanceViewState extends State<IndicesPerformanceView> {
  final ApiService _apiService = ApiService();
  final StreamService _streamService = StreamService();
  StreamSubscription? _streamSubscription;

  // State
  Map<String, List<Map<String, dynamic>>> _historicalDataCache = {};
  Set<String> _selectedForChart = {};
  bool _isLoadingHistorical = false;
  bool _showIndexList = false;
  String? _error;

  // Date range for historical data (last 30 days)
  late DateTime _fromDate;
  late DateTime _toDate;

  // Default indices to load on page load
  static const List<String> _defaultIndices = ['INDIA VIX', 'NIFTY 50'];

  @override
  void initState() {
    super.initState();
    _toDate = DateTime.now();
    _fromDate = _toDate.subtract(const Duration(days: 30));

    _streamService.connect();
    _setupStreamListener();
    
    // Pre-select default indices for chart
    _selectedForChart = Set.from(_defaultIndices);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    
    // Fetch historical data for default indices when live data is available
    final provider = context.watch<MarketProvider>();
    if (provider.allIndicesData.isNotEmpty && _historicalDataCache.isEmpty && !_isLoadingHistorical) {
      CommonLogger.info("IndicesPerformanceView.didChangeDependencies", 
          "Live data loaded (${provider.allIndicesData.length} indices), fetching historical data for defaults");
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _fetchHistoricalDataForIndices(_defaultIndices);
      });
    }
  }

  @override
  void dispose() {
    _streamSubscription?.cancel();
    _streamService.dispose();
    super.dispose();
  }

  void _setupStreamListener() {
    _streamSubscription = _streamService.stream.listen((message) {
      if (!mounted) return;

      if (message.containsKey('quotes')) {
        final provider = context.read<MarketProvider>();
        final newQuotes = message['quotes'] as Map<String, dynamic>;

        newQuotes.forEach((symbol, quoteData) {
          provider.updateLivePrice(quoteData);
        });
      }
    });
  }

  Future<void> _fetchHistoricalDataForIndices(List<String> indicesToFetch) async {
    final provider = context.read<MarketProvider>();
    
    CommonLogger.info("IndicesPerformanceView.fetchHistorical", 
        "Starting fetch for ${indicesToFetch.length} indices: ${indicesToFetch.join(', ')}");
    
    if (provider.allIndicesData.isEmpty) {
      CommonLogger.warning("IndicesPerformanceView.fetchHistorical", 
          "No indices data available, skipping historical fetch");
      return;
    }

    setState(() {
      _isLoadingHistorical = true;
      _error = null;
    });

    try {
      final dateFormat = DateFormat('yyyy-MM-dd');
      final fromStr = dateFormat.format(_fromDate);
      final toStr = dateFormat.format(_toDate);

      CommonLogger.info("IndicesPerformanceView.fetchHistorical", 
          "Fetching data from $fromStr to $toStr");

      final Map<String, List<Map<String, dynamic>>> newCache = Map.from(_historicalDataCache);

      // Fetch historical data only for specified indices
      for (final indexSymbol in indicesToFetch) {
        // Skip if already cached
        if (newCache.containsKey(indexSymbol)) {
          CommonLogger.info("IndicesPerformanceView.fetchHistorical", 
              "Skipping $indexSymbol (already cached)");
          continue;
        }

        try {
          CommonLogger.info("IndicesPerformanceView.fetchHistorical", 
              "Fetching for $indexSymbol");
          
          final data = await _apiService.fetchHistoricalData(
            symbols: [indexSymbol],
            from: fromStr,
            to: toStr,
            interval: '1D',
            isIndexSymbol: true,
            forceRefresh: false,
          );

          CommonLogger.info("IndicesPerformanceView.fetchHistorical", 
              "Response for $indexSymbol: ${data.keys.join(', ')}");

          if (data.containsKey('data')) {
            final historicalData = data['data'] as Map<String, dynamic>;
            historicalData.forEach((sym, stockData) {
              CommonLogger.info("IndicesPerformanceView.fetchHistorical", 
                  "Processing symbol: $sym");
              
              if (stockData is Map && stockData['dataPoints'] != null) {
                final dataPoints = (stockData['dataPoints'] as List)
                    .map((e) => Map<String, dynamic>.from(e))
                    .toList();

                if (dataPoints.isNotEmpty) {
                  newCache[sym] = dataPoints;
                  CommonLogger.info("IndicesPerformanceView.fetchHistorical", 
                      "Cached ${dataPoints.length} points for $sym");
                }
              }
            });
          }
        } catch (e) {
          CommonLogger.error("IndicesPerformanceView.fetchHistorical",
              "Error fetching data for $indexSymbol", e);
        }
      }

      setState(() {
        _historicalDataCache = newCache;
        _isLoadingHistorical = false;
      });

      CommonLogger.info("IndicesPerformanceView.fetchHistorical",
          "Completed! Total cached: ${newCache.length} indices: ${newCache.keys.join(', ')}");
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoadingHistorical = false;
      });
      CommonLogger.error(
          "IndicesPerformanceView.fetchHistorical", "Error fetching historical data", e);
    }
  }

  Future<void> _fetchHistoricalDataForAll() async {
    final provider = context.read<MarketProvider>();
    final allSymbols = provider.allIndicesData.map((e) => e.indexSymbol).toList();
    await _fetchHistoricalDataForIndices(allSymbols);
  }

  void _toggleIndexForChart(String symbol) {
    setState(() {
      if (_selectedForChart.contains(symbol)) {
        _selectedForChart.remove(symbol);
      } else {
        if (_selectedForChart.length < 3) {
          _selectedForChart.add(symbol);
          
          // Fetch historical data if not already cached
          if (!_historicalDataCache.containsKey(symbol)) {
            CommonLogger.info("IndicesPerformanceView.toggleIndexForChart", 
                "Fetching historical data for newly selected index: $symbol");
            _fetchHistoricalDataForIndices([symbol]);
          }
        } else {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Maximum 3 indices can be compared'),
              duration: Duration(seconds: 2),
            ),
          );
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<MarketProvider>(
      builder: (context, provider, child) {
        if (provider.isLoading) {
          // Return empty box so parent LoadingWrapper shows skeleton
          return const SizedBox.shrink(); 
        }

        if (provider.error != null) {
          return Center(
            child: Text(
              'Error: ${provider.error}',
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          );
        }

        if (provider.allIndicesData.isEmpty) {
          return Center(
            child: Text(
              'No data loaded. Click refresh to load indices.',
              style: TextStyle(color: Theme.of(context).disabledColor),
            ),
          );
        }

        final allIndices = List<StockIndicesMarketData>.from(provider.allIndicesData);
        allIndices.sort((a, b) => b.pChange.compareTo(a.pChange));

        return Container(
          color: Theme.of(context).scaffoldBackgroundColor,
          child: Column(
            children: [
              _buildHeader(context),
              _buildSelectedIndicesPills(context),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Chart Section
                      SizedBox(
                        height: 400,
                        child: MultiIndexChart(
                          historicalData: _historicalDataCache,
                          selectedIndices: _selectedForChart.toList(),
                          isLoading: _isLoadingHistorical,
                          error: _error,
                        ),
                      ),

                      const SizedBox(height: 24),

                      // Index Selector
                      _buildIndexSelector(context, allIndices),

                      const SizedBox(height: 24),

                      // Collapsible Index List
                      if (_showIndexList) _buildIndexGrid(context, allIndices),
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildHeader(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: theme.cardColor,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [theme.primaryColor, theme.primaryColorLight],
              ),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(Icons.dashboard, color: theme.colorScheme.onPrimary, size: 24),
          ),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'All Indices Overview',
                style: theme.textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                'Compare up to 3 indices',
                style: theme.textTheme.bodySmall,
              ),
            ],
          ),
          const Spacer(),
          IconButton(
            onPressed: _fetchHistoricalDataForAll,
            icon: const Icon(Icons.refresh),
            tooltip: 'Refresh Data',
            style: IconButton.styleFrom(
              backgroundColor: theme.canvasColor,
              foregroundColor: theme.iconTheme.color,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSelectedIndicesPills(BuildContext context) {
    if (_selectedForChart.isEmpty) return const SizedBox.shrink();
    final theme = Theme.of(context);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      decoration: BoxDecoration(
        color: theme.cardColor,
        border: Border(
          bottom: BorderSide(color: theme.dividerColor),
        ),
      ),
      child: Wrap(
        spacing: 8,
        runSpacing: 8,
        children: _selectedForChart.map((symbol) {
          final color = MultiIndexChart.indexColors[
              _selectedForChart.toList().indexOf(symbol) %
                  MultiIndexChart.indexColors.length];

          return Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: color),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: BoxDecoration(
                    color: color,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  symbol,
                  style: TextStyle(
                    color: color,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(width: 4),
                InkWell(
                  onTap: () => _toggleIndexForChart(symbol),
                  child: Icon(Icons.close, size: 16, color: color),
                ),
              ],
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildIndexSelector(BuildContext context, List<StockIndicesMarketData> allIndices) {
    final theme = Theme.of(context);
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: theme.cardColor,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Select Indices to Compare',
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.bold,
                ),
              ),
              TextButton.icon(
                onPressed: () => setState(() => _showIndexList = !_showIndexList),
                icon: Icon(_showIndexList ? Icons.expand_less : Icons.expand_more),
                label: Text(_showIndexList ? 'Hide All' : 'Show All'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: allIndices.take(15).map((indexData) {
              final isSelected = _selectedForChart.contains(indexData.indexSymbol);
              final isPositive = indexData.change >= 0;

              return InkWell(
                onTap: () => _toggleIndexForChart(indexData.indexSymbol),
                borderRadius: BorderRadius.circular(8),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: isSelected ? theme.primaryColor.withOpacity(0.1) : theme.canvasColor,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: isSelected ? theme.primaryColor : theme.dividerColor,
                      width: isSelected ? 1.5 : 1,
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (isSelected)
                        Padding(
                          padding: const EdgeInsets.only(right: 6),
                          child: Icon(Icons.check_circle, color: theme.primaryColor, size: 14),
                        ),
                      Text(
                        indexData.indexSymbol,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: isSelected ? theme.primaryColor : theme.textTheme.bodyMedium?.color,
                          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '${isPositive ? '+' : ''}${indexData.pChange.toStringAsFixed(2)}%',
                        style: TextStyle(
                          color: isPositive ? Colors.green : Colors.red,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildIndexGrid(BuildContext context, List<StockIndicesMarketData> allIndices) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'All Indices',
          style: theme.textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 16),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 4,
            childAspectRatio: 2.5,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
          ),
          itemCount: allIndices.length,
          itemBuilder: (context, index) {
            return _buildCompactIndexCard(context, allIndices[index]);
          },
        ),
      ],
    );
  }

  Widget _buildCompactIndexCard(BuildContext context, StockIndicesMarketData data) {
    final theme = Theme.of(context);
    final isPositive = data.change >= 0;
    final isSelected = _selectedForChart.contains(data.indexSymbol);

    return InkWell(
      onTap: () => _toggleIndexForChart(data.indexSymbol),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: theme.cardColor,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isSelected ? theme.primaryColor : theme.dividerColor,
            width: isSelected ? 2 : 1,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.05),
              blurRadius: 4,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    data.indexSymbol,
                    style: theme.textTheme.bodyMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                if (isSelected)
                  Icon(Icons.check_circle, color: theme.primaryColor, size: 16),
              ],
            ),
            Text(
              data.lastPrice.toStringAsFixed(2),
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w600,
                fontSize: 14,
              ),
            ),
            Row(
              children: [
                Icon(
                  isPositive ? Icons.arrow_upward : Icons.arrow_downward,
                  color: isPositive ? Colors.green : Colors.red,
                  size: 12,
                ),
                const SizedBox(width: 4),
                Text(
                  '${isPositive ? '+' : ''}${data.pChange.toStringAsFixed(2)}%',
                  style: TextStyle(
                    color: isPositive ? Colors.green : Colors.red,
                    fontSize: 11,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
