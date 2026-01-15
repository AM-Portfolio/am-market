import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_ui/core/styles/am_text_styles.dart';
import 'package:am_market_common/models/indices_performance_model.dart';
import 'package:am_market_ui/features/market_analysis/services/market_analysis_service.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/monthly_performance_card.dart';
import 'package:get_it/get_it.dart';

class HistoricalPerformanceSection extends StatefulWidget {
  const HistoricalPerformanceSection({Key? key}) : super(key: key);

  @override
  State<HistoricalPerformanceSection> createState() => _HistoricalPerformanceSectionState();
}

class _HistoricalPerformanceSectionState extends State<HistoricalPerformanceSection> {
  final MarketAnalysisService _service = GetIt.I<MarketAnalysisService>();
  late Future<IndicesHistoricalPerformanceResponse> _future;

  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _future = _service.getIndicesHistoricalPerformance(years: 10);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _scroll(double offset) {
    _scrollController.animateTo(
      (_scrollController.offset + offset).clamp(0.0, _scrollController.position.maxScrollExtent),
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
    );
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isMobile = constraints.maxWidth < 800;
        
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                   Expanded(
                    child: Text(
                      'Historical Monthly Performance (10 Years)',
                      style: AmTextStyles.h6.copyWith(
                        color: AppColors.textPrimaryDark,
                        fontSize: isMobile ? 16 : 18,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  // Hint text (Hide on very small screens)
                  if (!isMobile)
                  const Text(
                    'Scroll to view more months  ➡',
                    style: TextStyle(color: Colors.white24, fontSize: 12),
                  ),
                ],
              ),
            ),
              FutureBuilder<IndicesHistoricalPerformanceResponse>(
                future: _future,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: Padding(padding: EdgeInsets.all(20), child: CircularProgressIndicator()));
                  }
                  if (snapshot.hasError) {
                    return Center(child: Padding(padding: EdgeInsets.all(20), child: Text('Error: ${snapshot.error}', style: const TextStyle(color: Colors.redAccent))));
                  }
                  if (!snapshot.hasData || snapshot.data!.monthlyPerformance.isEmpty) {
                    return const Center(child: Padding(padding: EdgeInsets.all(20), child: Text('No data available', style: TextStyle(color: Colors.white54))));
                  }

                  final data = snapshot.data!.monthlyPerformance;
                  
                  // Group data by Year
                  final Map<int, Map<String, MonthlyIndicesPerformance>> groupedData = {};
                  for (var item in data) {
                    if (!groupedData.containsKey(item.year)) {
                      groupedData[item.year] = {};
                    }
                    groupedData[item.year]![item.monthName.toUpperCase()] = item;
                  }
                  
                  // Sort years descending
                  final sortedYears = groupedData.keys.toList()..sort((a, b) => b.compareTo(a));
                  
                  final months = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER'];
                  final shortMonths = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

                  // Dimensions
                  final double yearColWidth = isMobile ? 40.0 : 60.0;
                  final double cellWidth = isMobile ? 140.0 : 180.0; // Slightly smaller on mobile
                  final double totalWidth = yearColWidth + (cellWidth * 12);

                  return Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Left Scroll Button (Hide on Mobile)
                        if (!isMobile)
                        Padding(
                          padding: const EdgeInsets.only(top: 40.0), 
                          child: IconButton(
                              icon: const Icon(Icons.arrow_back_ios, color: Colors.white54),
                              onPressed: () => _scroll(-300),
                          ),
                        ),
                        Expanded(
                          child: Scrollbar(
                            controller: _scrollController,
                            thumbVisibility: true,
                            trackVisibility: true,
                            child: SingleChildScrollView(
                              controller: _scrollController,
                              scrollDirection: Axis.horizontal,
                              child: SizedBox(
                                width: totalWidth,
                                child: Column(
                                  children: [
                                    // Header Row
                                    Row(
                                      children: [
                                        SizedBox(width: yearColWidth), 
                                        ...shortMonths.map((m) => SizedBox(
                                          width: cellWidth,
                                          child: Center(
                                            child: Text(
                                              m,
                                              style: TextStyle(color: Colors.white54, fontWeight: FontWeight.bold, fontSize: isMobile ? 10 : 12),
                                            ),
                                          ),
                                        )),
                                      ],
                                    ),
                                    const SizedBox(height: 8),
                                    
                                    // Year Rows
                                    ...sortedYears.map((year) {
                                      return Padding(
                                        padding: const EdgeInsets.only(bottom: 8.0),
                                        child: IntrinsicHeight(
                                          child: Row(
                                            crossAxisAlignment: CrossAxisAlignment.stretch,
                                            children: [
                                              // Year Label
                                              SizedBox(
                                                width: yearColWidth,
                                                child: Center(
                                                  child: Text(
                                                    '$year',
                                                    style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: isMobile ? 12 : 16),
                                                  ),
                                                ),
                                              ),
                                              // Month Cells
                                              ...months.map((month) {
                                                final item = groupedData[year]?[month];
                                                return SizedBox(
                                                  width: cellWidth,
                                                  child: Padding(
                                                    padding: const EdgeInsets.symmetric(horizontal: 4.0),
                                                    child: item != null 
                                                      ? MonthlyPerformanceCard(data: item, isCompactTable: true)
                                                      : Container(decoration: BoxDecoration(color: Colors.white.withOpacity(0.02), borderRadius: BorderRadius.circular(8))),
                                                  ),
                                                );
                                              }),
                                            ],
                                          ),
                                        ),
                                      );
                                    }).toList(),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                        // Right Scroll Button (Hide on Mobile)
                        if (!isMobile)
                        Padding(
                          padding: const EdgeInsets.only(top: 40.0),
                          child: IconButton(
                              icon: const Icon(Icons.arrow_forward_ios, color: Colors.white54),
                              onPressed: () => _scroll(300),
                          ),
                        ),
                      ],
                    );
                },
              ),
          ],
        );
      }
    );
  }
}
