import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:am_market_ui/core/styles/am_text_styles.dart';
import 'package:am_market_ui/models/indices_performance_model.dart';
import 'package:am_market_ui/services/market_analysis_service.dart';
import 'package:am_market_ui/features/market/presentation/widgets/monthly_performance_card.dart';
import 'package:get_it/get_it.dart';

class HistoricalPerformanceSection extends StatefulWidget {
  const HistoricalPerformanceSection({Key? key}) : super(key: key);

  @override
  State<HistoricalPerformanceSection> createState() => _HistoricalPerformanceSectionState();
}

class _HistoricalPerformanceSectionState extends State<HistoricalPerformanceSection> {
  final MarketAnalysisService _service = GetIt.I<MarketAnalysisService>();
  late Future<IndicesHistoricalPerformanceResponse> _future;

  @override
  void initState() {
    super.initState();
    _future = _service.getIndicesHistoricalPerformance(years: 10);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
          child: Text(
            'Historical Monthly Performance (10 Years)',
            style: AmTextStyles.h6.copyWith(color: AppColors.textPrimaryDark),
          ),
        ),
          FutureBuilder<IndicesHistoricalPerformanceResponse>(
            future: _future,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snapshot.hasError) {
                return Center(child: Text('Error: ${snapshot.error}'));
              }
              if (!snapshot.hasData || snapshot.data!.monthlyPerformance.isEmpty) {
                return const Center(child: Text('No data available'));
              }

              final data = snapshot.data!.monthlyPerformance;

              return GridView.builder(
                padding: const EdgeInsets.all(16),
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 6, // Adjust based on screen width usually, but 6 is fine for wide screens
                  childAspectRatio: 1.2,
                  crossAxisSpacing: 12,
                  mainAxisSpacing: 12,
                ),
                itemCount: data.length,
                itemBuilder: (context, index) {
                  return MonthlyPerformanceCard(data: data[index]);
                },
              );
            },
          ),
      ],
    );
  }
}
