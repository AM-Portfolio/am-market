import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/core/theme/app_colors.dart';
import 'package:am_market_common/providers/market_provider.dart';
import 'package:am_market_ui/shared/widgets/glass_container.dart';
import 'package:intl/intl.dart';
import 'package:am_market_common/models/historical_performance_model.dart';
import 'package:am_market_common/models/seasonality_model.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/historical_performance_section.dart';

class HeatmapExplorerView extends StatefulWidget {
  const HeatmapExplorerView({super.key});

  @override
  State<HeatmapExplorerView> createState() => _HeatmapExplorerViewState();
}

class _HeatmapExplorerViewState extends State<HeatmapExplorerView> {
  String _selectedSymbol = 'NIFTY BANK'; // Default symbol
  final List<String> _months = [
    'JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE',
    'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'
  ];

  final List<String> _shortMonths = [
    'JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN',
    'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'
  ];
  
  late TextEditingController _searchController;

  // Heatmap State
  String _heatmapTimeframe = '1D';
  bool _showingIndices = true; // Use separate state for Heatmap section drill-down
  bool _isHeatmapExpanded = true; // Control visibility of the heatmap grid


  // _selectedSymbol is used for General Analysis (Seasonality/Historical)
  // For Heatmap, we use _showingIndices to determine if showing "List of Indices" or "Constituents of _selectedSymbol"
  // Wait, if _showingIndices is false, we show constituents of _selectedSymbol. 

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController(text: _selectedSymbol);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _fetchData();
    });
  }
  
  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _fetchData() {
    // 1. Fetch General Analysis Data
    if (_selectedSymbol.isNotEmpty && _selectedSymbol != "INDICES") {
        context.read<MarketProvider>().loadHistoricalPerformance(_selectedSymbol);
        context.read<MarketProvider>().loadSeasonality(_selectedSymbol);
    }
    
    // 2. Fetch Heatmap Data
    // Target: if showing indices -> "INDICES"
    // If showing constituents -> _selectedSymbol
    String heatmapTarget = _showingIndices ? 'INDICES' : _selectedSymbol;
    if (heatmapTarget.isEmpty && !_showingIndices) heatmapTarget = "NIFTY 50"; // Fallback
    
    context.read<MarketProvider>().loadHeatmap(heatmapTarget, _heatmapTimeframe);
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<MarketProvider>();
    final data = provider.historicalPerformance;

    return Column(
      children: [
        // 1. Header & Search
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.darkCard,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Colors.white.withOpacity(0.05)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Title REMOVED or Changed to Generic
              /*
              const Text(
                'Historical Performance',
                style: TextStyle(...),
              ),
              const SizedBox(height: 16),
              */
              
              // Search & Controls Row
              Row(
                children: [
                   // Expanded Search Field
                   Expanded(
                     child: Container(
                       height: 48,
                       decoration: BoxDecoration(
                         color: Colors.black.withOpacity(0.2),
                         borderRadius: BorderRadius.circular(12),
                         border: Border.all(color: Colors.white.withOpacity(0.1)),
                       ),
                       child: TextField(
                         controller: _searchController,
                         style: const TextStyle(color: Colors.white),
                         decoration: const InputDecoration(
                           hintText: 'Search Symbol (e.g. RELIANCE, NIFTY 50)',
                           hintStyle: TextStyle(color: Colors.white38),
                           prefixIcon: Icon(Icons.search, color: Colors.white54),
                           border: InputBorder.none,
                           contentPadding: EdgeInsets.symmetric(vertical: 14),
                         ),
                         onSubmitted: (value) {
                           if (value.isNotEmpty) {
                             setState(() {
                               _selectedSymbol = value.toUpperCase();
                             });
                             _fetchData();
                           }
                         },
                       ),
                     ),
                   ),
                   const SizedBox(width: 12),
                   
                   // Go Button
                   GestureDetector(
                     onTap: () {
                        if (_searchController.text.isNotEmpty) {
                           setState(() {
                             _selectedSymbol = _searchController.text.toUpperCase();
                           });
                           _fetchData();
                        }
                     },
                     child: Container(
                       height: 48,
                       padding: const EdgeInsets.symmetric(horizontal: 24),
                       decoration: BoxDecoration(
                         gradient: const LinearGradient(
                           colors: [Color(0xFF00D1FF), Color(0xFF0055FF)],
                           begin: Alignment.topLeft,
                           end: Alignment.bottomRight,
                         ),
                         borderRadius: BorderRadius.circular(12),
                         boxShadow: [
                           BoxShadow(
                             color: const Color(0xFF0055FF).withOpacity(0.3),
                             blurRadius: 8,
                             offset: const Offset(0, 4),
                           )
                         ],
                       ),
                       child: Center(
                         child: provider.isLoading 
                             ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                             : const Text(
                                 'GO',
                                 style: TextStyle(
                                   color: Colors.white,
                                   fontWeight: FontWeight.bold,
                                   fontSize: 16,
                                 ),
                               ),
                       ),
                     ),
                   ),
                ],
              ),
              const SizedBox(height: 12),
              
                  // Quick Suggestions
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    child: Row(
                      children: [
                        _buildQuickActionChip(provider, "NIFTY BANK", "NIFTY BANK"),
                        _buildQuickActionChip(provider, "NIFTY IT", "NIFTY IT"),
                        _buildQuickActionChip(provider, "MIDCAP", "NIFTY MIDCAP 50"),
                        _buildQuickActionChip(provider, "INDIA VIX", "INDIA VIX"),
                        _buildQuickActionChip(provider, "NIFTY 50", "NIFTY 50"),
                        _buildQuickActionChip(provider, "SMALL CAP", "NIFTY SMALLCAP 50"),
                      ],
                    ),
                  )
            ],
          ),
        ),

        const SizedBox(height: 16),

        // Main Content Area
        Expanded(
          child: _showingIndices
            ? SingleChildScrollView(
                child: Column(
                  children: [
                    _buildHeatmapSection(),
                    const SizedBox(height: 16),
                    Container(
                      decoration: BoxDecoration(
                        color: AppColors.darkCard,
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(color: Colors.white.withOpacity(0.05)),
                      ),
                      child: const HistoricalPerformanceSection(),
                    ),
                    const SizedBox(height: 16),
                  ],
                ),
              )
            : SingleChildScrollView(
                child: Column(
                  children: [
                    _buildHeatmapSection(),
                    const SizedBox(height: 16),
                    data == null 
                        ? SizedBox(
                            height: 200,
                            child: Center(child: Text(provider.isLoading ? 'Loading...' : 'No data available', style: const TextStyle(color: Colors.white54)))
                          )
                        : Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                                Container(
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(
                                  color: AppColors.darkCard,
                                  borderRadius: BorderRadius.circular(16),
                                  border: Border.all(color: Colors.white.withOpacity(0.05)),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                     // Overall Return Header (Optional)
                                     if (data.overallReturn != null)
                                       Padding(
                                         padding: const EdgeInsets.only(bottom: 16.0),
                                         child: Text(
                                           "Overall Return (${data.startYear}-${data.endYear}): ${data.overallReturn}%",
                                           style: TextStyle(
                                               color: _getColorForChange(data.overallReturn!),
                                               fontWeight: FontWeight.bold,
                                               fontSize: 16
                                           ),
                                         ),
                                       ),
          
                                     // Responsive Table with Horizontal Scroll
                                     LayoutBuilder(
                                       builder: (context, constraints) {
                                         // On Desktop, use full available width.
                                         // On Mobile, force a minimum width (e.g., 900) to prevent squashing, enabling scrolling.
                                         const double minTableWidth = 900.0;
                                         final double effectiveWidth = constraints.maxWidth < minTableWidth 
                                             ? minTableWidth 
                                             : constraints.maxWidth;

                                         return SingleChildScrollView(
                                           scrollDirection: Axis.horizontal,
                                           child: SizedBox(
                                             width: effectiveWidth,
                                             child: Column(
                                               children: [
                                                  // Header Row
                                                  Row(
                                                    children: [
                                                      const SizedBox(width: 60), // Year column width
                                                      ..._shortMonths.map((m) => Expanded(
                                                        child: Center(
                                                          child: Text(
                                                            m,
                                                            style: const TextStyle(color: Colors.white54, fontSize: 11, fontWeight: FontWeight.bold),
                                                          ),
                                                        ),
                                                      )),
                                                      const SizedBox(width: 60), // Yearly Total width
                                                    ],
                                                  ),
                                                  const SizedBox(height: 8),
                                                  Divider(color: Colors.white.withOpacity(0.1), height: 1),
                                                  const SizedBox(height: 8),

                                                  // Data Rows
                                                  ...data.yearlyPerformance.map((yearly) {
                                                      return Padding(
                                                        padding: const EdgeInsets.symmetric(vertical: 6.0),
                                                        child: Row(
                                                          children: [
                                                            // Year Label
                                                            SizedBox(
                                                              width: 60,
                                                              child: Text(
                                                                '${yearly.year}',
                                                                style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                                                              ),
                                                            ),
                                                            
                                                            // Monthly Cells
                                                            ..._months.map((monthKey) {
                                                                final val = yearly.monthlyReturns[monthKey];
                                                                return Expanded(
                                                                  child: Container(
                                                                    margin: const EdgeInsets.symmetric(horizontal: 2),
                                                                    height: 32,
                                                                    decoration: BoxDecoration(
                                                                      color: val != null ? _getColorForChange(val).withOpacity(0.8) : Colors.white.withOpacity(0.05),
                                                                      borderRadius: BorderRadius.circular(6),
                                                                    ),
                                                                    child: Center(
                                                                      child: Text(
                                                                        val != null ? val.toStringAsFixed(1) : '-',
                                                                        style: TextStyle(
                                                                          color: Colors.white, 
                                                                          fontSize: 11,
                                                                          fontWeight: val != null ? FontWeight.w600 : FontWeight.normal
                                                                        ),
                                                                      ),
                                                                    ),
                                                                  ),
                                                                );
                                                            }).toList(),

                                                            // Yearly Total
                                                            SizedBox(
                                                              width: 60,
                                                              child: Center(
                                                                child: Text(
                                                                  yearly.yearlyReturn != null ? '${yearly.yearlyReturn}%' : '-',
                                                                    style: TextStyle(
                                                                      color: _getColorForChange(yearly.yearlyReturn ?? 0),
                                                                      fontSize: 12,
                                                                      fontWeight: FontWeight.bold
                                                                    ),
                                                                    textAlign: TextAlign.end,
                                                                ),
                                                              ),
                                                            )
                                                          ],
                                                        ),
                                                      );
                                                  }).toList(),
                                               ],
                                             ),
                                           ),
                                         );
                                       }
                                     ),
                                  ],
                                ),
                            ),
                            const SizedBox(height: 16),
                            if (provider.seasonality != null) _buildSeasonality(provider.seasonality!),
                            const SizedBox(height: 16), // Bottom padding
                           ],
                          ),
                  ],
                ),
              ),
        ),
      ],
    );
     
  }


  Widget _buildSeasonality(SeasonalityResponse seasonality) {
    // Filter out weekends
    final dayOfWeekData = seasonality.dayOfWeekReturns.entries
        .where((e) => e.key != 'SATURDAY' && e.key != 'SUNDAY')
        .toList();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.darkCard,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.05)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text(
                'Seasonality Analysis',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                ),
              ),
              const SizedBox(width: 8),
              Tooltip(
                message: 'Average percentage return based on historical data.',
                triggerMode: TooltipTriggerMode.tap,
                child: Icon(Icons.info_outline, color: Colors.white.withOpacity(0.5), size: 14),
              ),
            ],
          ),
          const SizedBox(height: 12),
          LayoutBuilder(
            builder: (context, constraints) {
              final isMobile = constraints.maxWidth < 600;
              if (isMobile) {
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Day of Week Analysis
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Day of Week', style: TextStyle(color: Colors.white54, fontSize: 11, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 8),
                        ...dayOfWeekData.map((e) {
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 6.0),
                            child: Row(
                              children: [
                                SizedBox(width: 80, child: Text(e.key, style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w500))),
                                Expanded(
                                  child: Stack(
                                    children: [
                                      Container(height: 4, decoration: BoxDecoration(color: Colors.white.withOpacity(0.05), borderRadius: BorderRadius.circular(2))),
                                      FractionallySizedBox(
                                        widthFactor: (e.value.abs() / 1.0).clamp(0.0, 1.0), // Normalize
                                        child: Container(
                                          height: 4, 
                                          decoration: BoxDecoration(
                                            color: _getColorForChange(e.value),
                                            borderRadius: BorderRadius.circular(2),
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                SizedBox(width: 45, child: Text('${e.value.toStringAsFixed(2)}%', textAlign: TextAlign.end, style: TextStyle(color: _getColorForChange(e.value), fontSize: 11, fontWeight: FontWeight.bold))),
                              ],
                            ),
                          );
                        }).toList(),
                      ],
                    ),
                    const SizedBox(height: 24),
                    // Monthly Analysis
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('Monthly', style: TextStyle(color: Colors.white54, fontSize: 11, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 8),
                         ...seasonality.monthlyReturns.entries.map((e) {
                          return Padding(
                            padding: const EdgeInsets.only(bottom: 6.0),
                            child: Row(
                              children: [
                                SizedBox(width: 80, child: Text(e.key, style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w500))),
                                Expanded(
                                  child: Stack(
                                    children: [
                                      Container(height: 4, decoration: BoxDecoration(color: Colors.white.withOpacity(0.05), borderRadius: BorderRadius.circular(2))),
                                      FractionallySizedBox(
                                        widthFactor: (e.value.abs() / 5.0).clamp(0.0, 1.0), // Normalize
                                        child: Container(
                                          height: 4, 
                                          decoration: BoxDecoration(
                                            color: _getColorForChange(e.value),
                                            borderRadius: BorderRadius.circular(2),
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                SizedBox(width: 45, child: Text('${e.value.toStringAsFixed(2)}%', textAlign: TextAlign.end, style: TextStyle(color: _getColorForChange(e.value), fontSize: 11, fontWeight: FontWeight.bold))),
                              ],
                            ),
                          );
                        }).toList(),
                      ],
                    ),
                  ],
                );
              } else {
                return Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Day of Week Analysis
                    Expanded(
                      flex: 2,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Day of Week', style: TextStyle(color: Colors.white54, fontSize: 11, fontWeight: FontWeight.w600)),
                          const SizedBox(height: 8),
                          ...dayOfWeekData.map((e) {
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 6.0),
                              child: Row(
                                children: [
                                  SizedBox(width: 80, child: Text(e.key, style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w500))),
                                  Expanded(
                                    child: Stack(
                                      children: [
                                        Container(height: 4, decoration: BoxDecoration(color: Colors.white.withOpacity(0.05), borderRadius: BorderRadius.circular(2))),
                                        FractionallySizedBox(
                                          widthFactor: (e.value.abs() / 1.0).clamp(0.0, 1.0), // Normalize
                                          child: Container(
                                            height: 4, 
                                            decoration: BoxDecoration(
                                              color: _getColorForChange(e.value),
                                              borderRadius: BorderRadius.circular(2),
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  SizedBox(width: 45, child: Text('${e.value.toStringAsFixed(2)}%', textAlign: TextAlign.end, style: TextStyle(color: _getColorForChange(e.value), fontSize: 11, fontWeight: FontWeight.bold))),
                                ],
                              ),
                            );
                          }).toList(),
                        ],
                      ),
                    ),
                    const SizedBox(width: 24),
                    // Monthly Analysis
                    Expanded(
                      flex: 3,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text('Monthly', style: TextStyle(color: Colors.white54, fontSize: 11, fontWeight: FontWeight.w600)),
                          const SizedBox(height: 8),
                           ...seasonality.monthlyReturns.entries.map((e) {
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 6.0),
                              child: Row(
                                children: [
                                  SizedBox(width: 80, child: Text(e.key, style: const TextStyle(color: Colors.white70, fontSize: 11, fontWeight: FontWeight.w500))),
                                  Expanded(
                                    child: Stack(
                                      children: [
                                        Container(height: 4, decoration: BoxDecoration(color: Colors.white.withOpacity(0.05), borderRadius: BorderRadius.circular(2))),
                                        FractionallySizedBox(
                                          widthFactor: (e.value.abs() / 5.0).clamp(0.0, 1.0), // Normalize
                                          child: Container(
                                            height: 4, 
                                            decoration: BoxDecoration(
                                              color: _getColorForChange(e.value),
                                              borderRadius: BorderRadius.circular(2),
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  SizedBox(width: 45, child: Text('${e.value.toStringAsFixed(2)}%', textAlign: TextAlign.end, style: TextStyle(color: _getColorForChange(e.value), fontSize: 11, fontWeight: FontWeight.bold))),
                                ],
                              ),
                            );
                          }).toList(),
                        ],
                      ),
                    ),
                  ],
                );
              }
            }
          ),
        ],
      ),
    );
  }

  Color _getColorForChange(double pChange) {
      if (pChange >= 10) return const Color(0xFF00C853);
      if (pChange >= 5) return const Color(0xFF00E676);
      if (pChange > 0) return const Color(0xFF69F0AE);
      if (pChange == 0) return Colors.grey.withOpacity(0.5);
      if (pChange > -5) return const Color(0xFFFF8A80);
      if (pChange > -10) return const Color(0xFFFF5252);
      return const Color(0xFFD50000); 
  }

  // --- New Market Heatmap Section ---

  Widget _buildHeatmapSection() {
      // Logic to determine title
      String title = _showingIndices ? "Market Heatmap (Indices)" : "Heatmap: $_selectedSymbol";

      return Container(
          decoration: BoxDecoration(
            color: AppColors.darkCard,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: Colors.white.withOpacity(0.05)),
          ),
          padding: const EdgeInsets.all(16),
          child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                  Wrap(
                      alignment: WrapAlignment.spaceBetween,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      spacing: 8.0,
                      runSpacing: 8.0,
                      children: [
                          Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                  if (!_showingIndices)
                                    IconButton(
                                        icon: const Icon(Icons.arrow_back, color: Colors.white, size: 20),
                                        padding: EdgeInsets.zero,
                                        constraints: const BoxConstraints(),
                                        onPressed: _onBackToIndices,
                                    ),
                                  if (!_showingIndices) const SizedBox(width: 8),
                                  Flexible(
                                    child: Text(
                                      title,
                                      style: const TextStyle(
                                          color: Colors.white,
                                          fontSize: 18,
                                          fontWeight: FontWeight.bold,
                                          letterSpacing: 0.5
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  IconButton(
                                      icon: Icon(_isHeatmapExpanded ? Icons.expand_less : Icons.expand_more, color: Colors.white70),
                                      onPressed: () => setState(() => _isHeatmapExpanded = !_isHeatmapExpanded),
                                      tooltip: _isHeatmapExpanded ? "Minimize Section" : "Expand Section",
                                  ),
                              ],
                          ),
                          // Timeframe Selector
                          if (_isHeatmapExpanded)
                          SingleChildScrollView(
                              scrollDirection: Axis.horizontal,
                              child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: ['1D', '1W', '1M', '3M', '6M', '1Y', '5Y'].map((tf) {
                                      final isSelected = _heatmapTimeframe == tf;
                                      return Padding(
                                          padding: const EdgeInsets.only(left: 8),
                                          child: InkWell(
                                              onTap: () => _onHeatmapTimeframeChanged(tf),
                                              child: Container(
                                                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                                                  decoration: BoxDecoration(
                                                      color: isSelected ? const Color(0xFF0055FF) : Colors.black.withOpacity(0.3),
                                                      borderRadius: BorderRadius.circular(12),
                                                      border: Border.all(color: isSelected ? const Color(0xFF00D1FF) : Colors.white10),
                                                  ),
                                                  child: Text(
                                                      tf,
                                                      style: TextStyle(
                                                          color: isSelected ? Colors.white : Colors.white54,
                                                          fontSize: 12,
                                                          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                                                      ),
                                                  ),
                                              ),
                                          ),
                                      );
                                  }).toList(),
                              ),
                          ),
                      ],
                  ),
                  if (_isHeatmapExpanded) ...[
                      const SizedBox(height: 20),
                      _buildHeatmapGrid(),
                  ]
              ],
          ),
      );
  }

  void _onBackToIndices() {
      setState(() {
          _showingIndices = true;
          _selectedSymbol = ""; 
          _searchController.clear();
          _isHeatmapExpanded = true; // Reset expansion when going back
      });
      _fetchData();
  }

  void _onHeatmapTimeframeChanged(String tf) {
      setState(() {
          _heatmapTimeframe = tf;
      });
      _fetchData();
  }

  void _onHeatmapItemTap(String symbol, double value) {
      if (_showingIndices) {
          // Drill down
          setState(() {
              _showingIndices = false;
              _selectedSymbol = symbol;
              _isHeatmapExpanded = true; // Keep expanded to show constituents
          });
          _fetchData();
      } else {
          // Select stock but don't drill further (unless we have stock details)
          // Just update generalized view
          setState(() {
              _selectedSymbol = symbol;
              _isHeatmapExpanded = false; // Minimize heatmap to show details below
          });
          // Also fetch history/seasonality for this stock
          context.read<MarketProvider>().loadHistoricalPerformance(symbol);
          context.read<MarketProvider>().loadSeasonality(symbol);
      }
  }

  Widget _buildHeatmapGrid() {
      return Consumer<MarketProvider>(
          builder: (context, provider, child) {
              final data = provider.heatmapValues; // Map<String, double>
              if (provider.isLoading && (data == null || data.isEmpty)) {
                  return const Center(child: Padding(
                      padding: EdgeInsets.all(20),
                      child: CircularProgressIndicator(strokeWidth: 2),
                  ));
              }
              if (data == null || data.isEmpty) {
                  return const Center(child: Text("No heatmap data available", style: TextStyle(color: Colors.white38)));
              }
              
              return GridView.builder(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
                      maxCrossAxisExtent: 120, // Responsive width
                      childAspectRatio: 1.2,
                      crossAxisSpacing: 8,
                      mainAxisSpacing: 8,
                  ),
                  itemCount: data.length,
                  itemBuilder: (context, index) {
                      final symbol = data.keys.elementAt(index);
                      final value = data.values.elementAt(index);
                      return _buildHeatmapCard(symbol, value);
                  },
              );
          },
      );
  }

  Widget _buildHeatmapCard(String symbol, double value) {
      return InkWell(
          onTap: () => _onHeatmapItemTap(symbol, value),
          child: Container(
              decoration: BoxDecoration(
                  color: _getColorForChange(value).withOpacity(0.2),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: _getColorForChange(value).withOpacity(0.5)),
              ),
              child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                      Text(
                          symbol, 
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13), 
                          textAlign: TextAlign.center,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 4),
                      Text(
                          "${value > 0 ? '+' : ''}${value.toStringAsFixed(2)}%", 
                          style: TextStyle(color: _getColorForChange(value), fontSize: 12, fontWeight: FontWeight.bold)
                      ),
                  ],
              ),
          ),
      );
  }

  Widget _buildQuickActionChip(MarketProvider provider, String label, String symbol) {
    bool isSelected = _selectedSymbol == symbol;
    return GestureDetector(
      onTap: () {
        setState(() {
          _showingIndices = false; // Drill down into this index
          _selectedSymbol = symbol;
          _searchController.text = symbol;
        });
        _fetchData();
      },
      child: Container(
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isSelected ? Colors.white.withOpacity(0.2) : Colors.black.withOpacity(0.2),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: isSelected ? Colors.white54 : Colors.transparent),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: Colors.white.withOpacity(isSelected ? 1 : 0.7),
            fontSize: 12,
          ),
        ),
      ),
    );
  }

}
