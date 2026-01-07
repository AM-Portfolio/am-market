import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/market_provider.dart';
import '../widgets/heatmap_filters.dart';
import '../widgets/heatmap_grid.dart';

class HeatmapView extends StatefulWidget {
  const HeatmapView({super.key});

  @override
  State<HeatmapView> createState() => _HeatmapViewState();
}

class _HeatmapViewState extends State<HeatmapView> {
  String _timeFrame = '1D'; // Default to Day
  String? _percentFilter; // 'Above +5%', etc.

  final List<String> _timeFrames = ['5M', '10M', '15M', '30M', '1H', '1D'];
  final List<String> _filters = ['Above +5%', '+2 to +5%', '0 to +2%', '0 to -2%', '-2 to -5%', 'Below -5%'];

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<MarketProvider>();
    final data = provider.currentIndexData;

    if (data == null || data.stocks.isEmpty) {
      return const Center(child: Text('No data available'));
    }

    // Filter Logic
    List stocks = data.stocks;
    if (_percentFilter != null) {
      stocks = stocks.where((s) {
        final p = s.pChange;
        switch (_percentFilter) {
          case 'Above +5%': return p > 5;
          case '+2 to +5%': return p > 2 && p <= 5;
          case '0 to +2%': return p >= 0 && p <= 2;
          case '0 to -2%': return p < 0 && p >= -2;
          case '-2 to -5%': return p < -2 && p >= -5;
          case 'Below -5%': return p < -5;
          default: return true;
        }
      }).toList();
    }

    // Sort by pChange descending
    stocks.sort((a, b) => b.pChange.compareTo(a.pChange));

    return Column(
      children: [
        // Top Filter Bar
        HeatmapFilters(
          timeFrame: _timeFrame,
          onTimeFrameChanged: (val) => setState(() => _timeFrame = val!),
          percentFilter: _percentFilter,
          onPercentFilterChanged: (val) => setState(() => _percentFilter = val),
          timeFrames: _timeFrames,
          filters: _filters,
        ),

        // Grid
        Expanded(
          child: HeatmapGrid(
            stocks: stocks,
            provider: provider,
          )
        ),
      ],
    );
  }
}
