import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_common/providers/market_provider.dart';
import 'package:am_market_common/models/market_data.dart';
import 'package:am_market_common/models/top_mover_stock.dart';
import 'package:am_market_ui/shared/widgets/index_card.dart';
import '../widgets/top_movers_widget_v2.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/multi_index_chart.dart';
import 'package:am_market_common/services/api_service.dart';

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
  
  // Selected indices for comparison chart
  List<String> selectedIndicesForChart = ['NIFTY 50', 'NIFTY BANK'];
  
  // Top movers data
  List<TopMoverStock> topGainers = [];
  List<TopMoverStock> topLosers = [];
  bool isLoadingMovers = false;
  
  // Historical chart data
  Map<String, List<Map<String, dynamic>>> historicalData = {};
  bool isLoadingChart = false;
  String? chartError;
  String selectedTimeframe = '1Y'; // Default 1 year
  bool isBarChart = false; // Chart type toggle

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
      // Unified call for both gainers and losers
      final unifiedData = await _apiService.fetchMoversUnified(
        limit: 5,
        indexSymbol: selectedIndexForMovers,
        timeFrame: selectedTimeframe,
      );
      
      if (!mounted) return;
      
      setState(() {
        topGainers = (unifiedData['gainers'] ?? []).map((e) => TopMoverStock.fromJson(e)).toList();
        topLosers = (unifiedData['losers'] ?? []).map((e) => TopMoverStock.fromJson(e)).toList();
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
      final data = await _apiService.fetchHistoryBatch(selectedIndicesForChart, selectedTimeframe);

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

  void _showAddIndexDialog(MarketProvider provider) {
    showDialog(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text('Compare Indices', style: TextStyle(color: Colors.white)),
              backgroundColor: const Color(0xFF1E293B),
              content: SizedBox(
                width: double.maxFinite,
                height: 400,
                child: ListView.builder(
                  itemCount: provider.allIndicesData.length,
                  itemBuilder: (context, index) {
                    final data = provider.allIndicesData[index];
                    final isSelected = selectedIndicesForChart.contains(data.indexSymbol);
                    return CheckboxListTile(
                      title: Text(data.indexSymbol, style: const TextStyle(color: Colors.white)),
                      value: isSelected,
                      activeColor: const Color(0xFF00D1FF),
                      checkColor: Colors.black,
                      onChanged: (bool? value) {
                        setDialogState(() {
                          if (value == true) {
                            if (selectedIndicesForChart.length < 5) {
                              selectedIndicesForChart.add(data.indexSymbol);
                            }
                          } else {
                            if (selectedIndicesForChart.length > 1) {
                              selectedIndicesForChart.remove(data.indexSymbol);
                            }
                          }
                        });
                      },
                    );
                  },
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () {
                    Navigator.pop(context);
                    // Start reload
                    _loadHistoricalData();
                    // Parent setState to reflect changes if needed
                    this.setState(() {}); 
                  },
                  child: const Text('Done', style: TextStyle(color: Color(0xFF00D1FF))),
                ),
              ],
            );
          },
        );
      },
    );
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

                // --- GLOBAL FILTER BAR ---
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    Text(
                      'Timeframe: ',
                      style: TextStyle(
                        color: isDark ? Colors.white70 : Colors.black54,
                        fontSize: 12, 
                        fontWeight: FontWeight.bold
                      ),
                    ),
                    const SizedBox(width: 8),
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
                              _loadTopMovers();
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
                
                const SizedBox(height: 24),

                // --- INDICES COMPARISON SECTION (Moved Up) ---
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
                        // Chart Controls
                        Row(
                          children: [
                            // Toggle Chart Type
                            Container(
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.05),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Row(
                                children: [
                                  IconButton(
                                    icon: Icon(Icons.show_chart, 
                                        color: !isBarChart ? const Color(0xFF00D1FF) : Colors.white54,
                                        size: 20),
                                    onPressed: () => setState(() => isBarChart = false),
                                    tooltip: 'Line Chart',
                                  ),
                                  IconButton(
                                    icon: Icon(Icons.bar_chart, 
                                        color: isBarChart ? const Color(0xFF00D1FF) : Colors.white54,
                                        size: 20),
                                    onPressed: () => setState(() => isBarChart = true),
                                    tooltip: 'Bar Chart',
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 8),
                            // Add Index Button
                            IconButton(
                              icon: const Icon(Icons.add_circle_outline, color: Color(0xFF00D1FF)),
                              onPressed: () => _showAddIndexDialog(provider),
                              tooltip: 'Add Index',
                            ),
                          ],
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
                        isBarChart: isBarChart,
                      ),
                    ),
                  ],
                ),

                const SizedBox(height: 40),
                
                // --- TOP MOVERS SECTION (Moved Down) ---
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
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
                        // Move info hint
                        Text(
                          'Based on $selectedTimeframe',
                          style: TextStyle(color: Colors.white.withOpacity(0.4), fontSize: 10),
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
                
              ],
            ),
          ),
        );
      },
    );
  }
}
