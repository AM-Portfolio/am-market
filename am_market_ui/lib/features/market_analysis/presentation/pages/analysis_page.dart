import 'package:flutter/material.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/heatmap_explorer_view.dart';
import 'package:am_market_ui/features/market_analysis/presentation/widgets/indices_performance_view_v2.dart';

class AnalysisPage extends StatelessWidget {
  const AnalysisPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SingleChildScrollView(
        child: Column(
          children: [
            const IndicesPerformanceViewV2(),
            const SizedBox(height: 20),
            const SizedBox(height: 600, child: HeatmapExplorerView()),
          ],
        ),
      ),
    );
  }
}
