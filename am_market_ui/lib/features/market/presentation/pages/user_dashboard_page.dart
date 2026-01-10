import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_ui/providers/market_provider.dart';
import 'package:am_market_ui/models/market_data.dart';
import 'package:am_market_ui/models/top_mover_stock.dart';
import 'package:am_market_ui/widgets/index_card.dart';
import 'package:am_market_ui/widgets/top_movers_widget_v2.dart';
import 'package:am_market_ui/widgets/multi_index_chart.dart';
import 'package:am_market_ui/services/api_service.dart';

/// User Dashboard page with API-driven features
class UserDashboardPage extends StatefulWidget {
  const UserDashboardPage({super.key});

  @override
  State<UserDashboardPage> createState() => _UserDashboardPageState();
}

class _UserDashboardPageState extends State<UserDashboardPage> {
  late final ApiService _apiService;
  
  // Selected index for top movers (default: NIFTY 50)
  String selectedIndexForMovers = 'NIFTY 50';
  
  // Selected indices for comparison chart (default: 3 major indices)
  List<String> selectedIndicesForChart = ['NIFTY 50', 'SENSEX', 'NIFTY BANK'];
  
  // Top movers data
  List<TopMoverStock> topGainers = [];
  List<TopMoverStock> topLosers = [];
  bool isLoadingMovers = false;
  
  // Historical chart data
  Map<String, List<Map<String, dynamic>>> historicalData = {};
  bool isLoadingChart = false;
  String? chartError;
  String selectedTimeframe = '1Y'; // Default 1 year

  @override
  void initState() {
    super.initState();
    _apiService = ApiService();
    
    // Load initial data
    Future.delayed(Duration.zero, () {
      _loadTopMovers();
      _loadHistoricalData();
    });
  }

  /// Load top gainers and losers for selected index
  Future<void> _loadTopMovers() async {
    if (!mounted) return;
    
    setState(() {
      isLoadingMovers = true;
    });

    try {
      final gainersData = await _apiService.fetchMovers(
        type: 'gainers',
        limit: 5,
        indexSymbol: selectedIndexForMovers,
      );
      
      final losersData = await _apiService.fetchMovers(
        type: 'losers',
        limit: 5,
        indexSymbol: selectedIndexForMovers,
      );

      if (!mounted) return;
      
      setState(() {
        topGainers = gainersData.map((e) => TopMoverStock.fromJson(e)).toList();
        topLosers = losersData.map((e) => TopMoverStock.fromJson(e)).toList();
        isLoadingMovers = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        isLoadingMovers = false;
      });
      CommonLogger.error('Error loading top movers', tag: 'UserDashboardPage', error: e);
    }
  }

  /// Load historical data for selected indices
  Future<void> _loadHistoricalData() async {
    if (!mounted) return;
    
    setState(() {
      isLoadingChart = true;
      chartError = null;
    });

    try {
      final Map<String, List<Map<String, dynamic>>> data = {};
      
      for (final symbol in selectedIndicesForChart) {
        final symbolData = await _apiService.fetchHistory(symbol, selectedTimeframe);
        data[symbol] = symbolData;
      }

      if (!mounted) return;
      
      setState(() {
        historicalData = data;
        isLoadingChart = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        chartError = 'Failed to load chart data';
        isLoadingChart = false;
      });
      CommonLogger.error('Error loading historical data', tag: 'UserDashboardPage', error: e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    return Consumer<MarketProvider>(
      builder: (context, provider, _) {
        // Loading state
        if (provider.isLoading && provider.allIndicesData.isEmpty) {
          return const Center(
            child: CircularProgressIndicator(
              color: Color(0xFF00D1FF),
            ),
          );
        }

        // Main content
        return Container(
          decoration: AppGlassmorphismV2.techBackground(isDark: isDark),
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header Section
                InfoLayerCard(
                  title: 'Market Dashboard',
                  subtitle: 'Real-time market overview',
                  icon: Icons.dashboard_rounded,
                  colorScheme: 'primary',
                ),
                
                const SizedBox(height: 24),
                
                // Index Cards Carousel  
                SizedBox(
                  height: 140,
                  child: ListView.builder(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: provider.allIndicesData.length,
                    itemBuilder: (context, index) {
                      final data = provider.allIndicesData[index];
                      final isSelected = data.indexSymbol == selectedIndexForMovers;
                      
                      return GestureDetector(
                        onTap: () {
                          setState(() {
                            selectedIndexForMovers = data.indexSymbol;
                          });
                          _loadTopMovers();
                        },
                        child: Container(
                          decoration: isSelected
                              ? BoxDecoration(
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(
                                    color: const Color(0xFF00D1FF),
                                    width: 2,
                                  ),
                                )
                              : null,
                          child: IndexCard(data: data),
                        ),
                      );
                    },
                  ),
                ),
                
                const SizedBox(height: 32),
                
                // Top Movers Section
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Text(
                          'TOP MOVERS',
                          style: TextStyle(
                            color: isDark ? Colors.white38 : Colors.black45,
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 2,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: const Color(0xFF00D1FF).withOpacity(0.2),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            selectedIndexForMovers,
                            style: const TextStyle(
                              color: Color(0xFF00D1FF),
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: TopMoversWidgetV2(
                            movers: topGainers,
                            title: 'Top Gainers',
                            isGainers: true,
                            isLoading: isLoadingMovers,
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: TopMoversWidgetV2(
                            movers: topLosers,
                            title: 'Top Losers',
                            isGainers: false,
                            isLoading: isLoadingMovers,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
                
                const SizedBox(height: 40),
                
                // Historical Chart Section
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'INDICES COMPARISON',
                          style: TextStyle(
                            color: isDark ? Colors.white38 : Colors.black45,
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 2,
                          ),
                        ),
                        // Timeframe selector
                        Container(
                          padding: const EdgeInsets.all(4),
                          decoration: BoxDecoration(
                            color: isDark ? Colors.white.withOpacity(0.05) : Colors.black.withOpacity(0.05),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(
                              color: Colors.white.withOpacity(0.1),
                            ),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: ['1D', '1W', '1M', '3M', '6M', '1Y', '5Y'].map((tf) {
                              final isSelected = tf == selectedTimeframe;
                              return GestureDetector(
                                onTap: () {
                                  setState(() {
                                    selectedTimeframe = tf;
                                  });
                                  _loadHistoricalData();
                                },
                                child: Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                                  decoration: BoxDecoration(
                                    color: isSelected
                                        ? const Color(0xFF00D1FF).withOpacity(0.2)
                                        : Colors.transparent,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    tf,
                                    style: TextStyle(
                                      color: isSelected ? const Color(0xFF00D1FF) : Colors.white.withOpacity(0.6),
                                      fontSize: 11,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                              );
                            }).toList(),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    
                    // Multi-Index Chart
                    SizedBox(
                      height: 400,
                      child: MultiIndexChart(
                        historicalData: historicalData,
                        selectedIndices: selectedIndicesForChart,
                        isLoading: isLoadingChart,
                        error: chartError,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
