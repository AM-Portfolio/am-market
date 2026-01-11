import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:am_design_system/core/theme/app_colors.dart';
import 'package:am_market_ui/providers/market_provider.dart';
import 'package:am_market_ui/widgets/glass_container.dart';
import 'package:intl/intl.dart';
import '../../../../models/historical_performance_model.dart';
import '../../../../models/seasonality_model.dart';

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
    context.read<MarketProvider>().loadHistoricalPerformance(_selectedSymbol);
    context.read<MarketProvider>().loadSeasonality(_selectedSymbol);
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
              // Title
              const Text(
                'Historical Performance',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.white,
                  letterSpacing: 0.5,
                ),
              ),
              const SizedBox(height: 16),
              
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
                  children: (provider.availableIndices?.broad ?? ['NIFTY 50', 'NIFTY BANK', 'NIFTY IT']).take(5).map((idx) {
                    final isSelected = idx == _selectedSymbol;
                    return GestureDetector(
                      onTap: () {
                         setState(() {
                           _selectedSymbol = idx;
                           _searchController.text = idx;
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
                          idx,
                          style: TextStyle(
                            color: Colors.white.withOpacity(isSelected ? 1 : 0.7),
                            fontSize: 12,
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              )
            ],
          ),
        ),

        const SizedBox(height: 16),

        // 2. Heatmap Grid
        Expanded(
          child: data == null 
              ? Center(child: Text(provider.isLoading ? 'Loading...' : 'No data available', style: const TextStyle(color: Colors.white54)))
              : SingleChildScrollView(
                  scrollDirection: Axis.vertical,
                  child: Column(
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

                           // Fixed Header Row (Months)
                           Row(
                             children: [
                               const SizedBox(width: 60), // Year column width
                               ..._shortMonths.map((m) => Expanded(
                                 child: Center(
                                   child: Text(
                                     m,
                                     style: const TextStyle(color: Colors.white54, fontSize: 10, fontWeight: FontWeight.bold),
                                   ),
                                 ),
                               )),
                               const SizedBox(width: 50), // Yearly Total width
                             ],
                           ),
                           const SizedBox(height: 8),
                           Divider(color: Colors.white.withOpacity(0.1), height: 1),
                           const SizedBox(height: 8),

                           // Year Rows
                           ...data.yearlyPerformance.map((yearly) {
                              return Padding(
                                padding: const EdgeInsets.symmetric(vertical: 4.0),
                                child: Row(
                                  children: [
                                    // Year Label
                                    SizedBox(
                                      width: 60,
                                      child: Text(
                                        '${yearly.year}',
                                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                                      ),
                                    ),
                                    
                                    // Monthly Cells
                                    ..._months.map((monthKey) {
                                       final val = yearly.monthlyReturns[monthKey];
                                       return Expanded(
                                         child: Container(
                                           margin: const EdgeInsets.symmetric(horizontal: 2),
                                           height: 30,
                                           decoration: BoxDecoration(
                                             color: val != null ? _getColorForChange(val).withOpacity(0.8) : Colors.white.withOpacity(0.05),
                                             borderRadius: BorderRadius.circular(4),
                                           ),
                                           child: Center(
                                             child: Text(
                                               val != null ? val.toStringAsFixed(1) : '-',
                                               style: TextStyle(
                                                 color: Colors.white, 
                                                 fontSize: 10,
                                                 fontWeight: val != null ? FontWeight.w500 : FontWeight.normal
                                               ),
                                             ),
                                           ),
                                         ),
                                       );
                                    }).toList(),

                                    // Yearly Total
                                    SizedBox(
                                      width: 50,
                                      child: Center(
                                        child: Text(
                                          yearly.yearlyReturn != null ? '${yearly.yearlyReturn}%' : '-',
                                           style: TextStyle(
                                             color: _getColorForChange(yearly.yearlyReturn ?? 0),
                                             fontSize: 11,
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
                  const SizedBox(height: 16),
                  if (provider.seasonality != null) _buildSeasonality(provider.seasonality!),
                  const SizedBox(height: 16), // Bottom padding
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
          Row(
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
          ),
        ],
      ),
    );
  }

  Color _getColorForChange(double pChange) {
      if (pChange >= 10) return const Color(0xFF00C853); // Deep Green
      if (pChange >= 5) return const Color(0xFF00E676);
      if (pChange > 0) return const Color(0xFF69F0AE); // Light Green
      if (pChange == 0) return Colors.grey.withOpacity(0.5);
      if (pChange > -5) return const Color(0xFFFF8A80); // Light Red
      if (pChange > -10) return const Color(0xFFFF5252);
      return const Color(0xFFD50000); // Deep Red
  }
}
