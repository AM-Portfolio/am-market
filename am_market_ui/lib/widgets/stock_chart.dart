import 'package:flutter/material.dart';
import 'package:am_design_system/am_design_system.dart';
import 'package:intl/intl.dart';

class StockChart extends StatelessWidget {
  final List<Map<String, dynamic>> chartData;
  final bool isLoading;
  final String? error;

  const StockChart({
    super.key,
    required this.chartData,
    this.isLoading = false,
    this.error,
  });

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (error != null) {
      return Center(child: Text('Error: $error', style: TextStyle(color: AppColors.error)));
    }
    if (chartData.isEmpty) {
      return Center(child: Text('No data available', style: TextStyle(color: Theme.of(context).hintColor)));
    }

    final dataPoints = _mapDataToPoints();

    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: ChartFactory.line(
        data: dataPoints,
        height: 300,
        config: const CommonChartConfig(
          showGrid: true,
          showTitles: true,
          showTooltips: true,
          showLegend: false,
        ),
      ),
    );
  }

  List<CommonChartDataPoint> _mapDataToPoints() {
    return List.generate(chartData.length, (index) {
      final item = chartData[index];
      final closePrice = (item['close'] as num).toDouble();
      final dateStr = item['time'] as String;
      
      String? label;
      try {
        final date = DateTime.parse(dateStr);
        // Show label only for specific intervals to avoid crowding
        if (index % (chartData.length / 5).ceil() == 0) {
           label = DateFormat('dd MMM').format(date);
        }
      } catch (_) {}

      return CommonChartDataPoint(
        x: index.toDouble(),
        y: closePrice,
        xLabel: label,
        yLabel: closePrice.toStringAsFixed(2),
        meta: dateStr,
      );
    });
  }
}
